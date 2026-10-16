package com.clockwork.scheduler.controller.api;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.service.JobExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class ExecutionApiController {

    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;

    public ExecutionApiController(JobExecutionRepository jobExecutionRepository,
                                  JobExecutionService jobExecutionService) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutionService = jobExecutionService;
    }

    @GetMapping
    public ResponseEntity<Page<JobExecution>> getExecutions(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        Page<JobExecution> executions = jobExecutionRepository.searchExecutions(jobId, status, from, to, pageable);
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobExecution> getExecutionById(@PathVariable UUID id) {
        return jobExecutionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/requeue")
    public ResponseEntity<JobExecution> requeueExecution(@PathVariable UUID id) {
        JobExecution requeued = jobExecutionService.requeueExecution(id);
        return ResponseEntity.accepted().body(requeued);
    }

    @GetMapping("/stats")
    public ResponseEntity<JobExecutionService.ExecutionDashboardStats> getStats() {
        return ResponseEntity.ok(jobExecutionService.getDashboardStats());
    }
}
