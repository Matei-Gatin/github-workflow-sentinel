package com.github.matei.sentinel.monitor;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.matei.sentinel.client.GitHubApiClient;
import com.github.matei.sentinel.config.Configuration;
import com.github.matei.sentinel.formatter.EventFormatter;
import com.github.matei.sentinel.model.Job;
import com.github.matei.sentinel.model.MonitoringEvent;
import com.github.matei.sentinel.model.WorkflowRun;
import com.github.matei.sentinel.persistence.StateManager;
import com.github.matei.sentinel.util.Constants;
import com.github.matei.sentinel.util.Logger;
/**
 * Orchestrates the workflow monitoring process.
 * <p>
 * This class is the main coordinator that:
 * <ol>
 *   <li>Loads previous state from {@link StateManager}</li>
 *   <li>Enters a polling loop that runs every {@link Constants#POLL_INTERVAL_SECONDS} seconds</li>
 *   <li>Fetches workflow runs and jobs from {@link GitHubApiClient}</li>
 *   <li>Detects new or changed events using {@link EventDetector}</li>
 *   <li>Formats events using {@link EventFormatter} and outputs to stdout</li>
 *   <li>Updates and persists state after each poll</li>
 *   <li>Handles errors and retries gracefully</li>
 *   <li>Supports graceful shutdown via {@link #stop()}</li>
 * </ol>
 * </p>
 *
 * <h2>Polling Strategy</h2>
 * <ul>
 *   <li><b>First Run</b>: Sets last check time to current time, so only NEW events are reported</li>
 *   <li><b>Subsequent Runs</b>: Fetches events since last check time, catching up on missed events</li>
 *   <li><b>Interval</b>: Polls every 30 seconds (configurable via {@link Constants#POLL_INTERVAL_SECONDS})</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * The monitor handles various error scenarios without crashing:
 * <ul>
 *   <li><b>401 Unauthorized</b>: Invalid token - exits immediately with error message</li>
 *   <li><b>403 Forbidden</b>: Rate limit or permissions - retries after delay</li>
 *   <li><b>404 Not Found</b>: Repository not found - exits immediately with error message</li>
 *   <li><b>Timeout</b>: Network timeout - retries after delay</li>
 *   <li><b>Other errors</b>: Logs error and retries after delay</li>
 * </ul>
 *
 * <h2>Graceful Shutdown</h2>
 * The monitor supports graceful shutdown via:
 * <ul>
 *   <li>Calling {@link #stop()} from another thread</li>
 *   <li>Interrupting the monitoring thread ({@link InterruptedException})</li>
 *   <li>Shutdown hooks registered in {@link com.github.matei.sentinel.Main}</li>
 * </ul>
 *
 * <h2>Performance Metrics</h2>
 * The monitor tracks and displays:
 * <ul>
 *   <li>Total uptime</li>
 *   <li>Total number of API polls</li>
 *   <li>Total events reported</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is designed for single-threaded use. The {@code running} flag is
 * {@code volatile} to support stopping from a shutdown hook.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Configuration config = new Configuration("owner/repo", "ghp_token");
 * GitHubApiClient apiClient = new GitHubApiClientImpl("ghp_token");
 * StateManager stateManager = new FileStateManager();
 * EventDetector detector = new EventDetector("owner/repo");
 * EventFormatter formatter = new ConsoleEventFormatter();
 *
 * WorkflowMonitor monitor = new WorkflowMonitor(
 *     apiClient, stateManager, detector, formatter, config
 * );
 *
 * // Register shutdown hook
 * Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));
 *
 * // Start monitoring (blocks until stopped)
 * monitor.start();
 * }</pre>
 *
 * @see EventDetector
 * @see GitHubApiClient
 * @see StateManager
 * @see EventFormatter
 * @since 1.0
 */
public class WorkflowMonitor
{
    private final GitHubApiClient apiClient;
    private final StateManager stateManager;
    private final EventDetector eventDetector;
    private final EventFormatter eventFormatter;
    private final Configuration config;

    private volatile boolean running = true;

    // Performance metrics
    private long totalPollCount = 0;
    private long totalEventsReported = 0;
    private Instant monitoringStartTime;

    /**
     * Creates a new WorkflowMonitor.
     *
     * @param apiClient client for fetching workflow data from GitHub
     * @param stateManager manager for persisting state between runs
     * @param eventDetector detector for identifying new/changed events
     * @param eventFormatter formatter for outputting events
     * @param config application configuration
     */
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
        monitoringStartTime = Instant.now();

        Logger.info("Starting monitoring for repository: " + config.getRepository());
        Logger.info("Press CTRL+C to stop.");
        System.err.println();

        Optional<Instant> lastCheckTime = stateManager.getLastCheckTime(config.getRepository());

        if (lastCheckTime.isEmpty())
        {
            Logger.info("First run detected. Will only report events from now onwards");
            // Set initial timestamp to NOW so we only report future events
            stateManager.updateLastCheckTime(config.getRepository(), Instant.now());
            stateManager.save();
        }
        else
        {
            Logger.info("Previous run detected. Catching up on events since: " + lastCheckTime.get());
        }

        System.err.println();

        while (running)
        {
            try
            {
                totalPollCount++;
                int eventCount = pollAndProcessEvents();
                totalEventsReported += eventCount;

                // Sleep for polling interval
                Thread.sleep(Constants.POLL_INTERVAL_SECONDS * 1000L);  // ✅ Added 'L' for long literal
            }
            catch (InterruptedException e)
            {
                // Interrupted by shutdown hook - exit gracefully without extra message
                break;
            }
            catch (HttpTimeoutException e)
            {
                Logger.warn("Request timed out. GitHub API might be slow. Retrying in " +
                        Constants.POLL_INTERVAL_SECONDS + " seconds...");
                // Continue monitoring despite timeout
                try
                {
                    Thread.sleep(Constants.POLL_INTERVAL_SECONDS * 1000L);
                }
                catch (InterruptedException ie)
                {
                    break;
                }
            }
            catch (RuntimeException e)
            {
                String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                if (message.contains("401") || message.contains("unauthorized"))
                {
                    Logger.error("Authentication failed: Invalid or expired GitHub token.");
                    Logger.error("   → Generate a new token at: https://github.com/settings/tokens");
                    Logger.error("   → Required scope: 'repo' (Full control of private repositories)");
                    break;
                }
                else if (message.contains("403") || message.contains("forbidden"))
                {
                    if (message.contains("rate limit"))
                    {
                        Logger.error("GitHub API rate limit exceeded.");
                        Logger.error("   → You have 5000 requests/hour with authentication.");
                        Logger.error("   → Retrying in " + Constants.POLL_INTERVAL_SECONDS + " seconds...");
                    }
                    else
                    {
                        Logger.warn("Access forbidden: Check token permissions for this repository.");
                    }

                    // Continue monitoring (retry after delay)
                    try
                    {
                        Thread.sleep(Constants.POLL_INTERVAL_SECONDS * 1000L);
                    }
                    catch (InterruptedException ie)
                    {
                        break;
                    }
                }
                else if (message.contains("404") || message.contains("not found"))
                {
                    Logger.error("Repository '" + config.getRepository() + "' not found or not accessible.");
                    Logger.error("   → Verify the repository name format: owner/repo");
                    Logger.error("   → For private repos, ensure your token has 'repo' scope");
                    break;
                }
                else
                {
                    Logger.warn("Unexpected error: " + e.getMessage());
                    Logger.warn("   → Retrying in " + Constants.POLL_INTERVAL_SECONDS + " seconds...");

                    // Continue monitoring (retry after delay)
                    try
                    {
                        Thread.sleep(Constants.POLL_INTERVAL_SECONDS * 1000L);
                    }
                    catch (InterruptedException ie)
                    {
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                Logger.error("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                try
                {
                    Thread.sleep(Constants.POLL_INTERVAL_SECONDS * 1000L);
                }
                catch (InterruptedException ie)
                {
                    break;
                }
            }
        }

        Logger.info("Monitoring stopped.");
        printSummary();
    }

    /**
     * Stops the monitoring loop gracefully.
     * Called by shutdown hook.
     */
    public void stop()
    {
        running = false;
    }

    // ====== HELPER METHODS ======

    /**
     * Prints a summary of monitoring statistics.
     * Displays total runtime, number of polls, and events reported.
     */
    private void printSummary()
    {
        Duration uptime = Duration.between(monitoringStartTime, Instant.now());
        System.err.println();
        System.err.println("=== Monitoring Summary ===");
        System.err.println("Total runtime: " + formatDuration(uptime));
        System.err.println("Total polls: " + totalPollCount);
        System.err.println("Events reported: " + totalEventsReported);
        System.err.println("==========================");
    }

    /**
     * Formats a duration into a human-readable string.
     * <p>
     * Examples:
     * <ul>
     *   <li>90 seconds → "1m 30s"</li>
     *   <li>3661 seconds → "1h 1m 1s"</li>
     *   <li>45 seconds → "45s"</li>
     * </ul>
     * </p>
     *
     * @param duration the duration to format
     * @return formatted string (e.g., "1h 5m 30s")
     */
    private String formatDuration(Duration duration)
    {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();

        if (hours > 0)
        {
            sb.append(hours).append("h ");
        }
        if (minutes > 0)
        {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.isEmpty())  // Always show seconds if duration is 0
        {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Single poll cycle: fetch workflows, detect events, output them.
     *
     * @return number of new events detected and reported
     * @throws Exception if API call fails
     */
    private int pollAndProcessEvents() throws Exception
    {
        // Get last check time
        Optional<Instant> lastCheckTime = stateManager.getLastCheckTime(config.getRepository());
        Instant since = lastCheckTime.orElse(Instant.now());

        int eventCount = 0;
        boolean stateChanged = false;

        // Fetch workflow runs since last check
        List<WorkflowRun> workflowRuns = apiClient.getWorkflowRuns(
                config.getOwner(),
                config.getRepo(),
                since
        );

        Map<Long, List<Job>> jobsMap = new HashMap<>();

        // For each workflow run, fetch its jobs
        for (WorkflowRun run : workflowRuns)
        {
            List<Job> jobs = apiClient.getJobsForRun(
                    config.getOwner(),
                    config.getRepo(),
                    run.getId()
            );

            jobsMap.put(run.getId(), jobs);
        }

        // Detect events by comparing current state with previous state
        List<MonitoringEvent> events = eventDetector.detectEvents(
                workflowRuns,
                jobsMap
        );

        // Output each new event
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
                stateChanged = true;
                eventCount++;
            }
        }

        // Save state if anything changed
        if (stateChanged || events.isEmpty())
        {
            // Update last check time to now
            stateManager.updateLastCheckTime(config.getRepository(), Instant.now());
            stateManager.save();
        }

        return eventCount;
    }

    /**
     * Generates a unique ID for an event to prevent duplicates.
     * <p>
     * Format: {@code eventType_workflowName_jobName_stepName_timestamp}
     * </p>
     * <p>
     * Non-alphanumeric characters in names are replaced with underscores
     * to ensure the ID is filesystem-safe.
     * </p>
     *
     * @param event the monitoring event
     * @return unique event identifier string
     */
    private String generateEventId(MonitoringEvent event)
    {
        StringBuilder id = new StringBuilder();
        id.append(event.getType()).append("_");
        id.append(event.getWorkflowName().replaceAll("[^a-zA-Z0-9]", "_")).append("_");

        if (event.getJobName() != null)
        {
            id.append(event.getJobName().replaceAll("[^a-zA-Z0-9]", "_")).append("_");
        }

        if (event.getStepName() != null)
        {
            id.append(event.getStepName().replaceAll("[^a-zA-Z0-9]", "_")).append("_");
        }

        id.append(event.getTimestamp().toEpochMilli());

        return id.toString();
    }
}
