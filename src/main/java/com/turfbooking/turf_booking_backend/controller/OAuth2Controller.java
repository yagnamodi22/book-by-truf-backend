package com.turfbooking.turf_booking_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2Controller {

    @GetMapping("/oauth2/authorization/google")
    public String googleLogin() {
        return "Redirecting to Google OAuth...";
    }
}
