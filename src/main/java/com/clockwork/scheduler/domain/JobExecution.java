package com.clockwork.scheduler.domain;

import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
public class JobExecution {

    @Id
    private UUID id;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "job_runr_id", length = 100)
    private String jobRunrId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExecutionStatus status;

    @Column(nullable = false)
    private int attempt = 1;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    public JobExecution() {
    }

    public JobExecution(UUID id, UUID jobId, String jobRunrId, ExecutionStatus status, int attempt,
                        Instant startedAt, Instant finishedAt, Long durationMs,
                        String errorMessage, String stackTrace, String resultSummary) {
        this.id = id != null ? id : UUID.randomUUID();
        this.jobId = jobId;
        this.jobRunrId = jobRunrId;
        this.status = status != null ? status : ExecutionStatus.SCHEDULED;
        this.attempt = attempt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.resultSummary = resultSummary;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = ExecutionStatus.SCHEDULED;
        }
        if (this.attempt <= 0) {
            this.attempt = 1;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getJobRunrId() {
        return jobRunrId;
    }

    public void setJobRunrId(String jobRunrId) {
        this.jobRunrId = jobRunrId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }
}
