package com.github.matei.sentinel.model;


import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a GitHub Actions workflow run.
 * <p>
 * A workflow run is an instance of a workflow being executed, triggered by
 * events such as pushes, pull requests, or scheduled runs. Each run has
 * a unique ID and progresses through various states (queued, in_progress, completed).
 * </p>
 *
 * <h2>Workflow Lifecycle</h2>
 * <ol>
 *   <li><b>queued</b>: Workflow is waiting to start</li>
 *   <li><b>in_progress</b>: Workflow is currently running</li>
 *   <li><b>completed</b>: Workflow has finished (check conclusion for outcome)</li>
 * </ol>
 *
 * <h2>Timestamps</h2>
 * <ul>
 *   <li><b>updatedAt</b>: Last time the workflow was updated (always present)</li>
 *   <li><b>concludedAt</b>: When the workflow completed (null if not completed)</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * This class is immutable - all fields are {@code final}. This makes it
 * thread-safe and suitable for use as a HashMap key.
 *
 * <h2>Equality</h2>
 * Two workflow runs are equal if they have the same {@code id}. This is because
 * the ID is the unique identifier in GitHub's system.
 *
 * @see Job
 * @see <a href="https://docs.github.com/en/rest/actions/workflow-runs">GitHub Workflow Runs API</a>
 * @since 1.0
 */

@Getter
@ToString
public class WorkflowRun
{
    /**
     * Unique identifier for this workflow run.
     * This is GitHub's internal ID for the run.
     */
    private final long id;

    /**
     * Name of the workflow (from the workflow YAML file).
     * Example: "CI Pipeline", "Release", "Tests"
     */
    private final String name;

    /**
     * Current status of the workflow run.
     * Valid values: "queued", "in_progress", "completed"
     *
     * @see com.github.matei.sentinel.util.Constants#STATUS_QUEUED
     * @see com.github.matei.sentinel.util.Constants#STATUS_IN_PROGRESS
     * @see com.github.matei.sentinel.util.Constants#STATUS_COMPLETED
     */
    private final String status;

    /**
     * Conclusion of the workflow run (only present when status is "completed").
     * Valid values: "success", "failure", "cancelled", "skipped", "timed_out", "action_required"
     * May be {@code null} if the workflow has not completed.
     */
    private final String conclusion;

    /**
     * Name of the branch this workflow run was triggered on.
     * Example: "main", "develop", "feature/new-feature"
     */
    private final String headBranch;

    /**
     * SHA of the commit that triggered this workflow run.
     * This is the full 40-character SHA (typically displayed truncated to 7 chars).
     */
    private final String headSha;

    /**
     * Last time this workflow run was updated.
     * This timestamp changes as the workflow progresses through different states.
     */
    private final Instant updatedAt;

    /**
     * Time when this workflow run concluded (completed).
     * This is {@code null} if the workflow is still queued or in progress.
     */
    private final Instant concludedAt;

    /**
     * Creates a new WorkflowRun instance.
     *
     * @param id unique workflow run identifier
     * @param name workflow name from YAML
     * @param status current status ("queued", "in_progress", "completed")
     * @param conclusion outcome if completed ("success", "failure", etc.), may be null
     * @param headBranch branch name
     * @param headSha commit SHA (40 characters)
     * @param updatedAt last update timestamp
     * @param concludedAt completion timestamp, may be null
     */
    public WorkflowRun(long id, String name, String status, String conclusion,
                       String headBranch, String headSha, Instant updatedAt, Instant concludedAt)
    {
        this.id = id;
        this.name = name;
        this.status = status;
        this.conclusion = conclusion;
        this.headBranch = headBranch;
        this.headSha = headSha;
        this.updatedAt = updatedAt;
        this.concludedAt = concludedAt;
    }

    /**
     * Two workflow runs are equal if they have the same ID.
     *
     * @param o object to compare
     * @return true if both have the same ID
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowRun that = (WorkflowRun) o;
        return this.id == that.id;
    }

    /**
     * Hash code based on workflow run ID.
     *
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
