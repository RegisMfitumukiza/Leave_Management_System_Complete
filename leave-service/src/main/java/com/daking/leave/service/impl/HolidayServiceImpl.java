package com.daking.leave.service.impl;

import com.daking.leave.dto.response.HolidayResponse;
import com.daking.leave.model.Holiday;
import com.daking.leave.repository.HolidayRepository;
import com.daking.leave.service.interfaces.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {
    private final HolidayRepository holidayRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<HolidayResponse> getAllHolidays() {
        List<Holiday> holidays = holidayRepository.findAll();
        log.debug("Retrieved {} holidays", holidays.size());
        return holidays.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<HolidayResponse> getHolidaysInRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<Holiday> holidays = holidayRepository.findByDateBetween(start, end);
        log.debug("Retrieved {} holidays between {} and {}", holidays.size(), start, end);
        return holidays.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HolidayResponse addHoliday(HolidayResponse holiday) {
        log.info("Adding new holiday: {} on {}", holiday.getName(), holiday.getDate());

        // Validate holiday request
        validateHolidayRequest(holiday);

        // Check for duplicate holiday on the same date
        if (holidayRepository.existsByNameAndDate(holiday.getName(), holiday.getDate())) {
            throw new IllegalArgumentException(
                    "Holiday '" + holiday.getName() + "' already exists on " + holiday.getDate());
        }

        Holiday entity = new Holiday();
        entity.setName(holiday.getName());
        entity.setDate(holiday.getDate());
        entity.setDescription(holiday.getDescription());
        entity.setPublic(holiday.isPublic());

        entity = holidayRepository.save(entity);
        log.info("Successfully added holiday with ID: {}", entity.getId());
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteHoliday(Long id) {
        log.info("Attempting to delete holiday with ID: {}", id);

        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException("Holiday not found with ID: " + id);
        }

        holidayRepository.deleteById(id);
        log.info("Successfully deleted holiday with ID: {}", id);
    }

    @Override
    @Transactional
    public void importUSPublicHolidays(int year) {
        log.info("Starting import of US public holidays for year: {}", year);

        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100");
        }

        String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/US";

        try {
            PublicHolidayDTO[] holidays = restTemplate.getForObject(url, PublicHolidayDTO[].class);

            if (holidays == null) {
                log.warn("No holidays returned from API for year: {}", year);
                return;
            }

            log.info("Fetched {} holidays from Nager.Date API for year {}", holidays.length, year);

            int saved = 0;
            int skipped = 0;

            for (PublicHolidayDTO dto : holidays) {
                try {
                    LocalDate holidayDate = LocalDate.parse(dto.getDate());

                    // Check if holiday already exists
                    if (!holidayRepository.existsByNameAndDate(dto.getLocalName(), holidayDate)) {
                        Holiday holiday = new Holiday();
                        holiday.setName(dto.getLocalName());
                        holiday.setDate(holidayDate);
                        holiday.setDescription(dto.getName());
                        holiday.setPublic(true);

                        holidayRepository.save(holiday);
                        saved++;
                        log.debug("Saved holiday: {} on {}", dto.getLocalName(), holidayDate);
                    } else {
                        skipped++;
                        log.debug("Skipped duplicate holiday: {} on {}", dto.getLocalName(), holidayDate);
                    }
                } catch (Exception e) {
                    log.warn("Failed to process holiday {}: {}", dto.getLocalName(), e.getMessage());
                }
            }

            log.info("Import completed. Saved: {}, Skipped: {}", saved, skipped);

        } catch (RestClientException e) {
            log.error("Failed to fetch holidays from API for year {}: {}", year, e.getMessage(), e);
            throw new RuntimeException("Failed to import holidays from external API", e);
        } catch (Exception e) {
            log.error("Unexpected error during holiday import for year {}: {}", year, e.getMessage(), e);
            throw new RuntimeException("Failed to import holidays", e);
        }
    }

    private void validateHolidayRequest(HolidayResponse holiday) {
        if (holiday == null) {
            throw new IllegalArgumentException("Holiday request cannot be null");
        }

        if (holiday.getName() == null || holiday.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Holiday name cannot be null or empty");
        }

        if (holiday.getDate() == null) {
            throw new IllegalArgumentException("Holiday date cannot be null");
        }

        // Check if date is not in the past (optional validation)
        if (holiday.getDate().isBefore(LocalDate.now())) {
            log.warn("Adding holiday in the past: {} on {}", holiday.getName(), holiday.getDate());
        }

        // Check if date is not too far in the future (optional validation)
        if (holiday.getDate().isAfter(LocalDate.now().plusYears(10))) {
            log.warn("Adding holiday far in the future: {} on {}", holiday.getName(), holiday.getDate());
        }
    }

    private HolidayResponse toResponse(Holiday holiday) {
        HolidayResponse resp = new HolidayResponse();
        resp.setId(holiday.getId());
        resp.setName(holiday.getName());
        resp.setDate(holiday.getDate());
        resp.setDescription(holiday.getDescription());
        resp.setPublic(holiday.isPublic());
        return resp;
    }
}

// DTO for public holiday import
class PublicHolidayDTO {
    private String date;
    private String localName;
    private String name;

    public String getDate() {
        return date;
    }

    public String getLocalName() {
        return localName;
    }

    public String getName() {
        return name;
    }
}