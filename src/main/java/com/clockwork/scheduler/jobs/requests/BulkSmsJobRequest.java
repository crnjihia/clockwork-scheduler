package com.clockwork.scheduler.jobs.requests;

import com.clockwork.scheduler.jobs.handlers.BulkSmsJobHandler;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BulkSmsJobRequest implements JobRequest {

    private UUID executionId;
    private UUID scheduledJobId;
    private String campaignName;
    private List<String> phoneNumbers = new ArrayList<>();
    private String messageTemplate;
    private int batchSize = 100;
    private long delayBetweenBatchesMs = 1000;
    private boolean simulateFailure;

    public BulkSmsJobRequest() {
    }

    public BulkSmsJobRequest(UUID executionId, UUID scheduledJobId, String campaignName,
                             List<String> phoneNumbers, String messageTemplate,
                             int batchSize, long delayBetweenBatchesMs, boolean simulateFailure) {
        this.executionId = executionId;
        this.scheduledJobId = scheduledJobId;
        this.campaignName = campaignName != null ? campaignName : "Default Campaign";
        this.phoneNumbers = phoneNumbers != null ? phoneNumbers : new ArrayList<>();
        this.messageTemplate = messageTemplate != null ? messageTemplate : "Clockwork Alert: Notification for your account.";
        this.batchSize = batchSize > 0 ? batchSize : 100;
        this.delayBetweenBatchesMs = delayBetweenBatchesMs >= 0 ? delayBetweenBatchesMs : 1000;
        this.simulateFailure = simulateFailure;
    }

    @Override
    public Class<BulkSmsJobHandler> getJobRequestHandler() {
        return BulkSmsJobHandler.class;
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

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getDelayBetweenBatchesMs() {
        return delayBetweenBatchesMs;
    }

    public void setDelayBetweenBatchesMs(long delayBetweenBatchesMs) {
        this.delayBetweenBatchesMs = delayBetweenBatchesMs;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
