package com.clockwork.scheduler.jobs.filters;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.service.AlertingService;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;

@Component
public class ClockworkJobExecutionFilter implements ApplyStateFilter {

    private static final Logger log = LoggerFactory.getLogger(ClockworkJobExecutionFilter.class);

    private final JobExecutionRepository jobExecutionRepository;
    private final AlertingService alertingService;
    private final MeterRegistry meterRegistry;

    public ClockworkJobExecutionFilter(JobExecutionRepository jobExecutionRepository,
                                       AlertingService alertingService,
                                       MeterRegistry meterRegistry) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.alertingService = alertingService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        String jobRunrId = job.getId().toString();
        log.debug("JobRunr job {} transitioned from {} to {}", jobRunrId,
                oldState != null ? oldState.getName() : "NONE", newState.getName());

        if (newState.getName() == StateName.FAILED) {
            log.warn("JobRunr job {} reached terminal FAILED state! Marking execution as DEAD_LETTER.", jobRunrId);

            String errorMessage = "JobRunr execution failed";
            String stackTrace = "";

            if (newState instanceof FailedState failedState) {
                errorMessage = failedState.getMessage();
                if (failedState.getException() != null) {
                    StringWriter sw = new StringWriter();
                    failedState.getException().printStackTrace(new PrintWriter(sw));
                    stackTrace = sw.toString();
                }
            }

            Optional<JobExecution> execOpt = jobExecutionRepository.findByJobRunrId(jobRunrId);
            if (execOpt.isPresent()) {
                JobExecution exec = execOpt.get();
                exec.setStatus(ExecutionStatus.DEAD_LETTER);
                exec.setFinishedAt(Instant.now());
                exec.setErrorMessage(errorMessage);
                exec.setStackTrace(stackTrace);
                jobExecutionRepository.save(exec);

                alertingService.alertDeadLetterJob(exec.getId(), jobRunrId, job.getJobName(), errorMessage);
            } else {
                // If not tracked by JobRunrId, create DLQ entry
                JobExecution dlqExec = new JobExecution();
                dlqExec.setJobRunrId(jobRunrId);
                dlqExec.setStatus(ExecutionStatus.DEAD_LETTER);
                dlqExec.setStartedAt(Instant.now());
                dlqExec.setFinishedAt(Instant.now());
                dlqExec.setErrorMessage(errorMessage);
                dlqExec.setStackTrace(stackTrace);
                dlqExec.setResultSummary("Terminal failure in JobRunr: " + job.getJobName());
                jobExecutionRepository.save(dlqExec);

                alertingService.alertDeadLetterJob(dlqExec.getId(), jobRunrId, job.getJobName(), errorMessage);
            }
        }
    }
}
