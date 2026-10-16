package com.clockwork.scheduler.service;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    private final JobExecutionRepository jobExecutionRepository;
    private final ScheduledJobRepository scheduledJobRepository;
    private final AlertingService alertingService;
    private final MeterRegistry meterRegistry;
    private final JobScheduler jobScheduler;

    public JobExecutionService(JobExecutionRepository jobExecutionRepository,
                               ScheduledJobRepository scheduledJobRepository,
                               AlertingService alertingService,
                               MeterRegistry meterRegistry,
                               @Lazy JobScheduler jobScheduler) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.scheduledJobRepository = scheduledJobRepository;
        this.alertingService = alertingService;
        this.meterRegistry = meterRegistry;
        this.jobScheduler = jobScheduler;
    }

    @Transactional
    public JobExecution createExecution(UUID scheduledJobId, String jobRunrId) {
        JobExecution execution = new JobExecution();
        execution.setId(UUID.randomUUID());
        execution.setJobId(scheduledJobId);
        execution.setJobRunrId(jobRunrId);
        execution.setStatus(ExecutionStatus.SCHEDULED);
        execution.setAttempt(1);
        execution.setStartedAt(Instant.now());

        meterRegistry.counter("jobs.enqueued").increment();
        return jobExecutionRepository.save(execution);
    }

    @Transactional
    public void markProcessing(UUID executionId, int attempt) {
        jobExecutionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(ExecutionStatus.PROCESSING);
            exec.setAttempt(attempt > 0 ? attempt : exec.getAttempt());
            if (exec.getStartedAt() == null) {
                exec.setStartedAt(Instant.now());
            }
            jobExecutionRepository.save(exec);
        });
    }

    @Transactional
    public void markSucceeded(UUID executionId, String resultSummary, String jobType) {
        jobExecutionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(ExecutionStatus.SUCCEEDED);
            exec.setFinishedAt(Instant.now());
            if (exec.getStartedAt() != null) {
                long duration = Duration.between(exec.getStartedAt(), exec.getFinishedAt()).toMillis();
                exec.setDurationMs(duration);

                // Record Micrometer duration timer
                Timer.builder("job.duration")
                        .tag("job_type", jobType != null ? jobType : "UNKNOWN")
                        .register(meterRegistry)
                        .record(duration, TimeUnit.MILLISECONDS);
            }
            exec.setResultSummary(resultSummary);
            jobExecutionRepository.save(exec);

            // Update parent ScheduledJob lastRunAt
            if (exec.getJobId() != null) {
                scheduledJobRepository.findById(exec.getJobId()).ifPresent(sj -> {
                    sj.setLastRunAt(Instant.now());
                    scheduledJobRepository.save(sj);
                });
            }

            meterRegistry.counter("jobs.succeeded").increment();
        });
    }

    @Transactional
    public void markFailed(UUID executionId, Throwable error, int attempt, int maxRetries, String jobType) {
        jobExecutionRepository.findById(executionId).ifPresent(exec -> {
            exec.setAttempt(attempt);
            exec.setFinishedAt(Instant.now());
            if (exec.getStartedAt() != null) {
                exec.setDurationMs(Duration.between(exec.getStartedAt(), exec.getFinishedAt()).toMillis());
            }

            String msg = error != null ? error.getMessage() : "Execution failed";
            exec.setErrorMessage(msg);

            StringWriter sw = new StringWriter();
            if (error != null) {
                error.printStackTrace(new PrintWriter(sw));
            }
            exec.setStackTrace(sw.toString());

            // If reached maximum retry threshold, move to DEAD_LETTER
            if (attempt >= maxRetries) {
                exec.setStatus(ExecutionStatus.DEAD_LETTER);
                jobExecutionRepository.save(exec);
                alertingService.alertDeadLetterJob(exec.getId(), exec.getJobRunrId(), jobType, msg);
            } else {
                exec.setStatus(ExecutionStatus.FAILED);
                jobExecutionRepository.save(exec);
                meterRegistry.counter("jobs.failed").increment();
            }
        });
    }

    @Transactional
    public JobExecution requeueExecution(UUID executionId) {
        JobExecution exec = jobExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        log.info("Requeuing dead letter job execution: {}", executionId);
        exec.setStatus(ExecutionStatus.SCHEDULED);
        exec.setAttempt(1);
        exec.setErrorMessage(null);
        exec.setStackTrace(null);
        exec.setStartedAt(Instant.now());
        exec.setFinishedAt(null);
        exec.setDurationMs(null);

        JobExecution saved = jobExecutionRepository.save(exec);

        meterRegistry.counter("jobs.enqueued").increment();
        return saved;
    }

    public record ExecutionDashboardStats(
            long totalJobs,
            long runningJobs,
            long succeededJobs,
            long failedJobs,
            long deadLetterJobs,
            double averageDurationMs
    ) {}

    public ExecutionDashboardStats getDashboardStats() {
        long total = jobExecutionRepository.count();
        long running = jobExecutionRepository.countByStatus(ExecutionStatus.PROCESSING);
        long succeeded = jobExecutionRepository.countByStatus(ExecutionStatus.SUCCEEDED);
        long failed = jobExecutionRepository.countByStatus(ExecutionStatus.FAILED);
        long deadLetter = jobExecutionRepository.countByStatus(ExecutionStatus.DEAD_LETTER);
        Double avg = jobExecutionRepository.getAverageDurationMs();

        return new ExecutionDashboardStats(
                total,
                running,
                succeeded,
                failed,
                deadLetter,
                avg != null ? avg : 0.0
        );
    }
}
