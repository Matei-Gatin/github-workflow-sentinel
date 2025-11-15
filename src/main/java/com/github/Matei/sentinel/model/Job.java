package com.github.Matei.sentinel.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a job within a GitHub Actions workflow run.
 * A workflow contains multiple jobs (e.g., "build", "test", "deploy").
 */

@Getter
@ToString
public class Job
{
    // unique job id identifier
    private final long id;
    // The workflow run this job belongs to
    private final long runId;
    private final String name;
    // Current status "queued", "in_progress", "completed"
    private final String status;
    private final String conclusion;
    private final Instant startedAt;
    private final Instant completedAt;

    public Job(long id, long runId, String name, String status, String conclusion,
               Instant startedAt, Instant completedAt)
    {
        this.id = id;
        this.runId = runId;
        this.name = name;
        this.status = status;
        this.conclusion = conclusion;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job that = (Job) o;
        return this.id == that.id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
