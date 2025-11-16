package com.github.matei.sentinel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a monitoring event to be displayed to the user.
 * This is a "normalized" event that combines data from WorkflowRun, Job, and Step.
 */

@AllArgsConstructor
@Getter
@ToString
public class MonitoringEvent /* What we print to console */
{
    private final EventType type;
    // when this event occurred
    private final Instant timestamp;
    private final String repository;
    private final String branch;
    private final String sha;
    private final String workflowName;
    // Job name (e.g.: "build") - null for workflow-level events
    private final String jobName;
    // Step name (e.g.: "Run tests") - null for workflow-level / job events
    private final String stepName;
    // Conclusion/status (e.g.: "success", "failure") - null for STARTED/QUEUED events
    private final String conclusion;
    private final Duration duration;


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitoringEvent that = (MonitoringEvent) o;
        return type == that.type &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(workflowName, that.workflowName) &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(stepName, that.stepName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, timestamp, repository, workflowName, jobName, stepName);
    }
}
