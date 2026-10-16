package com.clockwork.scheduler.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter jobsEnqueuedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("jobs.enqueued")
                .description("Total number of jobs enqueued into the scheduling pipeline")
                .register(meterRegistry);
    }

    @Bean
    public Counter jobsSucceededCounter(MeterRegistry meterRegistry) {
        return Counter.builder("jobs.succeeded")
                .description("Total number of jobs that successfully completed")
                .register(meterRegistry);
    }

    @Bean
    public Counter jobsFailedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("jobs.failed")
                .description("Total number of job execution attempts that failed")
                .register(meterRegistry);
    }

    @Bean
    public Counter jobsDeadletterCounter(MeterRegistry meterRegistry) {
        return Counter.builder("jobs.deadletter")
                .description("Total number of jobs that exceeded retries and moved to DLQ")
                .register(meterRegistry);
    }
}
