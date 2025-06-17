package com.daking.leave.repository;

import com.daking.leave.model.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
        List<Leave> findByUserId(Long userId);

        List<Leave> findByStatus(Leave.LeaveStatus status);

        List<Leave> findByApproverId(Long approverId);

        List<Leave> findByApproverIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(Long approverId,
                        LocalDate start, LocalDate end);

        List<Leave> findByUserIdAndStatus(Long userId, Leave.LeaveStatus status);

        List<Leave> findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(Long userId, LocalDate start,
                        LocalDate end);

        List<Leave> findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(Long departmentId,
                        LocalDate start, LocalDate end);

        List<Leave> findByLeaveTypeIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(Long leaveTypeId,
                        LocalDate start,
                        LocalDate end);

        long countByStatus(Leave.LeaveStatus status);

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType WHERE l.userId = :userId ORDER BY l.createdAt DESC")
        List<Leave> findByUserIdWithType(@Param("userId") Long userId);

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType WHERE l.userId IN :userIds")
        List<Leave> findByUserIdsWithType(@Param("userIds") List<Long> userIds);

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType WHERE l.status = :status")
        List<Leave> findByStatusWithType(@Param("status") Leave.LeaveStatus status);

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType")
        List<Leave> findAllWithType();

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType WHERE l.userId = :userId AND l.startDate >= :start AND l.endDate <= :end")
        List<Leave> findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                        @Param("userId") Long userId,
                        @Param("start") java.time.LocalDate start,
                        @Param("end") java.time.LocalDate end);

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType WHERE l.documentIds LIKE CONCAT('%', :documentId, '%')")
        List<Leave> findByDocumentIdWithType(@Param("documentId") String documentId);

        @Query("SELECT l FROM Leave l JOIN FETCH l.leaveType WHERE l.departmentId = :departmentId AND l.startDate >= :start AND l.endDate <= :end")
        List<Leave> findByDepartmentIdAndStartDateGreaterThanEqualAndEndDateLessThanEqualWithType(
                        @Param("departmentId") Long departmentId,
                        @Param("start") java.time.LocalDate start,
                        @Param("end") java.time.LocalDate end);
}