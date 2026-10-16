package com.clockwork.scheduler.repository;

import com.clockwork.scheduler.domain.Workflow;
import com.clockwork.scheduler.domain.enums.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByStatus(WorkflowStatus status);
    Page<Workflow> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
