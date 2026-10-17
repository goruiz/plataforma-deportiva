package com.platform.backend.shared.infraestructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs (see {@code MatchAutoFinishJob}). */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
