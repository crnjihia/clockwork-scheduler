package com.clockwork.scheduler.jobs.handlers;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.Workflow;
import com.clockwork.scheduler.jobs.requests.StatementWorkflowJobRequest;
import com.clockwork.scheduler.service.JobExecutionService;
import com.clockwork.scheduler.workflow.StatementWorkflowService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StatementWorkflowJobHandler implements JobRequestHandler<StatementWorkflowJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(StatementWorkflowJobHandler.class);

    private final StatementWorkflowService statementWorkflowService;
    private final JobExecutionService jobExecutionService;

    public StatementWorkflowJobHandler(StatementWorkflowService statementWorkflowService,
                                      JobExecutionService jobExecutionService) {
        this.statementWorkflowService = statementWorkflowService;
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    @Job(name = "Statement Workflow Orchestrator", retries = 3)
    public void run(StatementWorkflowJobRequest request) throws Exception {
        UUID executionId = request.getExecutionId();
        if (executionId == null) {
            JobExecution exec = jobExecutionService.createExecution(request.getScheduledJobId(), "wf-" + UUID.randomUUID());
            executionId = exec.getId();
            request.setExecutionId(executionId);
        }

        jobExecutionService.markProcessing(executionId, 1);
        log.info("Executing StatementWorkflowJob [Execution: {}] for Customer: {}",
                executionId, request.getCustomerId());

        try {
            Workflow workflow;
            if (request.getWorkflowId() != null) {
                workflow = statementWorkflowService.executeWorkflow(request.getWorkflowId(), request.getSimulateFailureAtStep());
            } else {
                Workflow newWf = statementWorkflowService.createDefaultStatementWorkflow(
                        request.getCustomerId(),
                        request.getCustomerEmail(),
                        request.getCustomerPhone()
                );
                request.setWorkflowId(newWf.getId());
                workflow = statementWorkflowService.executeWorkflow(newWf.getId(), request.getSimulateFailureAtStep());
            }

            String summary = String.format("Workflow %s completed with state %s across all steps.",
                    workflow.getId(), workflow.getCurrentState());

            jobExecutionService.markSucceeded(executionId, summary, "StatementWorkflowJob");
            log.info("StatementWorkflowJob completed: {}", summary);

        } catch (Exception e) {
            jobExecutionService.markFailed(executionId, e, 1, 3, "StatementWorkflowJob");
            throw e;
        }
    }
}
