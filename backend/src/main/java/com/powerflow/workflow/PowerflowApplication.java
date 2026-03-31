package com.powerflow.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PowerflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PowerflowApplication.class, args);
    }
}
