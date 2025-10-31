package com.turfbooking.turf_booking_backend.repository;

import com.turfbooking.turf_booking_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if user exists by email
    boolean existsByEmail(String email);

    // Find user by email and role (for admin or owner login)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.role = :role")
    Optional<User> findByEmailAndRole(@Param("email") String email, @Param("role") User.Role role);
}
