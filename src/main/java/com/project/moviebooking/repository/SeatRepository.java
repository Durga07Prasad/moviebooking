package com.project.moviebooking.repository;

import com.project.moviebooking.model.Seat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends MongoRepository<Seat, String> {

    /** All seats for a show — renders the seat grid */
    List<Seat> findByShowId(String showId);

    /** Bulk fetch by seat numbers — used for atomic booking lock */
    List<Seat> findByShowIdAndSeatNumberIn(String showId, List<String> seatNumbers);

    /** Single seat lookup for validation */
    Optional<Seat> findByShowIdAndSeatNumber(String showId, String seatNumber);

    /** Count booked/available seats */
    long countByShowIdAndIsBooked(String showId, boolean isBooked);
}
