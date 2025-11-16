package com.github.matei.sentinel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a step within a GitHub Actions job.
 * Steps are the individual tasks that run within a job (e.g., "Checkout code", "Run tests", "Build").
 */

@AllArgsConstructor
@Getter
@ToString
public class Step
{
    private final String name;
    private final String status;
    private final String conclusion;
    private final Instant startedAt;
    private final Instant completedAt;

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
