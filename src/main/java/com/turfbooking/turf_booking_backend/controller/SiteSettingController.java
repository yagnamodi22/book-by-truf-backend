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
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Empty payload"
                ));
            }
            
            // Log the incoming request
            System.out.println("Received bulk update request with " + payload.size() + " settings");
            
            Map<String, Object> results = new HashMap<>();
            Map<String, String> successResults = new HashMap<>();
            Map<String, String> errorResults = new HashMap<>();
            
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                try {
                    System.out.println("Processing setting: " + key + " with value: " + value);
                    // Ensure we're using a transaction for each setting update
                    SiteSetting saved = siteSettingService.upsert(key, value, "text");
                    if (saved != null && saved.getId() != null) {
                        successResults.put(key, "success");
                    } else {
                        errorResults.put(key, "Failed to save setting (null result)");
                    }
                } catch (Exception e) {
                    String errorMsg = "Error updating " + key + ": " + e.getMessage();
                    System.err.println(errorMsg);
                    e.printStackTrace();
                    errorResults.put(key, errorMsg);
                }
            }
            
            results.put("success", successResults);
            results.put("errors", errorResults);
            
            System.out.println("Bulk update completed. Success: " + successResults.size() + ", Errors: " + errorResults.size());
            
            // Return a structured response with clear success/failure indication
            Map<String, Object> response = new HashMap<>();
            response.put("success", errorResults.isEmpty());
            response.put("message", errorResults.isEmpty() ? 
                "All settings updated successfully" : 
                "Some settings failed to update");
            response.put("results", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Critical error in bulk update: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Bulk update failed: " + e.getMessage()
            ));
        }
    }
}


