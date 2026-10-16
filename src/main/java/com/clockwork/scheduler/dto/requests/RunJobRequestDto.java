package com.clockwork.scheduler.dto.requests;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record RunJobRequestDto(
        @NotBlank(message = "Job class is required")
        String jobClass,

        String payload,

        Instant scheduleAt
) {}
