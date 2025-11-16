package com.github.matei.sentinel.model;

/**
 * Enumeration of all possible monitoring event types in GitHub Actions workflows.
 * <p>
 * This enum categorizes events into three levels:
 * <ul>
 *   <li><b>Workflow-level events</b>: State changes of the entire workflow run</li>
 *   <li><b>Job-level events</b>: State changes of individual jobs within a workflow</li>
 *   <li><b>Step-level events</b>: State changes of individual steps within a job</li>
 * </ul>
 * </p>
 *
 * <h2>Event Hierarchy and Flow</h2>
 * A typical workflow execution generates events in this order:
 * <pre>
 * 1. WORKFLOW_QUEUED    - Workflow is queued
 * 2. WORKFLOW_STARTED   - Workflow begins execution
 * 3. JOB_QUEUED         - First job is queued
 * 4. JOB_STARTED        - First job starts
 * 5. STEP_STARTED       - First step in job starts
 * 6. STEP_COMPLETED     - First step completes
 * 7. STEP_STARTED       - Second step starts
 * 8. STEP_COMPLETED     - Second step completes
 * ... (more steps)
 * 9. JOB_COMPLETED      - First job completes
 * 10. JOB_QUEUED        - Second job is queued (if exists)
 * ... (more jobs)
 * 11. WORKFLOW_COMPLETED - Workflow completes
 * </pre>
 *
 * <h2>Event Detection Strategy</h2>
 * Events are detected by comparing the previous state with the current state:
 * <ul>
 *   <li><b>QUEUED</b>: Entity exists with status "queued" and wasn't seen before</li>
 *   <li><b>STARTED</b>: Entity's status changed from "queued" to "in_progress"</li>
 *   <li><b>COMPLETED</b>: Entity's status changed to "completed"</li>
 * </ul>
 *
 * <h2>Usage in Formatting</h2>
 * Event formatters use this enum to determine:
 * <ul>
 *   <li>Which fields to display (workflow events don't have job/step names)</li>
 *   <li>Whether to show conclusion and duration (only for completed events)</li>
 *   <li>Color coding (e.g., red for failures, green for successes)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * EventType type = EventType.JOB_COMPLETED;
 *
 * // Check event level
 * if (type == EventType.WORKFLOW_COMPLETED ||
 *     type == EventType.WORKFLOW_STARTED ||
 *     type == EventType.WORKFLOW_QUEUED) {
 *     System.out.println("This is a workflow-level event");
 * }
 *
 * // Check if event includes duration
 * if (type.name().endsWith("COMPLETED")) {
 *     System.out.println("This event should include duration");
 * }
 * }</pre>
 *
 * @see MonitoringEvent
 * @see com.github.matei.sentinel.monitor.EventDetector
 * @since 1.0
 */
public enum EventType
{
    /**
     * Workflow run has been queued and is waiting to start.
     * <p>
     * Triggered when: A new workflow run is created (e.g., by a push or PR)
     * and is waiting in the queue for execution.
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - null</li>
     *   <li>stepName - null</li>
     *   <li>conclusion - null</li>
     *   <li>duration - null</li>
     * </ul>
     * </p>
     */
    WORKFLOW_QUEUED,

    /**
     * Workflow run has started execution.
     * <p>
     * Triggered when: The workflow transitions from "queued" to "in_progress"
     * status, meaning at least one job has begun running.
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - null</li>
     *   <li>stepName - null</li>
     *   <li>conclusion - null</li>
     *   <li>duration - null</li>
     * </ul>
     * </p>
     */
    WORKFLOW_STARTED,

    /**
     * Workflow run has completed (successfully or with failures).
     * <p>
     * Triggered when: The workflow status changes to "completed".
     * Check the conclusion field to determine if it succeeded or failed.
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - null</li>
     *   <li>stepName - null</li>
     *   <li>conclusion - "success", "failure", "cancelled", or "skipped"</li>
     *   <li>duration - time from workflow start to completion</li>
     * </ul>
     * </p>
     */
    WORKFLOW_COMPLETED,

    /**
     * Job has been queued and is waiting for a runner.
     * <p>
     * Triggered when: A new job appears in the workflow run with status "queued".
     * The job is waiting for an available runner (VM) to execute on.
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - always present</li>
     *   <li>stepName - null</li>
     *   <li>conclusion - null</li>
     *   <li>duration - null</li>
     * </ul>
     * </p>
     */
    JOB_QUEUED,

    /**
     * Job has started execution on a runner.
     * <p>
     * Triggered when: The job's status transitions from "queued" to "in_progress",
     * or when a job first appears with "in_progress" status. The job is now
     * actively executing steps on a runner.
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - always present</li>
     *   <li>stepName - null</li>
     *   <li>conclusion - null</li>
     *   <li>duration - null</li>
     * </ul>
     * </p>
     */
    JOB_STARTED,

    /**
     * Job has completed execution (successfully or with failures).
     * <p>
     * Triggered when: The job's status changes to "completed".
     * Check the conclusion field to determine the outcome.
     * A job is considered successful only if all its steps succeeded.
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - always present</li>
     *   <li>stepName - null</li>
     *   <li>conclusion - "success", "failure", "cancelled", or "skipped"</li>
     *   <li>duration - time from job start to completion</li>
     * </ul>
     * </p>
     */
    JOB_COMPLETED,

    /**
     * Step within a job has started execution.
     * <p>
     * Triggered when: A step's status transitions from "queued" to "in_progress",
     * or when a step first appears with "in_progress" status. The step is now
     * actively running (executing a command or action).
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - always present</li>
     *   <li>stepName - always present</li>
     *   <li>conclusion - null</li>
     *   <li>duration - null</li>
     * </ul>
     * </p>
     */
    STEP_STARTED,

    /**
     * Step within a job has completed execution (successfully or with failures).
     * <p>
     * Triggered when: The step's status changes to "completed".
     * Check the conclusion field to determine if the step succeeded or failed.
     * A failed step typically causes the job to fail (unless configured with
     * {@code continue-on-error: true}).
     * </p>
     * <p>
     * Associated fields in {@link MonitoringEvent}:
     * <ul>
     *   <li>workflowName - always present</li>
     *   <li>jobName - always present</li>
     *   <li>stepName - always present</li>
     *   <li>conclusion - "success", "failure", "cancelled", or "skipped"</li>
     *   <li>duration - time from step start to completion</li>
     * </ul>
     * </p>
     */
    STEP_COMPLETED
}
