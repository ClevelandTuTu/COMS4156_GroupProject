package com.project.airhotel.repository;

import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
public interface ReservationsRepository extends JpaRepository<Reservations, Long> {

  @Query("SELECT r FROM Reservations r WHERE r.hotel_id = :hotelId")
  List<Reservations> findByHotelId(@Param("hotelId") Long hotelId);

  @Query("SELECT r FROM Reservations r WHERE r.hotel_id = :hotelId AND r.status = :status")
  List<Reservations> findByHotelIdAndStatus(@Param("hotelId") Long hotelId,
                                            @Param("status") ReservationStatus status);

  @Query("""
      SELECT r FROM Reservations r
      WHERE r.hotel_id = :hotelId
        AND r.check_in_date >= :start
        AND r.check_out_date <= :end
      """)
  List<Reservations> findByHotelIdAndStayRange(@Param("hotelId") Long hotelId,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);

  @Query("""
      SELECT r FROM Reservations r
      WHERE r.hotel_id = :hotelId
        AND r.status = :status
        AND r.check_in_date >= :start
        AND r.check_out_date <= :end
      """)
  List<Reservations> findByHotelIdAndStatusAndStayRange(@Param("hotelId") Long hotelId,
                                                        @Param("status") ReservationStatus status,
                                                        @Param("start") LocalDate start,
                                                        @Param("end") LocalDate end);
}
