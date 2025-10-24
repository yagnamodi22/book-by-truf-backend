package com.turfbooking.turf_booking_backend.repository;

import com.turfbooking.turf_booking_backend.entity.Turf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TurfRepository extends JpaRepository<Turf, Long> {

    List<Turf> findByIsActive(Boolean isActive);

    Page<Turf> findByIsActive(Boolean isActive, Pageable pageable);

    List<Turf> findByLocationContainingIgnoreCase(String location);

    @Query("SELECT t FROM Turf t WHERE t.isActive = true AND t.location LIKE %:location% AND t.pricePerHour BETWEEN :minPrice AND :maxPrice")
    Page<Turf> findTurfsByLocationAndPriceRange(
            @Param("location") String location,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    List<Turf> findByOwnerId(Long ownerId);
}
