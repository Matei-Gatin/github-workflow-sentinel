package com.github.matei.sentinel.monitor;


import com.github.matei.sentinel.client.GitHubApiClient;
import com.github.matei.sentinel.config.Configuration;
import com.github.matei.sentinel.formatter.EventFormatter;
import com.github.matei.sentinel.model.Job;
import com.github.matei.sentinel.model.MonitoringEvent;
import com.github.matei.sentinel.model.WorkflowRun;
import com.github.matei.sentinel.persistence.StateManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main orchestrator for monitoring GitHub workflow runs.
 * Continuously polls GitHub API, detects events, and outputs them.
 */

public class WorkflowMonitor
{
    private static final int POLL_INTERVAL_SECONDS = 30;

    private final GitHubApiClient apiClient;
    private final StateManager stateManager;
    private final EventDetector eventDetector;
    private final EventFormatter eventFormatter;
    private final Configuration config;

    private volatile boolean running = true;

    public WorkflowMonitor(GitHubApiClient apiClient, StateManager stateManager,
                           EventDetector eventDetector, EventFormatter eventFormatter, Configuration config)
    {
        this.apiClient = apiClient;
        this.stateManager = stateManager;
        this.eventDetector = eventDetector;
        this.eventFormatter = eventFormatter;
        this.config = config;
    }

    /**
     * Starts the monitoring loop.
     * This is a blocking call that runs until interrupted.
     */
    public void start()
    {
        System.err.println("Starting monitoring for repository: " + config.getRepository());
        System.err.println("Press CTRL+C to stop.");
        System.err.println();

        Optional<Instant> lastCheckTime = stateManager.getLastCheckTime(config.getRepository());

        if (lastCheckTime.isEmpty())
        {
            System.err.println("First run detected. Will only report events from now onwards");
            // Set initial timestamp to NOW so we only report future events
            stateManager.updateLastCheckTime(config.getRepository(), Instant.now());
            stateManager.save();
        } else
        {
            System.err.println("Previous run detected. Catching up on events since: " + lastCheckTime.get());
        }

        System.err.println();

        while (running)
        {
            try
            {
                pollAndProcessEvents();

                // sleep 30 seconds
                Thread.sleep(POLL_INTERVAL_SECONDS * 1000);
            } catch (InterruptedException e)
            {
                System.err.println("Monitoring interrupted. Shutting down...");
                break;
            } catch (Exception e)
            {
                System.err.println("Error during monitoring: " + e.getMessage());
                // Continue monitoring despite errors
                try
                {
                    Thread.sleep(POLL_INTERVAL_SECONDS * 1000);
                } catch (InterruptedException ie)
                {
                    break;
                }
            }
        }

        System.err.println("Monitoring stopped.");
    }

    /**
     * Stops the monitoring loop gracefully.
     * Called by shutdown hook.
     */
    public void stop()
    {
        running = false;
    }

    /**
     * Single poll cycle: fetch workflows, detect events, output them.
     */
    private void pollAndProcessEvents() throws Exception {
        // Get last check time
        Optional<Instant> lastCheckTime = stateManager.getLastCheckTime(config.getRepository());
        Instant since = lastCheckTime.orElse(Instant.now());

        // Fetch workflow runs since last check
        List<WorkflowRun> workflowRuns = apiClient.getWorkflowRuns(
                config.getOwner(),
                config.getRepo(),
                since
        );

        Map<Long, List<Job>> jobsMap = new HashMap<>();

        // For each workflow run, fetch its jobs
        for (WorkflowRun run : workflowRuns) {
            List<Job> jobs = apiClient.getJobsForRun(
                    config.getOwner(),
                    config.getRepo(),
                    run.getId()
            );

            jobsMap.put(run.getId(), jobs);
        }

        List<MonitoringEvent> events = eventDetector.detectEvents(
                workflowRuns,
                jobsMap
        );

        // Output each event
        for (MonitoringEvent event : events)
        {
            // Check if we've already processed this event
            String eventId = generateEventId(event);
            if (!stateManager.getProcessedEventIds(config.getRepository()).contains(eventId))
            {
                // Format and print to stdout
                System.out.println(eventFormatter.format(event));

                // Mark as processed
                stateManager.addProcessedEventId(config.getRepository(), eventId);
            }
        }

        // Update last check time to now
        stateManager.updateLastCheckTime(config.getRepository(), Instant.now());
        stateManager.save();
    }

    /**
     * Generates a unique ID for an event to prevent duplicates.
     * Format: eventType_runId_jobId_stepName_timestamp
     */
    private String generateEventId(MonitoringEvent event) {
        StringBuilder id = new StringBuilder();
        id.append(event.getType()).append("_");
        id.append(event.getWorkflowName().replaceAll("[^a-zA-Z0-9]", "_")).append("_");

        if (event.getJobName() != null) {
            id.append(event.getJobName().replaceAll("[^a-zA-Z0-9]", "_")).append("_");
        }

        if (event.getStepName() != null) {
            id.append(event.getStepName().replaceAll("[^a-zA-Z0-9]", "_")).append("_");
        }

        id.append(event.getTimestamp().toEpochMilli());

        return id.toString();
    }

}
