package com.daking.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.daking.leave.client")
@EnableScheduling
public class LeaveServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}