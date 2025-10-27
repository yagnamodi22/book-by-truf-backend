package com.turfbooking.turf_booking_backend.service;

import com.turfbooking.turf_booking_backend.entity.Booking;
import com.turfbooking.turf_booking_backend.entity.Turf;
import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.repository.BookingRepository;
import com.turfbooking.turf_booking_backend.repository.TurfRepository;
import com.turfbooking.turf_booking_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TurfRepository turfRepository;

    @Autowired
    private UserRepository userRepository;

    public Booking createBooking(Long userId, Long turfId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate turf exists and is active
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        if (!turf.getIsActive()) {
            throw new RuntimeException("Turf is not available for booking");
        }

        // Reject dates in the past and past times for today
        LocalDate today = LocalDate.now();
        if (bookingDate.isBefore(today)) {
            throw new RuntimeException("Booking date cannot be in the past");
        }
        
        // Special handling for late night slots (00:00-03:00)
        boolean isLateNightSlot = startTime.getHour() >= 0 && startTime.getHour() < 3;
        
        // Only apply past time validation for today's date and non-late-night slots
        // or for late-night slots if we're already past that time today
        if (bookingDate.equals(today) && 
            (!isLateNightSlot && startTime.isBefore(LocalTime.now())) || 
            (isLateNightSlot && startTime.isBefore(LocalTime.now()) && LocalTime.now().getHour() < 3)) {
            throw new RuntimeException("You cannot book past time slots. Please select an upcoming time slot.");
        }

        // Check for booking conflicts
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                turfId, bookingDate, startTime, endTime);

        if (!conflictingBookings.isEmpty()) {
            throw new RuntimeException("Time slot is not available");
        }

        // Calculate total amount
        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        if (duration.toMinutesPart() > 0) {
            hours++; // Round up partial hours
        }
        BigDecimal totalAmount = turf.getPricePerHour().multiply(BigDecimal.valueOf(hours));

        // Create booking
        Booking booking = new Booking(user, turf, bookingDate, startTime, endTime);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(Booking.BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> findBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> findBookingsByTurf(Long turfId) {
        return bookingRepository.findByTurfId(turfId);
    }

    public List<Booking> findBookingsByOwner(Long ownerId) {
        return bookingRepository.findByTurfOwnerId(ownerId);
    }

    public List<Booking> findBookingsByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    public List<Booking> findUserBookingsBetweenDates(Long userId, LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findUserBookingsBetweenDates(userId, startDate, endDate);
    }

    public Booking updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    public Booking confirmBooking(Long id) {
        return updateBookingStatus(id, Booking.BookingStatus.CONFIRMED);
    }

    public Booking cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed booking");
        }
        return updateBookingStatus(id, Booking.BookingStatus.CANCELLED);
    }

    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new RuntimeException("Booking not found");
        }
        bookingRepository.deleteById(id);
    }

    public boolean isTimeSlotAvailable(Long turfId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(turfId, date, startTime, endTime);
        return conflictingBookings.isEmpty();
    }

    public List<LocalTime> getBookedStartTimes(Long turfId, LocalDate date) {
        List<Booking> bookings = bookingRepository.findByTurfId(turfId);
        List<LocalTime> booked = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getBookingDate() != null && b.getBookingDate().equals(date)
                    && (b.getStatus() == Booking.BookingStatus.PENDING || b.getStatus() == Booking.BookingStatus.CONFIRMED)) {
                booked.add(b.getStartTime());
            }
        }
        return booked;
    }

    public List<Booking> createMultipleBookings(Long userId, Long turfId, LocalDate date, List<LocalTime> slotStarts, String paymentMethod) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Turf turf = turfRepository.findById(turfId).orElseThrow(() -> new RuntimeException("Turf not found"));
        if (!turf.getIsActive()) throw new RuntimeException("Turf is not available for booking");

        List<Booking> created = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new RuntimeException("Booking date cannot be in the past");
        }

        for (LocalTime start : slotStarts) {
            LocalTime end = start.plusHours(1);
            
            // Special handling for late night slots (00:00-03:00)
            // These are valid even if they appear to be "in the past" when comparing just times
            boolean isLateNightSlot = start.getHour() >= 0 && start.getHour() < 3;
            
            // Only apply past time validation for today's date and non-late-night slots
            // or for late-night slots if we're already past that time today
            if (date.equals(today) && 
                (!isLateNightSlot && start.isBefore(LocalTime.now())) || 
                (isLateNightSlot && start.isBefore(LocalTime.now()) && LocalTime.now().getHour() < 3)) {
                throw new RuntimeException("You cannot book past time slots. Please select an upcoming time slot.");
            }
            
            if (!isTimeSlotAvailable(turfId, date, start, end)) {
                throw new RuntimeException("One or more selected slots are no longer available");
            }
            Booking booking = new Booking(user, turf, date, start, end);
            booking.setTotalAmount(turf.getPricePerHour());
            booking.setStatus(Booking.BookingStatus.CONFIRMED); // Set to CONFIRMED after payment
            created.add(bookingRepository.save(booking));
        }
        
        return created;
    }
}
