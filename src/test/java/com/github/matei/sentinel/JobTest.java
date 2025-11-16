package com.github.matei.sentinel;

import com.github.matei.sentinel.model.Job;
import com.github.matei.sentinel.model.Step;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobTest {

    @Test
    void testJobCreation() {
        Instant now = Instant.now();
        List<Step> steps = new ArrayList<>();

        Job job = new Job(
                456L,
                123L,
                "build",
                "completed",
                "success",
                now,
                now.plusSeconds(60),
                steps
        );

        assertEquals(456L, job.getId());
        assertEquals(123L, job.getRunId());
        assertEquals("build", job.getName());
        assertEquals("completed", job.getStatus());
        assertEquals("success", job.getConclusion());
        assertEquals(now, job.getStartedAt());
        assertEquals(now.plusSeconds(60), job.getCompletedAt());
        assertTrue(job.getSteps().isEmpty());
    }

    @Test
    void testJobWithNullSteps() {
        Instant now = Instant.now();
        Job job = new Job(456L, 123L, "build", "completed", "success", now, now, null);

        assertNotNull(job.getSteps());
        assertTrue(job.getSteps().isEmpty());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        Job job1 = new Job(123L, 456L, "build", "completed", "success", now, now, null);
        Job job2 = new Job(123L, 456L, "build", "completed", "success", now, now, null);
        Job job3 = new Job(789L, 456L, "test", "completed", "success", now, now, null);

        assertEquals(job1, job2);
        assertNotEquals(job1, job3);
        assertEquals(job1.hashCode(), job2.hashCode());
    }
}
