package com.clockwork.scheduler.jobs.handlers;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.jobs.requests.BulkSmsJobRequest;
import com.clockwork.scheduler.service.JobExecutionService;
import com.clockwork.scheduler.service.SmsService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BulkSmsJobHandler implements JobRequestHandler<BulkSmsJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(BulkSmsJobHandler.class);

    private final SmsService smsService;
    private final JobExecutionService jobExecutionService;

    public BulkSmsJobHandler(SmsService smsService, JobExecutionService jobExecutionService) {
        this.smsService = smsService;
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    @Job(name = "Bulk SMS Dispatch", retries = 3)
    public void run(BulkSmsJobRequest request) throws Exception {
        UUID executionId = request.getExecutionId();
        if (executionId == null) {
            JobExecution exec = jobExecutionService.createExecution(request.getScheduledJobId(), "sms-" + UUID.randomUUID());
            executionId = exec.getId();
            request.setExecutionId(executionId);
        }

        jobExecutionService.markProcessing(executionId, 1);
        log.info("Executing BulkSmsJob [Execution: {}] Campaign: {}, Recipients: {}",
                executionId, request.getCampaignName(), request.getPhoneNumbers().size());

        if (request.isSimulateFailure()) {
            RuntimeException err = new RuntimeException("Simulated SMPP Telco Gateway Peer Closed Connection");
            jobExecutionService.markFailed(executionId, err, 3, 3, "BulkSmsJob");
            throw err;
        }

        try {
            var result = smsService.dispatchBulk(
                    request.getPhoneNumbers(),
                    request.getMessageTemplate(),
                    request.getBatchSize(),
                    request.getDelayBetweenBatchesMs()
            );

            String summary = String.format("Dispatched %d messages in %d batches (%d ms). Delivered: %d, Failed: %d",
                    result.totalRecipients(), result.totalBatches(), result.executionTimeMs(),
                    result.deliveredCount(), result.failedCount());

            jobExecutionService.markSucceeded(executionId, summary, "BulkSmsJob");
            log.info("BulkSmsJob completed: {}", summary);

        } catch (Exception e) {
            jobExecutionService.markFailed(executionId, e, 1, 3, "BulkSmsJob");
            throw e;
        }
    }
}
