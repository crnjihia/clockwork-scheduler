package com.clockwork.scheduler.controller.admin;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.service.JobExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class AdminDlqController {

    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;

    public AdminDlqController(JobExecutionRepository jobExecutionRepository,
                              JobExecutionService jobExecutionService) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutionService = jobExecutionService;
    }

    @GetMapping("/admin/dlq")
    public String listDlqJobs(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<JobExecution> dlqPage = jobExecutionRepository.findByStatus(
                ExecutionStatus.DEAD_LETTER,
                PageRequest.of(page, 20, Sort.by("finishedAt").descending())
        );
        model.addAttribute("dlqExecutions", dlqPage);
        model.addAttribute("totalDlq", dlqPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dlqPage.getTotalPages());
        return "admin/dlq";
    }

    @PostMapping("/admin/jobs/{id}/requeue")
    public String requeueDlqJob(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            JobExecution requeued = jobExecutionService.requeueExecution(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Dead-letter job execution " + id + " has been successfully requeued!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to requeue job: " + e.getMessage());
        }
        return "redirect:/admin/dlq";
    }
}
