package com.github.matei.sentinel.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.ToString;

/**
 * Represents a GitHub Actions job within a workflow run.
 * <p>
 * A job is a unit of work within a workflow that runs on a specific runner
 * (virtual machine). Jobs can run in parallel or sequentially depending on
 * dependencies defined in the workflow YAML file. Each job contains multiple
 * steps that are executed sequentially.
 * </p>
 *
 * <h2>Job Lifecycle</h2>
 * <ol>
 *   <li><b>queued</b>: Job is waiting for a runner to become available</li>
 *   <li><b>in_progress</b>: Job is currently executing on a runner</li>
 *   <li><b>completed</b>: Job has finished (check conclusion for outcome)</li>
 * </ol>
 *
 * <h2>Job Structure</h2>
 * A typical job contains:
 * <ul>
 *   <li><b>Setup steps</b>: Checkout code, install dependencies</li>
 *   <li><b>Main steps</b>: Build, test, deploy</li>
 *   <li><b>Cleanup steps</b>: Upload artifacts, post-processing</li>
 * </ul>
 *
 * <h2>Timestamps</h2>
 * <ul>
 *   <li><b>startedAt</b>: When the job began execution (null if not started)</li>
 *   <li><b>completedAt</b>: When the job finished (null if not completed)</li>
 * </ul>
 * <p>
 * Duration can be calculated as: {@code Duration.between(startedAt, completedAt)}
 * </p>
 *
 * <h2>Conclusion Values</h2>
 * When a job completes, the conclusion indicates the outcome:
 * <ul>
 *   <li><b>success</b>: All steps completed successfully</li>
 *   <li><b>failure</b>: One or more steps failed</li>
 *   <li><b>cancelled</b>: Job was manually cancelled or workflow was cancelled</li>
 *   <li><b>skipped</b>: Job was skipped due to conditions (e.g., if: expression)</li>
 * </ul>
 *
 * <h2>Steps Collection</h2>
 * The steps list contains all steps defined in this job, in execution order.
 * The list is immutable and never null (may be empty for jobs without steps).
 *
 * <h2>Immutability</h2>
 * This class is immutable - all fields are {@code final} and the steps list
 * is an unmodifiable copy. This makes it thread-safe and suitable for use
 * as a HashMap key.
 *
 * <h2>Equality</h2>
 * Two jobs are equal if they have the same {@code id}. This is because the
 * ID is the unique identifier in GitHub's system.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * List<Step> steps = List.of(
 *     new Step("Checkout", "completed", "success", ...),
 *     new Step("Build", "completed", "success", ...)
 * );
 *
 * Job job = new Job(
 *     12345L,                  // id
 *     67890L,                  // runId
 *     "build",                 // name
 *     "completed",             // status
 *     "success",               // conclusion
 *     Instant.parse("2025-11-15T10:00:00Z"),  // startedAt
 *     Instant.parse("2025-11-15T10:05:30Z"),  // completedAt
 *     steps                    // steps
 * );
 *
 * Duration duration = Duration.between(job.getStartedAt(), job.getCompletedAt());
 * System.out.println("Job took: " + duration.toMinutes() + " minutes");
 * }</pre>
 *
 * @see WorkflowRun
 * @see Step
 * @see <a href="https://docs.github.com/en/rest/actions/workflow-jobs">GitHub Jobs API</a>
 * @since 1.0
 */
@Getter
@ToString
public class Job
{
    /**
     * Unique identifier for this job.
     * This is GitHub's internal ID for the job.
     */
    private final long id;

    /**
     * The workflow run ID that this job belongs to.
     * Links this job back to its parent workflow run.
     */
    private final long runId;

    /**
     * Name of the job as defined in the workflow YAML file.
     * Example: "build", "test", "deploy-production"
     */
    private final String name;

    /**
     * Current status of the job.
     * Valid values: "queued", "in_progress", "completed"
     *
     * @see com.github.matei.sentinel.util.Constants#STATUS_QUEUED
     * @see com.github.matei.sentinel.util.Constants#STATUS_IN_PROGRESS
     * @see com.github.matei.sentinel.util.Constants#STATUS_COMPLETED
     */
    private final String status;

    /**
     * Conclusion of the job (only present when status is "completed").
     * Valid values: "success", "failure", "cancelled", "skipped"
     * May be {@code null} if the job has not completed.
     */
    private final String conclusion;

    /**
     * Time when this job started execution.
     * This is {@code null} if the job is still queued.
     */
    private final Instant startedAt;

    /**
     * Time when this job finished execution.
     * This is {@code null} if the job is still queued or in progress.
     */
    private final Instant completedAt;

    /**
     * List of steps that are part of this job.
     * Steps are executed sequentially in the order they appear.
     * This list is immutable and never {@code null} (may be empty).
     */
    private final List<Step> steps;

    /**
     * Creates a new Job instance.
     *
     * @param id unique job identifier
     * @param runId workflow run ID this job belongs to
     * @param name job name from workflow YAML
     * @param status current status ("queued", "in_progress", "completed")
     * @param conclusion outcome if completed ("success", "failure", etc.), may be null
     * @param startedAt start timestamp, may be null if not started
     * @param completedAt completion timestamp, may be null if not completed
     * @param steps list of steps in this job, may be null (will be converted to empty list)
     */
    public Job(long id, long runId, String name, String status, String conclusion,
               Instant startedAt, Instant completedAt, List<Step> steps)
    {
        this.id = id;
        this.runId = runId;
        this.name = name;
        this.status = status;
        this.conclusion = conclusion;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        // Ensure steps is never null - convert null to empty immutable list
        this.steps = steps != null ? List.copyOf(steps) : Collections.emptyList();
    }

    /**
     * Two jobs are equal if they have the same ID.
     *
     * @param o object to compare
     * @return true if both have the same ID
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job that = (Job) o;
        return this.id == that.id;
    }

    /**
     * Hash code based on job ID.
     *
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
