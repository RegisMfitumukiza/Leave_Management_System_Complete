package com.daking.leave.service.impl;

import com.daking.leave.dto.response.HolidayResponse;
import com.daking.leave.model.Holiday;
import com.daking.leave.repository.HolidayRepository;
import com.daking.leave.service.interfaces.HolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HolidayServiceImpl implements HolidayService {
    private final HolidayRepository holidayRepository;
    private static final Logger logger = LoggerFactory.getLogger(HolidayServiceImpl.class);

    public HolidayServiceImpl(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Override
    public List<HolidayResponse> getAllHolidays() {
        return holidayRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<HolidayResponse> getHolidaysInRange(LocalDate start, LocalDate end) {
        return holidayRepository.findByDateBetween(start, end).stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HolidayResponse addHoliday(HolidayResponse holiday) {
        Holiday entity = new Holiday();
        entity.setName(holiday.getName());
        entity.setDate(holiday.getDate());
        entity.setDescription(holiday.getDescription());
        entity.setPublic(holiday.isPublic());
        entity = holidayRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    @Override
    public void importRwandaPublicHolidays(int year) {
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/US";
        RestTemplate restTemplate = new RestTemplate();
        PublicHolidayDTO[] holidays = restTemplate.getForObject(url, PublicHolidayDTO[].class);
        logger.info("Fetched {} holidays from Nager.Date for year {}", holidays != null ? holidays.length : 0, year);
        int saved = 0;
        if (holidays != null) {
            for (PublicHolidayDTO dto : holidays) {
                if (!holidayRepository.existsByNameAndDate(dto.getLocalName(), LocalDate.parse(dto.getDate()))) {
                    Holiday holiday = new Holiday();
                    holiday.setName(dto.getLocalName());
                    holiday.setDate(LocalDate.parse(dto.getDate()));
                    holiday.setDescription(dto.getName());
                    holiday.setPublic(true);
                    holidayRepository.save(holiday);
                    saved++;
                }
            }
        }
        logger.info("Saved {} new holidays to the database.", saved);
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