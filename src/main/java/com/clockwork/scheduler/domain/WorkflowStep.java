package com.clockwork.scheduler.domain;

import com.clockwork.scheduler.domain.enums.WorkflowStepStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private Workflow workflow;

    @Column(name = "workflow_id", insertable = false, updatable = false)
    private UUID workflowId;

    @Column(nullable = false)
    private String name;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false)
    private String handler;

    @Column(name = "on_success_state", nullable = false, length = 50)
    private String onSuccessState;

    @Column(name = "on_failure_state", nullable = false, length = 50)
    private String onFailureState;

    @Column(name = "retry_policy", length = 100)
    private String retryPolicy = "EXPONENTIAL_3";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkflowStepStatus status = WorkflowStepStatus.PENDING;

    @Column(name = "step_result", columnDefinition = "TEXT")
    private String stepResult;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public WorkflowStep() {
    }

    public WorkflowStep(UUID id, Workflow workflow, String name, int stepOrder, String handler,
                        String onSuccessState, String onFailureState, String retryPolicy,
                        WorkflowStepStatus status) {
        this.id = id != null ? id : UUID.randomUUID();
        this.workflow = workflow;
        if (workflow != null) {
            this.workflowId = workflow.getId();
        }
        this.name = name;
        this.stepOrder = stepOrder;
        this.handler = handler;
        this.onSuccessState = onSuccessState;
        this.onFailureState = onFailureState;
        this.retryPolicy = retryPolicy != null ? retryPolicy : "EXPONENTIAL_3";
        this.status = status != null ? status : WorkflowStepStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = WorkflowStepStatus.PENDING;
        }
        if (this.retryPolicy == null) {
            this.retryPolicy = "EXPONENTIAL_3";
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
        if (workflow != null) {
            this.workflowId = workflow.getId();
        }
    }

    public UUID getWorkflowId() {
        return workflowId != null ? workflowId : (workflow != null ? workflow.getId() : null);
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getOnSuccessState() {
        return onSuccessState;
    }

    public void setOnSuccessState(String onSuccessState) {
        this.onSuccessState = onSuccessState;
    }

    public String getOnFailureState() {
        return onFailureState;
    }

    public void setOnFailureState(String onFailureState) {
        this.onFailureState = onFailureState;
    }

    public String getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public WorkflowStepStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStepStatus status) {
        this.status = status;
    }

    public String getStepResult() {
        return stepResult;
    }

    public void setStepResult(String stepResult) {
        this.stepResult = stepResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}
