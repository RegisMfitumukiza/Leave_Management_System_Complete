package com.daking.leave.repository;

import com.daking.leave.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStartDateGreaterThanEqualAndEndDateLessThanEqual(LocalDateTime start, LocalDateTime end);
}