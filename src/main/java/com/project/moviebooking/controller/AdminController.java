package com.project.moviebooking.controller;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.*;
import com.project.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminController — ADMIN-ONLY CRUD for Movies, Theatres, Shows
 * ==============================================================
 * SOLID: D — all operations via repository interfaces
 * SECURITY: @PreAuthorize("hasRole('ADMIN')") — users cannot access
 *
 * ✅ Only ADMIN can: add/edit/delete movies, theatres, shows
 * ✅ Only ADMIN can: change prices, seat types, timings
 * ✅ Users get 403 Forbidden if they try these endpoints
 * ==============================================================
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
public class AdminController {

    private final MovieRepository   movieRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository    showRepository;
    private final SeatRepository    seatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository    userRepository;

    // ─────────────────────────────────────────────────────────────
    // ▶ MOVIE CRUD
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/movies")
    public ResponseEntity<ApiResponse<Movie>> addMovie(@RequestBody Movie movie) {
        movie.setActive(true);
        return ResponseEntity.ok(ApiResponse.success("Movie added", movieRepository.save(movie)));
    }

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success("All movies", movieRepository.findAll()));
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<Movie>> updateMovie(
            @PathVariable String id, @RequestBody Movie updated) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        if (updated.getTitle()           != null) movie.setTitle(updated.getTitle());
        if (updated.getGenre()           != null) movie.setGenre(updated.getGenre());
        if (updated.getLanguage()        != null) movie.setLanguage(updated.getLanguage());
        if (updated.getDurationMinutes() > 0)     movie.setDurationMinutes(updated.getDurationMinutes());
        if (updated.getRating()          > 0)     movie.setRating(updated.getRating());
        if (updated.getCertificate()     != null) movie.setCertificate(updated.getCertificate());
        if (updated.getDescription()     != null) movie.setDescription(updated.getDescription());
        if (updated.getPosterUrl()       != null) movie.setPosterUrl(updated.getPosterUrl());
        return ResponseEntity.ok(ApiResponse.success("Movie updated", movieRepository.save(movie)));
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMovie(@PathVariable String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        movie.setActive(false);  // soft delete
        movieRepository.save(movie);
        return ResponseEntity.ok(ApiResponse.success("Movie deactivated", id));
    }

    @DeleteMapping("/movies/{id}/hard")
    public ResponseEntity<ApiResponse<String>> hardDeleteMovie(@PathVariable String id) {
        movieRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Movie permanently deleted", id));
    }

    // ─────────────────────────────────────────────────────────────
    // ▶ THEATRE CRUD
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/theatres")
    public ResponseEntity<ApiResponse<Theatre>> addTheatre(@RequestBody Theatre theatre) {
        theatre.setActive(true);
        theatre.setTotalSeats(theatre.getRows() * theatre.getColumns());
        return ResponseEntity.ok(ApiResponse.success("Theatre added", theatreRepository.save(theatre)));
    }

    @GetMapping("/theatres")
    public ResponseEntity<ApiResponse<List<Theatre>>> getAllTheatres() {
        return ResponseEntity.ok(ApiResponse.success("All theatres", theatreRepository.findAll()));
    }

    @PutMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<Theatre>> updateTheatre(
            @PathVariable String id, @RequestBody Theatre updated) {
        Theatre t = theatreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theatre not found"));
        if (updated.getName()    != null) t.setName(updated.getName());
        if (updated.getCity()    != null) t.setCity(updated.getCity());
        if (updated.getAddress() != null) t.setAddress(updated.getAddress());
        if (updated.getRows()    > 0)     { t.setRows(updated.getRows()); t.setTotalSeats(t.getRows()*t.getColumns()); }
        if (updated.getColumns() > 0)     { t.setColumns(updated.getColumns()); t.setTotalSeats(t.getRows()*t.getColumns()); }
        return ResponseEntity.ok(ApiResponse.success("Theatre updated", theatreRepository.save(t)));
    }

    @DeleteMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTheatre(@PathVariable String id) {
        Theatre t = theatreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theatre not found"));
        t.setActive(false);
        theatreRepository.save(t);
        return ResponseEntity.ok(ApiResponse.success("Theatre deactivated", id));
    }

    // ─────────────────────────────────────────────────────────────
    // ▶ SHOW CRUD (with auto-seat generation + price update)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/shows")
    public ResponseEntity<ApiResponse<Show>> addShow(@RequestBody Map<String, Object> body) {
        Show show = new Show();
        show.setMovieId((String) body.get("movieId"));
        show.setTheatreId((String) body.get("theatreId"));
        show.setShowDate(LocalDate.parse((String) body.get("showDate")));
        show.setShowTime(LocalTime.parse((String) body.get("showTime")));
        show.setPrice(Double.parseDouble(body.get("price").toString()));
        show.setAvailableSeats(150);
        show.setBookedSeats(new ArrayList<>());
        show.setActive(true);

        Show saved = showRepository.save(show);

        // AUTO-GENERATE 150 SEATS for this show
        generateSeatsForShow(saved);

        return ResponseEntity.ok(ApiResponse.success("Show added with 150 seats", saved));
    }

    @GetMapping("/shows")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> getAllShows() {
        List<Show> shows = showRepository.findAll();
        List<Map<String,Object>> enriched = shows.stream().map(show -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("show", show);
            movieRepository.findById(show.getMovieId()).ifPresent(mv -> m.put("movieTitle", mv.getTitle()));
            theatreRepository.findById(show.getTheatreId()).ifPresent(th -> m.put("theatreName", th.getName()));
            m.put("bookedCount", seatRepository.countByShowIdAndIsBooked(show.getId(), true));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All shows", enriched));
    }

    /**
     * Update show price (admin only)
     * Also updates all UNBOOKED seats' prices proportionally
     */
    @PutMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<Show>> updateShow(
            @PathVariable String id, @RequestBody Map<String, Object> body) {

        Show show = showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        boolean priceChanged = false;
        double oldPrice = show.getPrice();

        if (body.containsKey("showDate"))  show.setShowDate(LocalDate.parse((String) body.get("showDate")));
        if (body.containsKey("showTime"))  show.setShowTime(LocalTime.parse((String) body.get("showTime")));
        if (body.containsKey("price")) {
            double newPrice = Double.parseDouble(body.get("price").toString());
            show.setPrice(newPrice);
            priceChanged = (newPrice != oldPrice);
        }
        if (body.containsKey("active"))    show.setActive((Boolean) body.get("active"));

        Show updated = showRepository.save(show);

        // If price changed, update all UNBOOKED seat prices
        if (priceChanged) {
            List<Seat> seats = seatRepository.findByShowId(id);
            double newBase = show.getPrice();
            seats.stream().filter(s -> !s.isBooked()).forEach(s -> {
                double multiplier = switch (s.getType()) {
                    case "VIP"     -> 2.0;
                    case "PREMIUM" -> 1.5;
                    default        -> 1.0;
                };
                s.setPrice(newBase * multiplier);
            });
            seatRepository.saveAll(seats);
        }

        return ResponseEntity.ok(ApiResponse.success("Show updated", updated));
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShow(@PathVariable String id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Show not found"));
        show.setActive(false);
        showRepository.save(show);
        seatRepository.deleteAll(seatRepository.findByShowId(id));
        return ResponseEntity.ok(ApiResponse.success("Show deactivated and seats removed", id));
    }

    // ─────────────────────────────────────────────────────────────
    // ▶ SEAT MANAGEMENT — Admin can change seat type/price
    // ─────────────────────────────────────────────────────────────

    @PutMapping("/seats/{seatId}")
    public ResponseEntity<ApiResponse<Seat>> updateSeat(
            @PathVariable String seatId, @RequestBody Map<String, Object> body) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        if (body.containsKey("type"))  seat.setType((String) body.get("type"));
        if (body.containsKey("price")) seat.setPrice(Double.parseDouble(body.get("price").toString()));
        return ResponseEntity.ok(ApiResponse.success("Seat updated", seatRepository.save(seat)));
    }

    // ─────────────────────────────────────────────────────────────
    // ▶ BOOKING MANAGEMENT (view only for admin)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        List<Map<String,Object>> enriched = bookings.stream().map(b -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("booking", b);
            userRepository.findById(b.getUserId())
                    .ifPresent(u -> { m.put("userName", u.getName()); m.put("userEmail", u.getEmail()); });
            movieRepository.findById(b.getMovieId())
                    .ifPresent(mv -> m.put("movieTitle", mv.getTitle()));
            showRepository.findById(b.getShowId())
                    .ifPresent(sh -> { m.put("showDate", sh.getShowDate()); m.put("showTime", sh.getShowTime()); });
            theatreRepository.findById(b.getTheatreId())
                    .ifPresent(th -> m.put("theatreName", th.getName()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All bookings", enriched));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Object>>> getAllUsers() {
        var users = userRepository.findAll().stream().map(u -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("phone", u.getPhone());
            m.put("active", u.isActive());
            return (Object) m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All users", users));
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER — Auto-generate 150 seats for a new show
    // ─────────────────────────────────────────────────────────────
    public void generateSeatsForShow(Show show) {
        char[] rowLabels = {'A','B','C','D','E','F','G','H','I','J'};
        List<Seat> seats = new ArrayList<>();
        double base = show.getPrice();

        for (int r = 0; r < 10; r++) {
            char row = rowLabels[r];
            String type  = (r < 2) ? "VIP" : (r < 5) ? "PREMIUM" : "REGULAR";
            double mult  = (r < 2) ? 2.0  : (r < 5) ? 1.5       : 1.0;

            for (int c = 1; c <= 15; c++) {
                Seat seat = new Seat();
                seat.setShowId(show.getId());
                seat.setTheatreId(show.getTheatreId());
                seat.setSeatNumber(row + String.valueOf(c));
                seat.setRow(String.valueOf(row));
                seat.setType(type);           // ← Seat.type field
                seat.setPrice(base * mult);
                seat.setBooked(false);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
    }
}
