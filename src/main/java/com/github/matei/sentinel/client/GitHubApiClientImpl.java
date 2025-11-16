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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of GitHubApiClient using Java's built-in HttpClient and Gson for JSON parsing.
 */
public class GitHubApiClientImpl implements GitHubApiClient
{

    private final HttpClient httpClient;
    private final Gson gson;
    private final String token;

    public GitHubApiClientImpl(String token)
    {
        this.token = token;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder()
                .setDateFormat(Constants.DATE_FORMAT)
                .create();
    }

    @Override
    public List<WorkflowRun> getWorkflowRuns(String owner, String repo, Instant since) throws Exception
    {
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
     * Builds the URL for fetching workflow runs.
     */
    private String buildWorkflowRunsUrl(String owner, String repo)
    {
        return String.format("%s/repos/%s/%s/actions/runs?per_page=%d",
                Constants.API_BASE_URL, owner, repo, Constants.MAX_RESULTS_PER_PAGE);
    }

    /**
     * Builds the URL for fetching jobs for a specific run.
     */
    private String buildJobsUrl(String owner, String repo, long runId)
    {
        return String.format("%s/repos/%s/%s/actions/runs/%d/jobs",
                Constants.API_BASE_URL, owner, repo, runId);
    }

    /**
     * Parses a WorkflowRun from JSON.
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
     */
    private String getStringOrNull(JsonObject json, String fieldName) {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            return null;
        }
        return json.get(fieldName).getAsString();
    }

    /**
     * Parses an Instant from JSON field (required field - throws if missing).
     */
    private Instant parseInstant(JsonObject json, String fieldName)
    {
        return Instant.parse(json.get(fieldName).getAsString());
    }

    /**
     * Safely parses an Instant from JSON, returning null if field is missing or null.
     */
    private Instant parseInstantOrNull(JsonObject json, String fieldName)
    {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            return null;
        }
        return Instant.parse(json.get(fieldName).getAsString());
    }

    /**
     * Makes an authenticated HTTP request to GitHub API.
     */
    private String makeRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header(Constants.HEADER_AUTHORIZATION, Constants.HEADER_BEARER_PREFIX + token)
                .header(Constants.HEADER_ACCEPT, Constants.HEADER_ACCEPT_VALUE)
                .header(Constants.HEADER_API_VERSION, Constants.HEADER_API_VERSION_VALUE)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != Constants.HTTP_OK) {
            throw new RuntimeException(
                    String.format("GitHub API error: %d - %s",
                            response.statusCode(), response.body())
            );
        }

        return response.body();
    }
}