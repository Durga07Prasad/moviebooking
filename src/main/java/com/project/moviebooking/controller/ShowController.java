package com.project.moviebooking.controller;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.Seat;
import com.project.moviebooking.model.Show;
import com.project.moviebooking.model.Theatre;
import com.project.moviebooking.model.Movie;
import com.project.moviebooking.repository.*;
import com.project.moviebooking.service.ISTTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ShowController — IST-aware show browsing endpoints.
 *
 * OOAD: GRASP Controller — delegates to ISTTimeService and repositories.
 *       Contains zero business logic; it is pure HTTP orchestration.
 * SOLID: D — depends on ISTTimeService (injected), not on time utilities directly.
 * SOLID: S — only responsible for show-related HTTP endpoints.
 *
 * IST FILTERING RATIONALE:
 * The server may run UTC. Without IST conversion a 10 AM show in Bengaluru
 * would appear as 4:30 AM UTC — and backend logic comparing LocalTime.now()
 * (UTC) to showTime (IST) would produce wrong results. ISTTimeService solves this.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
public class ShowController {

    private final ShowRepository    showRepository;
    private final SeatRepository    seatRepository;
    private final MovieRepository   movieRepository;
    private final TheatreRepository theatreRepository;
    private final ISTTimeService    ist;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * GET /api/shows/movie/{movieId}
     *
     * Returns IST-filtered, bookable shows grouped by date → list of show maps.
     * Response shape: Map<"YYYY-MM-DD", List<{show fields + displayTime + status...}>>
     *
     * OOAD: GRASP Information Expert — ISTTimeService decides bookability.
     * SOLID: O  — IST filtering rule lives in ISTTimeService, not here.
     *
     * Steps:
     *   1. findByMovieIdAndActiveTrue — only active shows
     *   2. Filter with ist.isShowBookable — drop past/expired shows
     *   3. Log total vs bookable count
     *   4. Enrich each show with displayTime, status, minutesLeft, isSoldOut
     *   5. Group by date using TreeMap (auto-sorted by date string)
     *   6. Sort each date's shows by showTime
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<Map<String, List<Map<String,Object>>>>> getShowsByMovie(
            @PathVariable String movieId) {

        // Step 1: fetch all active shows for this movie
        List<Show> allShows = showRepository.findByMovieIdAndActiveTrue(movieId);

        // Step 2: IST-aware filtering
        //   - todayIST base → drop any show from a past date entirely
        //   - TODAY's shows → apply isShowBookable() (time + 15-min grace)
        //   - FUTURE dates  → ALL shows are bookable (no time check needed)
        LocalDate todayIST = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));

        List<Show> bookable = allShows.stream()
                .filter(show -> {
                    LocalDate showDate = show.getShowDate();

                    // Drop shows from past dates entirely
                    if (showDate.isBefore(todayIST)) return false;

                    // Future dates (tomorrow, day after) → always bookable
                    if (showDate.isAfter(todayIST)) return true;

                    // Today's shows → apply IST time check with 15-min grace
                    return ist.isShowBookable(showDate.toString(), show.getShowTime().toString());
                })
                .collect(Collectors.toList());

        System.out.printf("🕐 [IST FILTER] Movie %s — Total: %d, Bookable: %d (IST: %s)%n",
                movieId, allShows.size(), bookable.size(), ist.currentISTString());

        // Step 4: enrich each show with display metadata
        List<Map<String,Object>> enriched = bookable.stream()
                .sorted(Comparator.comparing(Show::getShowDate).thenComparing(Show::getShowTime))
                .map(show -> enrichShow(show))
                .collect(Collectors.toList());

        // Step 5: group by date — TreeMap keeps dates sorted lexicographically
        Map<String, List<Map<String,Object>>> grouped = new TreeMap<>();
        for (Map<String,Object> showMap : enriched) {
            String date = (String) showMap.get("showDate");
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(showMap);
        }

        // Step 6: sort each date's shows by showTime
        grouped.forEach((date, shows) ->
            shows.sort(Comparator.comparing(m -> (String) m.get("showTime"))));

        return ResponseEntity.ok(ApiResponse.success(
                "Bookable shows for movie (IST-filtered): " + enriched.size(), grouped));
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * GET /api/shows/{showId}/seats
     *
     * Returns seats grouped by row for the seat selection grid.
     * Validates the show is still bookable BEFORE returning seats.
     *
     * OOAD: GRASP Controller — validates, then delegates; no seat logic here.
     * SOLID: S — only one concern: serve the seat grid.
     *
     * Response: { show, seats (grouped by row), totalSeats, bookedCount,
     *             availableCount, showStatus, minutesLeft, displayTime }
     */
    @GetMapping("/{showId}/seats")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getSeatsByShow(
            @PathVariable String showId) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found: " + showId));

        // IST bookability gate — cannot select seats for a started show
        if (!ist.isShowBookable(show.getShowDate().toString(), show.getShowTime().toString())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "This show has already started. Please choose an upcoming show. " +
                "(Current IST: " + ist.currentISTString() + ")"));
        }

        List<Seat> seats = seatRepository.findByShowId(showId);

        // Group seats by row → TreeMap keeps rows A–J sorted
        Map<String, List<Map<String,Object>>> seatsByRow = new TreeMap<>();
        for (Seat seat : seats) {
            String row = seat.getRow();
            Map<String,Object> seatMap = new LinkedHashMap<>();
            seatMap.put("id",              seat.getId());
            seatMap.put("seatNumber",      seat.getSeatNumber());
            seatMap.put("row",             seat.getRow());
            seatMap.put("type",            seat.getType());
            seatMap.put("price",           seat.getPrice());
            seatMap.put("isBooked",        seat.isBooked());
            seatMap.put("bookedByUserId",  seat.getBookedByUserId());
            seatsByRow.computeIfAbsent(row, k -> new ArrayList<>()).add(seatMap);
        }

        // Sort each row's seats by column number
        seatsByRow.forEach((row, rowSeats) ->
            rowSeats.sort(Comparator.comparingInt(m -> {
                String sn = (String) m.get("seatNumber");
                return Integer.parseInt(sn.substring(1));
            })));

        Movie   movie   = movieRepository.findById(show.getMovieId()).orElse(null);
        Theatre theatre = theatreRepository.findById(show.getTheatreId()).orElse(null);

        long bookedCount    = seats.stream().filter(Seat::isBooked).count();
        long availableCount = seats.size() - bookedCount;
        long minutesLeft    = ist.minutesUntilShow(show.getShowDate().toString(), show.getShowTime().toString());
        String status       = ist.getShowStatus(show.getShowDate().toString(), show.getShowTime().toString());
        String displayTime  = ist.formatTo12Hour(show.getShowTime().toString());

        Map<String,Object> response = new LinkedHashMap<>();
        response.put("show",          enrichShow(show));
        response.put("seatsByRow",    seatsByRow);
        response.put("allSeats",      seats);            // flat list for frontend flexibility
        response.put("movie",         movie);
        response.put("theatre",       theatre);
        response.put("totalSeats",    seats.size());
        response.put("bookedCount",   bookedCount);
        response.put("availableCount", availableCount);
        response.put("showStatus",    status);
        response.put("minutesLeft",   minutesLeft);
        response.put("displayTime",   displayTime);
        response.put("currentIST",    ist.currentISTString());

        return ResponseEntity.ok(ApiResponse.success("Seat grid loaded", response));
    }

    /**
     * GET /api/shows/{showId} — single show details
     */
    @GetMapping("/{showId}")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getShow(@PathVariable String showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));
        return ResponseEntity.ok(ApiResponse.success("Show", enrichShow(show)));
    }

    // ── Private helper: enriches a Show with IST display metadata ────────────
    /**
     * GRASP: Information Expert — show enrichment stays in controller since
     * it requires ISTTimeService + show data together (no other class has both).
     */
    private Map<String,Object> enrichShow(Show show) {
        String dateStr = show.getShowDate().toString();
        String timeStr = show.getShowTime().toString();

        long minutesLeft = ist.minutesUntilShow(dateStr, timeStr);
        String status    = ist.getShowStatus(dateStr, timeStr);
        String display12 = ist.formatTo12Hour(timeStr);
        boolean soldOut  = show.getAvailableSeats() == 0;
        boolean bookable = ist.isShowBookable(dateStr, timeStr) && !soldOut;

        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",             show.getId());
        m.put("movieId",        show.getMovieId());
        m.put("theatreId",      show.getTheatreId());
        m.put("showDate",       dateStr);
        m.put("showTime",       timeStr.substring(0, 5));
        m.put("price",          show.getPrice());
        m.put("availableSeats", show.getAvailableSeats());
        m.put("active",         show.isActive());
        // IST-enriched fields
        m.put("displayTime",    display12);
        m.put("status",         status);
        m.put("minutesLeft",    minutesLeft);
        m.put("bookable",       bookable);
        m.put("isSoldOut",      soldOut);
        return m;
    }
}
