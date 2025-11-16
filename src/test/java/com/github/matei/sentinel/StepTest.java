package com.github.matei.sentinel;

import com.github.matei.sentinel.model.Step;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class StepTest {

    @Test
    void testStepCreation() {
        Instant now = Instant.now();
        Step step = new Step(
                "Compile",
                "completed",
                "success",
                now,
                now.plusSeconds(30)
        );

        assertEquals("Compile", step.getName());
        assertEquals("completed", step.getStatus());
        assertEquals("success", step.getConclusion());
        assertEquals(now, step.getStartedAt());
        assertEquals(now.plusSeconds(30), step.getCompletedAt());
    }

    @Test
    void testStepWithNullTimestamps() {
        Step step = new Step("Setup", "queued", null, null, null);

        assertEquals("Setup", step.getName());
        assertEquals("queued", step.getStatus());
        assertNull(step.getConclusion());
        assertNull(step.getStartedAt());
        assertNull(step.getCompletedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        Step step1 = new Step("Build", "completed", "success", now, now);
        Step step2 = new Step("Build", "completed", "success", now, now);
        Step step3 = new Step("Test", "completed", "success", now, now);

        assertEquals(step1, step2);
        assertNotEquals(step1, step3);
        assertEquals(step1.hashCode(), step2.hashCode());
    }
}
