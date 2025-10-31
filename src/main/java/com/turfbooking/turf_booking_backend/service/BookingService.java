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

    public Booking createBooking(Long userId, Long turfId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                                 String fullName, String phoneNumber, String email, String paymentMode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        if (!turf.getIsActive()) {
            throw new RuntimeException("Turf is not available for booking");
        }

        LocalDate today = LocalDate.now();
        if (bookingDate.isBefore(today)) {
            throw new RuntimeException("Booking date cannot be in the past");
        }

        boolean isLateNightSlot = startTime.getHour() >= 0 && startTime.getHour() < 3;

        if (bookingDate.equals(today) &&
                ((!isLateNightSlot && startTime.isBefore(LocalTime.now())) ||
                        (isLateNightSlot && startTime.isBefore(LocalTime.now()) && LocalTime.now().getHour() < 3))) {
            throw new RuntimeException("You cannot book past time slots. Please select an upcoming time slot.");
        }

        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                turfId, bookingDate, startTime, endTime);

        if (!conflictingBookings.isEmpty()) {
            throw new RuntimeException("Time slot is not available");
        }

        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        if (duration.toMinutesPart() > 0) {
            hours++;
        }
        BigDecimal totalAmount = turf.getPricePerHour().multiply(BigDecimal.valueOf(hours));

        Booking booking = new Booking(user, turf, bookingDate, startTime, endTime);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setFullName(fullName);
        booking.setPhoneNumber(phoneNumber);
        booking.setEmail(email);
        booking.setPaymentMode(paymentMode);

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

    public Booking createOfflineBooking(Long ownerId, Long turfId, LocalDate bookingDate,
                                        LocalTime startTime, LocalTime endTime, BigDecimal amount) {
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        if (!turf.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("You can only create offline bookings for your own turfs");
        }

        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                turfId, bookingDate, startTime, endTime);

        if (!conflictingBookings.isEmpty()) {
            throw new RuntimeException("Time slot is already booked");
        }

        if (amount == null) {
            Duration duration = Duration.between(startTime, endTime);
            long hours = duration.toHours();
            if (duration.toMinutesPart() > 0) {
                hours++;
            }
            amount = turf.getPricePerHour().multiply(BigDecimal.valueOf(hours));
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Booking booking = new Booking(owner, turf, bookingDate, startTime, endTime);
        booking.setTotalAmount(amount);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setBookingType(Booking.BookingType.OFFLINE);
        booking.setFullName("Offline Customer");
        booking.setPhoneNumber("N/A");
        booking.setEmail("N/A");
        booking.setPaymentMode("CASH");

        return bookingRepository.save(booking);
    }

    public void deleteOfflineBooking(Long bookingId, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getBookingType() != Booking.BookingType.OFFLINE) {
            throw new RuntimeException("Only offline bookings can be deleted with this method");
        }

        if (!booking.getTurf().getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("You can only delete offline bookings for your own turfs");
        }

        bookingRepository.delete(booking);
    }
    
    public List<Booking> getOfflineBookingsByTurf(Long turfId, Long ownerId) {
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new RuntimeException("Turf not found"));
                
        if (!turf.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("You can only view offline bookings for your own turfs");
        }
        
        return bookingRepository.findByTurfIdAndBookingType(turfId, Booking.BookingType.OFFLINE);
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

    public List<Booking> createMultipleBookings(Long userId, Long turfId, LocalDate date,
                                                List<LocalTime> slotStarts, String paymentMode,
                                                String fullName, String phoneNumber, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        if (!turf.getIsActive()) {
            throw new RuntimeException("Turf is not available for booking");
        }

        List<Booking> created = new ArrayList<>();
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new RuntimeException("Booking date cannot be in the past");
        }

        for (LocalTime start : slotStarts) {
            LocalTime end = start.plusHours(1);

            boolean isLateNightSlot = start.getHour() >= 0 && start.getHour() < 3;

            if (date.equals(today) &&
                    ((!isLateNightSlot && start.isBefore(LocalTime.now())) ||
                            (isLateNightSlot && start.isBefore(LocalTime.now()) && LocalTime.now().getHour() < 3))) {
                throw new RuntimeException("You cannot book past time slots. Please select an upcoming time slot.");
            }

            if (!isTimeSlotAvailable(turfId, date, start, end)) {
                throw new RuntimeException("One or more selected slots are no longer available");
            }

            Booking booking = new Booking(user, turf, date, start, end);
            booking.setTotalAmount(turf.getPricePerHour());
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking.setFullName(fullName);
            booking.setPhoneNumber(phoneNumber);
            booking.setEmail(email);
            booking.setPaymentMode(paymentMode);

            created.add(bookingRepository.save(booking));
        }

        return created;
    }

    // ======= DASHBOARD STATS METHODS =======

    public long getTotalBookings() {
        return bookingRepository.count();
    }

    public double getTotalRevenue() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream()
                .filter(b -> b.getTotalAmount() != null)
                .mapToDouble(b -> b.getTotalAmount().doubleValue())
                .sum();
    }

    public long getActiveUserCount() {
        return userRepository.count();
    }
}
