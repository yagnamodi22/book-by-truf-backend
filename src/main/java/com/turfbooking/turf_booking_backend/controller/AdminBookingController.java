package com.turfbooking.turf_booking_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.turfbooking.turf_booking_backend.service.BookingService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", bookingService.getTotalBookings());
        stats.put("totalRevenue", bookingService.getTotalRevenue());
        stats.put("activeUsers", bookingService.getActiveUserCount());
        return ResponseEntity.ok(stats);
    }
}
