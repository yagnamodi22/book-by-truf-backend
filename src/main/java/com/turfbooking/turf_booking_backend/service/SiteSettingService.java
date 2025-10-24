package com.turfbooking.turf_booking_backend.service;

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

    @Autowired
    private SiteSettingRepository siteSettingRepository;

    public List<SiteSetting> getAll() {
        return siteSettingRepository.findAll();
    }

    public Optional<SiteSetting> getByKey(String key) {
        return siteSettingRepository.findBySettingKey(key);
    }

    public SiteSetting upsert(String key, String value, String type) {
        SiteSetting setting = siteSettingRepository.findBySettingKey(key)
                .orElseGet(SiteSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value == null ? "" : value);
        if (type != null && !type.isEmpty()) {
            setting.setSettingType(type);
        }
        return siteSettingRepository.save(setting);
    }

    public Map<String, String> getAsMap() {
        Map<String, String> map = new HashMap<>();
        for (SiteSetting s : siteSettingRepository.findAll()) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }
}


