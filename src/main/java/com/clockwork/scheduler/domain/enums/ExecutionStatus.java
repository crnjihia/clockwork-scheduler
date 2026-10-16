package com.clockwork.scheduler.domain.enums;

public enum ExecutionStatus {
    SCHEDULED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    DEAD_LETTER
}
