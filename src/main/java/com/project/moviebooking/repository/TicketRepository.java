package com.project.moviebooking.repository;

import com.project.moviebooking.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TicketRepository - MongoDB CRUD for Ticket
 */
@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {

    List<Ticket> findByUserId(String userId);

    Optional<Ticket> findByBookingId(String bookingId);

    Optional<Ticket> findByTicketCode(String ticketCode);
}
