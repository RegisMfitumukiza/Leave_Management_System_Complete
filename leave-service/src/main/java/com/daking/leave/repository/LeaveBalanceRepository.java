package com.daking.leave.repository;

import com.daking.leave.model.LeaveBalance;
import com.daking.leave.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserId(Long userId);

    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);

    Optional<LeaveBalance> findByUserIdAndLeaveTypeAndYear(Long userId, LeaveType leaveType, Integer year);

    List<LeaveBalance> findByYear(Integer year);

    @Query("SELECT b FROM LeaveBalance b JOIN FETCH b.leaveType WHERE b.userId = :userId")
    List<LeaveBalance> findByUserIdWithType(@Param("userId") Long userId);

    @Query("SELECT b FROM LeaveBalance b JOIN FETCH b.leaveType")
    List<LeaveBalance> findAllWithType();
}