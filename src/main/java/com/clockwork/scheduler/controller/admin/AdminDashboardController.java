package com.clockwork.scheduler.controller.admin;

import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import com.clockwork.scheduler.repository.WorkflowRepository;
import com.clockwork.scheduler.service.JobExecutionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final JobExecutionService jobExecutionService;
    private final JobExecutionRepository jobExecutionRepository;
    private final ScheduledJobRepository scheduledJobRepository;
    private final WorkflowRepository workflowRepository;

    public AdminDashboardController(JobExecutionService jobExecutionService,
                                    JobExecutionRepository jobExecutionRepository,
                                    ScheduledJobRepository scheduledJobRepository,
                                    WorkflowRepository workflowRepository) {
        this.jobExecutionService = jobExecutionService;
        this.jobExecutionRepository = jobExecutionRepository;
        this.scheduledJobRepository = scheduledJobRepository;
        this.workflowRepository = workflowRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        var stats = jobExecutionService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("scheduledCount", scheduledJobRepository.count());
        model.addAttribute("workflowsCount", workflowRepository.count());
        model.addAttribute("recentExecutions", jobExecutionRepository.findTop10ByOrderByStartedAtDesc());
        return "admin/dashboard";
    }

    @GetMapping("/stats-fragment")
    public String statsFragment(Model model) {
        model.addAttribute("stats", jobExecutionService.getDashboardStats());
        return "admin/fragments/stats :: statsCards";
    }
}
