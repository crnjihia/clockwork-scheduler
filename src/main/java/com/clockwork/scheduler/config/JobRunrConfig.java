package com.clockwork.scheduler.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRunrConfig {

    private static final Logger log = LoggerFactory.getLogger(JobRunrConfig.class);

    @Bean
    public RetryFilter retryFilter() {
        log.info("Configuring JobRunr RetryFilter: retries=3, exponential backoff seed=2");
        return new RetryFilter(3, 2);
    }

    @Bean
    public Object queueDepthGauge(StorageProvider storageProvider, MeterRegistry meterRegistry) {
        log.info("Registering Micrometer queue.depth gauge with JobRunr StorageProvider");
        return Gauge.builder("queue.depth", storageProvider, sp -> {
            try {
                return (double) sp.countJobs(StateName.ENQUEUED);
            } catch (Exception e) {
                return 0.0;
            }
        })
        .description("Number of jobs currently enqueued in JobRunr")
        .register(meterRegistry);
    }
}
