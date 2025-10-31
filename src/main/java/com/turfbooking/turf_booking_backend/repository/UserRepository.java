package com.turfbooking.turf_booking_backend.repository;

import com.turfbooking.turf_booking_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.role = :role")
    Optional<User> findByEmailAndRole(@Param("email") String email, @Param("role") User.Role role);

    // ✅ Step 2 — Add Google OAuth support
    @Query("SELECT u FROM User u WHERE u.googleId = :googleId")
    Optional<User> findByGoogleId(@Param("googleId") String googleId);

    boolean existsByGoogleId(String googleId);
}
