package com.github.matei.sentinel;

import com.github.matei.sentinel.model.WorkflowRun;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowRunTest {

    @Test
    void testWorkflowRunCreation() {
        Instant now = Instant.now();
        WorkflowRun run = new WorkflowRun(
                12345L,
                "CI Pipeline",
                "completed",
                "success",
                "main",
                "abc123def456",
                now,
                now.plusSeconds(120)
        );

        assertEquals(12345L, run.getId());
        assertEquals("CI Pipeline", run.getName());
        assertEquals("completed", run.getStatus());
        assertEquals("success", run.getConclusion());
        assertEquals("main", run.getHeadBranch());
        assertEquals("abc123def456", run.getHeadSha());
        assertEquals(now, run.getUpdatedAt());
        assertEquals(now.plusSeconds(120), run.getConcludedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        WorkflowRun run1 = new WorkflowRun(123L, "CI", "completed", "success", "main", "abc", now, now);
        WorkflowRun run2 = new WorkflowRun(123L, "CI", "completed", "success", "main", "abc", now, now);
        WorkflowRun run3 = new WorkflowRun(456L, "CI", "completed", "success", "main", "abc", now, now);

        assertEquals(run1, run2);
        assertNotEquals(run1, run3);
        assertEquals(run1.hashCode(), run2.hashCode());
        assertNotEquals(run1.hashCode(), run3.hashCode());
    }

    @Test
    void testToString() {
        Instant now = Instant.now();
        WorkflowRun run = new WorkflowRun(123L, "CI", "completed", "success", "main", "abc", now, now);
        String str = run.toString();

        assertTrue(str.contains("123"));
        assertTrue(str.contains("CI"));
        assertTrue(str.contains("completed"));
    }
}
