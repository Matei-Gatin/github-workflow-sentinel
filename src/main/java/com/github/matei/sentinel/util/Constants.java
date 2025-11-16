package com.github.matei.sentinel.util;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Centralized constants for the GitHub Workflow Sentinel application.
 * <p>
 * This class contains all magic numbers, strings, and configuration values
 * used throughout the application. Centralizing constants:
 * <ul>
 *   <li>Prevents duplication and inconsistency</li>
 *   <li>Makes values easy to find and modify</li>
 *   <li>Improves code maintainability</li>
 *   <li>Enables easy testing (can mock if needed)</li>
 * </ul>
 * </p>
 *
 * <h2>Categories</h2>
 * Constants are organized into logical groups:
 * <ul>
 *   <li><b>GitHub API Constants</b> - Base URL, endpoints, pagination</li>
 *   <li><b>HTTP Constants</b> - Status codes, headers, authentication</li>
 *   <li><b>JSON Field Names</b> - Field names used in GitHub API responses</li>
 *   <li><b>Status Constants</b> - Workflow/job/step status values</li>
 *   <li><b>Monitoring Constants</b> - Polling interval, state file path</li>
 *   <li><b>CLI Constants</b> - Command-line argument names</li>
 *   <li><b>Formatting Constants</b> - Date formats, display lengths</li>
 *   <li><b>Memory Management Constants</b> - Cleanup thresholds, limits</li>
 *   <li><b>Logger Colors</b> - ANSI color codes for console output</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 * <ul>
 *   <li>Always use these constants instead of hardcoding values</li>
 *   <li>If you need to add a new constant, follow the existing naming conventions</li>
 *   <li>Add a comment explaining what each constant is for</li>
 *   <li>Group related constants together</li>
 * </ul>
 *
 * <h2>Design Note</h2>
 * This class uses a private constructor to prevent instantiation, as it
 * only contains static fields and should never be instantiated.
 *
 * @since 1.0
 */
public final class Constants {

    /**
     * Private constructor to prevent instantiation.
     * This class should only be used for its static constants.
     *
     * @throws UnsupportedOperationException if called via reflection
     */
    private Constants()
    {
        throw new UnsupportedOperationException("Constants class should not be instantiated");
    }

    // ========== GitHub API Constants ==========

    /**
     * Base URL for the GitHub REST API v3.
     * All API endpoints are relative to this URL.
     */
    public static final String API_BASE_URL = "https://api.github.com";

    /**
     * Maximum number of results per page returned by GitHub API.
     * This is GitHub's default and maximum page size.
     */
    public static final int MAX_RESULTS_PER_PAGE = 100;

    // ========== HTTP Constants ==========

    /**
     * HTTP status code for successful requests.
     */
    public static final int HTTP_OK = 200;

    /**
     * HTTP header name for authorization.
     * Used to send the GitHub Personal Access Token.
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * HTTP header name for specifying accepted response format.
     */
    public static final String HEADER_ACCEPT = "Accept";

    /**
     * HTTP header name for GitHub API version.
     */
    public static final String HEADER_API_VERSION = "X-GitHub-Api-Version";

    /**
     * Prefix for Bearer token authentication.
     * Format: "Bearer {token}"
     */
    public static final String HEADER_BEARER_PREFIX = "Bearer ";

    /**
     * Value for Accept header to request GitHub API v3 JSON format.
     */
    public static final String HEADER_ACCEPT_VALUE = "application/vnd.github+json";

    /**
     * GitHub API version to use.
     * This version ensures consistent API behavior.
     */
    public static final String HEADER_API_VERSION_VALUE = "2022-11-28";

    // ========== JSON Field Names ==========

    /**
     * JSON field name for the array of workflow runs in API response.
     * Used when parsing: {@code GET /repos/{owner}/{repo}/actions/runs}
     */
    public static final String FIELD_WORKFLOW_RUNS = "workflow_runs";

    /**
     * JSON field name for the array of jobs in API response.
     * Used when parsing: {@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs}
     */
    public static final String FIELD_JOBS = "jobs";

    /**
     * JSON field name for the array of steps within a job.
     * Each job contains a "steps" array with step details.
     */
    public static final String FIELD_STEPS = "steps";

    /**
     * JSON field name for the unique identifier of a workflow run or job.
     * This is a numeric ID assigned by GitHub.
     */
    public static final String FIELD_ID = "id";

    /**
     * JSON field name for the workflow run ID that a job belongs to.
     * Links a job back to its parent workflow run.
     */
    public static final String FIELD_RUN_ID = "run_id";

    /**
     * JSON field name for the name of a workflow, job, or step.
     * Example: "CI Pipeline", "build", "Run tests"
     */
    public static final String FIELD_NAME = "name";

    /**
     * JSON field name for the current status.
     * Values: "queued", "in_progress", "completed"
     *
     * @see #STATUS_QUEUED
     * @see #STATUS_IN_PROGRESS
     * @see #STATUS_COMPLETED
     */
    public static final String FIELD_STATUS = "status";

    /**
     * JSON field name for the conclusion (outcome) of a completed entity.
     * Values: "success", "failure", "cancelled", "skipped", "timed_out"
     * Only present when status is "completed".
     */
    public static final String FIELD_CONCLUSION = "conclusion";

    /**
     * JSON field name for the branch name in a workflow run.
     * Example: "main", "develop", "feature/new-feature"
     */
    public static final String FIELD_HEAD_BRANCH = "head_branch";

    /**
     * JSON field name for the commit SHA in a workflow run.
     * This is the full 40-character Git commit hash.
     */
    public static final String FIELD_HEAD_SHA = "head_sha";

    /**
     * JSON field name for the last update timestamp.
     * Used for workflow runs to track when they were last modified.
     * Format: ISO-8601 (e.g., "2025-11-15T10:30:45Z")
     */
    public static final String FIELD_UPDATED_AT = "updated_at";

    /**
     * JSON field name for the workflow run finish timestamp.
     * Only present when a workflow run has concluded.
     * Format: ISO-8601
     */
    public static final String FIELD_RUN_FINISHED_AT = "run_finished_at";

    /**
     * JSON field name for the start timestamp of a job or step.
     * Format: ISO-8601
     */
    public static final String FIELD_STARTED_AT = "started_at";

    /**
     * JSON field name for the completion timestamp of a job or step.
     * Only present when the job/step has finished.
     * Format: ISO-8601
     */
    public static final String FIELD_COMPLETED_AT = "completed_at";

    // ========== Workflow Status Constants ==========

    /**
     * Status value indicating a workflow/job/step is queued.
     * The entity is waiting in the queue to start execution.
     */
    public static final String STATUS_QUEUED = "queued";

    /**
     * Status value indicating a workflow/job/step is in progress.
     * The entity is currently executing.
     */
    public static final String STATUS_IN_PROGRESS = "in_progress";

    /**
     * Status value indicating a workflow/job/step has completed.
     * Check the conclusion field to determine the outcome (success, failure, etc.).
     */
    public static final String STATUS_COMPLETED = "completed";

    // ========== Monitoring Constants ==========

    /**
     * Interval between API polls in seconds.
     * Default: 30 seconds (balances data freshness vs. API rate limits)
     * <p>
     * GitHub allows 5000 authenticated requests per hour, so 30-second
     * polling means 120 polls/hour, well within the limit.
     * </p>
     */
    public static final int POLL_INTERVAL_SECONDS = 30;

    /**
     * Path to the state file that persists data between runs.
     * This file stores:
     * <ul>
     *   <li>Last check timestamp per repository</li>
     *   <li>Processed event IDs to prevent duplicates</li>
     * </ul>
     * <p>
     * Format: JSON
     * Location: Current working directory
     * </p>
     */
    public static final String STATE_FILE = ".sentinel-state.json";


    // ========== CLI Constants ==========

    /**
     * Long form of the repository argument: --repo
     * Usage: {@code --repo owner/repo}
     */
    public static final String ARG_REPO_LONG = "--repo";

    /**
     * Short form of the repository argument: -r
     * Usage: {@code -r owner/repo}
     */
    public static final String ARG_REPO_SHORT = "-r";

    /**
     * Long form of the token argument: --token
     * Usage: {@code --token ghp_xxxxx}
     */
    public static final String ARG_TOKEN_LONG = "--token";

    /**
     * Short form of the token argument: -t
     * Usage: {@code -t ghp_xxxxx}
     */
    public static final String ARG_TOKEN_SHORT = "-t";

    /**
     * Minimum number of command-line arguments required.
     * Must provide: --repo value --token value (4 args total)
     * <p>
     * Example: {@code java -jar sentinel.jar --repo owner/repo --token ghp_xxx}
     * </p>
     */
    public static final int MIN_ARGS_COUNT = 4;

    /**
     * Separator character in repository format: "owner/repo"
     * Used to split the repository string into owner and repo name.
     */
    public static final String REPO_FORMAT_SEPARATOR = "/";

    /**
     * Expected number of parts when splitting repository by separator.
     * Format: "owner/repo" splits into exactly 2 parts.
     * <p>
     * Used for validation to reject invalid formats like:
     * <ul>
     *   <li>"owner" (missing separator)</li>
     *   <li>"owner/repo/extra" (too many parts)</li>
     *   <li>"/repo" or "owner/" (empty parts)</li>
     * </ul>
     * </p>
     */
    public static final int REPO_FORMAT_PARTS = 2;

    // ========== Formatting Constants ==========

    /**
     * Date/time formatter for ISO-8601 instant format.
     * Example output: "2025-11-15T10:30:45Z"
     * <p>
     * This formatter is used for displaying timestamps in event output.
     * It matches GitHub's timestamp format for consistency.
     * </p>
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Date format string used by Gson for parsing JSON timestamps.
     * Pattern: "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * <p>
     * This must match the format used by GitHub API responses.
     * The 'Z' indicates UTC timezone (zero offset).
     * </p>
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Number of characters to display from commit SHA.
     * Full SHA is 40 characters, but we truncate to 7 for readability.
     * <p>
     * Example: "abc123def456..." becomes "abc123d"
     * This matches Git's default short SHA length.
     * </p>
     */
    public static final int SHA_DISPLAY_LENGTH = 7;

    // ========== Memory Management Constants ==========

    /**
     * Threshold for cleaning up old workflow/job/step data.
     * Entries not seen for more than this duration are removed from memory.
     * Default: 1 hour
     * <p>
     * <b>Why 1 hour?</b>
     * <ul>
     *   <li>Most workflows complete within 1 hour</li>
     *   <li>Prevents unbounded memory growth for long-running monitoring</li>
     *   <li>Still allows tracking of very long-running workflows</li>
     * </ul>
     * </p>
     *
     * @see com.github.matei.sentinel.monitor.EventDetector#cleanupOldEntries(Instant)
     */
    public static final Duration CLEANUP_THRESHOLD = Duration.ofHours(1);

    /**
     * Maximum number of event IDs to store in state file per repository.
     * When this limit is exceeded, the oldest IDs are removed (FIFO).
     * Default: 1000
     * <p>
     * <b>Why 1000?</b>
     * <ul>
     *   <li>Prevents unbounded file growth over time</li>
     *   <li>Sufficient to track recent events within polling intervals</li>
     *   <li>Balances memory usage vs. duplicate detection accuracy</li>
     * </ul>
     * </p>
     *
     * @see com.github.matei.sentinel.persistence.FileStateManager#addProcessedEventId
     */
    public static final int MAX_EVENT_IDS = 1000;

    /**
     * Time-to-live for HTTP response cache.
     * API responses are cached for this duration to reduce redundant calls.
     * Default: 10 seconds (shorter than poll interval to ensure freshness)
     * <p>
     * <b>Why 10 seconds?</b>
     * <ul>
     *   <li>Reduces API calls if multiple requests happen in quick succession</li>
     *   <li>Shorter than 30s poll interval ensures fresh data each poll</li>
     *   <li>Helps during startup when fetching multiple workflow runs</li>
     * </ul>
     * </p>
     */
    public static final Duration CACHE_TTL = Duration.ofSeconds(10);

    /**
     * Timeout for establishing HTTP connection to GitHub API.
     * Default: 10 seconds
     * <p>
     * If the connection cannot be established within this time, a
     * {@link java.net.http.HttpTimeoutException} is thrown.
     * </p>
     */
    public static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Timeout for entire HTTP request (including reading response body).
     * Default: 30 seconds
     * <p>
     * This covers the time from sending the request to receiving the
     * complete response. If exceeded, a {@link java.net.http.HttpTimeoutException}
     * is thrown.
     * </p>
     */
    public static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    // ========== Logger Colors ==========

    /**
     * ANSI escape code to reset terminal colors to default.
     * Used after colored text to return to normal formatting.
     * <p>
     * Example: {@code System.err.println(ANSI_RED + "Error" + ANSI_RESET);}
     * </p>
     */
    public static final String ANSI_RESET = "\u001B[0m";

    /**
     * ANSI escape code for blue text color.
     * Used for informational messages.
     * <p>
     * Visual indicator: ℹ (information symbol)
     * </p>
     */
    public static final String ANSI_BLUE = "\u001B[34m";

    /**
     * ANSI escape code for yellow text color.
     * Used for warning messages.
     * <p>
     * Visual indicator: ⚠ (warning symbol)
     * </p>
     */
    public static final String ANSI_YELLOW = "\u001B[33m";

    /**
     * ANSI escape code for red text color.
     * Used for error messages.
     * <p>
     * Visual indicator: ✖ (error symbol)
     * </p>
     */
    public static final String ANSI_RED = "\u001B[31m";
}