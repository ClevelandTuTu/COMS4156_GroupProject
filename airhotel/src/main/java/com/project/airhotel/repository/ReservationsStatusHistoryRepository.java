package com.project.airhotel.repository;

import com.project.airhotel.model.ReservationsStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for ReservationsStatusHistory.
 * Provides CRUD and pagination/sorting for status history records
 * associated with reservations.
 */
@Repository
public interface ReservationsStatusHistoryRepository
    extends JpaRepository<ReservationsStatusHistory, Long> {
}
