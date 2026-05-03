package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Show — a specific screening of a Movie at a Theatre
 * GRASP: Information Expert — holds all show data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shows")
public class Show {

    @Id
    private String id;

    private String movieId;

    private String theatreId;   // which theatre this show runs at

    private LocalDate showDate;

    private LocalTime showTime;

    private double price;       // base ticket price (seats add multiplier)

    private String screen = "Screen 1";

    private List<String> bookedSeats = new ArrayList<>(); // seat numbers already booked

    private int availableSeats = 150;

    private boolean active = true;
}
