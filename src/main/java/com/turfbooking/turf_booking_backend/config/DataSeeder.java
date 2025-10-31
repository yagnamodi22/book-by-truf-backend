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
            createOrUpdateUser(userRepository, passwordEncoder,
                    "john@example.com", "John", "Doe", User.Role.USER, "User@123", "+919876543210");

            createOrUpdateUser(userRepository, passwordEncoder,
                    "jane@example.com", "Jane", "Smith", User.Role.OWNER, "Owner@123", "+919876543211");

            createOrUpdateUser(userRepository, passwordEncoder,
                    "admin@example.com", "Admin", "User", User.Role.ADMIN, "Admin@123", "+919876543212");
        };
    }

    private void createOrUpdateUser(UserRepository repo, PasswordEncoder encoder,
                                    String email, String firstName, String lastName,
                                    User.Role role, String rawPassword, String phone) {

        email = email.toLowerCase().trim();

        User user = repo.findByEmail(email).orElse(null);
        boolean isNew = (user == null);

        if (isNew) {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setPassword(encoder.encode(rawPassword));
            user.setRole(role);
            repo.save(user);
            System.out.println("✅ Created default " + role + " user: " + email);
        } else {
            boolean updated = false;

            if (!user.getRole().equals(role)) {
                user.setRole(role);
                updated = true;
            }

            String currentPassword = user.getPassword();
            boolean hashed = currentPassword != null && (
                    currentPassword.startsWith("$2a$") ||
                    currentPassword.startsWith("$2b$") ||
                    currentPassword.startsWith("$2y$")
            );
            if (!hashed) {
                user.setPassword(encoder.encode(rawPassword));
                updated = true;
            }

            if (updated) {
                repo.save(user);
                System.out.println("🔄 Updated user: " + email);
            } else {
                System.out.println("✔️ User already up-to-date: " + email);
            }
        }
    }
}
