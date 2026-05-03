package com.project.moviebooking.service;

import com.project.moviebooking.model.Movie;
import com.project.moviebooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MovieService - handles all movie-related business logic
 * SOLID: S - Single Responsibility
 * SOLID: O - Open/Closed: add new search methods without modifying existing ones
 */
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    /**
     * Add a new movie (Admin only)
     */
    public Movie addMovie(Movie movie) {
        movie.setActive(true);
        return movieRepository.save(movie);
    }

    /**
     * Get all active movies
     */
    public List<Movie> getAllActiveMovies() {
        return movieRepository.findByActiveTrue();
    }

    /**
     * Get all movies (admin view)
     */
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    /**
     * Get movie by ID
     */
    public Movie getMovieById(String id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + id));
    }

    /**
     * Update movie details (Admin only)
     */
    public Movie updateMovie(String id, Movie updatedMovie) {
        Movie existing = getMovieById(id);
        existing.setTitle(updatedMovie.getTitle());
        existing.setDescription(updatedMovie.getDescription());
        existing.setGenre(updatedMovie.getGenre());
        existing.setLanguage(updatedMovie.getLanguage());
        existing.setDurationMinutes(updatedMovie.getDurationMinutes());
        existing.setDirector(updatedMovie.getDirector());
        existing.setCast(updatedMovie.getCast());
        existing.setPosterUrl(updatedMovie.getPosterUrl());
        existing.setRating(updatedMovie.getRating());
        existing.setReleaseDate(updatedMovie.getReleaseDate());
        existing.setCertificate(updatedMovie.getCertificate());
        return movieRepository.save(existing);
    }

    /**
     * Soft delete movie (set active = false)
     * SOLID: O - doesn't modify existing data, just deactivates
     */
    public void deleteMovie(String id) {
        Movie movie = getMovieById(id);
        movie.setActive(false);
        movieRepository.save(movie);
    }

    /**
     * Search movies by genre
     */
    public List<Movie> getMoviesByGenre(String genre) {
        return movieRepository.findByGenreIgnoreCase(genre);
    }

    /**
     * Search movies by language
     */
    public List<Movie> getMoviesByLanguage(String language) {
        return movieRepository.findByLanguageIgnoreCase(language);
    }

    /**
     * Search movies by title (partial match)
     */
    public List<Movie> searchMovies(String keyword) {
        return movieRepository.findByTitleContainingIgnoreCase(keyword);
    }
}
