package com.daking.auth.config;

import com.daking.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2) // Run after DataInitializationConfig
public class StartupConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting application initialization...");

        // Fix users without department assignment
        try {
            int fixedCount = userService.fixUsersWithoutDepartment();
            if (fixedCount > 0) {
                logger.info("Fixed {} users without department assignment during startup", fixedCount);
            } else {
                logger.info("No users needed department assignment fix during startup");
            }
        } catch (Exception e) {
            logger.error("Error fixing users without department assignment during startup: {}", e.getMessage());
        }

        // Sync Google profile pictures
        try {
            int syncedCount = userService.syncGoogleProfilePictures();
            if (syncedCount > 0) {
                logger.info("Found {} users with Google IDs but no avatars during startup", syncedCount);
            } else {
                logger.info("No users needed profile picture sync during startup");
            }
        } catch (Exception e) {
            logger.error("Error syncing Google profile pictures during startup: {}", e.getMessage());
        }

        logger.info("Application initialization completed");
    }
}