package com.project.moviebooking.controller;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.model.Movie;
import com.project.moviebooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MovieController — Public movie browsing endpoints
 * SOLID: D — depends on MovieRepository interface
 * GRASP: Controller — zero business logic, delegates to repository
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001","http://localhost:5173"})
public class MovieController {

    private final MovieRepository movieRepository;

    /**
     * GET /api/movies — all active movies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {
        List<Movie> active = movieRepository.findAll().stream()
                .filter(Movie::isActive)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Active movies: " + active.size(), active));
    }

    /**
     * GET /api/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> getMovie(@PathVariable String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Movie found", movie));
    }

    /**
     * GET /api/movies/search?title=X
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Movie>>> search(@RequestParam String title) {
        List<Movie> results = movieRepository.findAll().stream()
                .filter(Movie::isActive)
                .filter(m -> m.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Search results", results));
    }

    /**
     * GET /api/movies/genre/{genre}
     */
    @GetMapping("/genre/{genre}")
    public ResponseEntity<ApiResponse<List<Movie>>> byGenre(@PathVariable String genre) {
        List<Movie> results = movieRepository.findAll().stream()
                .filter(Movie::isActive)
                .filter(m -> m.getGenre() != null &&
                        m.getGenre().toLowerCase().contains(genre.toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Movies in genre: " + genre, results));
    }
}
