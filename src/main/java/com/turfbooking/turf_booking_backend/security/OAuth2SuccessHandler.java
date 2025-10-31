package com.turfbooking.turf_booking_backend.security;

import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.service.JwtService;
import com.turfbooking.turf_booking_backend.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauthUser.getAttributes();

        // ✅ Extract user details from Google response
        String email = Optional.ofNullable((String) attributes.get("email")).orElse("");
        String name = Optional.ofNullable((String) attributes.get("name")).orElse("");
        String picture = Optional.ofNullable((String) attributes.get("picture")).orElse("");
        String googleId = Optional.ofNullable((String) attributes.get("sub")).orElse("");

        // ✅ Split full name into first and last name (if possible)
        String firstName = name;
        String lastName = "";
        if (name != null && name.contains(" ")) {
            int idx = name.indexOf(" ");
            firstName = name.substring(0, idx);
            lastName = name.substring(idx + 1);
        }

        // ✅ Find user by email or Google ID
        User user = userService.findByEmail(email).orElseGet(() ->
                userService.findByGoogleId(googleId).orElse(null)
        );

        // ✅ Create new user if not found
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setGoogleId(googleId);
            user.setAvatar(picture);
            user.setPassword("GOOGLE_AUTH_USER"); // placeholder, not used
            user = userService.createUser(user);
        } else {
            // ✅ Update existing user info (if changed)
            boolean updated = false;
            if (!googleId.equals(user.getGoogleId())) {
                user.setGoogleId(googleId);
                updated = true;
            }
            if (!picture.equals(user.getAvatar())) {
                user.setAvatar(picture);
                updated = true;
            }
            if (updated) userService.updateUser(user);
        }

        // ✅ Generate JWT token
        String token = jwtService.generateToken(user);

        // ✅ Encode and redirect to frontend callback with token
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String redirectUrl = String.format("%s/auth/callback?token=%s", frontendUrl, encodedToken);
        response.sendRedirect(redirectUrl);
    }
}
