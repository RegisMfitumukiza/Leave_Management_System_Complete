package com.daking.auth.repository;

import com.daking.auth.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    boolean existsByName(String name);

    List<Department> findByIsActiveTrue();

    @Query("SELECT d FROM Department d WHERE d.createdAt >= :startDate AND d.createdAt <= :endDate")
    List<Department> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM Department d WHERE d.manager.id = :managerId")
    List<Department> findByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.users WHERE d.manager.id = :managerId")
    List<Department> findByManagerIdWithUsers(@Param("managerId") Long managerId);
}