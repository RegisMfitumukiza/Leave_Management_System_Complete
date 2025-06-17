package com.daking.leave.service;

import com.daking.leave.model.Settings;
import com.daking.leave.repository.SettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final SettingsRepository settingsRepository;

    public Settings getSettings() {
        return settingsRepository.findAll().stream().findFirst().orElseGet(this::createDefaultSettings);
    }

    @Transactional
    public Settings updateSettings(Settings newSettings) {
        Settings current = getSettings();
        current.setAccrualRate(newSettings.getAccrualRate());
        current.setMaxCarryover(newSettings.getMaxCarryover());
        current.setCarryoverExpiryDate(newSettings.getCarryoverExpiryDate());
        current.setDocumentRequiredFor(newSettings.getDocumentRequiredFor());
        current.setApprovalWorkflow(newSettings.getApprovalWorkflow());
        current.setNotificationPreferences(newSettings.getNotificationPreferences());
        current.setHolidayCalendarSource(newSettings.getHolidayCalendarSource());
        return settingsRepository.save(current);
    }

    private Settings createDefaultSettings() {
        Settings defaults = Settings.builder()
                .accrualRate(1.66)
                .maxCarryover(5)
                .carryoverExpiryDate(LocalDate.of(LocalDate.now().getYear() + 1, 1, 31))
                .documentRequiredFor("")
                .approvalWorkflow("single")
                .notificationPreferences("email,in-app")
                .holidayCalendarSource("internal")
                .build();
        return settingsRepository.save(defaults);
    }
}