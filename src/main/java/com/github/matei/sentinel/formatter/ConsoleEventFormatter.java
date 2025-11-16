package com.github.matei.sentinel.formatter;

import com.github.matei.sentinel.model.MonitoringEvent;
import com.github.matei.sentinel.util.Constants;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Console implementation of EventFormatter.
 * Formats events in a readable single-line format for stdout.
 *
 * Format: [TIMESTAMP] [EVENT_TYPE] [STATUS] repo:owner/repo branch:main sha:abc123 workflow:"CI" job:build step:compile - Duration: 2m 30s
 */

public class ConsoleEventFormatter implements EventFormatter
{

    @Override
    public String format(MonitoringEvent event) {
        StringBuilder sb = new StringBuilder();

        // [TIMESTAMP]
        sb.append("[").append(Constants.TIMESTAMP_FORMATTER.format(event.getTimestamp())).append("]");

        // [EVENT_TYPE]
        sb.append("[").append(event.getType()).append("] ");

        // [STATUS] (only if conclusion is present)
        if (event.getConclusion() != null && !event.getConclusion().isEmpty())
        {
            sb.append("[").append(event.getConclusion().toUpperCase()).append("] ");
        }

        // repo:owner/repo
        sb.append("repo:").append(event.getRepository()).append(" ");

        // branch:main
        if (event.getBranch() != null)
        {
            sb.append("branch:").append(event.getBranch()).append(" ");
        }

        // sha:abc123
        if (event.getSha() != null)
        {
            sb.append("sha:").append(event.getSha(), 0, Math.min(Constants.SHA_DISPLAY_LENGTH, event.getSha().length())).append(" ");
        }

        // workflow:"CI Pipeline"
        if (event.getWorkflowName() != null)
        {
            sb.append("workflow:\"").append(event.getWorkflowName()).append("\" ");
        }

        // job:build
        if (event.getJobName() != null)
        {
            sb.append("job:").append(event.getJobName()).append(" ");
        }

        // step
        if (event.getStepName() != null)
        {
            sb.append("step:").append(event.getStepName()).append(" ");
        }

        // duration
        if (event.getDuration() != null)
        {
            sb.append("- Duration: ").append(formatDuration(event.getDuration()));
        }

        return sb.toString().trim();
    }

    /**
     * Formats a Duration into a human-readable string.
     * Examples: "2m 30s", "1h 15m 45s", "45s"
     *
     * @param duration the duration to format
     * @return formatted duration string
     */
    private String formatDuration(Duration duration)
    {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder result = new StringBuilder();

        if (hours > 0)
        {
            result.append(hours).append("h ");
        }

        if (minutes > 0)
        {
            result.append(minutes).append("m ");
        }

        if (seconds > 0 || result.isEmpty())
        {
            result.append(seconds).append("s");
        }

        return result.toString().trim();
    }
}
