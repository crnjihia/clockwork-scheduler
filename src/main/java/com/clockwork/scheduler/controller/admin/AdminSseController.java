package com.clockwork.scheduler.controller.admin;

import com.clockwork.scheduler.service.JobExecutionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/admin/sse")
public class AdminSseController {

    private final JobExecutionService jobExecutionService;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public AdminSseController(JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        var scheduledFuture = executor.scheduleAtFixedRate(() -> {
            try {
                var stats = jobExecutionService.getDashboardStats();
                emitter.send(SseEmitter.event()
                        .name("stats-update")
                        .data(stats));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 5, TimeUnit.SECONDS);

        emitter.onCompletion(() -> scheduledFuture.cancel(true));
        emitter.onTimeout(() -> scheduledFuture.cancel(true));
        emitter.onError(e -> scheduledFuture.cancel(true));

        return emitter;
    }
}
