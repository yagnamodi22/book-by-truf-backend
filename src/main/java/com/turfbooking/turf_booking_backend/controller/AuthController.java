package com.turfbooking.turf_booking_backend.controller;

import com.turfbooking.turf_booking_backend.dto.AuthRequest;
import com.turfbooking.turf_booking_backend.dto.AuthResponse;
import com.turfbooking.turf_booking_backend.dto.UserRegistrationDTO;
import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.service.JwtService;
import com.turfbooking.turf_booking_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        try {
            // registration request received
            User user = new User(
                    registrationDTO.getFirstName(),
                    registrationDTO.getLastName(),
                    registrationDTO.getEmail(),
                    registrationDTO.getPassword(),
                    registrationDTO.getPhone()
            );

            if (registrationDTO.getRole() != null) {
                user.setRole(registrationDTO.getRole());
            }

            User savedUser = userService.createUser(user);
            String token = jwtService.generateToken(savedUser);

            return ResponseEntity.ok(new AuthResponse(
                token, 
                savedUser.getEmail(),
                savedUser.getFirstName() + " " + savedUser.getLastName(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getPhone(),
                savedUser.getRole().name(),
                savedUser.getCreatedAt()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            // login attempt
            
            // Check if user exists (optional pre-check; main validation happens during authenticate)
            userService.findByEmail(authRequest.getEmail()).orElse(null);
            // authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()
                    )
            );

            // Always fetch the latest user data from database after authentication
            User user = userService.findByEmail(authRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));
            
            // role fetched for token generation
            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(new AuthResponse(
                token, 
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt()
            ));

        } catch (AuthenticationException e) {
            // authentication failed
            return ResponseEntity.badRequest().body("Invalid credentials");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User user = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(new AuthResponse(
                null, // No token needed for profile
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get profile: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserRegistrationDTO updateDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User user = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update only allowed fields directly on the user object
            if (updateDTO.getFirstName() != null) {
                user.setFirstName(updateDTO.getFirstName());
            }
            if (updateDTO.getLastName() != null) {
                user.setLastName(updateDTO.getLastName());
            }
            if (updateDTO.getPhone() != null) {
                user.setPhone(updateDTO.getPhone());
            }
            if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
                // Set the raw password - UserService will handle validation and encoding
            // updating password for user
                user.setPassword(updateDTO.getPassword());
            }

            // Use UserService to handle password validation and encoding
            User updatedUser = userService.updateUser(user);
            // user saved successfully
            
            return ResponseEntity.ok(new AuthResponse(
                null, // No token needed for profile update
                updatedUser.getEmail(),
                updatedUser.getFirstName() + " " + updatedUser.getLastName(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                updatedUser.getPhone(),
                updatedUser.getRole().name(),
                updatedUser.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update profile: " + e.getMessage());
        }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest passwordRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User user = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify current password
            try {
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(currentUserEmail, passwordRequest.getCurrentPassword())
                );
            } catch (AuthenticationException e) {
                return ResponseEntity.badRequest().body("Current password is incorrect");
            }

            // Update password using new password
            user.setPassword(passwordRequest.getNewPassword()); // validated and hashed by UserService
            userService.updateUser(user);
            
            return ResponseEntity.ok("Password changed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to change password: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String username = jwtService.extractUsername(token);
            User user = (User) userService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, user)) {
                return ResponseEntity.ok("Token is valid");
            } else {
                return ResponseEntity.badRequest().body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Token validation failed");
        }
    }
}
