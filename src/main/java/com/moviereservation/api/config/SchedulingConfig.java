package com.moviereservation.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution.
 * 
 * This enables @Scheduled annotations to work.
 * Tasks will run in a separate thread pool.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // No additional configuration needed for basic scheduling
    // Spring Boot auto-configures a TaskScheduler
}