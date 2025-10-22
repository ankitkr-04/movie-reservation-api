package com.moviereservation.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.SeatInstance;

@Repository
public interface SeatInstanceRepository
        extends JpaRepository<SeatInstance, UUID>, JpaSpecificationExecutor<SeatInstance> {

}
