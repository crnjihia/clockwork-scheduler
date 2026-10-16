-- Clockwork Scheduler Database Schema
-- Version: V1

CREATE TABLE IF NOT EXISTS scheduled_jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- RECURRING, ONE_OFF
    cron_expression VARCHAR(100),
    job_class VARCHAR(255) NOT NULL,
    payload JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PAUSED, DISABLED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at TIMESTAMP WITH TIME ZONE,
    next_run_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_status ON scheduled_jobs(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_type ON scheduled_jobs(type);

CREATE TABLE IF NOT EXISTS job_executions (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES scheduled_jobs(id) ON DELETE SET NULL,
    job_runr_id VARCHAR(100),
    status VARCHAR(50) NOT NULL, -- SCHEDULED, PROCESSING, SUCCEEDED, FAILED, DEAD_LETTER
    attempt INT NOT NULL DEFAULT 1,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    error_message TEXT,
    stack_trace TEXT,
    result_summary TEXT
);

CREATE INDEX IF NOT EXISTS idx_job_executions_job_id ON job_executions(job_id);
CREATE INDEX IF NOT EXISTS idx_job_executions_status ON job_executions(status);
CREATE INDEX IF NOT EXISTS idx_job_executions_started_at ON job_executions(started_at);
CREATE INDEX IF NOT EXISTS idx_job_executions_jobrunr_id ON job_executions(job_runr_id);

CREATE TABLE IF NOT EXISTS workflows (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    initial_state VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    current_state VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    context JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflows_status ON workflows(status);
CREATE INDEX IF NOT EXISTS idx_workflows_current_state ON workflows(current_state);

CREATE TABLE IF NOT EXISTS workflow_steps (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    step_order INT NOT NULL,
    handler VARCHAR(255) NOT NULL,
    on_success_state VARCHAR(50) NOT NULL,
    on_failure_state VARCHAR(50) NOT NULL,
    retry_policy VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    step_result TEXT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_workflow_steps_workflow_id ON workflow_steps(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_steps_status ON workflow_steps(status);

-- Seed initial default recurring jobs for Kenyan businesses
INSERT INTO scheduled_jobs (id, name, type, cron_expression, job_class, payload, status, created_at, next_run_at)
VALUES 
(
    '11111111-1111-1111-1111-111111111111',
    'Nightly M-Pesa Reconciliation',
    'RECURRING',
    '0 0 2 * * ?', -- 2:00 AM daily
    'MpesaReconciliationJob',
    '{"paybillNumber": "654321", "accountReference": "KENYA-SETTLEMENT", "autoFlagDiscrepancyThreshold": 5000.0, "notifyEmail": "settlements@clockwork.co.ke"}'::jsonb,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 day'
),
(
    '22222222-2222-2222-2222-222222222222',
    'Daily DB Snapshot Backup',
    'RECURRING',
    '0 30 1 * * ?', -- 1:30 AM daily
    'DatabaseSnapshotJob',
    '{"databaseName": "clockwork_db", "retentionDays": 30, "targetS3Bucket": "clockwork-backups", "compression": "gzip"}'::jsonb,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 day'
),
(
    '33333333-3333-3333-3333-333333333333',
    'Monthly Customer Statement Run',
    'RECURRING',
    '0 0 6 1 * ?', -- 6:00 AM on the 1st of every month
    'StatementWorkflowJob',
    '{"businessUnit": "Nairobi-HQ", "notifyChannel": "EMAIL_AND_SMS", "archiveBucket": "clockwork-archive"}'::jsonb,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 month'
);
