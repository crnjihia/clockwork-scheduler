package com.clockwork.scheduler.repository;

import com.clockwork.scheduler.domain.JobExecution;
import com.clockwork.scheduler.domain.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    Optional<JobExecution> findByJobRunrId(String jobRunrId);

    List<JobExecution> findByJobIdOrderByStartedAtDesc(UUID jobId);

    Page<JobExecution> findByStatus(ExecutionStatus status, Pageable pageable);

    long countByStatus(ExecutionStatus status);

    @Query("SELECT AVG(e.durationMs) FROM JobExecution e WHERE e.status = 'SUCCEEDED' AND e.durationMs IS NOT NULL")
    Double getAverageDurationMs();

    @Query("SELECT COUNT(e) FROM JobExecution e WHERE e.status = 'DEAD_LETTER' AND e.finishedAt >= :since")
    long countDeadLetterSince(@Param("since") Instant since);

    @Query("SELECT e FROM JobExecution e WHERE " +
           "(:jobId IS NULL OR e.jobId = :jobId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:fromInstant IS NULL OR e.startedAt >= :fromInstant) AND " +
           "(:toInstant IS NULL OR e.startedAt <= :toInstant) " +
           "ORDER BY e.startedAt DESC")
    Page<JobExecution> searchExecutions(
            @Param("jobId") UUID jobId,
            @Param("status") ExecutionStatus status,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant,
            Pageable pageable
    );

    List<JobExecution> findTop10ByOrderByStartedAtDesc();
}
