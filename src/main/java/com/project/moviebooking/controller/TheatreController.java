package com.project.moviebooking.controller;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.Theatre;
import com.project.moviebooking.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TheatreController — PUBLIC read endpoints for theatre browsing.
 *
 * SOLID: S — only theatre HTTP concerns.
 * SOLID: D — depends on TheatreRepository (interface).
 * GRASP: Controller — delegates to repository, no business logic.
 *
 * Security: All GET endpoints are PUBLIC (see SecurityConfig):
 *   .requestMatchers(HttpMethod.GET, "/api/theatres/**").permitAll()
 *
 * Admin CRUD for theatres remains in AdminController (/api/admin/theatres).
 */
@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
public class TheatreController {

    private final TheatreRepository theatreRepository;

    /**
     * GET /api/theatres — all active theatres (public)
     * Used by ShowSelection.jsx to build the theatre name map.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Theatre>>> getAllTheatres() {
        return ResponseEntity.ok(ApiResponse.success(
                "All theatres", theatreRepository.findAll()));
    }

    /**
     * GET /api/theatres/{id} — single theatre by ID (public)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Theatre>> getTheatre(@PathVariable String id) {
        Theatre t = theatreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theatre not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Theatre", t));
    }

    /**
     * GET /api/theatres/city/{city} — theatres by city (public)
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<Theatre>>> getTheatresByCity(@PathVariable String city) {
        List<Theatre> result = theatreRepository.findAll().stream()
                .filter(t -> city.equalsIgnoreCase(t.getCity()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Theatres in " + city, result));
    }
}
