package com.project.moviebooking.repository;

import com.project.moviebooking.model.Theatre;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TheatreRepository - MongoDB CRUD for Theatre
 */
@Repository
public interface TheatreRepository extends MongoRepository<Theatre, String> {

    List<Theatre> findByActiveTrue();

    List<Theatre> findByCityIgnoreCase(String city);
}
