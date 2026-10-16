# Clockwork Scheduler — Cron Expressions Reference

Clockwork Scheduler utilizes standard **Quartz-format 6-field cron expressions** (`second minute hour day-of-month month day-of-week`) with dynamic translation and validation for JobRunr distributed recurring jobs.

---

## 1. Cron Expression Field Specification

```
 *     *     *     *     *     ?
 |     |     |     |     |     |
 |     |     |     |     |     +-- Day of Week (1 - 7 or SUN - SAT, or ? when Day of Month is specified)
 |     |     |     |     +-------- Month (1 - 12 or JAN - DEC)
 |     |     |     +-------------- Day of Month (1 - 31, or ? when Day of Week is specified)
 |     |     +-------------------- Hour (0 - 23)
 |     +-------------------------- Minute (0 - 59)
 +-------------------------------- Second (0 - 59)
```

> **Note on `?` vs `*`**: Quartz requires that either *Day-of-Month* or *Day-of-Week* be specified as `?` to prevent conflicting day constraints. Clockwork automatically normalizes `?` to `*` when registering with JobRunr.

---

## 2. Kenyan Enterprise Production Schedules

The table below outlines real-world schedules deployed at Kenyan financial institutions, telcos, and SMEs:

| Workflow / Automation Run | Cron Expression | Quartz Meaning | Business Rationale (EAT / Nairobi Time) |
|---|---|---|---|
| **Nightly M-Pesa Reconciliation** | `0 0 2 * * ?` | At 02:00:00 AM every day | Matches Safaricom Daraja batch midnight cutoff; reconciles before morning office hours. |
| **Daily DB Snapshot (pg_dump)** | `0 30 1 * * ?` | At 01:30:00 AM every day | Off-peak period; low connection activity ensures snapshot consistency. |
| **Monthly Customer Statements** | `0 0 6 1 * ?` | At 06:00:00 AM, on day 1 of the month | Delivers billing statements to finance directors on the first morning of the month. |
| **Staff Salary Bulk SMS** | `0 0 9 25 * ?` | At 09:00:00 AM, on day 25 of the month | Dispatches salary credit notifications on Kenyan corporate payroll date. |
| **High-Frequency Payment Polling** | `0 */5 * * * ?` | Every 5 minutes | For near real-time payment reconciliation during operational hours. |
| **Business Hours Sync (Mon - Fri)** | `0 0 8-17/1 ? * MON-FRI` | Hourly from 8am to 5pm on weekdays | Working hours ledger sync between branch offices and Nairobi HQ. |
| **Weekly Data Warehousing Sync** | `0 0 23 ? * SUN` | At 11:00:00 PM on Sunday | End-of-week data warehouse rollups and archiving to cold storage. |
| **Bi-Weekly VAT / KRA Audit** | `0 0 4 10,20 * ?` | At 04:00:00 AM on days 10 & 20 of the month | Kenya Revenue Authority (KRA) eTIMS audit log aggregation ahead of statutory deadlines. |

---

## 3. High-Frequency & Development Schedules

For testing, load testing, or rapid verification in local and sandbox environments:

| Use Case | Cron Expression | Interval |
|---|---|---|
| Every 10 seconds | `*/10 * * * * ?` | 10s |
| Every 30 seconds | `*/30 * * * * ?` | 30s |
| Every 1 minute | `0 * * * * ?` | 60s |
| Every 15 minutes | `0 */15 * * * ?` | 15 min |
| Twice daily (Noon & Midnight) | `0 0 0,12 * * ?` | 12 hours |

---

## 4. Special Characters & Syntax Reference

| Character | Description | Example |
|---|---|---|
| `*` | Matches every possible value of the field | `*` in hour = every hour |
| `?` | No specific value (used in day-of-month or day-of-week) | `0 0 2 * * ?` |
| `-` | Defines a closed range of values | `MON-FRI` = Monday through Friday |
| `,` | Separates a list of discrete values | `1,15` = 1st and 15th of month |
| `/` | Specifies incremental step values | `*/15` in minutes = every 15 minutes |
| `L` | "Last" — last day of the month or last Friday | `0 0 17 L * ?` = 5 PM on last day of month |

---

## 5. REST API Cron Modification

To update an existing schedule via Clockwork's REST API:

```http
PUT /api/jobs/{jobId}/cron
Content-Type: application/json

{
  "cronExpression": "0 0 3 * * ?"
}
```

The server automatically unregisters the existing `RecurringJob` in JobRunr, validates the new expression, updates the PostgreSQL database record, and re-registers the schedule with zero downtime.
