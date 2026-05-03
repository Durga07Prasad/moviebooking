package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Movie Model - stores movie details
 * OOP: Encapsulation - all fields private with getters/setters via @Data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "movies")
public class Movie {

    @Id
    private String id;

    private String title;

    private String description;

    private String genre;          // Action, Drama, Comedy, etc.

    private String language;       // English, Hindi, Telugu, etc.

    private int durationMinutes;

    private String director;

    private List<String> cast;

    private String posterUrl;

    private double rating;         // IMDb style rating 0-10

    private String releaseDate;

    private String certificate;    // U, UA, A

    private boolean active = true;
}
