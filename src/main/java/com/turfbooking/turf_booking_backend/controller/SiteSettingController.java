package com.turfbooking.turf_booking_backend.controller;

import com.turfbooking.turf_booking_backend.entity.SiteSetting;
import com.turfbooking.turf_booking_backend.service.SiteSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/site-settings")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "https://frontend-bookmytruf.vercel.app", "https://frontend-bookmytruf-git-main-yagnamodi22s-projects.vercel.app"}, allowCredentials = "true", maxAge = 3600)
public class SiteSettingController {

    @Autowired
    private SiteSettingService siteSettingService;

    @GetMapping
    public ResponseEntity<List<SiteSetting>> getAll() {
        try {
            return ResponseEntity.ok(siteSettingService.getAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getMap() {
        try {
            return ResponseEntity.ok(siteSettingService.getAsMap());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of());
        }
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSetting> upsert(@RequestParam String key,
                                              @RequestParam String value,
                                              @RequestParam(required = false) String type) {
        try {
            return ResponseEntity.ok(siteSettingService.upsert(key, value, type));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSetting> upsertJson(@PathVariable("key") String key,
                                                  @RequestBody Map<String, Object> body) {
        try {
            String value = body != null && body.get("value") != null ? String.valueOf(body.get("value")) : "";
            String type = body != null && body.get("type") != null ? String.valueOf(body.get("type")) : "text";
            return ResponseEntity.ok(siteSettingService.upsert(key, value, type));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> upsertBulk(@RequestBody Map<String, String> payload) {
        try {
            if (payload == null || payload.isEmpty()) {
                return ResponseEntity.badRequest().body("Empty payload");
            }
            
            Map<String, String> results = new HashMap<>();
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                try {
                    siteSettingService.upsert(entry.getKey(), entry.getValue(), "text");
                    results.put(entry.getKey(), "success");
                } catch (Exception e) {
                    results.put(entry.getKey(), "error: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Bulk update failed: " + e.getMessage());
        }
    }
}


