package com.turfbooking.turf_booking_backend.controller;

import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.service.JwtService;
import com.turfbooking.turf_booking_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/auth/google")
@CrossOrigin(
    origins = {
        "https://frontend-bookmytruf.vercel.app",
        "https://frontend-bookmytruf-git-main-yagnamodi22s-projects.vercel.app",
        "http://localhost:5173",
        "http://localhost:3000"
    },
    allowCredentials = "true"
)
public class GoogleAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/callback")
    public ResponseEntity<?> googleCallback(@RequestParam("code") String code) {
        try {
            // ✅ Exchange code for Google user info
            String googleApiUrl = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + code;
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> googleUser = restTemplate.getForObject(googleApiUrl, Map.class);

            if (googleUser == null || googleUser.get("email") == null) {
                return ResponseEntity.badRequest().body("❌ Failed to get Google user info");
            }

            String email = googleUser.get("email").toString();
            String firstName = googleUser.get("given_name").toString();
            String lastName = googleUser.get("family_name") != null ? googleUser.get("family_name").toString() : "";

            // ✅ Create new user if not exists
            User user = userService.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setPassword("GOOGLE_LOGIN"); // dummy password (not used)
                newUser.setRole(User.Role.USER);
                return userService.createUser(newUser);
            });

            // ✅ Generate JWT token
            String token = jwtService.generateToken(user);

            // ✅ Redirect to frontend with token
            String redirectUrl = "https://frontend-bookmytruf.vercel.app/oauth2/callback?token=" + token;
            return ResponseEntity.status(302).header("Location", redirectUrl).build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Google login failed: " + e.getMessage());
        }
    }
}
