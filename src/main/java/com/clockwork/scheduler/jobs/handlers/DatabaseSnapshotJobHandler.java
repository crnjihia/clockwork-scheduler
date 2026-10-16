package com.clockwork.scheduler.jobs.handlers;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.jobs.requests.DatabaseSnapshotJobRequest;
import com.clockwork.scheduler.service.DatabaseSnapshotService;
import com.clockwork.scheduler.service.JobExecutionService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseSnapshotJobHandler implements JobRequestHandler<DatabaseSnapshotJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSnapshotJobHandler.class);

    private final DatabaseSnapshotService databaseSnapshotService;
    private final JobExecutionService jobExecutionService;

    public DatabaseSnapshotJobHandler(DatabaseSnapshotService databaseSnapshotService,
                                     JobExecutionService jobExecutionService) {
        this.databaseSnapshotService = databaseSnapshotService;
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    @Job(name = "Nightly Database Snapshot", retries = 3)
    public void run(DatabaseSnapshotJobRequest request) throws Exception {
        UUID executionId = request.getExecutionId();
        if (executionId == null) {
            JobExecution exec = jobExecutionService.createExecution(request.getScheduledJobId(), "db-" + UUID.randomUUID());
            executionId = exec.getId();
            request.setExecutionId(executionId);
        }

        jobExecutionService.markProcessing(executionId, 1);
        log.info("Executing DatabaseSnapshotJob [Execution: {}] for DB: {}",
                executionId, request.getDatabaseName());

        if (request.isSimulateFailure()) {
            RuntimeException err = new RuntimeException("Simulated pg_dump pipe broken / Connection terminated");
            jobExecutionService.markFailed(executionId, err, 3, 3, "DatabaseSnapshotJob");
            throw err;
        }

        try {
            var result = databaseSnapshotService.createAndUploadSnapshot(
                    request.getDatabaseName(),
                    request.getRetentionDays(),
                    request.getTargetS3Bucket(),
                    request.getCompression()
            );

            jobExecutionService.markSucceeded(executionId, result.details(), "DatabaseSnapshotJob");
            log.info("DatabaseSnapshotJob completed: {}", result.details());

        } catch (Exception e) {
            jobExecutionService.markFailed(executionId, e, 1, 3, "DatabaseSnapshotJob");
            throw e;
        }
    }
}
