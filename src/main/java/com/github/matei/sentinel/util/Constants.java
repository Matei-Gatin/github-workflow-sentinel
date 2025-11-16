package com.github.matei.sentinel.util;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Centralized constants for the GitHub Workflow Sentinel application.
 */
public final class Constants {

    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }

    // ========== GitHub API Constants ==========

    public static final String API_BASE_URL = "https://api.github.com";
    public static final int MAX_RESULTS_PER_PAGE = 100;

    // API Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_API_VERSION = "X-GitHub-Api-Version";
    public static final String HEADER_BEARER_PREFIX = "Bearer ";
    public static final String HEADER_ACCEPT_VALUE = "application/vnd.github+json";
    public static final String HEADER_API_VERSION_VALUE = "2022-11-28";

    // API Response Codes
    public static final int HTTP_OK = 200;

    // ========== JSON Field Names ==========

    public static final String FIELD_WORKFLOW_RUNS = "workflow_runs";
    public static final String FIELD_JOBS = "jobs";
    public static final String FIELD_STEPS = "steps";
    public static final String FIELD_ID = "id";
    public static final String FIELD_RUN_ID = "run_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_CONCLUSION = "conclusion";
    public static final String FIELD_HEAD_BRANCH = "head_branch";
    public static final String FIELD_HEAD_SHA = "head_sha";
    public static final String FIELD_UPDATED_AT = "updated_at";
    public static final String FIELD_RUN_FINISHED_AT = "run_finished_at";
    public static final String FIELD_STARTED_AT = "started_at";
    public static final String FIELD_COMPLETED_AT = "completed_at";

    // ========== Workflow Status Constants ==========

    public static final String STATUS_QUEUED = "queued";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED = "completed";

    // ========== Monitoring Constants ==========

    public static final int POLL_INTERVAL_SECONDS = 30;

    // ========== State Management Constants ==========

    public static final String STATE_FILE = ".sentinel-state.json";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // ========== CLI Constants ==========

    public static final String ARG_REPO_LONG = "--repo";
    public static final String ARG_REPO_SHORT = "-r";
    public static final String ARG_TOKEN_LONG = "--token";
    public static final String ARG_TOKEN_SHORT = "-t";
    public static final int MIN_ARGS_COUNT = 4;

    // Repository format validation
    public static final String REPO_FORMAT_SEPARATOR = "/";
    public static final int REPO_FORMAT_PARTS = 2;

    // ========== Formatting Constants ==========

    public static final int SHA_DISPLAY_LENGTH = 7;

    // ========== Timestamp Formatter ==========
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    // ========== Cleanup Threshold ==========
    public static final Duration CLEANUP_THRESHOLD = Duration.ofHours(1);
    public static final int MAX_EVENT_IDS = 1000;

}