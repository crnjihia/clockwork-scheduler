package com.clockwork.scheduler.service;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.enums.JobStatus;
import com.clockwork.scheduler.domain.enums.JobType;
import com.clockwork.scheduler.jobs.requests.*;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulingService.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobExecutionService jobExecutionService;
    private final JobRequestScheduler jobRequestScheduler;
    private final ObjectMapper objectMapper;

    public JobSchedulingService(ScheduledJobRepository scheduledJobRepository,
                                JobExecutionService jobExecutionService,
                                JobRequestScheduler jobRequestScheduler,
                                ObjectMapper objectMapper) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.jobExecutionService = jobExecutionService;
        this.jobRequestScheduler = jobRequestScheduler;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncActiveJobsOnStartup() {
        log.info("Clockwork Scheduler: Syncing active recurring jobs with JobRunr...");
        List<ScheduledJob> activeJobs = scheduledJobRepository.findByStatus(JobStatus.ACTIVE);
        for (ScheduledJob job : activeJobs) {
            if (job.getType() == JobType.RECURRING && job.getCronExpression() != null) {
                try {
                    registerRecurringJob(job);
                } catch (Exception e) {
                    log.error("Failed to register recurring job {} [{}]: {}", job.getName(), job.getId(), e.getMessage());
                }
            }
        }
        log.info("Startup recurring job synchronization completed. Total active synced: {}", activeJobs.size());
    }

    public void registerRecurringJob(ScheduledJob job) {
        String recurringId = job.getId().toString();
        String cron = job.getCronExpression();

        // Convert 6-field quartz cron if needed or validate
        String normalizedCron = normalizeCronForJobRunr(cron);
        JobRequest jobRequest = buildJobRequest(job.getJobClass(), job.getPayload(), job.getId(), null);

        log.info("Registering recurring job with JobRunr: id='{}', name='{}', cron='{}'",
                recurringId, job.getName(), normalizedCron);

        jobRequestScheduler.scheduleRecurrently(recurringId, normalizedCron, jobRequest);

        // Update next run at
        try {
            CronExpression springCron = CronExpression.parse(cron);
            ZonedDateTime next = springCron.next(ZonedDateTime.now(ZoneId.of("Africa/Nairobi")));
            if (next != null) {
                job.setNextRunAt(next.toInstant());
                scheduledJobRepository.save(job);
            }
        } catch (Exception e) {
            log.debug("Could not compute next run time for cron {}: {}", cron, e.getMessage());
        }
    }

    @Transactional
    public ScheduledJob updateJobCron(UUID jobId, String newCron) {
        ScheduledJob job = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ScheduledJob not found: " + jobId));

        // Validate cron expression
        if (!CronExpression.isValidExpression(newCron)) {
            throw new IllegalArgumentException("Invalid cron expression: " + newCron);
        }

        log.info("Updating cron for job {} to '{}'", jobId, newCron);

        // Unregister existing recurring schedule in JobRunr
        try {
            jobRequestScheduler.deleteRecurringJob(job.getId().toString());
        } catch (Exception e) {
            log.warn("Could not unregister recurring job {}: {}", jobId, e.getMessage());
        }

        job.setCronExpression(newCron);
        ScheduledJob saved = scheduledJobRepository.save(job);

        // Re-register if still active
        if (saved.getStatus() == JobStatus.ACTIVE) {
            registerRecurringJob(saved);
        }

        return saved;
    }

    @Transactional
    public ScheduledJob pauseJob(UUID jobId) {
        ScheduledJob job = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ScheduledJob not found: " + jobId));

        log.info("Pausing job {}", jobId);
        try {
            jobRequestScheduler.deleteRecurringJob(job.getId().toString());
        } catch (Exception e) {
            log.warn("Could not delete recurring job in JobRunr: {}", e.getMessage());
        }

        job.setStatus(JobStatus.PAUSED);
        return scheduledJobRepository.save(job);
    }

    @Transactional
    public ScheduledJob resumeJob(UUID jobId) {
        ScheduledJob job = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ScheduledJob not found: " + jobId));

        log.info("Resuming job {}", jobId);
        job.setStatus(JobStatus.ACTIVE);
        ScheduledJob saved = scheduledJobRepository.save(job);
        registerRecurringJob(saved);
        return saved;
    }

    @Transactional
    public ScheduledJob disableJob(UUID jobId) {
        ScheduledJob job = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ScheduledJob not found: " + jobId));

        log.info("Disabling job {}", jobId);
        try {
            jobRequestScheduler.deleteRecurringJob(job.getId().toString());
        } catch (Exception ignored) {}

        job.setStatus(JobStatus.DISABLED);
        return scheduledJobRepository.save(job);
    }

    public JobExecution runNow(UUID jobId) {
        ScheduledJob job = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ScheduledJob not found: " + jobId));

        log.info("Triggering immediate run for ScheduledJob: {} ({})", job.getId(), job.getName());
        JobExecution execution = jobExecutionService.createExecution(job.getId(), "trigger-" + UUID.randomUUID());

        JobRequest jobRequest = buildJobRequest(job.getJobClass(), job.getPayload(), job.getId(), execution.getId());
        JobId jobRunrId = jobRequestScheduler.enqueue(jobRequest);

        execution.setJobRunrId(jobRunrId.toString());
        return execution;
    }

    public JobExecution scheduleOneOff(String jobClass, String payloadJson, Instant scheduleAt) {
        JobExecution execution = jobExecutionService.createExecution(null, "oneoff-" + UUID.randomUUID());
        JobRequest jobRequest = buildJobRequest(jobClass, payloadJson, null, execution.getId());

        JobId jobRunrId;
        if (scheduleAt != null && scheduleAt.isAfter(Instant.now())) {
            jobRunrId = jobRequestScheduler.schedule(scheduleAt, jobRequest);
        } else {
            jobRunrId = jobRequestScheduler.enqueue(jobRequest);
        }

        execution.setJobRunrId(jobRunrId.toString());
        return execution;
    }

    public JobRequest buildJobRequest(String jobClass, String payloadJson, UUID scheduledJobId, UUID executionId) {
        try {
            String safePayload = (payloadJson == null || payloadJson.isBlank()) ? "{}" : payloadJson.trim();
            if (safePayload.startsWith("\"") && safePayload.endsWith("\"")) {
                try {
                    safePayload = objectMapper.readValue(safePayload, String.class);
                } catch (Exception ignored) {}
            }

            if ("MpesaReconciliationJob".equalsIgnoreCase(jobClass)) {
                MpesaReconciliationJobRequest req = objectMapper.readValue(safePayload, MpesaReconciliationJobRequest.class);
                req.setScheduledJobId(scheduledJobId);
                req.setExecutionId(executionId);
                return req;
            } else if ("BulkSmsJob".equalsIgnoreCase(jobClass)) {
                BulkSmsJobRequest req = objectMapper.readValue(safePayload, BulkSmsJobRequest.class);
                req.setScheduledJobId(scheduledJobId);
                req.setExecutionId(executionId);
                return req;
            } else if ("PdfReportJob".equalsIgnoreCase(jobClass)) {
                PdfReportJobRequest req = objectMapper.readValue(safePayload, PdfReportJobRequest.class);
                req.setScheduledJobId(scheduledJobId);
                req.setExecutionId(executionId);
                return req;
            } else if ("DatabaseSnapshotJob".equalsIgnoreCase(jobClass)) {
                DatabaseSnapshotJobRequest req = objectMapper.readValue(safePayload, DatabaseSnapshotJobRequest.class);
                req.setScheduledJobId(scheduledJobId);
                req.setExecutionId(executionId);
                return req;
            } else if ("StatementWorkflowJob".equalsIgnoreCase(jobClass)) {
                StatementWorkflowJobRequest req = objectMapper.readValue(safePayload, StatementWorkflowJobRequest.class);
                req.setScheduledJobId(scheduledJobId);
                req.setExecutionId(executionId);
                return req;
            } else {
                throw new IllegalArgumentException("Unknown job class: " + jobClass);
            }
        } catch (Exception e) {
            log.error("Failed to parse payload for {}: {}", jobClass, e.getMessage());
            // Fallback default instances
            if ("MpesaReconciliationJob".equalsIgnoreCase(jobClass)) {
                return new MpesaReconciliationJobRequest(executionId, scheduledJobId, "654321", "SETTLEMENT", 1000.0, "admin@clockwork.co.ke", false);
            } else if ("BulkSmsJob".equalsIgnoreCase(jobClass)) {
                return new BulkSmsJobRequest(executionId, scheduledJobId, "Adhoc", List.of("+254712345678"), "Notice", 100, 1000, false);
            } else if ("PdfReportJob".equalsIgnoreCase(jobClass)) {
                return new PdfReportJobRequest(executionId, scheduledJobId, "FINANCIAL_SUMMARY", "Kenya Business", "admin@clockwork.co.ke", "clockwork-reports", false);
            } else if ("DatabaseSnapshotJob".equalsIgnoreCase(jobClass)) {
                return new DatabaseSnapshotJobRequest(executionId, scheduledJobId, "clockwork_db", 30, "clockwork-backups", "gzip", false);
            } else {
                return new StatementWorkflowJobRequest(executionId, scheduledJobId, null, "CUST-1", "client@ke.co", "+254700000000", "2026-10", null);
            }
        }
    }

    private String normalizeCronForJobRunr(String cron) {
        if (cron == null) return "0 0 * * *";
        String[] parts = cron.trim().split("\\s+");
        // JobRunr supports standard 5-part cron (minute hour day month day-of-week) or 6-part
        // If 6 parts with leading second (e.g. "0 0 2 * * ?"), JobRunr accepts it or standard cron
        if (parts.length == 6 && "?".equals(parts[5])) {
            parts[5] = "*";
            return String.join(" ", parts);
        }
        return cron.replace('?', '*');
    }
}
