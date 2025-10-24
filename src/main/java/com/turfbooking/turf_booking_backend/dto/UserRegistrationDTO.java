package com.turfbooking.turf_booking_backend.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.turfbooking.turf_booking_backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegistrationDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phone;

    private User.Role role;

    // Constructors, getters, and setters
    public UserRegistrationDTO() {}

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public User.Role getRole() { return role; }
    
    public void setRole(User.Role role) { 
        this.role = role; 
    }
    
    // Add a setter that accepts String for JSON deserialization
    @JsonSetter("role")
    public void setRoleFromString(String roleString) {
        if (roleString != null && !roleString.trim().isEmpty()) {
            try {
                this.role = User.Role.valueOf(roleString.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                // Default to USER if invalid role
                this.role = User.Role.USER;
            }
        } else {
            this.role = User.Role.USER;
        }
    }
}
