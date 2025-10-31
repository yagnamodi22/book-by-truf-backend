package com.turfbooking.turf_booking_backend.repository;

import com.turfbooking.turf_booking_backend.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByTurfId(Long turfId);
    
    // Add paginated version with default sorting by bookingDate and startTime in descending order
    @Query("SELECT b FROM Booking b WHERE b.turf.id = :turfId ORDER BY b.bookingDate DESC, b.startTime DESC")
    Page<Booking> findByTurfIdOrderByBookingDateDescStartTimeDesc(
            @Param("turfId") Long turfId, 
            Pageable pageable);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.turf.owner.id = :ownerId")
    List<Booking> findByTurfOwnerId(@Param("ownerId") Long ownerId);
    
    // Add paginated version with default sorting
    @Query("SELECT b FROM Booking b WHERE b.turf.owner.id = :ownerId ORDER BY b.bookingDate DESC, b.startTime DESC")
    Page<Booking> findByTurfOwnerIdOrderByBookingDateDescStartTimeDesc(
            @Param("ownerId") Long ownerId, 
            Pageable pageable);
            
    // Find bookings by turf ID and booking type
    List<Booking> findByTurfIdAndBookingType(Long turfId, Booking.BookingType bookingType);

    @Query("SELECT b FROM Booking b WHERE b.turf.id = :turfId AND b.bookingDate = :date AND " +
            "((b.startTime < :endTime AND b.endTime > :startTime)) AND " +
            "(b.status IN ('PENDING', 'CONFIRMED') OR b.bookingType = 'OFFLINE')")
    List<Booking> findConflictingBookings(
            @Param("turfId") Long turfId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate")
    List<Booking> findUserBookingsBetweenDates(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
