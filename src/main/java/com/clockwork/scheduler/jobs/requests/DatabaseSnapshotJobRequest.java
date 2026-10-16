package com.clockwork.scheduler.jobs.requests;

import com.clockwork.scheduler.jobs.handlers.DatabaseSnapshotJobHandler;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public class DatabaseSnapshotJobRequest implements JobRequest {

    private UUID executionId;
    private UUID scheduledJobId;
    private String databaseName;
    private int retentionDays;
    private String targetS3Bucket;
    private String compression;
    private boolean simulateFailure;

    public DatabaseSnapshotJobRequest() {
    }

    public DatabaseSnapshotJobRequest(UUID executionId, UUID scheduledJobId, String databaseName,
                                      int retentionDays, String targetS3Bucket, String compression,
                                      boolean simulateFailure) {
        this.executionId = executionId;
        this.scheduledJobId = scheduledJobId;
        this.databaseName = databaseName != null ? databaseName : "clockwork_db";
        this.retentionDays = retentionDays > 0 ? retentionDays : 30;
        this.targetS3Bucket = targetS3Bucket != null ? targetS3Bucket : "clockwork-backups";
        this.compression = compression != null ? compression : "gzip";
        this.simulateFailure = simulateFailure;
    }

    @Override
    public Class<DatabaseSnapshotJobHandler> getJobRequestHandler() {
        return DatabaseSnapshotJobHandler.class;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public UUID getScheduledJobId() {
        return scheduledJobId;
    }

    public void setScheduledJobId(UUID scheduledJobId) {
        this.scheduledJobId = scheduledJobId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getTargetS3Bucket() {
        return targetS3Bucket;
    }

    public void setTargetS3Bucket(String targetS3Bucket) {
        this.targetS3Bucket = targetS3Bucket;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
