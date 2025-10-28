package com.turfbooking.turf_booking_backend.controller;

import com.turfbooking.turf_booking_backend.dto.AuthRequest;
import com.turfbooking.turf_booking_backend.dto.AuthResponse;
import com.turfbooking.turf_booking_backend.dto.UserRegistrationDTO;
import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.service.JwtService;
import com.turfbooking.turf_booking_backend.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(
    origins = {
        "https://frontend-bookmytruf.vercel.app",
        "https://frontend-bookmytruf-git-main-yagnamodi22s-projects.vercel.app",
        "http://localhost:5173",
        "http://localhost:3000"
    },
    allowCredentials = "true"
)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO, HttpServletResponse response) {
        try {
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

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None") // ✅ Important for cross-domain cookies
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

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
            return ResponseEntity.badRequest().body("❌ Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Registration failed: " + e.getMessage());
        }
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );

            User user = userService.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

            String token = jwtService.generateToken(user);

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None") // ✅ allows frontend & backend from different domains
                    .path("/")
                    .maxAge(24 * 60 * 60)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

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
            return ResponseEntity.badRequest().body("❌ Invalid credentials");
        }
    }

    // ✅ GET PROFILE
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(new AuthResponse(
                null,
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to get profile: " + e.getMessage());
        }
    }

    // ✅ UPDATE PROFILE
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserRegistrationDTO updateDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (updateDTO.getFirstName() != null) user.setFirstName(updateDTO.getFirstName());
            if (updateDTO.getLastName() != null) user.setLastName(updateDTO.getLastName());
            if (updateDTO.getPhone() != null) user.setPhone(updateDTO.getPhone());
            if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty())
                user.setPassword(updateDTO.getPassword());

            User updatedUser = userService.updateUser(user);

            return ResponseEntity.ok(new AuthResponse(
                null,
                updatedUser.getEmail(),
                updatedUser.getFirstName() + " " + updatedUser.getLastName(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                updatedUser.getPhone(),
                updatedUser.getRole().name(),
                updatedUser.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to update profile: " + e.getMessage());
        }
    }

    // ✅ CHANGE PASSWORD
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

            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(currentUserEmail, passwordRequest.getCurrentPassword())
            );

            user.setPassword(passwordRequest.getNewPassword());
            userService.updateUser(user);

            return ResponseEntity.ok("✅ Password changed successfully");
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body("❌ Current password is incorrect");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to change password: " + e.getMessage());
        }
    }

    // ✅ VALIDATE TOKEN
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token, HttpServletResponse response) {
        try {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String username = jwtService.extractUsername(token);
            User user = (User) userService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, user)) {
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "user", Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                    )
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Invalid token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "valid", false,
                "message", "Token validation failed: " + e.getMessage()
            ));
        }
    }

    // ✅ LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            SecurityContextHolder.clearContext();

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("jwt") || cookie.getName().equals("refreshToken")) {
                        cookie.setValue("");
                        cookie.setPath("/");
                        cookie.setMaxAge(0);
                        cookie.setHttpOnly(true);
                        cookie.setSecure(true);
                        response.addCookie(cookie);
                    }
                }
            }

            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Logged out successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "❌ Logout failed: " + e.getMessage()
            ));
        }
    }
}
