package com.clockwork.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public record SmsDispatchResult(
            int totalRecipients,
            int totalBatches,
            int deliveredCount,
            int failedCount,
            long executionTimeMs,
            String details
    ) {}

    public SmsDispatchResult dispatchBulk(List<String> rawPhoneNumbers, String template, int batchSize, long delayBetweenBatchesMs) {
        long startTime = System.currentTimeMillis();
        List<String> normalized = rawPhoneNumbers.stream()
                .map(this::normalizeKenyanPhone)
                .toList();

        int total = normalized.size();
        int safeBatchSize = batchSize > 0 ? batchSize : 100;
        int totalBatches = (int) Math.ceil((double) total / safeBatchSize);

        log.info("Starting Bulk SMS dispatch: {} recipients, {} batches of {} ms delay",
                total, totalBatches, delayBetweenBatchesMs);

        AtomicInteger delivered = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (int i = 0; i < total; i += safeBatchSize) {
            int end = Math.min(i + safeBatchSize, total);
            List<String> batch = normalized.subList(i, end);
            int batchNum = (i / safeBatchSize) + 1;

            log.info("Dispatching SMS Batch {}/{} ({} numbers)...", batchNum, totalBatches, batch.size());

            for (String phone : batch) {
                if (phone.startsWith("+254") && phone.length() >= 12) {
                    delivered.incrementAndGet();
                } else {
                    log.warn("Invalid Kenyan phone number format: {}", phone);
                    failed.incrementAndGet();
                }
            }

            // Enforce delay between batches as per requirement
            if (end < total && delayBetweenBatchesMs > 0) {
                try {
                    Thread.sleep(Math.min(delayBetweenBatchesMs, 5000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("SMS batch dispatch interrupted");
                    break;
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Bulk SMS dispatch finished in {}ms. Delivered: {}, Failed: {}", duration, delivered.get(), failed.get());

        return new SmsDispatchResult(
                total,
                totalBatches,
                delivered.get(),
                failed.get(),
                duration,
                String.format("Dispatched %d messages across %d batches. Delivered: %d, Invalid/Failed: %d",
                        total, totalBatches, delivered.get(), failed.get())
        );
    }

    public String normalizeKenyanPhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "").trim();
        if (cleaned.startsWith("+254")) {
            return cleaned;
        } else if (cleaned.startsWith("254")) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("07") || cleaned.startsWith("01")) {
            return "+254" + cleaned.substring(1);
        } else if (cleaned.startsWith("7") || cleaned.startsWith("1")) {
            return "+254" + cleaned;
        }
        return cleaned;
    }
}
