package com.turfbooking.turf_booking_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add no-cache headers to all responses to prevent browser caching
 * of sensitive data and pages.
 */
public class NoCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Add no-cache headers to all responses
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Surrogate-Control", "no-store");
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}