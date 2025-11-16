package com.github.matei.sentinel.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a normalized monitoring event detected from GitHub workflow state changes.
 * <p>
 * This class serves as a unified representation of different types of events
 * (workflow queued, job started, step completed, etc.) that occur during
 * GitHub Actions execution. It is the output format that gets formatted and
 * printed to the console.
 * </p>
 *
 * <h2>Event Types</h2>
 * Events are categorized by {@link EventType}:
 * <ul>
 *   <li><b>Workflow events</b>: WORKFLOW_QUEUED, WORKFLOW_STARTED, WORKFLOW_COMPLETED</li>
 *   <li><b>Job events</b>: JOB_QUEUED, JOB_STARTED, JOB_COMPLETED</li>
 *   <li><b>Step events</b>: STEP_STARTED, STEP_COMPLETED</li>
 * </ul>
 *
 * <h2>Event Hierarchy</h2>
 * <pre>
 * Workflow Run (e.g., "CI Pipeline")
 *   ├── Job 1 (e.g., "build")
 *   │   ├── Step 1 (e.g., "Checkout")
 *   │   ├── Step 2 (e.g., "Build")
 *   │   └── Step 3 (e.g., "Test")
 *   └── Job 2 (e.g., "deploy")
 *       ├── Step 1 (e.g., "Deploy to staging")
 *       └── Step 2 (e.g., "Run smoke tests")
 * </pre>
 *
 * <h2>Field Availability by Event Type</h2>
 * <table border="1">
 *   <tr>
 *     <th>Event Type</th>
 *     <th>workflowName</th>
 *     <th>jobName</th>
 *     <th>stepName</th>
 *     <th>conclusion</th>
 *     <th>duration</th>
 *   </tr>
 *   <tr>
 *     <td>WORKFLOW_QUEUED</td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>WORKFLOW_STARTED</td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>WORKFLOW_COMPLETED</td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *   </tr>
 *   <tr>
 *     <td>JOB_STARTED</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>JOB_COMPLETED</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *   </tr>
 *   <tr>
 *     <td>STEP_STARTED</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>STEP_COMPLETED</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *   </tr>
 * </table>
 *
 * <h2>Common Fields</h2>
 * All events include:
 * <ul>
 *   <li><b>timestamp</b>: When the event occurred (always present)</li>
 *   <li><b>repository</b>: Format "owner/repo" (always present)</li>
 *   <li><b>branch</b>: Branch name where workflow runs (always present)</li>
 *   <li><b>sha</b>: Commit SHA (always present)</li>
 *   <li><b>workflowName</b>: Name of the workflow (always present)</li>
 * </ul>
 *
 * <h2>Optional Fields</h2>
 * Depending on event type:
 * <ul>
 *   <li><b>jobName</b>: Present for job and step events</li>
 *   <li><b>stepName</b>: Present only for step events</li>
 *   <li><b>conclusion</b>: Present only for completed events (success, failure, etc.)</li>
 *   <li><b>duration</b>: Present only for completed events (time taken to execute)</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * This class is immutable - all fields are {@code final}. This makes it
 * thread-safe and suitable for concurrent processing.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Workflow started event
 * MonitoringEvent workflowEvent = new MonitoringEvent(
 *     EventType.WORKFLOW_STARTED,
 *     Instant.now(),
 *     "microsoft/vscode",
 *     "main",
 *     "abc123def456",
 *     "CI Pipeline",
 *     null,  // no job name
 *     null,  // no step name
 *     null,  // no conclusion yet
 *     null   // no duration yet
 * );
 *
 * // Job completed event with duration
 * MonitoringEvent jobEvent = new MonitoringEvent(
 *     EventType.JOB_COMPLETED,
 *     Instant.now(),
 *     "microsoft/vscode",
 *     "main",
 *     "abc123def456",
 *     "CI Pipeline",
 *     "build",               // job name
 *     null,                  // no step name
 *     "success",             // conclusion
 *     Duration.ofMinutes(5)  // duration
 * );
 *
 * // Step completed event
 * MonitoringEvent stepEvent = new MonitoringEvent(
 *     EventType.STEP_COMPLETED,
 *     Instant.now(),
 *     "microsoft/vscode",
 *     "main",
 *     "abc123def456",
 *     "CI Pipeline",
 *     "build",               // job name
 *     "Run tests",           // step name
 *     "failure",             // conclusion
 *     Duration.ofSeconds(45) // duration
 * );
 * }</pre>
 *
 * @see EventType
 * @see com.github.matei.sentinel.formatter.EventFormatter
 * @see com.github.matei.sentinel.monitor.EventDetector
 * @since 1.0
 */
@Getter
@ToString
public class MonitoringEvent
{
    /**
     * Type of event that occurred.
     * Determines which fields are relevant for this event.
     *
     * @see EventType
     */
    private final EventType type;

    /**
     * Timestamp when this event occurred.
     * For started events: when execution began.
     * For completed events: when execution finished.
     */
    private final Instant timestamp;

    /**
     * Repository where this event occurred.
     * Format: "owner/repo" (e.g., "microsoft/vscode")
     */
    private final String repository;

    /**
     * Branch name where the workflow run was triggered.
     * Examples: "main", "develop", "feature/new-feature"
     */
    private final String branch;

    /**
     * Commit SHA that triggered the workflow run.
     * Full 40-character Git hash (typically displayed truncated).
     */
    private final String sha;

    /**
     * Name of the workflow from the YAML file.
     * Example: "CI Pipeline", "Release", "Tests"
     */
    private final String workflowName;

    /**
     * Name of the job (only present for job and step events).
     * May be {@code null} for workflow-level events.
     * Example: "build", "test", "deploy"
     */
    private final String jobName;

    /**
     * Name of the step (only present for step events).
     * May be {@code null} for workflow and job events.
     * Example: "Checkout code", "Run tests", "Deploy to production"
     */
    private final String stepName;

    /**
     * Conclusion of the completed entity (only for completed events).
     * Values: "success", "failure", "cancelled", "skipped"
     * May be {@code null} for queued/started events.
     */
    private final String conclusion;

    /**
     * Duration of execution (only for completed events).
     * Calculated as: end timestamp - start timestamp
     * May be {@code null} for queued/started events.
     */
    private final Duration duration;

    /**
     * Creates a new MonitoringEvent.
     *
     * @param type the event type
     * @param timestamp when the event occurred
     * @param repository repository in format "owner/repo"
     * @param branch branch name
     * @param sha commit SHA
     * @param workflowName workflow name
     * @param jobName job name (may be null)
     * @param stepName step name (may be null)
     * @param conclusion outcome (may be null)
     * @param duration execution time (may be null)
     */
    public MonitoringEvent(EventType type, Instant timestamp, String repository, String branch,
                           String sha, String workflowName, String jobName, String stepName,
                           String conclusion, Duration duration)
    {
        this.type = type;
        this.timestamp = timestamp;
        this.repository = repository;
        this.branch = branch;
        this.sha = sha;
        this.workflowName = workflowName;
        this.jobName = jobName;
        this.stepName = stepName;
        this.conclusion = conclusion;
        this.duration = duration;
    }
}
