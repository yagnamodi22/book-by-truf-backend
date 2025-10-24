package com.turfbooking.turf_booking_backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.turfbooking.turf_booking_backend.dto.TurfDTO;
import com.turfbooking.turf_booking_backend.entity.Turf;
import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.service.TurfService;
import com.turfbooking.turf_booking_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import com.turfbooking.turf_booking_backend.entity.Booking;
import com.turfbooking.turf_booking_backend.service.BookingService;

@RestController
@RequestMapping("/turfs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TurfController {

    @Autowired
    private TurfService turfService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/public")
    public ResponseEntity<List<Turf>> getAllActiveTurfs() {
        List<Turf> turfs = turfService.findAllActiveTurfs();
        return ResponseEntity.ok(turfs);
    }

    @GetMapping("/public/paginated")
    public ResponseEntity<Page<Turf>> getAllActiveTurfsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Turf> turfs = turfService.findAllActiveTurfs(pageable);
        return ResponseEntity.ok(turfs);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<Turf> getTurfById(@PathVariable Long id) {
        Optional<Turf> turf = turfService.findById(id);
        return turf.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/search")
    public ResponseEntity<List<Turf>> searchTurfsByLocation(@RequestParam String location) {
        List<Turf> turfs = turfService.findTurfsByLocation(location);
        return ResponseEntity.ok(turfs);
    }

    @GetMapping("/public/filter")
    public ResponseEntity<Page<Turf>> filterTurfs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (location == null) location = "";
        if (minPrice == null) minPrice = BigDecimal.ZERO;
        if (maxPrice == null) maxPrice = BigDecimal.valueOf(10000);

        Page<Turf> turfs = turfService.findTurfsByLocationAndPrice(location, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(turfs);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> createTurf(@Valid @RequestBody TurfDTO turfDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User owner = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Turf turf = new Turf(
                    turfDTO.getName(),
                    turfDTO.getDescription(),
                    turfDTO.getLocation(),
                    turfDTO.getPricePerHour(),
                    owner
            );
            turf.setAmenities(turfDTO.getAmenities());
            
            // Handle images - if imageArray is provided, use it; otherwise use images string
            if (turfDTO.getImageArray() != null && turfDTO.getImageArray().length > 0) {
                String[] processedImages = turfService.processImages(turfDTO.getImageArray());
                turf.setImages(String.join(",", processedImages));
            } else {
                turf.setImages(turfDTO.getImages());
            }
            // Owner submissions should be inactive (pending) until admin approves
            if (!owner.getRole().equals(User.Role.ADMIN)) {
                turf.setIsActive(false);
                System.out.println("Setting turf as inactive (pending approval) for owner: " + owner.getEmail());
            } else {
                turf.setIsActive(true);
                System.out.println("Setting turf as active for admin: " + owner.getEmail());
            }
            Turf savedTurf = turfService.createTurf(turf);
            System.out.println("Turf created with ID: " + savedTurf.getId() + ", isActive: " + savedTurf.getIsActive());
            return ResponseEntity.ok(savedTurf);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create turf: " + e.getMessage());
        }
    }

    @GetMapping("/my-turfs")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<Turf>> getMyTurfs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User owner = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Turf> turfs = turfService.findTurfsByOwner(owner.getId());
        return ResponseEntity.ok(turfs);
    }

    @GetMapping("/my-turfs/stats")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> getMyTurfsStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User owner = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingService.findBookingsByOwner(owner.getId());
        final int totalBookings = bookings != null ? bookings.size() : 0;
        final BigDecimal totalRevenue = (bookings != null)
                ? bookings.stream()
                    .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("totalRevenue", totalRevenue);
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateTurf(@PathVariable Long id, @Valid @RequestBody TurfDTO turfDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            User currentUser = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user owns this turf or is admin
            Optional<Turf> existingTurf = turfService.findById(id);
            if (existingTurf.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                    !existingTurf.get().getOwner().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You don't have permission to update this turf");
            }

            Turf updatedTurf = new Turf();
            updatedTurf.setName(turfDTO.getName());
            updatedTurf.setDescription(turfDTO.getDescription());
            updatedTurf.setLocation(turfDTO.getLocation());
            updatedTurf.setPricePerHour(turfDTO.getPricePerHour());
            updatedTurf.setAmenities(turfDTO.getAmenities());
            
            // Handle images - if imageArray is provided, use it; otherwise use images string
            if (turfDTO.getImageArray() != null && turfDTO.getImageArray().length > 0) {
                String[] processedImages = turfService.processImages(turfDTO.getImageArray());
                updatedTurf.setImages(String.join(",", processedImages));
            } else {
                updatedTurf.setImages(turfDTO.getImages());
            }

            Turf savedTurf = turfService.updateTurf(id, updatedTurf);
            return ResponseEntity.ok(savedTurf);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update turf: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteTurf(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            User currentUser = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user owns this turf or is admin
            Optional<Turf> existingTurf = turfService.findById(id);
            if (existingTurf.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                    !existingTurf.get().getOwner().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You don't have permission to delete this turf");
            }

            turfService.deleteTurf(id);
            return ResponseEntity.ok("Turf deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete turf: " + e.getMessage());
        }
    }

    // Admin endpoints for moderation
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Turf>> listPendingTurfs() {
        try {
            System.out.println("in pending");
            List<Turf> pendingTurfs = turfService.findAllInactiveTurfs();
            System.out.println("Found " + pendingTurfs.size() + " pending turfs");
            return ResponseEntity.ok(pendingTurfs);
        } catch (Exception e) {
            System.err.println("Error fetching pending turfs: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveTurf(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(turfService.approveTurf(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to approve turf: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectTurf(@PathVariable Long id) {
        try {
            turfService.deleteTurf(id);
            return ResponseEntity.ok("Turf rejected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reject turf: " + e.getMessage());
        }
    }
}

