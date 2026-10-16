package com.clockwork.scheduler.config;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.Workflow;
import com.clockwork.scheduler.domain.WorkflowStep;
import com.clockwork.scheduler.domain.enums.*;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import com.clockwork.scheduler.repository.WorkflowRepository;
import com.clockwork.scheduler.repository.WorkflowStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;

    public DemoDataInitializer(ScheduledJobRepository scheduledJobRepository,
                               JobExecutionRepository jobExecutionRepository,
                               WorkflowRepository workflowRepository,
                               WorkflowStepRepository workflowStepRepository) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.jobExecutionRepository = jobExecutionRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
    }

    @Override
    public void run(String... args) {
        if (scheduledJobRepository.count() > 0) {
            return;
        }

        log.info("Clockwork Scheduler: Initializing demo data for Kenyan back-office operations...");

        Instant now = Instant.now();

        // 1. M-Pesa Nightly Recon Job
        ScheduledJob mpesaJob = new ScheduledJob();
        mpesaJob.setId(UUID.fromString("efff42ef-d8b4-47f1-8087-0f9bafe81c71"));
        mpesaJob.setName("M-Pesa Nightly Reconciliation (C2B Paybill)");
        mpesaJob.setType(JobType.RECURRING);
        mpesaJob.setCronExpression("0 3 * * *");
        mpesaJob.setJobClass("MpesaReconciliationJob");
        mpesaJob.setPayload("{\"paybillNumber\":\"654321\",\"accountReference\":\"KENYA-HQ\"}");
        mpesaJob.setStatus(JobStatus.ACTIVE);
        mpesaJob.setCreatedAt(now.minus(7, ChronoUnit.DAYS));
        mpesaJob.setLastRunAt(now.minus(5, ChronoUnit.HOURS));
        mpesaJob.setNextRunAt(now.plus(19, ChronoUnit.HOURS));
        scheduledJobRepository.save(mpesaJob);

        // 2. Bulk SMS Dispatch
        ScheduledJob smsJob = new ScheduledJob();
        smsJob.setId(UUID.fromString("b8bcfa8e-b70d-4dfe-a8a2-27f7e297eac0"));
        smsJob.setName("Safaricom & Airtel Batch SMS Broadcast");
        smsJob.setType(JobType.RECURRING);
        smsJob.setCronExpression("0 9 * * 1-5");
        smsJob.setJobClass("BulkSmsJob");
        smsJob.setPayload("{\"campaignName\":\"Morning Billing Reminders\",\"recipients\":[\"+254722123456\",\"+254733654321\"],\"message\":\"Dear Customer, your statement is ready.\"}");
        smsJob.setStatus(JobStatus.ACTIVE);
        smsJob.setCreatedAt(now.minus(5, ChronoUnit.DAYS));
        smsJob.setLastRunAt(now.minus(1, ChronoUnit.HOURS));
        smsJob.setNextRunAt(now.plus(11, ChronoUnit.HOURS));
        scheduledJobRepository.save(smsJob);

        // 3. Tax Audit PDF Report
        ScheduledJob pdfJob = new ScheduledJob();
        pdfJob.setId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
        pdfJob.setName("Monthly Tax Audit PDF Report");
        pdfJob.setType(JobType.RECURRING);
        pdfJob.setCronExpression("0 0 1 * *");
        pdfJob.setJobClass("PdfReportJob");
        pdfJob.setPayload("{\"reportType\":\"TAX_AUDIT\",\"organization\":\"Clockwork East Africa Ltd\",\"recipientEmail\":\"audit@clockwork.co.ke\"}");
        pdfJob.setStatus(JobStatus.ACTIVE);
        pdfJob.setCreatedAt(now.minus(10, ChronoUnit.DAYS));
        pdfJob.setLastRunAt(now.minus(2, ChronoUnit.DAYS));
        pdfJob.setNextRunAt(now.plus(14, ChronoUnit.DAYS));
        scheduledJobRepository.save(pdfJob);

        // 4. PostgreSQL Database Snapshot
        ScheduledJob dbJob = new ScheduledJob();
        dbJob.setId(UUID.fromString("c9d8e7f6-a5b4-3210-fedc-ba0987654321"));
        dbJob.setName("PostgreSQL Database Nightly Snapshot");
        dbJob.setType(JobType.RECURRING);
        dbJob.setCronExpression("0 2 * * *");
        dbJob.setJobClass("DatabaseSnapshotJob");
        dbJob.setPayload("{\"databaseName\":\"clockwork_db\",\"s3Bucket\":\"clockwork-backups\",\"retentionDays\":30}");
        dbJob.setStatus(JobStatus.ACTIVE);
        dbJob.setCreatedAt(now.minus(15, ChronoUnit.DAYS));
        dbJob.setLastRunAt(now.minus(6, ChronoUnit.HOURS));
        dbJob.setNextRunAt(now.plus(18, ChronoUnit.HOURS));
        scheduledJobRepository.save(dbJob);

        // 5. Seed Execution History
        createExecution(mpesaJob.getId(), ExecutionStatus.SUCCEEDED, now.minus(5, ChronoUnit.HOURS),
                now.minus(5, ChronoUnit.HOURS).plusMillis(420), "Reconciled 1,420 transactions (KES 48,512,000.00). Variance: 0.", null);

        createExecution(smsJob.getId(), ExecutionStatus.SUCCEEDED, now.minus(1, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS).plusMillis(890), "Dispatched 5,000 SMS messages in 5 batches. Delivery rate: 99.8%.", null);

        createExecution(pdfJob.getId(), ExecutionStatus.SUCCEEDED, now.minus(2, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.DAYS).plusMillis(1250), "Generated PDF report 'TAX_AUDIT' (1.4 MB). Uploaded to s3://clockwork-reports.", null);

        // DLQ Execution for demonstration
        createExecution(mpesaJob.getId(), ExecutionStatus.DEAD_LETTER, now.minus(30, ChronoUnit.MINUTES),
                now.minus(30, ChronoUnit.MINUTES).plusMillis(2500), null, "Safaricom Daraja API Gateway Timeout: Connection reset by peer after 3 retries (504 Gateway Timeout)");

        // 6. Seed Multi-Step Statement Workflow
        Workflow wf = new Workflow();
        wf.setId(UUID.fromString("e6db9896-a17b-4061-9f78-252f1a334e68"));
        wf.setName("Statement Run: CUST-KE-254 (Safaricom Dealer)");
        wf.setDescription("Multi-step financial statement generation, dispatch, archiving, and customer alert");
        wf.setInitialState("CREATED");
        wf.setCurrentState("COMPLETED");
        wf.setStatus(WorkflowStatus.COMPLETED);
        wf.setContext("{\"customerId\": \"CUST-KE-254\", \"email\": \"accounts@safdealer.ke\", \"phone\": \"+254722123456\"}");
        workflowRepository.save(wf);

        workflowStepRepository.save(new WorkflowStep(UUID.randomUUID(), wf, "Generate Statement PDF", 1,
                "StatementGenerateStepHandler", "EMAILING", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.COMPLETED));

        workflowStepRepository.save(new WorkflowStep(UUID.randomUUID(), wf, "Email Statement to Customer", 2,
                "StatementEmailStepHandler", "ARCHIVING", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.COMPLETED));

        workflowStepRepository.save(new WorkflowStep(UUID.randomUUID(), wf, "Archive to S3 Cold Storage", 3,
                "StatementArchiveStepHandler", "COMPLETED", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.COMPLETED));

        workflowStepRepository.save(new WorkflowStep(UUID.randomUUID(), wf, "Notify Customer via SMS", 4,
                "StatementNotifyStepHandler", "COMPLETED", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.COMPLETED));

        log.info("Clockwork Scheduler: Demo dataset seeded successfully with 4 jobs, execution history, and statement workflow!");
    }

    private void createExecution(UUID jobId, ExecutionStatus status, Instant start, Instant finish, String result, String error) {
        JobExecution exec = new JobExecution();
        exec.setId(UUID.randomUUID());
        exec.setJobId(jobId);
        exec.setJobRunrId(UUID.randomUUID().toString());
        exec.setStatus(status);
        exec.setAttempt(1);
        exec.setStartedAt(start);
        exec.setFinishedAt(finish);
        exec.setDurationMs(finish != null ? ChronoUnit.MILLIS.between(start, finish) : 0);
        exec.setResultSummary(result);
        exec.setErrorMessage(error);
        if (error != null) {
            exec.setStackTrace("com.clockwork.scheduler.exception.GatewayTimeoutException: Connection timeout\n\tat com.clockwork.scheduler.jobs.handlers.MpesaReconciliationJobHandler.execute(MpesaReconciliationJobHandler.java:62)");
        }
        jobExecutionRepository.save(exec);
    }
}
