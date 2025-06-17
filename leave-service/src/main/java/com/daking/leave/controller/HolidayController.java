package com.daking.leave.controller;

import com.daking.leave.dto.response.HolidayResponse;
import com.daking.leave.service.interfaces.HolidayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService holidayService;
    private static final Logger logger = LoggerFactory.getLogger(HolidayController.class);

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getAllHolidays() {
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    @GetMapping("/range")
    public ResponseEntity<List<HolidayResponse>> getHolidaysInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(holidayService.getHolidaysInRange(start, end));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponse> addHoliday(@RequestBody HolidayResponse holiday) {
        return ResponseEntity.ok(holidayService.addHoliday(holiday));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-public-holidays")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importRwandaPublicHolidays(@RequestParam int year) {
        logger.info("Received request to import Rwanda public holidays for year {}", year);
        holidayService.importRwandaPublicHolidays(year);
        return ResponseEntity.ok("Imported Rwanda public holidays for " + year);
    }
}