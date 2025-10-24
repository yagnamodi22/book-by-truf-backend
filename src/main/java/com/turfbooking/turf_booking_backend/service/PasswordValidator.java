package com.turfbooking.turf_booking_backend.service;

import java.util.regex.Pattern;

public final class PasswordValidator {
    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"
    );

    private PasswordValidator() {}

    public static void validateOrThrow(String password) {
        if (password == null || !POLICY.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character."
            );
        }
    }
}


