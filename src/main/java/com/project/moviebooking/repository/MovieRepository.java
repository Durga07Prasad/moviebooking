package com.project.moviebooking.repository;

import com.project.moviebooking.model.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MovieRepository - MongoDB CRUD for Movie
 */
@Repository
public interface MovieRepository extends MongoRepository<Movie, String> {

    List<Movie> findByActiveTrue();

    List<Movie> findByGenreIgnoreCase(String genre);

    List<Movie> findByLanguageIgnoreCase(String language);

    List<Movie> findByTitleContainingIgnoreCase(String title);
}
