package com.clockwork.scheduler.controller.api;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.enums.JobStatus;
import com.clockwork.scheduler.domain.enums.JobType;
import com.clockwork.scheduler.dto.requests.RunJobRequestDto;
import com.clockwork.scheduler.dto.requests.ScheduleJobRequestDto;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import com.clockwork.scheduler.service.JobSchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobApiController {

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobSchedulingService jobSchedulingService;

    public JobApiController(ScheduledJobRepository scheduledJobRepository,
                            JobSchedulingService jobSchedulingService) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.jobSchedulingService = jobSchedulingService;
    }

    @PostMapping("/schedule")
    public ResponseEntity<ScheduledJob> scheduleRecurringJob(@Valid @RequestBody ScheduleJobRequestDto request) {
        ScheduledJob job = new ScheduledJob();
        job.setId(UUID.randomUUID());
        job.setName(request.name());
        job.setType(JobType.RECURRING);
        job.setCronExpression(request.cronExpression());
        job.setJobClass(request.jobClass());
        job.setPayload(request.payload());
        job.setStatus(JobStatus.ACTIVE);
        job.setCreatedAt(Instant.now());

        ScheduledJob saved = scheduledJobRepository.save(job);
        jobSchedulingService.registerRecurringJob(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/run")
    public ResponseEntity<JobExecution> runOneOffJob(@Valid @RequestBody RunJobRequestDto request) {
        JobExecution execution = jobSchedulingService.scheduleOneOff(
                request.jobClass(),
                request.payload(),
                request.scheduleAt()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(execution);
    }

    @GetMapping
    public ResponseEntity<List<ScheduledJob>> getAllJobs() {
        return ResponseEntity.ok(scheduledJobRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledJob> getJobById(@PathVariable UUID id) {
        return scheduledJobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/run-now")
    public ResponseEntity<JobExecution> runNow(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(jobSchedulingService.runNow(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ScheduledJob> pauseJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobSchedulingService.pauseJob(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ScheduledJob> resumeJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobSchedulingService.resumeJob(id));
    }

    @PutMapping("/{id}/cron")
    public ResponseEntity<ScheduledJob> updateCron(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String newCron = body.get("cronExpression");
        if (newCron == null || newCron.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(jobSchedulingService.updateJobCron(id, newCron));
    }
}
