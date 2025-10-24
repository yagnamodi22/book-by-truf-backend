package com.turfbooking.turf_booking_backend.config;

import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            upsertUser(userRepository, passwordEncoder,
                    "john@example.com", "John", "Doe", "USER", "User@123", "+919876543210");

            upsertUser(userRepository, passwordEncoder,
                    "jane@example.com", "Jane", "Smith", "OWNER", "Owner@123", "+919876543211");

            upsertUser(userRepository, passwordEncoder,
                    "admin@example.com", "Admin", "User", "ADMIN", "Admin@123", "+919876543212");
        };
    }

    private void upsertUser(UserRepository repo, PasswordEncoder encoder,
                            String email, String firstName, String lastName,
                            String role, String rawPassword, String phone) {
        User user = repo.findByEmail(email).orElseGet(User::new);

        boolean isNew = user.getId() == null;
        boolean changed = false;

        if (isNew || user.getEmail() == null || !user.getEmail().equals(email)) {
            user.setEmail(email);
            changed = true;
        }
        if (isNew || user.getFirstName() == null || !user.getFirstName().equals(firstName)) {
            user.setFirstName(firstName);
            changed = true;
        }
        if (isNew || user.getLastName() == null || !user.getLastName().equals(lastName)) {
            user.setLastName(lastName);
            changed = true;
        }
        if (isNew || user.getPhone() == null || !user.getPhone().equals(phone)) {
            user.setPhone(phone);
            changed = true;
        }
        try {
            User.Role targetRole = User.Role.valueOf(role);
            if (isNew || user.getRole() == null || !user.getRole().equals(targetRole)) {
                user.setRole(targetRole);
                changed = true;
            }
        } catch (IllegalArgumentException ignored) {
            // keep default
        }

        String current = user.getPassword();
        boolean looksHashed = current != null && (current.startsWith("$2a$") || current.startsWith("$2b$") || current.startsWith("$2y$"));
        if (!looksHashed) {
            user.setPassword(encoder.encode(rawPassword));
            changed = true;
        }

        if (isNew || changed) {
            repo.save(user);
        }
    }
}


