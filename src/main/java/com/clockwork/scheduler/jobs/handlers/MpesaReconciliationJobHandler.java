package com.clockwork.scheduler.jobs.handlers;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.jobs.requests.MpesaReconciliationJobRequest;
import com.clockwork.scheduler.service.JobExecutionService;
import com.clockwork.scheduler.service.MpesaStatementService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MpesaReconciliationJobHandler implements JobRequestHandler<MpesaReconciliationJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(MpesaReconciliationJobHandler.class);

    private final MpesaStatementService mpesaStatementService;
    private final JobExecutionService jobExecutionService;

    public MpesaReconciliationJobHandler(MpesaStatementService mpesaStatementService,
                                         JobExecutionService jobExecutionService) {
        this.mpesaStatementService = mpesaStatementService;
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    @Job(name = "M-Pesa Reconciliation Run", retries = 3)
    public void run(MpesaReconciliationJobRequest request) throws Exception {
        UUID executionId = request.getExecutionId();
        if (executionId == null) {
            JobExecution exec = jobExecutionService.createExecution(request.getScheduledJobId(), "mpesa-" + UUID.randomUUID());
            executionId = exec.getId();
            request.setExecutionId(executionId);
        }

        jobExecutionService.markProcessing(executionId, 1);
        log.info("Executing MpesaReconciliationJob [Execution: {}] for Paybill: {}",
                executionId, request.getPaybillNumber());

        if (request.isSimulateFailure()) {
            RuntimeException err = new RuntimeException("Simulated Safaricom Daraja API 503 Gateway Timeout");
            jobExecutionService.markFailed(executionId, err, 3, 3, "MpesaReconciliationJob");
            throw err;
        }

        try {
            var report = mpesaStatementService.performReconciliation(
                    request.getPaybillNumber(),
                    request.getAccountReference(),
                    request.getDiscrepancyThreshold(),
                    request.getNotifyEmail()
            );

            String summary = String.format("Reconciled %d transactions. Settled: KES %,.2f. Mismatches: %d (Variance: KES %,.2f).",
                    report.totalMpesaRecords(), report.totalSettledAmountKes(),
                    report.mismatchCount(), report.discrepancyTotalKes());

            jobExecutionService.markSucceeded(executionId, summary, "MpesaReconciliationJob");
            log.info("MpesaReconciliationJob completed successfully: {}", summary);

        } catch (Exception e) {
            jobExecutionService.markFailed(executionId, e, 1, 3, "MpesaReconciliationJob");
            throw e;
        }
    }
}
