package com.project.airhotel.repository;

import com.project.airhotel.model.RoomTypeInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Spring Data JPA repository for RoomTypeInventory. Provides CRUD operations
 * and a locked read for a specific hotel, room type, and stay date. The locked
 * read uses pessimistic write locking to coordinate inventory updates safely
 * when multiple transactions target the same row.
 */
public interface RoomTypeInventoryRepository
    extends JpaRepository<RoomTypeInventory, Long> {

  /**
   * Finds a single inventory row for the given hotel, room type, and stay date,
   * acquiring a pessimistic write lock on the selected row. This is typically
   * used before mutating fields such as reserved, blocked, or available to
   * ensure consistency during concurrent updates.
   * <p>
   * Locking semantics:
   * - PESSIMISTIC_WRITE prevents other transactions from acquiring locks that
   * would conflict with updates to the same row until the current transaction
   * ends.
   *
   * @param hotelId    the hotel identifier
   * @param roomTypeId the room type identifier
   * @param stayDate   the date for which inventory is recorded
   * @return an Optional containing the locked inventory row if present
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
         select i
           from RoomTypeInventory i
          where i.hotelId    = :hotelId
            and i.roomTypeId = :roomTypeId
            and i.stayDate   = :stayDate
         """)
  Optional<RoomTypeInventory> findForUpdate(
      @Param("hotelId") Long hotelId,
      @Param("roomTypeId") Long roomTypeId,
      @Param("stayDate") LocalDate stayDate);
}
