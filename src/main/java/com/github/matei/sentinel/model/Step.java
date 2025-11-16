package com.github.matei.sentinel.model;


import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a step within a GitHub Actions job.
 * <p>
 * A step is an individual task within a job that runs a command or action.
 * Steps are executed sequentially within their parent job, and each step
 * can either succeed or fail. If a step fails, subsequent steps in the job
 * are typically skipped (unless configured otherwise).
 * </p>
 *
 * <h2>Step Types</h2>
 * GitHub Actions supports two types of steps:
 * <ul>
 *   <li><b>Run steps</b>: Execute shell commands (e.g., {@code run: npm test})</li>
 *   <li><b>Action steps</b>: Use pre-built actions (e.g., {@code uses: actions/checkout@v3})</li>
 * </ul>
 *
 * <h2>Step Lifecycle</h2>
 * <ol>
 *   <li><b>queued</b>: Step is waiting to run (parent job hasn't started yet)</li>
 *   <li><b>in_progress</b>: Step is currently executing</li>
 *   <li><b>completed</b>: Step has finished (check conclusion for outcome)</li>
 * </ol>
 *
 * <h2>Timestamps</h2>
 * <ul>
 *   <li><b>startedAt</b>: When the step began execution (null if not started)</li>
 *   <li><b>completedAt</b>: When the step finished (null if not completed)</li>
 * </ul>
 * <p>
 * Duration can be calculated as: {@code Duration.between(startedAt, completedAt)}
 * </p>
 *
 * <h2>Conclusion Values</h2>
 * When a step completes, the conclusion indicates the outcome:
 * <ul>
 *   <li><b>success</b>: Step completed successfully (exit code 0)</li>
 *   <li><b>failure</b>: Step failed (non-zero exit code)</li>
 *   <li><b>cancelled</b>: Step was cancelled (usually because job was cancelled)</li>
 *   <li><b>skipped</b>: Step was skipped due to conditions or previous step failure</li>
 * </ul>
 *
 * <h2>Common Step Examples</h2>
 * <ul>
 *   <li>"Checkout code" - {@code uses: actions/checkout@v3}</li>
 *   <li>"Set up Node" - {@code uses: actions/setup-node@v3}</li>
 *   <li>"Install dependencies" - {@code run: npm install}</li>
 *   <li>"Run tests" - {@code run: npm test}</li>
 *   <li>"Build" - {@code run: npm run build}</li>
 *   <li>"Upload artifacts" - {@code uses: actions/upload-artifact@v3}</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * This class is immutable - all fields are {@code final}. This makes it
 * thread-safe and suitable for use in collections.
 *
 * <h2>Equality</h2>
 * Two steps are equal if they have the same name, status, conclusion, and timestamps.
 * Note: Steps don't have unique IDs in GitHub's API, so we use all fields for equality.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Step step = new Step(
 *     "Run tests",                    // name
 *     "completed",                    // status
 *     "success",                      // conclusion
 *     Instant.parse("2025-11-15T10:02:00Z"),  // startedAt
 *     Instant.parse("2025-11-15T10:03:45Z")   // completedAt
 * );
 *
 * if (step.getConclusion().equals("success")) {
 *     Duration duration = Duration.between(step.getStartedAt(), step.getCompletedAt());
 *     System.out.println("Tests passed in " + duration.toSeconds() + " seconds");
 * }
 * }</pre>
 *
 * @see Job
 * @see <a href="https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#jobsjob_idsteps">GitHub Steps Documentation</a>
 * @since 1.0
 */
@Getter
@ToString
public class Step
{
    /**
     * Name of the step as defined in the workflow YAML file.
     * Examples: "Checkout code", "Run tests", "Build application"
     * <p>
     * For action steps, this is typically the action name.
     * For run steps, this is the "name" field or auto-generated from the command.
     * </p>
     */
    private final String name;

    /**
     * Current status of the step.
     * Valid values: "queued", "in_progress", "completed"
     *
     * @see com.github.matei.sentinel.util.Constants#STATUS_QUEUED
     * @see com.github.matei.sentinel.util.Constants#STATUS_IN_PROGRESS
     * @see com.github.matei.sentinel.util.Constants#STATUS_COMPLETED
     */
    private final String status;

    /**
     * Conclusion of the step (only present when status is "completed").
     * Valid values: "success", "failure", "cancelled", "skipped"
     * May be {@code null} if the step has not completed.
     */
    private final String conclusion;

    /**
     * Time when this step started execution.
     * This is {@code null} if the step hasn't started yet (parent job not started).
     */
    private final Instant startedAt;

    /**
     * Time when this step finished execution.
     * This is {@code null} if the step is still queued or in progress.
     */
    private final Instant completedAt;

    /**
     * Creates a new Step instance.
     *
     * @param name step name from workflow YAML
     * @param status current status ("queued", "in_progress", "completed")
     * @param conclusion outcome if completed ("success", "failure", etc.), may be null
     * @param startedAt start timestamp, may be null if not started
     * @param completedAt completion timestamp, may be null if not completed
     */
    public Step(String name, String status, String conclusion, Instant startedAt, Instant completedAt)
    {
        this.name = name;
        this.status = status;
        this.conclusion = conclusion;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    /**
     * Two steps are equal if all their fields match.
     * <p>
     * Note: Steps don't have unique IDs in GitHub's API, so we compare
     * all fields to determine equality.
     * </p>
     *
     * @param o object to compare
     * @return true if all fields match
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Step that = (Step) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.status, that.status) &&
                Objects.equals(this.conclusion, that.conclusion) &&
                Objects.equals(this.startedAt, that.startedAt) &&
                Objects.equals(this.completedAt, that.completedAt);
    }

    /**
     * Hash code based on all fields.
     *
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(name, status, conclusion, startedAt, completedAt);
    }
}
