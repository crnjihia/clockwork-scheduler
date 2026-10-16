package com.clockwork.scheduler.dto.requests;

public record CreateWorkflowRequestDto(
        String name,
        String customerId,
        String email,
        String phone,
        String simulateFailureAtStep
) {}
