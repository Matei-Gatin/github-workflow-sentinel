package com.github.matei.sentinel.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a step within a GitHub Actions job.
 * Steps are the individual tasks that run within a job (e.g., "Checkout code", "Run tests", "Build").
 */

@Getter
@ToString
public class Step
{
    private final String name;
    private final String status;
    private final String conclusion;
    private final Instant startedAt;
    private final Instant completedAt;

    public Step(String name, String status, String conclusion, Instant startedAt, Instant completedAt)
    {
        this.name = name;
        this.status = status;
        this.conclusion = conclusion;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    // Two steps are equal if they have the same name
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Step that = (Step) o;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.name);
    }
}
