package com.daking.leave.repository;

import com.daking.leave.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByDateBetween(LocalDate start, LocalDate end);

    List<Holiday> findByIsPublicTrue();

    boolean existsByNameAndDate(String name, LocalDate date);
}