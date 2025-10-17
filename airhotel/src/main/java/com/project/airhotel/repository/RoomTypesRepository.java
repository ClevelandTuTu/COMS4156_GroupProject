package com.project.airhotel.repository;

import com.project.airhotel.model.RoomTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Repository
public interface RoomTypesRepository extends JpaRepository<RoomTypes, Long> {
}
