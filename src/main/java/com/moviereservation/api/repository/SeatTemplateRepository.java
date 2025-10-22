package com.moviereservation.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.SeatTemplate;

@Repository
public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, UUID> {
    List<SeatTemplate> findByScreenNumber(Short screenNumber);

    void deleteByScreenNumber(Short screenNumber);
}
