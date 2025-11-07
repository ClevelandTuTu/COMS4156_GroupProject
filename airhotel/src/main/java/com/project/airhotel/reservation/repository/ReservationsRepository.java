package com.project.airhotel.reservation.repository;

import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for Reservations. Provides CRUD plus custom query
 * methods to fetch reservations by hotel scope, status, stay date ranges, and
 * ownership by user.
 * <p>
 * Author: Ziyang Su Version: 1.0.0
 */
@Repository
public interface ReservationsRepository extends JpaRepository<Reservations,
    Long> {

  /**
   * Returns all reservations under a given hotel.
   *
   * @param hotelId hotel identifier
   * @return list of reservations for the hotel
   */
  @Query("SELECT r FROM Reservations r WHERE r.hotelId = :hotelId")
  List<Reservations> findByHotelId(@Param("hotelId") Long hotelId);

  /**
   * Returns reservations under a given hotel filtered by status.
   *
   * @param hotelId hotel identifier
   * @param status  reservation status filter
   * @return list of reservations for the hotel and status
   */
  @Query("SELECT r FROM Reservations r WHERE r.hotelId = :hotelId AND r"
      + ".status = :status")
  List<Reservations> findByHotelIdAndStatus(
      @Param("hotelId") Long hotelId,
      @Param("status") ReservationStatus status);

  /**
   * Returns reservations under a hotel within a stay range. Both bounds apply
   * to check-in and check-out respectively: check_in_date >= start and
   * check_out_date <= end.
   *
   * @param hotelId hotel identifier
   * @param start   inclusive start date of stay range
   * @param end     inclusive end date of stay range
   * @return list of reservations in the date window
   */
  @Query("""
      SELECT r FROM Reservations r
      WHERE r.hotelId = :hotelId
        AND r.checkInDate >= :start
        AND r.checkOutDate <= :end
      """)
  List<Reservations> findByHotelIdAndStayRange(@Param("hotelId") Long hotelId,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);

  /**
   * Returns reservations under a hotel filtered by status and stay range.
   * Applies the same date window semantics as findByHotelIdAndStayRange.
   *
   * @param hotelId hotel identifier
   * @param status  reservation status filter
   * @param start   inclusive start date of stay range
   * @param end     exclusive end date of stay range
   * @return list of reservations matching hotel, status, and date window
   */
  @Query("""
      SELECT r FROM Reservations r
      WHERE r.hotelId = :hotelId
        AND r.status = :status
        AND r.checkInDate >= :start
        AND r.checkOutDate <= :end
      """)
  List<Reservations> findByHotelIdAndStatusAndStayRange(
      @Param("hotelId") Long hotelId,
      @Param("status") ReservationStatus status,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  /**
   * Returns all reservations owned by a given user.
   *
   * @param userId user identifier
   * @return list of user-owned reservations
   */
  @Query("SELECT r FROM Reservations r WHERE r.userId = :userId")
  List<Reservations> findByUserId(@Param("userId") Long userId);

  /**
   * Resolves a reservation by id only if it is owned by the given user.
   *
   * @param id     reservation identifier
   * @param userId user identifier
   * @return optional reservation when found and owned by the user
   */
  @Query("SELECT r FROM Reservations r WHERE r.id = :id AND r.userId = "
      + ":userId")
  java.util.Optional<Reservations> findByIdAndUserId(
      @Param("id") Long id,
      @Param("userId") Long userId);
}
