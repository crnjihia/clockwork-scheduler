package com.clockwork.scheduler.jobs.requests;

import com.clockwork.scheduler.jobs.handlers.MpesaReconciliationJobHandler;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public class MpesaReconciliationJobRequest implements JobRequest {

    private UUID executionId;
    private UUID scheduledJobId;
    private String paybillNumber;
    private String accountReference;
    private double discrepancyThreshold;
    private String notifyEmail;
    private boolean simulateFailure;

    public MpesaReconciliationJobRequest() {
    }

    public MpesaReconciliationJobRequest(UUID executionId, UUID scheduledJobId, String paybillNumber,
                                         String accountReference, double discrepancyThreshold,
                                         String notifyEmail, boolean simulateFailure) {
        this.executionId = executionId;
        this.scheduledJobId = scheduledJobId;
        this.paybillNumber = paybillNumber != null ? paybillNumber : "654321";
        this.accountReference = accountReference != null ? accountReference : "KENYA-SETTLEMENT";
        this.discrepancyThreshold = discrepancyThreshold > 0 ? discrepancyThreshold : 1000.0;
        this.notifyEmail = notifyEmail != null ? notifyEmail : "settlements@clockwork.co.ke";
        this.simulateFailure = simulateFailure;
    }

    @Override
    public Class<MpesaReconciliationJobHandler> getJobRequestHandler() {
        return MpesaReconciliationJobHandler.class;
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

    public String getPaybillNumber() {
        return paybillNumber;
    }

    public void setPaybillNumber(String paybillNumber) {
        this.paybillNumber = paybillNumber;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public void setAccountReference(String accountReference) {
        this.accountReference = accountReference;
    }

    public double getDiscrepancyThreshold() {
        return discrepancyThreshold;
    }

    public void setDiscrepancyThreshold(double discrepancyThreshold) {
        this.discrepancyThreshold = discrepancyThreshold;
    }

    public String getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(String notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
