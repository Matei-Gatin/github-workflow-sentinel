package com.github.matei.sentinel;

import com.github.matei.sentinel.util.Constants;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void testApiConstants() {
        assertEquals("https://api.github.com", Constants.API_BASE_URL);
        assertEquals(100, Constants.MAX_RESULTS_PER_PAGE);
        assertEquals(200, Constants.HTTP_OK);
    }

    @Test
    void testHeaderConstants() {
        assertEquals("Authorization", Constants.HEADER_AUTHORIZATION);
        assertEquals("Bearer ", Constants.HEADER_BEARER_PREFIX);
        assertEquals("application/vnd.github+json", Constants.HEADER_ACCEPT_VALUE);
    }

    @Test
    void testStatusConstants() {
        assertEquals("queued", Constants.STATUS_QUEUED);
        assertEquals("in_progress", Constants.STATUS_IN_PROGRESS);
        assertEquals("completed", Constants.STATUS_COMPLETED);
    }

    @Test
    void testMonitoringConstants() {
        assertEquals(30, Constants.POLL_INTERVAL_SECONDS);
        assertEquals(".sentinel-state.json", Constants.STATE_FILE);
    }

    @Test
    void testCliConstants() {
        assertEquals("--repo", Constants.ARG_REPO_LONG);
        assertEquals("-r", Constants.ARG_REPO_SHORT);
        assertEquals("--token", Constants.ARG_TOKEN_LONG);
        assertEquals("-t", Constants.ARG_TOKEN_SHORT);
        assertEquals(4, Constants.MIN_ARGS_COUNT);
    }
}
