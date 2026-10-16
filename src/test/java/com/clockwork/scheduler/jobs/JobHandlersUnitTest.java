package com.clockwork.scheduler.jobs;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.jobs.handlers.*;
import com.clockwork.scheduler.jobs.requests.*;
import com.clockwork.scheduler.service.*;
import com.clockwork.scheduler.workflow.StatementWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobHandlersUnitTest {

    @Mock
    private JobExecutionService jobExecutionService;

    @Mock
    private MpesaStatementService mpesaStatementService;

    @Mock
    private SmsService smsService;

    @Mock
    private PdfReportService pdfReportService;

    @Mock
    private DatabaseSnapshotService databaseSnapshotService;

    @Mock
    private StorageService storageService;

    @Mock
    private StatementWorkflowService statementWorkflowService;

    private MpesaReconciliationJobHandler mpesaHandler;
    private BulkSmsJobHandler smsHandler;
    private PdfReportJobHandler pdfHandler;
    private DatabaseSnapshotJobHandler dbSnapshotHandler;
    private StatementWorkflowJobHandler workflowHandler;

    @BeforeEach
    void setUp() {
        mpesaHandler = new MpesaReconciliationJobHandler(mpesaStatementService, jobExecutionService);
        smsHandler = new BulkSmsJobHandler(smsService, jobExecutionService);
        pdfHandler = new PdfReportJobHandler(pdfReportService, storageService, jobExecutionService);
        dbSnapshotHandler = new DatabaseSnapshotJobHandler(databaseSnapshotService, jobExecutionService);
        workflowHandler = new StatementWorkflowJobHandler(statementWorkflowService, jobExecutionService);
    }

    @Test
    @DisplayName("MpesaReconciliationJobHandler executes reconciliation and records success")
    void testMpesaReconciliationSuccess() throws Exception {
        UUID execId = UUID.randomUUID();
        MpesaReconciliationJobRequest request = new MpesaReconciliationJobRequest(
                execId, UUID.randomUUID(), "654321", "KENYA-SETTLEMENT", 1000.0, "finance@ke.co", false
        );

        when(mpesaStatementService.performReconciliation(anyString(), anyString(), anyDouble(), anyString()))
                .thenReturn(new MpesaStatementService.ReconciliationReport(
                        "654321", "KENYA-SETTLEMENT", 5, 5, 4, 1, 150000.0, 5000.0, List.of("Mismatch"), "Sent"
                ));

        mpesaHandler.run(request);

        verify(jobExecutionService).markProcessing(eq(execId), eq(1));
        verify(jobExecutionService).markSucceeded(eq(execId), contains("Reconciled"), eq("MpesaReconciliationJob"));
    }

    @Test
    @DisplayName("MpesaReconciliationJobHandler handles failure simulation")
    void testMpesaReconciliationFailure() {
        UUID execId = UUID.randomUUID();
        MpesaReconciliationJobRequest request = new MpesaReconciliationJobRequest(
                execId, UUID.randomUUID(), "654321", "KENYA-SETTLEMENT", 1000.0, "finance@ke.co", true
        );

        assertThrows(RuntimeException.class, () -> mpesaHandler.run(request));
        verify(jobExecutionService).markFailed(eq(execId), any(), eq(3), eq(3), eq("MpesaReconciliationJob"));
    }

    @Test
    @DisplayName("BulkSmsJobHandler dispatches batches and records success")
    void testBulkSmsSuccess() throws Exception {
        UUID execId = UUID.randomUUID();
        BulkSmsJobRequest request = new BulkSmsJobRequest(
                execId, UUID.randomUUID(), "Billing Campaign", List.of("+254712345678", "+254722998877"),
                "Your balance is due", 100, 0, false
        );

        when(smsService.dispatchBulk(anyList(), anyString(), anyInt(), anyLong()))
                .thenReturn(new SmsService.SmsDispatchResult(2, 1, 2, 0, 50, "Dispatched 2 messages"));

        smsHandler.run(request);

        verify(jobExecutionService).markProcessing(eq(execId), eq(1));
        verify(jobExecutionService).markSucceeded(eq(execId), contains("Dispatched"), eq("BulkSmsJob"));
    }

    @Test
    @DisplayName("PdfReportJobHandler generates PDF and uploads to storage")
    void testPdfReportSuccess() throws Exception {
        UUID execId = UUID.randomUUID();
        PdfReportJobRequest request = new PdfReportJobRequest(
                execId, UUID.randomUUID(), "TAX_AUDIT", "Clockwork KE", "audit@ke.co", "clockwork-reports", false
        );

        when(pdfReportService.generateFinancialReport(anyString(), anyString(), anyString()))
                .thenReturn(new byte[]{1, 2, 3, 4});
        when(storageService.uploadBytes(anyString(), anyString(), any(byte[].class), anyString()))
                .thenReturn("s3://clockwork-reports/reports/tax_audit.pdf");

        pdfHandler.run(request);

        verify(jobExecutionService).markProcessing(eq(execId), eq(1));
        verify(jobExecutionService).markSucceeded(eq(execId), contains("Generated PDF report"), eq("PdfReportJob"));
    }

    @Test
    @DisplayName("DatabaseSnapshotJobHandler creates snapshot, compresses and retains last 30")
    void testDatabaseSnapshotSuccess() throws Exception {
        UUID execId = UUID.randomUUID();
        DatabaseSnapshotJobRequest request = new DatabaseSnapshotJobRequest(
                execId, UUID.randomUUID(), "clockwork_db", 30, "clockwork-backups", "gzip", false
        );

        when(databaseSnapshotService.createAndUploadSnapshot(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(new DatabaseSnapshotService.SnapshotResult(
                        "backups/db-snapshots/dump.sql.gz", "s3://clockwork-backups/dump.sql.gz",
                        1024, 256, 2, "Snapshot dump.sql.gz completed. Retained last 30."
                ));

        dbSnapshotHandler.run(request);

        verify(jobExecutionService).markProcessing(eq(execId), eq(1));
        verify(jobExecutionService).markSucceeded(eq(execId), contains("Retained last 30"), eq("DatabaseSnapshotJob"));
    }

    @Test
    @DisplayName("StatementWorkflowJobHandler coordinates multi-step state machine workflow")
    void testStatementWorkflowSuccess() throws Exception {
        UUID execId = UUID.randomUUID();
        UUID wfId = UUID.randomUUID();
        StatementWorkflowJobRequest request = new StatementWorkflowJobRequest(
                execId, UUID.randomUUID(), wfId, "CUST-KE-1", "user@client.co.ke", "+254700000000", "2026-10", null
        );

        com.clockwork.scheduler.domain.Workflow mockWf = new com.clockwork.scheduler.domain.Workflow();
        mockWf.setId(wfId);
        mockWf.setCurrentState("COMPLETED");

        when(statementWorkflowService.executeWorkflow(eq(wfId), isNull())).thenReturn(mockWf);

        workflowHandler.run(request);

        verify(jobExecutionService).markProcessing(eq(execId), eq(1));
        verify(jobExecutionService).markSucceeded(eq(execId), contains("COMPLETED"), eq("StatementWorkflowJob"));
    }
}
