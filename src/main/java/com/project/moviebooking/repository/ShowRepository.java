package com.project.moviebooking.repository;

import com.project.moviebooking.model.Show;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowRepository extends MongoRepository<Show, String> {

    /** Used by ShowController — returns only active shows for IST filtering */
    List<Show> findByMovieIdAndActiveTrue(String movieId);

    List<Show> findByMovieId(String movieId);

    List<Show> findByTheatreId(String theatreId);

    List<Show> findByShowDate(LocalDate showDate);

    List<Show> findByShowDateIn(List<LocalDate> dates);

    /** Delete all shows for a given date — used during show refresh */
    void deleteByShowDate(LocalDate showDate);

    List<Show> findByMovieIdAndShowDate(String movieId, LocalDate showDate);

    List<Show> findByMovieIdAndActive(String movieId, boolean active);

    List<Show> findByMovieIdAndTheatreId(String movieId, String theatreId);

    List<Show> findByMovieIdAndShowDateAndActive(String movieId, LocalDate showDate, boolean active);

    /** Active-only show listing — used by ShowService.getAllShows() */
    List<Show> findByActiveTrue();
}
