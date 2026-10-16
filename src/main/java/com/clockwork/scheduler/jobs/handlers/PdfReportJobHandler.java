package com.clockwork.scheduler.jobs.handlers;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.jobs.requests.PdfReportJobRequest;
import com.clockwork.scheduler.service.JobExecutionService;
import com.clockwork.scheduler.service.PdfReportService;
import com.clockwork.scheduler.service.StorageService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class PdfReportJobHandler implements JobRequestHandler<PdfReportJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(PdfReportJobHandler.class);

    private final PdfReportService pdfReportService;
    private final StorageService storageService;
    private final JobExecutionService jobExecutionService;

    public PdfReportJobHandler(PdfReportService pdfReportService,
                               StorageService storageService,
                               JobExecutionService jobExecutionService) {
        this.pdfReportService = pdfReportService;
        this.storageService = storageService;
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    @Job(name = "PDF Report Generation", retries = 3)
    public void run(PdfReportJobRequest request) throws Exception {
        UUID executionId = request.getExecutionId();
        if (executionId == null) {
            JobExecution exec = jobExecutionService.createExecution(request.getScheduledJobId(), "pdf-" + UUID.randomUUID());
            executionId = exec.getId();
            request.setExecutionId(executionId);
        }

        jobExecutionService.markProcessing(executionId, 1);
        log.info("Executing PdfReportJob [Execution: {}] Type: {}, Org: {}",
                executionId, request.getReportType(), request.getOrganizationName());

        if (request.isSimulateFailure()) {
            RuntimeException err = new RuntimeException("Simulated JasperReports/OpenPDF OutOfMemory / Thread Interrupted");
            jobExecutionService.markFailed(executionId, err, 3, 3, "PdfReportJob");
            throw err;
        }

        try {
            byte[] pdfBytes = pdfReportService.generateFinancialReport(
                    request.getReportType(),
                    request.getOrganizationName(),
                    request.getRecipientEmail()
            );

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String objectKey = String.format("reports/%s_%s.pdf", request.getReportType().toLowerCase(), timestamp);
            String storageUri = storageService.uploadBytes(request.getTargetS3Bucket(), objectKey, pdfBytes, "application/pdf");

            String emailNotice = String.format("Emailed download link to %s (URI: %s)", request.getRecipientEmail(), storageUri);
            log.info(emailNotice);

            String summary = String.format("Generated PDF report '%s' (%d bytes). Stored at: %s. %s",
                    request.getReportType(), pdfBytes.length, storageUri, emailNotice);

            jobExecutionService.markSucceeded(executionId, summary, "PdfReportJob");
            log.info("PdfReportJob completed: {}", summary);

        } catch (Exception e) {
            jobExecutionService.markFailed(executionId, e, 1, 3, "PdfReportJob");
            throw e;
        }
    }
}
