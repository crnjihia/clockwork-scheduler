package com.clockwork.scheduler.repository;

import com.clockwork.scheduler.domain.ScheduledJob;
import com.clockwork.scheduler.domain.enums.JobStatus;
import com.clockwork.scheduler.domain.enums.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, UUID> {
    List<ScheduledJob> findByStatus(JobStatus status);
    List<ScheduledJob> findByType(JobType type);
    List<ScheduledJob> findByJobClass(String jobClass);
}
