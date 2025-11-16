package com.github.matei.sentinel;

import com.github.matei.sentinel.persistence.FileStateManager;
import com.github.matei.sentinel.util.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileStateManagerTest {

    private static final String TEST_STATE_FILE = ".sentinel-state-test.json";
    private FileStateManager stateManager;

    @BeforeEach
    void setUp() throws IOException {
        // Clean up any existing test file BEFORE creating FileStateManager
        Path testPath = Path.of(TEST_STATE_FILE);
        if (Files.exists(testPath)) {
            Files.delete(testPath);
        }

        // Also clean up the default state file if it exists
        Path defaultPath = Path.of(Constants.STATE_FILE);
        if (Files.exists(defaultPath)) {
            Files.delete(defaultPath);
        }

        // Create FileStateManager - it will load state from Constants.STATE_FILE
        stateManager = new FileStateManager();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        Files.deleteIfExists(Path.of(TEST_STATE_FILE));
        Files.deleteIfExists(Path.of(Constants.STATE_FILE));
    }

    @Test
    void testGetLastCheckTime_FirstRun() {
        // On first run with no existing state file, should return empty Optional
        Optional<Instant> lastCheck = stateManager.getLastCheckTime("test-owner/test-repo");

        assertTrue(lastCheck.isEmpty(),
                "First run should return empty Optional. If this fails, a state file might exist from a previous run.");
    }

    @Test
    void testUpdateAndGetLastCheckTime() {
        Instant now = Instant.now();
        String testRepo = "owner/repo";

        stateManager.updateLastCheckTime(testRepo, now);

        Optional<Instant> retrieved = stateManager.getLastCheckTime(testRepo);
        assertTrue(retrieved.isPresent());
        assertEquals(now, retrieved.get());
    }

    @Test
    void testAddAndGetProcessedEventIds() {
        String testRepo = "owner/repo";

        stateManager.addProcessedEventId(testRepo, "event1");
        stateManager.addProcessedEventId(testRepo, "event2");
        stateManager.addProcessedEventId(testRepo, "event3");

        Set<String> eventIds = stateManager.getProcessedEventIds(testRepo);

        assertEquals(3, eventIds.size());
        assertTrue(eventIds.contains("event1"));
        assertTrue(eventIds.contains("event2"));
        assertTrue(eventIds.contains("event3"));
    }

    @Test
    void testEventIdLimit() {
        String testRepo = "owner/repo";

        // Add more than MAX_EVENT_IDS (1000)
        for (int i = 0; i < 1100; i++) {
            stateManager.addProcessedEventId(testRepo, "event" + i);
        }

        Set<String> eventIds = stateManager.getProcessedEventIds(testRepo);

        // Should be limited to 1000
        assertEquals(1000, eventIds.size(), "Event IDs should be limited to 1000");

        // Should keep the most recent ones (last 1000)
        assertTrue(eventIds.contains("event1099"), "Should contain most recent event");
        assertFalse(eventIds.contains("event0"), "Oldest event should be removed");
    }

    @Test
    void testStatePersistence() {
        Instant now = Instant.now();
        String testRepo = "owner/repo";

        stateManager.updateLastCheckTime(testRepo, now);
        stateManager.addProcessedEventId(testRepo, "event1");
        stateManager.save();

        // Verify file was created
        assertTrue(Files.exists(Path.of(Constants.STATE_FILE)), "State file should be created");

        // Create new instance to test loading
        FileStateManager newStateManager = new FileStateManager();

        Optional<Instant> lastCheck = newStateManager.getLastCheckTime(testRepo);
        assertTrue(lastCheck.isPresent(), "Should load saved state");
        assertEquals(now, lastCheck.get(), "Should load correct timestamp");

        Set<String> eventIds = newStateManager.getProcessedEventIds(testRepo);
        assertTrue(eventIds.contains("event1"), "Should load saved event IDs");
    }

    @Test
    void testMultipleRepositories() {
        Instant now1 = Instant.now();
        Instant now2 = now1.plusSeconds(60);

        stateManager.updateLastCheckTime("owner1/repo1", now1);
        stateManager.updateLastCheckTime("owner2/repo2", now2);
        stateManager.addProcessedEventId("owner1/repo1", "event1");
        stateManager.addProcessedEventId("owner2/repo2", "event2");

        assertEquals(now1, stateManager.getLastCheckTime("owner1/repo1").get());
        assertEquals(now2, stateManager.getLastCheckTime("owner2/repo2").get());

        assertTrue(stateManager.getProcessedEventIds("owner1/repo1").contains("event1"));
        assertTrue(stateManager.getProcessedEventIds("owner2/repo2").contains("event2"));

        assertFalse(stateManager.getProcessedEventIds("owner1/repo1").contains("event2"));
        assertFalse(stateManager.getProcessedEventIds("owner2/repo2").contains("event1"));
    }

    @Test
    void testEmptyRepository() {
        // Test with a repository that has no state
        Optional<Instant> lastCheck = stateManager.getLastCheckTime("nonexistent/repo");
        assertTrue(lastCheck.isEmpty(), "Non-existent repository should return empty");

        Set<String> eventIds = stateManager.getProcessedEventIds("nonexistent/repo");
        assertTrue(eventIds.isEmpty(), "Non-existent repository should return empty set");
    }
}
