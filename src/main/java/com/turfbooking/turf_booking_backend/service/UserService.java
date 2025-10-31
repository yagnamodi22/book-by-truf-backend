package com.turfbooking.turf_booking_backend.service;

import com.turfbooking.turf_booking_backend.entity.User;
import com.turfbooking.turf_booking_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Load user by email for authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    // Create a new user
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        PasswordValidator.validateOrThrow(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Find user by email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Find user by ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Get all users (list)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // Get all users (paginated)
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // Update user by ID
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        if (userDetails.getFirstName() != null) user.setFirstName(userDetails.getFirstName());
        if (userDetails.getLastName() != null) user.setLastName(userDetails.getLastName());
        if (userDetails.getPhone() != null) user.setPhone(userDetails.getPhone());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            PasswordValidator.validateOrThrow(userDetails.getPassword());
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    // Update user directly (must include ID)
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new RuntimeException("User ID is required for update");
        }

        return updateUser(user.getId(), user);
    }

    // Delete single user by ID
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    // Delete multiple users by IDs
    public void deleteUsers(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            userRepository.deleteAllById(ids);
        }
    }

    // Find user by email and role (for admin/owner login)
    public Optional<User> findByEmailAndRole(String email, User.Role role) {
        return userRepository.findByEmailAndRole(email, role);
    }
}
