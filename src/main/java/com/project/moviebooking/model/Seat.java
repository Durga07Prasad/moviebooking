package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Seat — one physical seat in a show
 * SOLID: S — only seat state, nothing else
 * GRASP: Information Expert — knows own booking state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "seats")
public class Seat {

    @Id
    private String id;

    private String showId;

    private String theatreId;

    private String seatNumber;   // e.g. "A1", "C5", "J15"

    private String row;           // single char "A"–"J"

    /** VIP / PREMIUM / REGULAR */
    private String type;

    private double price;

    private boolean isBooked = false;

    private String bookedByUserId;
}
