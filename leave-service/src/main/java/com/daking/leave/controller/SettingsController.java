package com.daking.leave.controller;

import com.daking.leave.model.Settings;
import com.daking.leave.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {
    private final SettingsService settingsService;

    /**
     * Get the current application settings
     * 
     * @return Settings object
     */
    @GetMapping
    public ResponseEntity<Settings> getSettings() {
        log.info("Fetching application settings");
        try {
            Settings settings = settingsService.getSettings();
            log.debug("Settings fetched successfully");
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Failed to fetch settings: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Update application settings (admin only)
     * 
     * @param settings new settings
     * @return updated Settings object
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Settings> updateSettings(@Valid @RequestBody Settings settings) {
        log.info("Updating application settings");
        try {
            Settings updated = settingsService.updateSettings(settings);
            log.debug("Settings updated successfully");
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update settings: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}