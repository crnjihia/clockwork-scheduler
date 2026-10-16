package com.clockwork.scheduler.service;

import com.clockwork.scheduler.repository.JobExecutionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertingService {

    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);

    private final JobExecutionRepository jobExecutionRepository;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    @Value("${app.alerts.webhook-url:https://hooks.slack.com/services/mock/clockwork/alerts}")
    private String webhookUrl;

    @Value("${app.alerts.dlq-threshold-hourly:10}")
    private int dlqHourlyThreshold;

    public AlertingService(JobExecutionRepository jobExecutionRepository, MeterRegistry meterRegistry) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.meterRegistry = meterRegistry;
        this.restClient = RestClient.builder().build();
    }

    public void alertDeadLetterJob(UUID executionId, String jobRunrId, String jobClass, String errorMessage) {
        log.warn("CRITICAL ALERT: Job {} [{}] transitioned to DEAD_LETTER! Error: {}",
                jobClass, executionId, errorMessage);

        meterRegistry.counter("jobs.deadletter").increment();

        // Check if DLQ count > threshold in the past 1 hour
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long dlqCountLastHour = jobExecutionRepository.countDeadLetterSince(oneHourAgo);

        log.info("Current DLQ count in the last 1 hour: {} (Threshold: {})", dlqCountLastHour, dlqHourlyThreshold);

        String message = String.format("""
                🚨 *Clockwork Scheduler Alert* 🚨
                *Job Moved to DLQ*: `%s`
                *Execution ID*: `%s`
                *JobRunr ID*: `%s`
                *Error*: %s
                *DLQ Count (Last 1hr)*: %d %s
                """, jobClass, executionId, jobRunrId, errorMessage, dlqCountLastHour,
                dlqCountLastHour > dlqHourlyThreshold ? "⚠️ *[EXCEEDED HOURLY THRESHOLD!]*" : "");

        sendWebhookNotification(message);
    }

    public void checkAndAlertHourlyDlqThreshold() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long dlqCount = jobExecutionRepository.countDeadLetterSince(oneHourAgo);
        if (dlqCount > dlqHourlyThreshold) {
            String alert = String.format("🚨 CRITICAL: High DLQ Volume! %d jobs moved to DEAD_LETTER in the past 1 hour (Threshold: %d)",
                    dlqCount, dlqHourlyThreshold);
            log.warn(alert);
            sendWebhookNotification(alert);
        }
    }

    private void sendWebhookNotification(String markdownText) {
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("mock")) {
            log.info("[MOCK WEBHOOK ALERT DISPATCHED]:\n{}", markdownText);
            return;
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", markdownText))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Webhook alert delivered successfully to {}", webhookUrl);
        } catch (Exception e) {
            log.warn("Failed to dispatch alert webhook to {}: {}", webhookUrl, e.getMessage());
        }
    }
}
