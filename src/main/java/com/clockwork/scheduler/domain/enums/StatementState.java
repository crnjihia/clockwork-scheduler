package com.clockwork.scheduler.domain.enums;

public enum StatementState {
    CREATED,
    GENERATING,
    EMAILING,
    ARCHIVING,
    COMPLETED,
    FAILED
}
