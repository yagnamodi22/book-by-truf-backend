package com.turfbooking.turf_booking_backend.service;

import com.turfbooking.turf_booking_backend.entity.Turf;
import com.turfbooking.turf_booking_backend.repository.TurfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TurfService {

    @Autowired
    private TurfRepository turfRepository;

    public Turf createTurf(Turf turf) {
        return turfRepository.save(turf);
    }

    public Optional<Turf> findById(Long id) {
        return turfRepository.findById(id);
    }

    public List<Turf> findAllActiveTurfs() {
        return turfRepository.findByIsActive(true);
    }

    public Page<Turf> findAllActiveTurfs(Pageable pageable) {
        return turfRepository.findByIsActive(true, pageable);
    }

    public List<Turf> findAllInactiveTurfs() {
        List<Turf> inactiveTurfs = turfRepository.findByIsActive(false);
        System.out.println("TurfService: Found " + inactiveTurfs.size() + " inactive turfs");
        return inactiveTurfs;
    }

    public List<Turf> findTurfsByLocation(String location) {
        return turfRepository.findByLocationContainingIgnoreCase(location);
    }

    public Page<Turf> findTurfsByLocationAndPrice(String location, BigDecimal minPrice,
                                                  BigDecimal maxPrice, Pageable pageable) {
        return turfRepository.findTurfsByLocationAndPriceRange(location, minPrice, maxPrice, pageable);
    }

    public List<Turf> findTurfsByOwner(Long ownerId) {
        return turfRepository.findByOwnerId(ownerId);
    }

    public Turf updateTurf(Long id, Turf turfDetails) {
        Turf turf = turfRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        turf.setName(turfDetails.getName());
        turf.setDescription(turfDetails.getDescription());
        turf.setLocation(turfDetails.getLocation());
        turf.setPricePerHour(turfDetails.getPricePerHour());
        turf.setAmenities(turfDetails.getAmenities());
        turf.setImages(turfDetails.getImages());

        return turfRepository.save(turf);
    }

    public void deleteTurf(Long id) {
        Turf turf = turfRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turf not found"));
        turfRepository.delete(turf);
    }

    public void permanentlyDeleteTurf(Long id) {
        if (!turfRepository.existsById(id)) {
            throw new RuntimeException("Turf not found");
        }
        turfRepository.deleteById(id);
    }

    public Turf approveTurf(Long id) {
        Turf turf = turfRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turf not found"));
        turf.setIsActive(true);
        return turfRepository.save(turf);
    }

    // Helper method to validate and process images
    public String[] processImages(String[] imageArray) {
        if (imageArray == null || imageArray.length == 0) {
            return new String[0];
        }
        
        // Limit to 5 images maximum
        int maxImages = Math.min(imageArray.length, 5);
        String[] processedImages = new String[maxImages];
        
        for (int i = 0; i < maxImages; i++) {
            String image = imageArray[i];
            if (image != null && !image.trim().isEmpty()) {
                processedImages[i] = image.trim();
            }
        }
        
        return processedImages;
    }
}
