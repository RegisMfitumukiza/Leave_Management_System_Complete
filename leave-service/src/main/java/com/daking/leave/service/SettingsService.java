package com.daking.leave.service;

import com.daking.leave.exception.ValidationException;
import com.daking.leave.model.LeaveType;
import com.daking.leave.model.NotificationType;
import com.daking.leave.model.Settings;
import com.daking.leave.repository.LeaveTypeRepository;
import com.daking.leave.repository.SettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {
    private final SettingsRepository settingsRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public Settings getSettings() {
        log.debug("Fetching application settings");
        try {
            Settings settings = settingsRepository.findAll().stream()
                    .findFirst()
                    .orElseGet(this::createDefaultSettings);
            log.debug("Settings retrieved successfully");
            return settings;
        } catch (Exception e) {
            log.error("Failed to fetch settings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch settings", e);
        }
    }

    @Transactional
    public Settings updateSettings(Settings newSettings) {
        log.info("Updating application settings");
        
        if (newSettings == null) {
            throw new ValidationException("Settings cannot be null");
        }
        
        try {
            Settings current = getSettings();
            
            // Validate and update accrual rate
            if (newSettings.getAccrualRate() > 0) {
                current.setAccrualRate(newSettings.getAccrualRate());
                log.debug("Updated accrual rate to: {}", newSettings.getAccrualRate());
            }
            
            // Validate and update max carryover
            if (newSettings.getMaxCarryover() >= 0) {
                current.setMaxCarryover(newSettings.getMaxCarryover());
                log.debug("Updated max carryover to: {}", newSettings.getMaxCarryover());
            }
            
            // Update carryover expiry date
            if (newSettings.getCarryoverExpiryDate() != null) {
                current.setCarryoverExpiryDate(newSettings.getCarryoverExpiryDate());
                log.debug("Updated carryover expiry date to: {}", newSettings.getCarryoverExpiryDate());
            }
            
            // Update document required for leave types
            if (newSettings.getDocumentRequiredFor() != null) {
                current.setDocumentRequiredFor(newSettings.getDocumentRequiredFor());
                log.debug("Updated document required for {} leave types", 
                         newSettings.getDocumentRequiredFor().size());
            }
            
            // Update approval workflow
            if (StringUtils.hasText(newSettings.getApprovalWorkflow())) {
                validateApprovalWorkflow(newSettings.getApprovalWorkflow());
                current.setApprovalWorkflow(newSettings.getApprovalWorkflow());
                log.debug("Updated approval workflow to: {}", newSettings.getApprovalWorkflow());
            }
            
            // Update notification preferences
            if (newSettings.getNotificationPreferences() != null) {
                current.setNotificationPreferences(newSettings.getNotificationPreferences());
                log.debug("Updated notification preferences to: {}", 
                         newSettings.getNotificationPreferences());
            }
            
            // Update holiday calendar source
            if (StringUtils.hasText(newSettings.getHolidayCalendarSource())) {
                validateHolidayCalendarSource(newSettings.getHolidayCalendarSource());
                current.setHolidayCalendarSource(newSettings.getHolidayCalendarSource());
                log.debug("Updated holiday calendar source to: {}", newSettings.getHolidayCalendarSource());
            }
            
            Settings savedSettings = settingsRepository.save(current);
            log.info("Settings updated successfully");
            return savedSettings;
        } catch (ValidationException e) {
            log.warn("Validation error while updating settings: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to update settings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update settings", e);
        }
    }

    private Settings createDefaultSettings() {
        log.info("Creating default settings");
        try {
            // Get all available leave types for default document requirement
            Set<LeaveType> allLeaveTypes = new HashSet<>(leaveTypeRepository.findAll());
            
            // Create default notification preferences
            Set<NotificationType> defaultNotifications = new HashSet<>();
            defaultNotifications.add(NotificationType.LEAVE_APPLICATION);
            defaultNotifications.add(NotificationType.LEAVE_STATUS);
            
            Settings defaults = Settings.builder()
                    .accrualRate(1.66)
                    .maxCarryover(5)
                    .carryoverExpiryDate(LocalDate.of(LocalDate.now().getYear() + 1, 1, 31))
                    .documentRequiredFor(allLeaveTypes) // All leave types require documents by default
                    .approvalWorkflow("single")
                    .notificationPreferences(defaultNotifications)
                    .holidayCalendarSource("internal")
                    .build();
            
            Settings savedSettings = settingsRepository.save(defaults);
            log.info("Default settings created successfully");
            return savedSettings;
        } catch (Exception e) {
            log.error("Failed to create default settings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create default settings", e);
        }
    }

    private void validateApprovalWorkflow(String workflow) {
        if (!"single".equals(workflow) && !"multi-level".equals(workflow)) {
            throw new ValidationException("Approval workflow must be 'single' or 'multi-level'");
        }
    }

    private void validateHolidayCalendarSource(String source) {
        if (!"internal".equals(source) && !"external".equals(source)) {
            throw new ValidationException("Holiday calendar source must be 'internal' or 'external'");
        }
    }

    // Utility methods for managing document requirements
    public void addDocumentRequirement(Long leaveTypeId) {
        log.debug("Adding document requirement for leave type: {}", leaveTypeId);
        
        if (leaveTypeId == null || leaveTypeId <= 0) {
            throw new ValidationException("Leave type ID must be a positive number");
        }
        
        try {
            LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                    .orElseThrow(() -> new ValidationException("Leave type not found with ID: " + leaveTypeId));
            
            Settings settings = getSettings();
            settings.getDocumentRequiredFor().add(leaveType);
            settingsRepository.save(settings);
            
            log.info("Document requirement added for leave type: {}", leaveType.getName());
        } catch (ValidationException e) {
            log.warn("Validation error adding document requirement: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to add document requirement for leave type {}: {}", leaveTypeId, e.getMessage(), e);
            throw new RuntimeException("Failed to add document requirement", e);
        }
    }

    public void removeDocumentRequirement(Long leaveTypeId) {
        log.debug("Removing document requirement for leave type: {}", leaveTypeId);
        
        if (leaveTypeId == null || leaveTypeId <= 0) {
            throw new ValidationException("Leave type ID must be a positive number");
        }
        
        try {
            LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                    .orElseThrow(() -> new ValidationException("Leave type not found with ID: " + leaveTypeId));
            
            Settings settings = getSettings();
            settings.getDocumentRequiredFor().remove(leaveType);
            settingsRepository.save(settings);
            
            log.info("Document requirement removed for leave type: {}", leaveType.getName());
        } catch (ValidationException e) {
            log.warn("Validation error removing document requirement: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove document requirement for leave type {}: {}", leaveTypeId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove document requirement", e);
        }
    }

    // Utility methods for managing notification preferences
    public void addNotificationPreference(NotificationType notificationType) {
        log.debug("Adding notification preference: {}", notificationType);
        
        if (notificationType == null) {
            throw new ValidationException("Notification type cannot be null");
        }
        
        try {
            Settings settings = getSettings();
            settings.getNotificationPreferences().add(notificationType);
            settingsRepository.save(settings);
            
            log.info("Notification preference added: {}", notificationType);
        } catch (Exception e) {
            log.error("Failed to add notification preference {}: {}", notificationType, e.getMessage(), e);
            throw new RuntimeException("Failed to add notification preference", e);
        }
    }

    public void removeNotificationPreference(NotificationType notificationType) {
        log.debug("Removing notification preference: {}", notificationType);
        
        if (notificationType == null) {
            throw new ValidationException("Notification type cannot be null");
        }
        
        try {
            Settings settings = getSettings();
            settings.getNotificationPreferences().remove(notificationType);
            settingsRepository.save(settings);
            
            log.info("Notification preference removed: {}", notificationType);
        } catch (Exception e) {
            log.error("Failed to remove notification preference {}: {}", notificationType, e.getMessage(), e);
            throw new RuntimeException("Failed to remove notification preference", e);
        }
    }
} 