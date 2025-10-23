package com.moviereservation.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moviereservation.api.domain.entities.SeatInstance;

import jakarta.persistence.LockModeType;

@Repository
public interface SeatInstanceRepository
                extends JpaRepository<SeatInstance, UUID>, JpaSpecificationExecutor<SeatInstance> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query(" SELECT s FROM SeatInstance s WHERE s.id IN :ids ")
        List<SeatInstance> findAllByIdWithLock(@Param("ids") List<UUID> ids);
}
