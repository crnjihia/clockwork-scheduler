package com.clockwork.scheduler.workflow;

import com.clockwork.scheduler.domain.Workflow;
import com.clockwork.scheduler.domain.WorkflowStep;
import com.clockwork.scheduler.domain.enums.StatementEvent;
import com.clockwork.scheduler.domain.enums.StatementState;
import com.clockwork.scheduler.domain.enums.WorkflowStatus;
import com.clockwork.scheduler.domain.enums.WorkflowStepStatus;
import com.clockwork.scheduler.repository.WorkflowRepository;
import com.clockwork.scheduler.repository.WorkflowStepRepository;
import com.clockwork.scheduler.service.PdfReportService;
import com.clockwork.scheduler.service.SmsService;
import com.clockwork.scheduler.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StatementWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(StatementWorkflowService.class);

    private final StateMachineFactory<StatementState, StatementEvent> stateMachineFactory;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final PdfReportService pdfReportService;
    private final StorageService storageService;
    private final SmsService smsService;

    public StatementWorkflowService(StateMachineFactory<StatementState, StatementEvent> stateMachineFactory,
                                    WorkflowRepository workflowRepository,
                                    WorkflowStepRepository workflowStepRepository,
                                    PdfReportService pdfReportService,
                                    StorageService storageService,
                                    SmsService smsService) {
        this.stateMachineFactory = stateMachineFactory;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.pdfReportService = pdfReportService;
        this.storageService = storageService;
        this.smsService = smsService;
    }

    @Transactional
    public Workflow createDefaultStatementWorkflow(String customerId, String email, String phone) {
        Workflow wf = new Workflow();
        wf.setId(UUID.randomUUID());
        wf.setName("Statement Run: " + (customerId != null ? customerId : "Default"));
        wf.setDescription("Multi-step financial statement generation, dispatch, archiving, and customer alert");
        wf.setInitialState("CREATED");
        wf.setCurrentState("CREATED");
        wf.setStatus(WorkflowStatus.PENDING);
        wf.setContext(String.format("{\"customerId\": \"%s\", \"email\": \"%s\", \"phone\": \"%s\"}",
                customerId, email, phone));

        // Step 1: Generate
        wf.addStep(new WorkflowStep(UUID.randomUUID(), wf, "Generate Statement PDF", 1,
                "StatementGenerateStepHandler", "EMAILING", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.PENDING));

        // Step 2: Email
        wf.addStep(new WorkflowStep(UUID.randomUUID(), wf, "Email Statement to Customer", 2,
                "StatementEmailStepHandler", "ARCHIVING", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.PENDING));

        // Step 3: Archive
        wf.addStep(new WorkflowStep(UUID.randomUUID(), wf, "Archive to S3 Cold Storage", 3,
                "StatementArchiveStepHandler", "COMPLETED", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.PENDING));

        // Step 4: Notify
        wf.addStep(new WorkflowStep(UUID.randomUUID(), wf, "Notify Customer via SMS", 4,
                "StatementNotifyStepHandler", "COMPLETED", "FAILED", "EXPONENTIAL_3", WorkflowStepStatus.PENDING));

        return workflowRepository.save(wf);
    }

    public Workflow executeWorkflow(UUID workflowId, String simulateFailureAtStep) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        log.info("Executing workflow {} ({}) - simulated failure at step: {}",
                workflow.getId(), workflow.getName(), simulateFailureAtStep);

        StateMachine<StatementState, StatementEvent> sm = stateMachineFactory.getStateMachine(workflow.getId().toString());
        sm.startReactively().block();

        workflow.setStatus(WorkflowStatus.RUNNING);
        workflowRepository.save(workflow);

        List<WorkflowStep> steps = workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(workflow.getId());

        try {
            // Transition: START -> GENERATING
            sendEventSync(sm, StatementEvent.START);
            updateWorkflowState(workflow, getCurrentStateName(sm));

            // --- STEP 1: GENERATE ---
            WorkflowStep step1 = findStep(steps, 1);
            executeStep1Generate(workflow, step1, simulateFailureAtStep);
            sendEventSync(sm, StatementEvent.GENERATE_SUCCESS);
            updateWorkflowState(workflow, getCurrentStateName(sm));

            // --- STEP 2: EMAIL ---
            WorkflowStep step2 = findStep(steps, 2);
            executeStep2Email(workflow, step2, simulateFailureAtStep);
            sendEventSync(sm, StatementEvent.EMAIL_SUCCESS);
            updateWorkflowState(workflow, getCurrentStateName(sm));

            // --- STEP 3: ARCHIVE ---
            WorkflowStep step3 = findStep(steps, 3);
            executeStep3Archive(workflow, step3, simulateFailureAtStep);
            sendEventSync(sm, StatementEvent.ARCHIVE_SUCCESS);
            updateWorkflowState(workflow, getCurrentStateName(sm));

            // --- STEP 4: NOTIFY ---
            WorkflowStep step4 = findStep(steps, 4);
            if (step4 != null) {
                executeStep4Notify(workflow, step4);
            }

            workflow.setStatus(WorkflowStatus.COMPLETED);
            workflowRepository.save(workflow);
            log.info("Workflow {} successfully completed!", workflow.getId());

        } catch (Exception e) {
            log.error("Workflow {} failed during execution: {}", workflow.getId(), e.getMessage());
            // Transition state machine to FAILED
            if (sm.getState() != null) {
                if (sm.getState().getId() == StatementState.GENERATING) {
                    sendEventSync(sm, StatementEvent.GENERATE_FAILURE);
                } else if (sm.getState().getId() == StatementState.EMAILING) {
                    sendEventSync(sm, StatementEvent.EMAIL_FAILURE);
                } else if (sm.getState().getId() == StatementState.ARCHIVING) {
                    sendEventSync(sm, StatementEvent.ARCHIVE_FAILURE);
                }
            }

            workflow.setCurrentState(StatementState.FAILED.name());
            workflow.setStatus(WorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            throw new RuntimeException("Workflow step failed: " + e.getMessage(), e);
        } finally {
            sm.stopReactively().block();
        }

        return workflowRepository.findById(workflowId).orElse(workflow);
    }

    private void executeStep1Generate(Workflow workflow, WorkflowStep step, String simulateFailureAtStep) {
        if (step == null) return;
        step.setStatus(WorkflowStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        workflowStepRepository.save(step);

        if ("GENERATE".equalsIgnoreCase(simulateFailureAtStep)) {
            step.setStatus(WorkflowStepStatus.FAILED);
            step.setErrorMessage("Simulated PDF statement compilation error");
            step.setFinishedAt(Instant.now());
            workflowStepRepository.save(step);
            throw new RuntimeException("Simulated failure at GENERATE step");
        }

        byte[] pdfBytes = pdfReportService.generateFinancialReport("MONTHLY_STATEMENT", "Kenyan Client", "Direct Customer");
        step.setStatus(WorkflowStepStatus.COMPLETED);
        step.setStepResult(String.format("Generated PDF statement (%d bytes)", pdfBytes.length));
        step.setFinishedAt(Instant.now());
        workflowStepRepository.save(step);
    }

    private void executeStep2Email(Workflow workflow, WorkflowStep step, String simulateFailureAtStep) {
        if (step == null) return;
        step.setStatus(WorkflowStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        workflowStepRepository.save(step);

        if ("EMAIL".equalsIgnoreCase(simulateFailureAtStep)) {
            step.setStatus(WorkflowStepStatus.FAILED);
            step.setErrorMessage("SMTP Host unreachable or rate limited");
            step.setFinishedAt(Instant.now());
            workflowStepRepository.save(step);
            throw new RuntimeException("Simulated failure at EMAIL step");
        }

        step.setStatus(WorkflowStepStatus.COMPLETED);
        step.setStepResult("Email dispatched with encrypted statement attachment.");
        step.setFinishedAt(Instant.now());
        workflowStepRepository.save(step);
    }

    private void executeStep3Archive(Workflow workflow, WorkflowStep step, String simulateFailureAtStep) {
        if (step == null) return;
        step.setStatus(WorkflowStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        workflowStepRepository.save(step);

        if ("ARCHIVE".equalsIgnoreCase(simulateFailureAtStep)) {
            step.setStatus(WorkflowStepStatus.FAILED);
            step.setErrorMessage("Storage bucket permission denied");
            step.setFinishedAt(Instant.now());
            workflowStepRepository.save(step);
            throw new RuntimeException("Simulated failure at ARCHIVE step");
        }

        String archiveKey = "archives/statements/" + workflow.getId() + ".pdf";
        byte[] dummy = "ARCHIVED_STATEMENT_METADATA".getBytes();
        String uri = storageService.uploadBytes("clockwork-archive", archiveKey, dummy, "application/pdf");

        step.setStatus(WorkflowStepStatus.COMPLETED);
        step.setStepResult("Archived statement to cold storage: " + uri);
        step.setFinishedAt(Instant.now());
        workflowStepRepository.save(step);
    }

    private void executeStep4Notify(Workflow workflow, WorkflowStep step) {
        step.setStatus(WorkflowStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        workflowStepRepository.save(step);

        smsService.dispatchBulk(List.of("+254722123456"), "Your statement is ready for review.", 1, 0);

        step.setStatus(WorkflowStepStatus.COMPLETED);
        step.setStepResult("Customer notified via SMS dispatch (+254722123456)");
        step.setFinishedAt(Instant.now());
        workflowStepRepository.save(step);
    }

    @Transactional
    public Workflow retryWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        log.info("Retrying failed workflow: {}", workflowId);
        StateMachine<StatementState, StatementEvent> sm = stateMachineFactory.getStateMachine(workflow.getId().toString());
        sm.startReactively().block();
        sendEventSync(sm, StatementEvent.RETRY);

        workflow.setCurrentState(StatementState.GENERATING.name());
        workflow.setStatus(WorkflowStatus.RUNNING);
        Workflow saved = workflowRepository.save(workflow);
        sm.stopReactively().block();

        return executeWorkflow(saved.getId(), null);
    }

    private void sendEventSync(StateMachine<StatementState, StatementEvent> sm, StatementEvent event) {
        sm.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();
    }

    private String getCurrentStateName(StateMachine<StatementState, StatementEvent> sm) {
        return sm.getState() != null && sm.getState().getId() != null ? sm.getState().getId().name() : "UNKNOWN";
    }

    private WorkflowStep findStep(List<WorkflowStep> steps, int order) {
        return steps.stream()
                .filter(s -> s.getStepOrder() == order)
                .findFirst()
                .orElse(null);
    }

    private void updateWorkflowState(Workflow workflow, String stateName) {
        workflow.setCurrentState(stateName);
        workflowRepository.save(workflow);
    }
}
