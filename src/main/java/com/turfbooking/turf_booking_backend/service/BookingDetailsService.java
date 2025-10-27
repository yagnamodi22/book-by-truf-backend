package com.turfbooking.turf_booking_backend.service;

import com.turfbooking.turf_booking_backend.dto.BookingDetailsDTO;
import com.turfbooking.turf_booking_backend.entity.Booking;
import com.turfbooking.turf_booking_backend.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
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
                      "WHERE b.turf.id = :turfId " +
                      "ORDER BY b.bookingDate DESC, b.startTime DESC";
        
        List<Booking> bookings = entityManager.createQuery(jpql, Booking.class)
                .setParameter("turfId", turfId)
                .getResultList();
        
        // Convert to DTOs
        return bookings.stream()
                .map(BookingDetailsDTO::new)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<BookingDetailsDTO> getBookingDetailsByTurfPaginated(Long turfId, Pageable pageable) {
        // Count query for total elements
        String countJpql = "SELECT COUNT(b) FROM Booking b WHERE b.turf.id = :turfId";
        Long total = entityManager.createQuery(countJpql, Long.class)
                .setParameter("turfId", turfId)
                .getSingleResult();
        
        // Data query with pagination
        String jpql = "SELECT b FROM Booking b " +
                      "LEFT JOIN FETCH b.user " +
                      "LEFT JOIN FETCH b.turf " +
                      "LEFT JOIN FETCH b.payment " +
                      "WHERE b.turf.id = :turfId " +
                      "ORDER BY b.bookingDate DESC, b.startTime DESC";
        
        TypedQuery<Booking> query = entityManager.createQuery(jpql, Booking.class)
                .setParameter("turfId", turfId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());
        
        List<Booking> bookings = query.getResultList();
        
        // Convert to DTOs
        List<BookingDetailsDTO> dtos = bookings.stream()
                .map(BookingDetailsDTO::new)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, total);
    }
}