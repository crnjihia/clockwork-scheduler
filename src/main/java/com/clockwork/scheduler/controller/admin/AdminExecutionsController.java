package com.clockwork.scheduler.controller.admin;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping("/admin/executions")
public class AdminExecutionsController {

    private final JobExecutionRepository jobExecutionRepository;
    private final ScheduledJobRepository scheduledJobRepository;

    public AdminExecutionsController(JobExecutionRepository jobExecutionRepository,
                                     ScheduledJobRepository scheduledJobRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.scheduledJobRepository = scheduledJobRepository;
    }

    @GetMapping
    public String listExecutions(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        Page<JobExecution> executions = jobExecutionRepository.searchExecutions(jobId, status, null, null, pageable);

        model.addAttribute("executions", executions);
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ExecutionStatus.values());
        model.addAttribute("scheduledJobs", scheduledJobRepository.findAll());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", executions.getTotalPages());

        return "admin/executions";
    }
}
