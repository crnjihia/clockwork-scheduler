package com.clockwork.scheduler.jobs.requests;

import com.clockwork.scheduler.jobs.handlers.StatementWorkflowJobHandler;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public class StatementWorkflowJobRequest implements JobRequest {

    private UUID executionId;
    private UUID scheduledJobId;
    private UUID workflowId;
    private String customerId;
    private String customerEmail;
    private String customerPhone;
    private String billingPeriod;
    private String simulateFailureAtStep;

    public StatementWorkflowJobRequest() {
    }

    public StatementWorkflowJobRequest(UUID executionId, UUID scheduledJobId, UUID workflowId,
                                       String customerId, String customerEmail, String customerPhone,
                                       String billingPeriod, String simulateFailureAtStep) {
        this.executionId = executionId;
        this.scheduledJobId = scheduledJobId;
        this.workflowId = workflowId;
        this.customerId = customerId != null ? customerId : "CUST-KE-9872";
        this.customerEmail = customerEmail != null ? customerEmail : "customer@safari-ventures.co.ke";
        this.customerPhone = customerPhone != null ? customerPhone : "+254722123456";
        this.billingPeriod = billingPeriod != null ? billingPeriod : "2026-10";
        this.simulateFailureAtStep = simulateFailureAtStep;
    }

    @Override
    public Class<StatementWorkflowJobHandler> getJobRequestHandler() {
        return StatementWorkflowJobHandler.class;
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

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public String getSimulateFailureAtStep() {
        return simulateFailureAtStep;
    }

    public void setSimulateFailureAtStep(String simulateFailureAtStep) {
        this.simulateFailureAtStep = simulateFailureAtStep;
    }
}
