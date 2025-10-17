package com.project.airhotel.repository;

import com.project.airhotel.model.Hotels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Repository
public interface HotelsRepository extends JpaRepository<Hotels, Long> {
}
