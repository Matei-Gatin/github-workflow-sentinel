package com.github.matei.sentinel.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Manages persistent state for the monitoring tool.
 * Remembers what we've seen so we don't report duplicates.
 */

public interface StateManager
{
    /**
     * Gets the last time we checked this repository.
     *
     * @param repository Repository in "owner/repo" format
     * @return Last check timestamp, or empty if first run
     */
    Optional<Instant> getLastCheckTime(String repository);

    /**
     * Updates the last check time for this repository.
     *
     * @param repository Repository in "owner/repo" format
     * @param timestamp New timestamp to save
     */

    void updateLastCheckTime(String repository, Instant timestamp);

    /**
     * Gets IDs of events we've already processed.
     * This prevents showing the same event twice.
     *
     * @param repository Repository in "owner/repo" format
     * @return Set of processed event IDs
     */

    Set<String> getProcessedEventIds(String repository);

    /**
     * Marks an event as processed.
     *
     * @param repository Repository in "owner/repo" format
     * @param eventId Unique event ID (e.g., "run_123_job_456")
     */
    void addProcessedEventId(String repository, String eventId);

    /**
     * Saves all state to persistent storage (file).
     * Should be called after updates.
     */
    void save();
}
