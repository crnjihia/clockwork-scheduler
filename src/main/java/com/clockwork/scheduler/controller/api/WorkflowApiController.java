package com.clockwork.scheduler.controller.api;

import com.clockwork.scheduler.domain.Workflow;
import com.clockwork.scheduler.dto.requests.CreateWorkflowRequestDto;
import com.clockwork.scheduler.repository.WorkflowRepository;
import com.clockwork.scheduler.workflow.StatementWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowApiController {

    private final WorkflowRepository workflowRepository;
    private final StatementWorkflowService statementWorkflowService;

    public WorkflowApiController(WorkflowRepository workflowRepository,
                                 StatementWorkflowService statementWorkflowService) {
        this.workflowRepository = workflowRepository;
        this.statementWorkflowService = statementWorkflowService;
    }

    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@RequestBody CreateWorkflowRequestDto request) {
        Workflow workflow = statementWorkflowService.createDefaultStatementWorkflow(
                request.customerId(),
                request.email(),
                request.phone()
        );
        if (request.name() != null && !request.name().isBlank()) {
            workflow.setName(request.name());
            workflowRepository.save(workflow);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(workflow);
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> getAllWorkflows() {
        return ResponseEntity.ok(workflowRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getWorkflowById(@PathVariable UUID id) {
        return workflowRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Workflow> startWorkflow(@PathVariable UUID id,
                                                  @RequestParam(required = false) String simulateFailureAtStep) {
        Workflow result = statementWorkflowService.executeWorkflow(id, simulateFailureAtStep);
        return ResponseEntity.accepted().body(result);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Workflow> retryWorkflow(@PathVariable UUID id) {
        Workflow retried = statementWorkflowService.retryWorkflow(id);
        return ResponseEntity.accepted().body(retried);
    }
}
