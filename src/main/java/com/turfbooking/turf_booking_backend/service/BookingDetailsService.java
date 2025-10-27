package com.turfbooking.turf_booking_backend.service;

import com.turfbooking.turf_booking_backend.dto.BookingDetailsDTO;
import com.turfbooking.turf_booking_backend.entity.Booking;
import com.turfbooking.turf_booking_backend.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingDetailsService {

    @Autowired
    private BookingRepository bookingRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<BookingDetailsDTO> getBookingDetailsByTurf(Long turfId) {
        // Use eager fetching to avoid N+1 problem
        String jpql = "SELECT b FROM Booking b " +
                      "LEFT JOIN FETCH b.user " +
                      "LEFT JOIN FETCH b.turf " +
                      "LEFT JOIN FETCH b.payment " +
                      "WHERE b.turf.id = :turfId";
        
        List<Booking> bookings = entityManager.createQuery(jpql, Booking.class)
                .setParameter("turfId", turfId)
                .getResultList();
        
        // Convert to DTOs
        return bookings.stream()
                .map(BookingDetailsDTO::new)
                .collect(Collectors.toList());
    }
}