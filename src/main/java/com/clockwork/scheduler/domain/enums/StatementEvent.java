package com.clockwork.scheduler.domain.enums;

public enum StatementEvent {
    START,
    GENERATE_SUCCESS,
    GENERATE_FAILURE,
    EMAIL_SUCCESS,
    EMAIL_FAILURE,
    ARCHIVE_SUCCESS,
    ARCHIVE_FAILURE,
    RETRY
}
