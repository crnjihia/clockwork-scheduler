# Changelog & Release Notes

All notable changes to the **Clockwork Scheduler** platform will be documented in this file.

## [v1.0.2] - 2026-10-16

### Enhanced Documentation, Live Screenshot & Standalone Demo Engine

#### 🌟 What's New
- **Live Mission Control Screenshot**: Embedded authentic dashboard capture showing real-time execution telemetry, live SSE status, active background workers, and DLQ diagnostics.
- **Standalone Demo Profile (`--spring.profiles.active=demo`)**: Allows anyone to launch the platform locally without Docker or external PostgreSQL, auto-seeding M-Pesa reconciliations, bulk SMS campaigns, and statement workflows.
- **Enhanced README**: Added system architecture diagrams, state progression sequences, observability mappings, and REST API examples.
- **MIT License**: Included official open-source license.

---

## [v1.0.1] - 2026-10-16

### GitHub Pages Web Showcase & Interactive Simulator

#### 🚀 Features
- **Interactive Web Showcase**: Hosted on GitHub Pages with dark glassmorphic styling, live stats counter, and responsive layouts.
- **Live Workflow State Machine Simulator**: Client-side interactive runner simulating linear progression and failure injection into DLQ.
- **Kenya Business Cron Calculator**: Interactive tool computing next run timestamps for M-Pesa recon (3:00 AM EAT), bulk SMS, and database backups.
- **Automated CI/CD**: Added GitHub Actions deployment workflow for GitHub Pages (`.github/workflows/pages.yml`).

---

## [v1.0.0] - 2026-10-16

### Initial Production Release - Distributed Job Scheduling Platform

#### 🚀 Features & Capabilities
- **Distributed Core Engine**: Powered by JobRunr 7.2.2 with 8 concurrent background workers and 15s polling interval backed by PostgreSQL storage provider and Redis caching.
- **Kenyan Back-Office Workflows**:
  - **M-Pesa Nightly Reconciliation**: Parses Paybill C2B statement files, calculates variances, matches settlements, and alerts on anomalies.
  - **Bulk SMS Dispatches**: Rate-limited chunked SMS dispatcher (50 ms delays) supporting batch delivery to Safaricom/Airtel networks with failed message retry buffers.
  - **On-Demand PDF Reports**: Dynamic financial statements, tax audits, and operational summaries generated using OpenPDF and uploaded to S3/MinIO.
  - **Automated Database Snapshots**: PostgreSQL automated dumps with gzip compression and retention window enforcement.
  - **Multi-Step Statement Pipeline**: Spring State Machine 3.2.1 orchestrating linear progression (`CREATED -> GENERATING -> EMAILING -> ARCHIVING -> COMPLETED`) with failure handling and step retry capability.
- **Reliability & Dead Letter Queue (DLQ)**:
  - Custom `ClockworkJobExecutionFilter` intercepting terminal job failures.
  - Exponential backoff retry policies (`RetryFilter(3, 2)`).
  - Automated Slack/Webhook alerting when DLQ threshold exceeds 10 jobs/hour.
  - Admin UI single-click requeue and batch re-drive.
- **Admin UI & Real-Time Monitoring**:
  - Responsive Thymeleaf + Vanilla CSS dashboard with Dark Slate / Amber accents.
  - Server-Sent Events (SSE) `/admin/sse/metrics` broadcasting real-time execution counts and queue depth.
  - Embedded JobRunr management console on port 8000.
- **Observability & Infrastructure**:
  - Actuator `/actuator/prometheus` and custom Micrometer gauges (`queue.depth`, `dlq.size`).
  - Pre-configured Grafana dashboard (`docker/grafana/dashboards/clockwork-scheduler.json`).
  - Production `docker-compose.yml` orchestrating App, PostgreSQL 15, Redis 7, MinIO, Prometheus, and Grafana.
  - Postman API collection for all Job, Execution, and Workflow REST endpoints.
