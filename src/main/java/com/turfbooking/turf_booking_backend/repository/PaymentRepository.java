package com.turfbooking.turf_booking_backend.repository;

import com.turfbooking.turf_booking_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Add custom query methods if needed
}