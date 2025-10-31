package com.turfbooking.turf_booking_backend.controller;

import com.turfbooking.turf_booking_backend.dto.BookingDTO;
import com.turfbooking.turf_booking_backend.dto.BookingDetailsDTO;
import com.turfbooking.turf_booking_backend.entity.Booking;
import com.turfbooking.turf_booking_backend.dto.MultiBookingDTO;
import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.service.BookingDetailsService;
import com.turfbooking.turf_booking_backend.service.BookingService;
import com.turfbooking.turf_booking_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@RestController
@RequestMapping("/bookings")
@CrossOrigin(origins = "http://localhost:3000")
public class BookingController {
    
    // DTO class for offline booking requests
    public static class OfflineBookingRequest {
        private Long turfId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal amount;
        
        public Long getTurfId() { return turfId; }
        public void setTurfId(Long turfId) { this.turfId = turfId; }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private BookingDetailsService bookingDetailsService;

    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingDTO bookingDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User user = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Booking booking = bookingService.createBooking(
                    user.getId(),
                    bookingDTO.getTurfId(),
                    bookingDTO.getBookingDate(),
                    bookingDTO.getStartTime(),
                    bookingDTO.getEndTime(),
                    bookingDTO.getFullName(),
                    bookingDTO.getPhoneNumber(),
                    bookingDTO.getEmail(),
                    bookingDTO.getPaymentMethod()
            );

            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create booking: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        Optional<Booking> booking = bookingService.findById(id);
        return booking.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingService.findBookingsByUser(user.getId());
        return ResponseEntity.ok(bookings);
    }

    // Offline booking endpoints
    @PostMapping("/offline")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createOfflineBooking(@Valid @RequestBody OfflineBookingRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User owner = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Owner not found"));

            // Create offline booking
            Booking booking = bookingService.createOfflineBooking(
                    owner.getId(),
                    request.getTurfId(),
                    request.getDate(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getAmount()
            );

            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create offline booking: " + e.getMessage());
        }
    }

    @DeleteMapping("/offline/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteOfflineBooking(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User owner = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Owner not found"));

            bookingService.deleteOfflineBooking(id, owner.getId());
            return ResponseEntity.ok().body("Offline booking successfully deleted");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete offline booking: " + e.getMessage());
        }
    }
    
    @GetMapping("/offline/turf/{turfId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getOfflineBookingsByTurf(@PathVariable Long turfId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User owner = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Owner not found"));

            List<Booking> bookings = bookingService.getOfflineBookingsByTurf(turfId, owner.getId());
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch offline bookings: " + e.getMessage());
        }
    }

    @GetMapping("/my-bookings/stats")
    public ResponseEntity<?> getMyBookingStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingService.findBookingsByUser(user.getId());
        final int totalBookings = bookings != null ? bookings.size() : 0;
        final BigDecimal totalSpent = (bookings != null)
                ? bookings.stream()
                    .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("totalSpent", totalSpent);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/my-bookings/date-range")
    public ResponseEntity<List<Booking>> getMyBookingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingService.findUserBookingsBetweenDates(user.getId(), startDate, endDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/turf/{turfId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> getBookingsByTurf(@PathVariable Long turfId) {
        try {
            List<BookingDetailsDTO> bookingDetails = bookingDetailsService.getBookingDetailsByTurf(turfId);
            return ResponseEntity.ok(bookingDetails);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching bookings: " + e.getMessage());
        }
    }
    
    @GetMapping("/turf/{turfId}/paginated")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> getBookingsByTurfPaginated(
            @PathVariable Long turfId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            org.springframework.data.domain.Page<BookingDetailsDTO> bookingDetails = 
                bookingDetailsService.getBookingDetailsByTurfPaginated(turfId, pageable);
            
            return ResponseEntity.ok(bookingDetails);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching paginated bookings: " + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getBookingsByStatus(@PathVariable Booking.BookingStatus status) {
        List<Booking> bookings = bookingService.findBookingsByStatus(status);
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> confirmBooking(@PathVariable Long id) {
        try {
            Booking booking = bookingService.confirmBooking(id);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to confirm booking: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            User currentUser = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Booking> booking = bookingService.findById(id);
            if (booking.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if user owns this booking or is admin/owner
            if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                    !booking.get().getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You don't have permission to cancel this booking");
            }

            Booking cancelledBooking = bookingService.cancelBooking(id);
            return ResponseEntity.ok(cancelledBooking);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to cancel booking: " + e.getMessage());
        }
    }

    @GetMapping("/availability")
    public ResponseEntity<Boolean> checkTimeSlotAvailability(
            @RequestParam Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {

        boolean available = bookingService.isTimeSlotAvailable(turfId, date, startTime, endTime);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/availability/day")
    public ResponseEntity<List<LocalTime>> getBookedSlotsForDay(
            @RequestParam Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getBookedStartTimes(turfId, date));
    }

    @PostMapping("/multi")
    public ResponseEntity<?> createMultiple(@Valid @RequestBody MultiBookingDTO body) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            User user = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<LocalTime> starts = body.getSlots().stream().map(s -> s.getStartTime()).toList();
            return ResponseEntity.ok(
                    bookingService.createMultipleBookings(
                        user.getId(), 
                        body.getTurfId(), 
                        body.getBookingDate(), 
                        starts, 
                        body.getPaymentMethod(),
                        body.getFullName(),
                        body.getPhoneNumber(),
                        body.getEmail()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create bookings: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        try {
            bookingService.deleteBooking(id);
            return ResponseEntity.ok("Booking deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete booking: " + e.getMessage());
        }
    }
}

