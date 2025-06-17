package com.daking.leave.service.interfaces;

import com.daking.leave.dto.response.HolidayResponse;
import java.time.LocalDate;
import java.util.List;

public interface HolidayService {
    List<HolidayResponse> getAllHolidays();

    List<HolidayResponse> getHolidaysInRange(LocalDate start, LocalDate end);

    HolidayResponse addHoliday(HolidayResponse holiday);

    void deleteHoliday(Long id);

    void importRwandaPublicHolidays(int year);
}