package com.clockwork.scheduler.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record ScheduleJobRequestDto(
        @NotBlank(message = "Job name is required")
        String name,

        @NotBlank(message = "Cron expression is required")
        String cronExpression,

        @NotBlank(message = "Job class is required (e.g. MpesaReconciliationJob, BulkSmsJob, etc.)")
        String jobClass,

        String payload
) {}
