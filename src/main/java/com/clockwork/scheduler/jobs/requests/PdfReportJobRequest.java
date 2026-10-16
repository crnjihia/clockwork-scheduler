package com.clockwork.scheduler.jobs.requests;

import com.clockwork.scheduler.jobs.handlers.PdfReportJobHandler;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public class PdfReportJobRequest implements JobRequest {

    private UUID executionId;
    private UUID scheduledJobId;
    private String reportType;
    private String organizationName;
    private String recipientEmail;
    private String targetS3Bucket;
    private boolean simulateFailure;

    public PdfReportJobRequest() {
    }

    public PdfReportJobRequest(UUID executionId, UUID scheduledJobId, String reportType,
                               String organizationName, String recipientEmail,
                               String targetS3Bucket, boolean simulateFailure) {
        this.executionId = executionId;
        this.scheduledJobId = scheduledJobId;
        this.reportType = reportType != null ? reportType : "FINANCIAL_SUMMARY";
        this.organizationName = organizationName != null ? organizationName : "Clockwork Kenya Ltd";
        this.recipientEmail = recipientEmail != null ? recipientEmail : "reports@clockwork.co.ke";
        this.targetS3Bucket = targetS3Bucket != null ? targetS3Bucket : "clockwork-reports";
        this.simulateFailure = simulateFailure;
    }

    @Override
    public Class<PdfReportJobHandler> getJobRequestHandler() {
        return PdfReportJobHandler.class;
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

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getTargetS3Bucket() {
        return targetS3Bucket;
    }

    public void setTargetS3Bucket(String targetS3Bucket) {
        this.targetS3Bucket = targetS3Bucket;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
