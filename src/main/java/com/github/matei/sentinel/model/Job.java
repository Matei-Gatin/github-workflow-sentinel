package com.github.matei.sentinel.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.ToString;

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
    // Steps within this job (populated when fetching job details)
    private final List<Step> steps;

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
        this.steps = steps != null ? steps : Collections.emptyList();
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
