package com.turfbooking.turf_booking_backend.repository;

import com.turfbooking.turf_booking_backend.entity.Booking;
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

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.turf.owner.id = :ownerId")
    List<Booking> findByTurfOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT b FROM Booking b WHERE b.turf.id = :turfId AND b.bookingDate = :date AND " +
            "((b.startTime < :endTime AND b.endTime > :startTime)) AND " +
            "b.status IN ('PENDING', 'CONFIRMED')")
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
