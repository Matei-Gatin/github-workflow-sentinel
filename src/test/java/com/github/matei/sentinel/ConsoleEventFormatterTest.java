package com.github.matei.sentinel;

import com.github.matei.sentinel.formatter.ConsoleEventFormatter;
import com.github.matei.sentinel.model.EventType;
import com.github.matei.sentinel.model.MonitoringEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleEventFormatterTest {

    private ConsoleEventFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ConsoleEventFormatter();
    }

    @Test
    void testFormatWorkflowStarted() {
        Instant timestamp = Instant.parse("2025-11-15T10:30:00Z");
        MonitoringEvent event = new MonitoringEvent(
                EventType.WORKFLOW_STARTED,
                timestamp,
                "owner/repo",
                "main",
                "abc123def",
                "CI Pipeline",
                null,
                null,
                null,
                null
        );

        String formatted = formatter.format(event);

        assertTrue(formatted.contains("[2025-11-15T10:30:00Z]"));
        assertTrue(formatted.contains("[WORKFLOW_STARTED]"));
        assertTrue(formatted.contains("repo:owner/repo"));
        assertTrue(formatted.contains("branch:main"));
        assertTrue(formatted.contains("sha:abc123d"));
        assertTrue(formatted.contains("workflow:\"CI Pipeline\""));
        assertFalse(formatted.contains("job:"));
        assertFalse(formatted.contains("step:"));
    }

    @Test
    void testFormatJobCompleted() {
        Instant timestamp = Instant.parse("2025-11-15T10:35:00Z");
        Duration duration = Duration.ofMinutes(5);

        MonitoringEvent event = new MonitoringEvent(
                EventType.JOB_COMPLETED,
                timestamp,
                "owner/repo",
                "main",
                "abc123def",
                "CI Pipeline",
                "build",
                null,
                "success",
                duration
        );

        String formatted = formatter.format(event);

        assertTrue(formatted.contains("[JOB_COMPLETED]"));
        assertTrue(formatted.contains("[SUCCESS]"));
        assertTrue(formatted.contains("job:build"));
        assertTrue(formatted.contains("Duration: 5m"));
        assertFalse(formatted.contains("step:"));
    }

    @Test
    void testFormatStepCompleted() {
        Instant timestamp = Instant.parse("2025-11-15T10:32:00Z");
        Duration duration = Duration.ofSeconds(45);

        MonitoringEvent event = new MonitoringEvent(
                EventType.STEP_COMPLETED,
                timestamp,
                "owner/repo",
                "feature-branch",
                "xyz789abc",
                "Test Suite",
                "test",
                "Run Tests",
                "failure",
                duration
        );

        String formatted = formatter.format(event);

        assertTrue(formatted.contains("[STEP_COMPLETED]"));
        assertTrue(formatted.contains("[FAILURE]"));
        assertTrue(formatted.contains("job:test"));
        assertTrue(formatted.contains("step:Run Tests"));
        assertTrue(formatted.contains("Duration: 45s"));
    }

    @Test
    void testFormatWithLongSha() {
        MonitoringEvent event = new MonitoringEvent(
                EventType.WORKFLOW_QUEUED,
                Instant.now(),
                "owner/repo",
                "main",
                "abcdef1234567890",  // Long SHA
                "CI",
                null,
                null,
                null,
                null
        );

        String formatted = formatter.format(event);

        // Should truncate to 7 characters
        assertTrue(formatted.contains("sha:abcdef1"));
        assertFalse(formatted.contains("sha:abcdef1234567890"));
    }

    @Test
    void testFormatDurationHoursMinutesSeconds() {
        Duration duration = Duration.ofHours(2).plusMinutes(35).plusSeconds(42);
        MonitoringEvent event = new MonitoringEvent(
                EventType.JOB_COMPLETED,
                Instant.now(),
                "owner/repo",
                "main",
                "abc",
                "Long Job",
                "build",
                null,
                "success",
                duration
        );

        String formatted = formatter.format(event);
        assertTrue(formatted.contains("Duration: 2h 35m 42s"));
    }
}
