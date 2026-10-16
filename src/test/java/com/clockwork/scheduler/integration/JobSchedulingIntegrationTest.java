package com.clockwork.scheduler.integration;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import com.clockwork.scheduler.domain.enums.JobStatus;
import com.clockwork.scheduler.domain.enums.JobType;
import com.clockwork.scheduler.repository.JobExecutionRepository;
import com.clockwork.scheduler.repository.ScheduledJobRepository;
import com.clockwork.scheduler.service.JobSchedulingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JobSchedulingIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulingIntegrationTest.class);

    private static PostgreSQLContainer<?> postgres;
    private static boolean dockerAvailable = false;

    static {
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("clockwork_test")
                        .withUsername("test")
                        .withPassword("test");
                postgres.start();
                dockerAvailable = true;
                log.info("Testcontainers PostgreSQL started successfully at {}", postgres.getJdbcUrl());
            } else {
                log.warn("Docker environment not available for Testcontainers; using in-memory PostgreSQL test profile.");
            }
        } catch (Throwable t) {
            log.warn("Testcontainers PostgreSQL could not start: {}. Falling back to in-memory PostgreSQL mode.", t.getMessage());
            dockerAvailable = false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (dockerAvailable && postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Autowired
    private JobSchedulingService jobSchedulingService;

    @Autowired
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Test
    @DisplayName("Schedule job and verify execution row persistence in Database")
    void testScheduleJobAndAssertExecutionRow() {
        // 1. Create and persist ScheduledJob
        ScheduledJob job = new ScheduledJob();
        job.setId(UUID.randomUUID());
        job.setName("Integration Test M-Pesa Recon");
        job.setType(JobType.RECURRING);
        job.setCronExpression("0 0 3 * * ?");
        job.setJobClass("MpesaReconciliationJob");
        job.setPayload("{\"paybillNumber\":\"654321\",\"accountReference\":\"TEST\"}");
        job.setStatus(JobStatus.ACTIVE);
        job.setCreatedAt(Instant.now());

        ScheduledJob savedJob = scheduledJobRepository.save(job);
        assertNotNull(savedJob.getId());

        // 2. Trigger run now via scheduling service
        JobExecution execution = jobSchedulingService.runNow(savedJob.getId());
        assertNotNull(execution);
        assertNotNull(execution.getId());

        // 3. Assert execution row is in Database
        Optional<JobExecution> foundOpt = jobExecutionRepository.findById(execution.getId());
        assertTrue(foundOpt.isPresent(), "Job execution row should exist in database");

        JobExecution found = foundOpt.get();
        assertEquals(savedJob.getId(), found.getJobId());
        assertEquals(ExecutionStatus.SCHEDULED, found.getStatus());
        assertNotNull(found.getStartedAt());
    }
}
