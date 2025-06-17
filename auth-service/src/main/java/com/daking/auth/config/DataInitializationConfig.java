package com.daking.auth.config;

import com.daking.auth.model.Department;
import com.daking.auth.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1) // Run before StartupConfig
public class DataInitializationConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationConfig.class);

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing default data...");
        
        // Ensure at least one department exists
        if (departmentRepository.count() == 0) {
            logger.info("No departments found. Creating default department...");
            
            Department defaultDepartment = new Department();
            defaultDepartment.setName("General Department");
            defaultDepartment.setDescription("Default department for all users");
            defaultDepartment.setIsActive(true);
            defaultDepartment.setCreatedAt(LocalDateTime.now());
            defaultDepartment.setUpdatedAt(LocalDateTime.now());
            
            Department savedDepartment = departmentRepository.save(defaultDepartment);
            logger.info("Created default department: {} (ID: {})", savedDepartment.getName(), savedDepartment.getId());
        } else {
            logger.info("Found {} existing departments", departmentRepository.count());
        }
        
        logger.info("Data initialization completed");
    }
} 