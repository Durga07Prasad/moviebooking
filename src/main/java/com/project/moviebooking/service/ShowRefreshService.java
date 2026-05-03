package com.project.moviebooking.service;

import com.project.moviebooking.model.Movie;
import com.project.moviebooking.model.Seat;
import com.project.moviebooking.model.Show;
import com.project.moviebooking.model.Theatre;
import com.project.moviebooking.repository.MovieRepository;
import com.project.moviebooking.repository.SeatRepository;
import com.project.moviebooking.repository.ShowRepository;
import com.project.moviebooking.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ShowRefreshService — Production-grade IST-aware Show Generator
 * ==============================================================
 * PATTERN: Strategy-inspired scheduling  (SOLID: O — new timing = new entry)
 * SOLID:   S — single responsibility: only show refresh logic lives here
 * GRASP:   Information Expert — owns all show-generation knowledge
 *
 * ALGORITHM:
 *   On startup  → ensureShowsExist() checks today + tomorrow (IST)
 *   At midnight → @Scheduled fires ensureShowsExist() for the next day
 *
 * IDEMPOTENCY GUARANTEE:
 *   If shows already exist for a date → skip that date entirely.
 *   If NOT → delete any stale unbooked shows for that date, then seed fresh.
 * ==============================================================
 */
@Service
@RequiredArgsConstructor
public class ShowRefreshService {

    /* ── IST Zone constant (UTC+5:30) ─────────────────────────── */
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /* ── Show timings: Morning / Afternoon / Evening — 3 slots/day ─ */
    private record Timing(String time, double price, String label) {}
    private static final List<Timing> TIMINGS = List.of(
        new Timing("10:00", 150.0, "Morning"),
        new Timing("14:00", 200.0, "Afternoon"),
        new Timing("18:00", 250.0, "Evening")
    );

    /* ── Seat layout: rows A-J, 15 columns = 150 seats/show ────── */
    private static final char[] ROWS    = {'A','B','C','D','E','F','G','H','I','J'};
    private static final int    COLUMNS = 15;

    private final ShowRepository    showRepository;
    private final SeatRepository    seatRepository;
    private final MovieRepository   movieRepository;
    private final TheatreRepository theatreRepository;

    // ═══════════════════════════════════════════════════════════════
    //  PUBLIC API — called from DataLoader on startup
    // ═══════════════════════════════════════════════════════════════

    /**
     * Entry point: ensures shows exist for TODAY and TOMORROW (IST).
     * Called on application startup AND by the midnight scheduler.
     *
     * Idempotent — safe to call multiple times.
     */
    public void ensureShowsExist() {
        LocalDate todayIST    = ZonedDateTime.now(IST).toLocalDate();
        LocalDate tomorrowIST = todayIST.plusDays(1);

        System.out.println("\n🕐 [SHOW-REFRESH] IST date today    : " + todayIST);
        System.out.println("🕐 [SHOW-REFRESH] IST date tomorrow : " + tomorrowIST);

        List<Movie>   movies   = movieRepository.findAll();
        List<Theatre> theatres = theatreRepository.findAll();

        if (movies.isEmpty() || theatres.isEmpty()) {
            System.out.println("⚠️  [SHOW-REFRESH] No movies or theatres found — skipping show refresh.");
            return;
        }

        int totalGenerated = 0;
        totalGenerated += refreshForDate(todayIST,    movies, theatres);
        totalGenerated += refreshForDate(tomorrowIST, movies, theatres);

        System.out.println("✅ [SHOW-REFRESH] Refresh complete. New shows generated: " + totalGenerated);
        System.out.printf("   Shows in DB: %d  |  Seats in DB: %d%n%n",
            showRepository.count(), seatRepository.count());
    }

    // ═══════════════════════════════════════════════════════════════
    //  SCHEDULER — Midnight IST trigger for next-day shows
    //  cron = "0 0 0 * * ?" → fires at 00:00:00 IST every day
    //  (Spring cron runs in JVM timezone; we use ZoneId in the logic)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Runs at midnight IST every day.
     * Generates shows for the day-after-tomorrow so the app always
     * has 2 days of shows ready without needing a restart.
     *
     * Cron: "second minute hour day month weekday"
     *       "0 30 18 * * ?"  = 18:30 UTC = midnight IST (UTC+5:30)
     */
    @Scheduled(cron = "0 30 18 * * ?", zone = "UTC")
    public void midnightRefresh() {
        System.out.println("\n⏰ [SHOW-REFRESH] Midnight IST scheduler triggered.");
        ensureShowsExist();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CORE LOGIC
    // ═══════════════════════════════════════════════════════════════

    /**
     * Checks if shows exist for the given date.
     * If YES → skip (idempotent).
     * If NO  → delete old unbooked shows for that date, then generate fresh.
     *
     * @return number of NEW shows inserted (0 if already existed)
     */
    private int refreshForDate(LocalDate date,
                               List<Movie> movies,
                               List<Theatre> theatres) {

        List<Show> existing = showRepository.findByShowDate(date);

        if (!existing.isEmpty()) {
            System.out.println("ℹ️  [SHOW-REFRESH] Shows already exist for " + date
                + " (" + existing.size() + " shows) — skipping.");
            return 0;
        }

        System.out.println("🔄 [SHOW-REFRESH] No shows found for " + date + " → generating...");

        // Delete any old stale (non-booked) shows for this date just in case
        deleteStaleShowsForDate(date);

        // Generate fresh shows + seats
        return generateShowsAndSeats(date, movies, theatres);
    }

    /**
     * Deletes shows for a date ONLY if they have no booked seats.
     * Preserves shows that have actual bookings (data integrity).
     */
    private void deleteStaleShowsForDate(LocalDate date) {
        List<Show> stale = showRepository.findByShowDate(date);
        List<String> showIdsToDelete = new ArrayList<>();

        for (Show s : stale) {
            if (s.getBookedSeats() == null || s.getBookedSeats().isEmpty()) {
                showIdsToDelete.add(s.getId());
            }
        }

        if (!showIdsToDelete.isEmpty()) {
            // Delete seats for these shows first (referential cleanup)
            for (String showId : showIdsToDelete) {
                List<Seat> seats = seatRepository.findByShowId(showId);
                seatRepository.deleteAll(seats);
            }
            // Delete the shows themselves
            showIdsToDelete.forEach(id -> showRepository.deleteById(id));
            System.out.println("🗑️  [SHOW-REFRESH] Removed " + showIdsToDelete.size()
                + " stale shows for " + date);
        }
    }

    /**
     * Core generator — creates shows for every movie × theatre × timing combo
     * for a given date, then creates 150 seats per show.
     *
     * Structure: movie → theatre → timing
     *   7 movies × 10 theatres × 3 timings = 210 shows per day
     *   210 shows × 150 seats              = 31,500 seats per day
     *
     * @return count of shows inserted
     */
    private int generateShowsAndSeats(LocalDate date,
                                      List<Movie> movies,
                                      List<Theatre> theatres) {

        List<Show> showsBatch = new ArrayList<>();

        // ── Every movie runs at every theatre at every timing ──────────
        for (Movie movie : movies) {
            for (Theatre theatre : theatres) {
                for (int timingIdx = 0; timingIdx < TIMINGS.size(); timingIdx++) {
                    Timing timing = TIMINGS.get(timingIdx);

                    Show show = new Show();
                    show.setMovieId(movie.getId());
                    show.setTheatreId(theatre.getId());
                    show.setShowDate(date);
                    show.setShowTime(LocalTime.parse(timing.time()));
                    show.setPrice(timing.price());
                    show.setScreen("Screen " + (timingIdx + 1));
                    show.setAvailableSeats(COLUMNS * ROWS.length);  // 150
                    show.setBookedSeats(new ArrayList<>());
                    show.setActive(true);

                    showsBatch.add(show);
                }
            }
        }

        List<Show> savedShows = showRepository.saveAll(showsBatch);
        System.out.println("✅ [SHOW-REFRESH] Inserted " + savedShows.size()
            + " shows for " + date
            + "  (" + movies.size() + " movies × "
            + theatres.size() + " theatres × "
            + TIMINGS.size() + " timings)");

        // ── Generate seats for every new show ──────────────────────
        List<Seat> seatsBatch = new ArrayList<>();

        for (Show show : savedShows) {
            double basePrice = show.getPrice();

            for (int r = 0; r < ROWS.length; r++) {
                char   rowChar = ROWS[r];
                String type    = (r < 2) ? "VIP" : (r < 5) ? "PREMIUM" : "REGULAR";
                double mult    = (r < 2) ? 2.0   : (r < 5) ? 1.5        : 1.0;

                for (int c = 1; c <= COLUMNS; c++) {
                    Seat seat = new Seat();
                    seat.setShowId(show.getId());
                    seat.setTheatreId(show.getTheatreId());
                    seat.setSeatNumber(rowChar + String.valueOf(c));
                    seat.setRow(String.valueOf(rowChar));
                    seat.setType(type);
                    seat.setPrice(basePrice * mult);
                    seat.setBooked(false);
                    seat.setBookedByUserId(null);
                    seatsBatch.add(seat);
                }
            }
        }

        // Chunk inserts (500 per batch) to avoid MongoDB 16MB doc limit
        int chunkSize = 500;
        for (int i = 0; i < seatsBatch.size(); i += chunkSize) {
            seatRepository.saveAll(
                seatsBatch.subList(i, Math.min(i + chunkSize, seatsBatch.size()))
            );
        }

        System.out.println("✅ [SHOW-REFRESH] Inserted " + seatsBatch.size()
            + " seats for " + date);

        return savedShows.size();
    }
}
