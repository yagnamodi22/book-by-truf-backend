package com.turfbooking.turf_booking_backend.dto;

import com.turfbooking.turf_booking_backend.entity.Booking;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingDetailsDTO {
    private Long id;
    private String turfName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String userName;
    private String userPhone;
    private String userEmail;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private String status;

    public BookingDetailsDTO() {
    }

    public BookingDetailsDTO(Booking booking) {
        this.id = booking.getId();
        this.bookingDate = booking.getBookingDate();
        this.startTime = booking.getStartTime();
        this.endTime = booking.getEndTime();
        this.totalAmount = booking.getTotalAmount();
        this.status = booking.getStatus().toString();
        
        if (booking.getTurf() != null) {
            this.turfName = booking.getTurf().getName();
        }
        
        if (booking.getUser() != null) {
            this.userName = booking.getUser().getFirstName() + " " + booking.getUser().getLastName();
            this.userPhone = booking.getUser().getPhone();
            this.userEmail = booking.getUser().getEmail();
        }
        
        if (booking.getPayment() != null) {
            this.paymentMethod = booking.getPayment().getPaymentMethod();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTurfName() {
        return turfName;
    }

    public void setTurfName(String turfName) {
        this.turfName = turfName;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}