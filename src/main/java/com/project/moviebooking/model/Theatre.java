package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Theatre — physical cinema hall
 * Admin CRUD: POST/PUT/DELETE /api/admin/theatres
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "theatres")
public class Theatre {

    @Id
    private String id;

    private String name;

    private String city;

    private String address;

    private int rows = 10;       // default 10 rows

    private int columns = 15;    // default 15 columns

    private int totalSeats = 150;

    private boolean active = true;
}
