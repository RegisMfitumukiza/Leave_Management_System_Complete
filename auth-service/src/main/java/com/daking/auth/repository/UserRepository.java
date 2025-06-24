package com.daking.auth.repository;

import com.daking.auth.model.User;
import com.daking.auth.model.Department;
import com.daking.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    List<User> findByActiveTrue();

    List<User> findByDepartment(Department department);

    List<User> findByDepartmentId(Long departmentId);

    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);

    List<User> findByOnLeaveTrue();

    List<User> findByPendingApprovalsTrue();

    @Query("SELECT u FROM User u WHERE u.lastLogin < :beforeDate AND u.active = true")
    List<User> findInactiveUsers(@Param("beforeDate") LocalDateTime beforeDate);

    List<User> findByManagerId(Long managerId);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.departmentId = :departmentId AND u.active = true")
    List<User> findActiveUsersByRoleAndDepartment(@Param("role") Role role, @Param("departmentId") Long departmentId);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);
}