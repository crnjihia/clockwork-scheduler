package com.clockwork.scheduler.controller.admin;

import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.enums.JobStatus;
import com.clockwork.scheduler.domain.enums.JobType;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import com.clockwork.scheduler.service.JobSchedulingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping("/admin/scheduled")
public class AdminScheduledJobsController {

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobSchedulingService jobSchedulingService;

    public AdminScheduledJobsController(ScheduledJobRepository scheduledJobRepository,
                                       JobSchedulingService jobSchedulingService) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.jobSchedulingService = jobSchedulingService;
    }

    @GetMapping
    public String listScheduledJobs(Model model) {
        model.addAttribute("jobs", scheduledJobRepository.findAll());
        return "admin/scheduled";
    }

    @PostMapping("/create")
    public String createJob(@RequestParam String name,
                            @RequestParam String cronExpression,
                            @RequestParam String jobClass,
                            @RequestParam(required = false) String payload,
                            RedirectAttributes redirectAttributes) {
        ScheduledJob job = new ScheduledJob();
        job.setId(UUID.randomUUID());
        job.setName(name);
        job.setType(JobType.RECURRING);
        job.setCronExpression(cronExpression);
        job.setJobClass(jobClass);
        job.setPayload(payload != null && !payload.isBlank() ? payload : "{}");
        job.setStatus(JobStatus.ACTIVE);
        job.setCreatedAt(Instant.now());

        ScheduledJob saved = scheduledJobRepository.save(job);
        jobSchedulingService.registerRecurringJob(saved);

        redirectAttributes.addFlashAttribute("successMessage", "Job '" + name + "' scheduled successfully!");
        return "redirect:/admin/scheduled";
    }

    @PostMapping("/{id}/run-now")
    public String runNow(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        jobSchedulingService.runNow(id);
        redirectAttributes.addFlashAttribute("successMessage", "Job triggered for immediate execution!");
        return "redirect:/admin/scheduled";
    }

    @PostMapping("/{id}/pause")
    public String pauseJob(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        jobSchedulingService.pauseJob(id);
        redirectAttributes.addFlashAttribute("infoMessage", "Job paused.");
        return "redirect:/admin/scheduled";
    }

    @PostMapping("/{id}/resume")
    public String resumeJob(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        jobSchedulingService.resumeJob(id);
        redirectAttributes.addFlashAttribute("successMessage", "Job resumed and registered with scheduler.");
        return "redirect:/admin/scheduled";
    }

    @PostMapping("/{id}/disable")
    public String disableJob(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        jobSchedulingService.disableJob(id);
        redirectAttributes.addFlashAttribute("warningMessage", "Job disabled.");
        return "redirect:/admin/scheduled";
    }

    @PostMapping("/{id}/cron")
    public String updateCron(@PathVariable UUID id,
                             @RequestParam String cronExpression,
                             RedirectAttributes redirectAttributes) {
        jobSchedulingService.updateJobCron(id, cronExpression);
        redirectAttributes.addFlashAttribute("successMessage", "Cron schedule updated to '" + cronExpression + "'!");
        return "redirect:/admin/scheduled";
    }
}
