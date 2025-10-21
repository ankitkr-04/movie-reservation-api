package com.moviereservation.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>, JpaSpecificationExecutor<Movie> {
    boolean existsByTitle(String title);

}