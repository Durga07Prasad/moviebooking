package com.project.moviebooking.repository;

import com.project.moviebooking.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    /** Returns bookings newest-first — used in My Bookings page */
    List<Booking> findByUserIdOrderByBookingTimeDesc(String userId);

    /** Used to clean up stale PENDING bookings before creating a new one */
    List<Booking> findByUserIdAndShowIdAndBookingStatus(String userId, String showId, String bookingStatus);
}

