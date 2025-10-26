package com.turfbooking.turf_booking_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.turfbooking.turf_booking_backend.entity.SiteSetting;
import com.turfbooking.turf_booking_backend.repository.SiteSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SiteSettingService {

    // ✅ Logger added to track what’s happening
    private static final Logger log = LoggerFactory.getLogger(SiteSettingService.class);

    @Autowired
    private SiteSettingRepository siteSettingRepository;

    public List<SiteSetting> getAll() {
        return siteSettingRepository.findAll();
    }

    public Optional<SiteSetting> getByKey(String key) {
        return siteSettingRepository.findBySettingKey(key);
    }

    // ✅ Added logging inside upsert()
    public SiteSetting upsert(String key, String value, String type) {
        log.info("🔹 Upsert called with key={}, value={}, type={}", key, value, type);

        if (key == null || key.trim().isEmpty()) {
            log.error("❌ Cannot upsert setting with null or empty key");
            throw new IllegalArgumentException("Setting key cannot be null or empty");
        }

        try {
            SiteSetting setting = siteSettingRepository.findBySettingKey(key)
                    .orElseGet(SiteSetting::new);

            setting.setSettingKey(key);
            setting.setSettingValue(value == null ? "" : value);

            if (type != null && !type.isEmpty()) {
                setting.setSettingType(type);
            }

            // Ensure we flush to the database immediately
            SiteSetting saved = siteSettingRepository.saveAndFlush(setting);
            log.info("✅ Successfully saved site setting with key={}, id={}", key, saved.getId());
            return saved;

        } catch (Exception e) {
            log.error("❌ Error while upserting site setting (key={}): {}", key, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, String> getAsMap() {
        Map<String, String> map = new HashMap<>();
        for (SiteSetting s : siteSettingRepository.findAll()) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }
}
