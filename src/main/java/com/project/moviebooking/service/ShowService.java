package com.project.moviebooking.service;

import com.project.moviebooking.model.Seat;
import com.project.moviebooking.model.Show;
import com.project.moviebooking.repository.SeatRepository;
import com.project.moviebooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ShowService - manages shows and seat generation
 * SOLID: S - only handles show-related logic
 */
@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    /**
     * Add a new show and auto-generate seats
     */
    public Show addShow(Show show) {
        show.setActive(true);
        Show saved = showRepository.save(show);

        // Auto-generate seats for the show
        generateSeatsForShow(saved);

        return saved;
    }

    /**
     * Auto-generate seats for a show (A1 to J15 = 150 seats)
     * GRASP: Creator - ShowService creates Seat objects
     */
    private void generateSeatsForShow(Show show) {
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

        for (String row : rows) {
            for (int col = 1; col <= 15; col++) {
                Seat seat = new Seat();
                seat.setShowId(show.getId());
                seat.setTheatreId(show.getTheatreId());
                seat.setSeatNumber(row + col);
                seat.setBooked(false);

                // Pricing based on row position
                if (row.equals("A") || row.equals("B")) {
                    seat.setSeatType("VIP");
                    seat.setPrice(show.getTicketPrice() * 2);
                } else if (row.equals("C") || row.equals("D") || row.equals("E")) {
                    seat.setSeatType("PREMIUM");
                    seat.setPrice(show.getTicketPrice() * 1.5);
                } else {
                    seat.setSeatType("REGULAR");
                    seat.setPrice(show.getTicketPrice());
                }

                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
        System.out.println("✅ Generated " + seats.size() + " seats for show: " + show.getId());
    }

    /**
     * Get all shows for a movie
     */
    public List<Show> getShowsByMovie(String movieId) {
        return showRepository.findByMovieId(movieId);
    }

    /**
     * Get all shows for a movie on a specific date
     */
    public List<Show> getShowsByMovieAndDate(String movieId, LocalDate date) {
        return showRepository.findByMovieIdAndShowDate(movieId, date);
    }

    /**
     * Get show by ID
     */
    public Show getShowById(String id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Show not found: " + id));
    }

    /**
     * Get all active shows
     */
    public List<Show> getAllShows() {
        return showRepository.findByActiveTrue();
    }

    /**
     * Get all seats for a show (for seat selection grid)
     */
    public List<Seat> getSeatsByShow(String showId) {
        return seatRepository.findByShowId(showId);
    }

    /**
     * Get available (unbooked) seats for a show
     */
    public List<Seat> getAvailableSeats(String showId) {
        return seatRepository.findByShowIdAndBookedFalse(showId);
    }
}
