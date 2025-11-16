package com.github.matei.sentinel.client;

import com.github.matei.sentinel.model.Job;
import com.github.matei.sentinel.model.Step;
import com.github.matei.sentinel.model.WorkflowRun;
import com.github.matei.sentinel.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
/**
 * Implementation of {@link GitHubApiClient} using Java 11+ HttpClient.
 * <p>
 * This client communicates with the GitHub REST API v3 to fetch workflow runs,
 * jobs, and steps. It uses Bearer token authentication and includes timeout
 * protection for network requests.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Bearer token authentication with GitHub PAT</li>
 *   <li>Connection timeout: 10 seconds</li>
 *   <li>Request timeout: 30 seconds</li>
 *   <li>Simple in-memory caching (10-second TTL) to reduce redundant API calls</li>
 *   <li>JSON parsing with Gson</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * This implementation throws {@link RuntimeException} for API errors:
 * <ul>
 *   <li>HTTP 401: "GitHub API error: 401 - Unauthorized"</li>
 *   <li>HTTP 403: "GitHub API error: 403 - Forbidden" (rate limit or permissions)</li>
 *   <li>HTTP 404: "GitHub API error: 404 - Not Found"</li>
 *   <li>Other status codes: "GitHub API error: {code} - {response body}"</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is NOT thread-safe. It should only be used from a single thread.
 *
 * @since 1.0
 */
public class GitHubApiClientImpl implements GitHubApiClient
{

    private final HttpClient httpClient;
    private final Gson gson;
    private final String token;
    private final SimpleCache<String, String> responseCache;

    /**
     * Creates a new GitHub API client with the given authentication token.
     *
     * @param token GitHub Personal Access Token (PAT) for authentication
     * @throws IllegalArgumentException if token is null or empty
     */
    public GitHubApiClientImpl(String token)
    {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub token cannot be null or empty");
        }

        this.token = token;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Constants.HTTP_CONNECT_TIMEOUT)
                .build();
        this.gson = new GsonBuilder()
                .setDateFormat(Constants.DATE_FORMAT)
                .create();
        this.responseCache = new SimpleCache<>(Constants.CACHE_TTL);
    }

    @Override
    public List<WorkflowRun> getWorkflowRuns(String owner, String repo, Instant since) throws Exception
    {
        // Validate parameters
        validateRepositoryParams(owner, repo);

        String url = buildWorkflowRunsUrl(owner, repo);
        String responseBody = makeRequest(url);

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray workflowRuns = json.getAsJsonArray(Constants.FIELD_WORKFLOW_RUNS);

        List<WorkflowRun> result = new ArrayList<>();

        for (JsonElement element : workflowRuns)
        {
            JsonObject runJson = element.getAsJsonObject();

            // Parse workflow run
            WorkflowRun workflowRun = parseWorkflowRun(runJson);

            // Filter by 'since' if provided
            if (since != null && workflowRun.getUpdatedAt().isBefore(since))
            {
                continue;
            }

            result.add(workflowRun);
        }

        return result;
    }

    @Override
    public List<Job> getJobsForRun(String owner, String repo, long runId) throws Exception {
        // Validate parameters
        validateRepositoryParams(owner, repo);
        if (runId <= 0)
        {
            throw new IllegalArgumentException("Run ID must be positive, got: " + runId);
        }

        String url = buildJobsUrl(owner, repo, runId);
        String responseBody = makeRequest(url);

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray jobs = json.getAsJsonArray(Constants.FIELD_JOBS);

        List<Job> result = new ArrayList<>();

        for (JsonElement element : jobs) {
            JsonObject jobJson = element.getAsJsonObject();
            Job job = parseJob(jobJson);
            result.add(job);
        }

        return result;
    }

    // ========== Helper Methods ==========

    /**
     * Validates repository owner and name parameters.
     *
     * @param owner the repository owner (GitHub username or organization)
     * @param repo the repository name
     * @throws IllegalArgumentException if owner or repo is null or empty
     */
    private void validateRepositoryParams(String owner, String repo)
    {
        if (owner == null || owner.trim().isEmpty())
        {
            throw new IllegalArgumentException("Repository owner cannot be null or empty");
        }
        if (repo == null || repo.trim().isEmpty())
        {
            throw new IllegalArgumentException("Repository name cannot be null or empty");
        }
    }

    /**
     * Builds the URL for fetching workflow runs.
     *
     * @param owner repository owner
     * @param repo repository name
     * @return full API endpoint URL
     */
    private String buildWorkflowRunsUrl(String owner, String repo)
    {
        return String.format("%s/repos/%s/%s/actions/runs?per_page=%d",
                Constants.API_BASE_URL, owner, repo, Constants.MAX_RESULTS_PER_PAGE);
    }

    /**
     * Builds the URL for fetching jobs for a specific run.
     *
     * @param owner repository owner
     * @param repo repository name
     * @param runId workflow run ID
     * @return full API endpoint URL
     */
    private String buildJobsUrl(String owner, String repo, long runId)
    {
        return String.format("%s/repos/%s/%s/actions/runs/%d/jobs",
                Constants.API_BASE_URL, owner, repo, runId);
    }

    /**
     * Parses a WorkflowRun from JSON.
     *
     * @param json JSON object representing a workflow run
     * @return parsed WorkflowRun object
     */
    private WorkflowRun parseWorkflowRun(JsonObject json)
    {
        long id = json.get(Constants.FIELD_ID).getAsLong();
        String name = json.get(Constants.FIELD_NAME).getAsString();
        String status = json.get(Constants.FIELD_STATUS).getAsString();
        String conclusion = getStringOrNull(json, Constants.FIELD_CONCLUSION);
        String headBranch = json.get(Constants.FIELD_HEAD_BRANCH).getAsString();
        String headSha = json.get(Constants.FIELD_HEAD_SHA).getAsString();

        Instant updatedAt = parseInstant(json, Constants.FIELD_UPDATED_AT);
        Instant concludedAt = parseInstantOrNull(json, Constants.FIELD_RUN_FINISHED_AT);

        return new WorkflowRun(id, name, status, conclusion, headBranch, headSha,
                updatedAt, concludedAt);
    }

    /**
     * Parses a Job from JSON, including its steps.
     *
     * @param json JSON object representing a job
     * @return parsed Job object with steps
     */
    private Job parseJob(JsonObject json)
    {
        long id = json.get(Constants.FIELD_ID).getAsLong();
        long runId = json.get(Constants.FIELD_RUN_ID).getAsLong();
        String name = json.get(Constants.FIELD_NAME).getAsString();
        String status = json.get(Constants.FIELD_STATUS).getAsString();
        String conclusion = getStringOrNull(json, Constants.FIELD_CONCLUSION);

        Instant startedAt = parseInstantOrNull(json, Constants.FIELD_STARTED_AT);
        Instant completedAt = parseInstantOrNull(json, Constants.FIELD_COMPLETED_AT);

        // Parse steps if present
        List<Step> steps = parseSteps(json);

        return new Job(id, runId, name, status, conclusion, startedAt, completedAt, steps);
    }

    /**
     * Parses steps from a job JSON object.
     *
     * @param jobJson JSON object representing a job
     * @return list of steps, may be empty but never null
     */
    private List<Step> parseSteps(JsonObject jobJson)
    {
        List<Step> steps = new ArrayList<>();

        if (!jobJson.has(Constants.FIELD_STEPS) || jobJson.get(Constants.FIELD_STEPS).isJsonNull())
        {
            return steps; // Return empty list if no steps
        }

        JsonArray stepsArray = jobJson.getAsJsonArray(Constants.FIELD_STEPS);

        for (JsonElement element : stepsArray)
        {
            JsonObject stepJson = element.getAsJsonObject();
            Step step = parseStep(stepJson);
            steps.add(step);
        }

        return steps;
    }

    /**
     * Parses a Step from JSON.
     *
     * @param json JSON object representing a step
     * @return parsed Step object
     */
    private Step parseStep(JsonObject json)
    {
        String name = json.get(Constants.FIELD_NAME).getAsString();
        String status = json.get(Constants.FIELD_STATUS).getAsString();
        String conclusion = getStringOrNull(json, Constants.FIELD_CONCLUSION);

        Instant startedAt = parseInstantOrNull(json, Constants.FIELD_STARTED_AT);
        Instant completedAt = parseInstantOrNull(json, Constants.FIELD_COMPLETED_AT);

        return new Step(name, status, conclusion, startedAt, completedAt);
    }

    /**
     * Safely gets a string value from JSON, returning null if field is missing or null.
     *
     * @param json JSON object to extract from
     * @param fieldName name of the field
     * @return field value as string, or null if missing/null
     */
    private String getStringOrNull(JsonObject json, String fieldName)
    {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            return null;
        }
        return json.get(fieldName).getAsString();
    }

    /**
     * Parses an Instant from JSON field (required field - throws if missing).
     *
     * @param json JSON object to extract from
     * @param fieldName name of the timestamp field
     * @return parsed Instant
     * @throws com.google.gson.JsonParseException if field is missing or invalid
     */
    private Instant parseInstant(JsonObject json, String fieldName)
    {
        return Instant.parse(json.get(fieldName).getAsString());
    }

    /**
     * Safely parses an Instant from JSON, returning null if field is missing or null.
     *
     * @param json JSON object to extract from
     * @param fieldName name of the timestamp field
     * @return parsed Instant, or null if missing/null
     */
    private Instant parseInstantOrNull(JsonObject json, String fieldName)
    {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull())
        {
            return null;
        }
        return Instant.parse(json.get(fieldName).getAsString());
    }

    /**
     * Makes an authenticated HTTP request to GitHub API.
     * <p>
     * This method checks the cache first to avoid redundant API calls. If the
     * response is not cached, it makes the HTTP request and caches the result.
     * </p>
     *
     * @param url the API endpoint URL to request
     * @return the response body as a String
     * @throws IllegalArgumentException if url is null or empty
     * @throws RuntimeException if the API request fails (non-200 status code)
     * @throws Exception if network error occurs
     */
    private String makeRequest(String url) throws Exception
    {
        // Defensive validation
        if (url == null || url.trim().isEmpty())
        {
            throw new IllegalArgumentException("API URL cannot be null or empty");
        }

        // Check cache first
        String cached = responseCache.get(url);
        if (cached != null)
        {
            return cached;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Constants.HTTP_REQUEST_TIMEOUT)
                .header(Constants.HEADER_AUTHORIZATION, Constants.HEADER_BEARER_PREFIX + token)
                .header(Constants.HEADER_ACCEPT, Constants.HEADER_ACCEPT_VALUE)
                .header(Constants.HEADER_API_VERSION, Constants.HEADER_API_VERSION_VALUE)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != Constants.HTTP_OK)
        {
            throw new RuntimeException(
                    String.format("GitHub API error: %d - %s",
                            response.statusCode(), response.body())
            );
        }

        String body = response.body();

        // Cache the response
        responseCache.put(url, body);

        return body;
    }
}