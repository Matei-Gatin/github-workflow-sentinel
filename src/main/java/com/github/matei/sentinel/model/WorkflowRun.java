package com.github.matei.sentinel.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a GitHub Actions workflow run.
 * This is the top-level entity - a workflow contains multiple jobs.
 */

@Getter
@ToString
public class WorkflowRun
{
    private final long id;
    private final String name;
    private final String status;
    // Conclusion: "success", "failure", "cancelled", etc. (null if still running)
    private final String conclusion;
    private final String headBranch;
    private final String headSha;
    private final Instant updatedAt;
    // When the workflow finished (null if still running)
    private final Instant concludedAt;

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

    // For comparing Workflow runs (two runs are equal if they have the same ID)
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowRun that = (WorkflowRun) o;
        return id == that.id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
