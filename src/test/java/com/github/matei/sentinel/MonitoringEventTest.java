package com.github.matei.sentinel;

import com.github.matei.sentinel.model.EventType;
import com.github.matei.sentinel.model.MonitoringEvent;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class MonitoringEventTest {

    @Test
    void testWorkflowEvent() {
        Instant now = Instant.now();
        MonitoringEvent event = new MonitoringEvent(
                EventType.WORKFLOW_STARTED,
                now,
                "owner/repo",
                "main",
                "abc123",
                "CI Pipeline",
                null,
                null,
                null,
                null
        );

        assertEquals(EventType.WORKFLOW_STARTED, event.getType());
        assertEquals(now, event.getTimestamp());
        assertEquals("owner/repo", event.getRepository());
        assertEquals("main", event.getBranch());
        assertEquals("abc123", event.getSha());
        assertEquals("CI Pipeline", event.getWorkflowName());
        assertNull(event.getJobName());
        assertNull(event.getStepName());
    }

    @Test
    void testJobEventWithDuration() {
        Instant now = Instant.now();
        Duration duration = Duration.ofMinutes(5);

        MonitoringEvent event = new MonitoringEvent(
                EventType.JOB_COMPLETED,
                now,
                "owner/repo",
                "main",
                "abc123",
                "CI Pipeline",
                "build",
                null,
                "success",
                duration
        );

        assertEquals(EventType.JOB_COMPLETED, event.getType());
        assertEquals("build", event.getJobName());
        assertEquals("success", event.getConclusion());
        assertEquals(duration, event.getDuration());
    }

    @Test
    void testStepEvent() {
        Instant now = Instant.now();
        Duration duration = Duration.ofSeconds(30);

        MonitoringEvent event = new MonitoringEvent(
                EventType.STEP_COMPLETED,
                now,
                "owner/repo",
                "main",
                "abc123",
                "CI Pipeline",
                "build",
                "Compile",
                "success",
                duration
        );

        assertEquals(EventType.STEP_COMPLETED, event.getType());
        assertEquals("build", event.getJobName());
        assertEquals("Compile", event.getStepName());
        assertEquals("success", event.getConclusion());
    }
}
