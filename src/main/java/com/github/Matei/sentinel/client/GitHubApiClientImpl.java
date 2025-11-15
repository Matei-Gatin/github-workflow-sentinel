package com.github.Matei.sentinel.client;

import com.github.Matei.sentinel.model.Job;
import com.github.Matei.sentinel.model.Step;
import com.github.Matei.sentinel.model.WorkflowRun;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GitHubApiClient using Java's built-in HttpClient and Gson for JSON parsing.
 */

public class GitHubApiClientImpl implements GitHubApiClient
{
    private static final String API_BASE_URL = "https://api.github.com";

    private final HttpClient httpClient;
    private final Gson gson;
    private final String token;

    public GitHubApiClientImpl(String token)
    {
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
    }

    @Override
    public List<WorkflowRun> getWorkflowRuns(String owner, String repo, Instant since) throws Exception
    {
        String url = API_BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs";

        // Add a query parameter if we have a 'since' timestamp
        if (since != null)
        {
            url += "?per_page=100"; // get up to 100 result
        }

        // Make HTTP request
        String responseBody = makeRequest(url);

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray workflowRuns = json.getAsJsonArray("workflow_runs");

        List<WorkflowRun> result = new ArrayList<>();

        for (int i = 0; i < workflowRuns.size(); i++)
        {
            JsonObject runJson = workflowRuns.get(i).getAsJsonObject();

            // Extract fields from JSON
            long id = runJson.get("id").getAsLong();
            String name = runJson.get("name").getAsString();
            String status = runJson.get("status").getAsString();
            String conclusion = runJson.has("conclusion") && !runJson.get("conclusion").isJsonNull()
                    ? runJson.get("conclusion").getAsString()
                    : null;
            String headBranch = runJson.get("head_branch").getAsString();
            String headSha = runJson.get("head_sha").getAsString();

            Instant updatedAt = Instant.parse(runJson.get("updated_at").getAsString());
            Instant concludedAt = runJson.has("run_finished_at") && !runJson.get("run_finished_at").isJsonNull()
                    ? Instant.parse(runJson.get("run_finished_at").getAsString())
                    : null;

            if (since != null && updatedAt.isBefore(since))
            {
                continue;
            }

            WorkflowRun workflowRun = new WorkflowRun(
                    id, name, status, conclusion, headBranch, headSha, updatedAt, concludedAt
            );

            result.add(workflowRun);
        }

        return result;
    }

    @Override
    public List<Job> getJobsForRun(String owner, String repo, long runId) throws Exception
    {
        String url = API_BASE_URL + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs";

        // make HTTP request
        String responseBody = makeRequest(url);

        // Parse JSON response
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray jobs = json.getAsJsonArray("jobs");

        List<Job> result = new ArrayList<>();

        for (int i = 0; i < jobs.size(); i++)
        {
            JsonObject jobJson = jobs.get(i).getAsJsonObject();

            long id = jobJson.get("id").getAsLong();
            long jobRunId = jobJson.get("run_id").getAsLong();
            String name = jobJson.get("name").getAsString();
            String status = jobJson.get("status").getAsString();
            String conclusion = jobJson.has("conclusion") && !jobJson.get("conclusion").isJsonNull()
                    ? jobJson.get("conclusion").getAsString()
                    : null;

            Instant startedAt = jobJson.has("started_at") && !jobJson.get("started_at").isJsonNull()
                    ? Instant.parse(jobJson.get("started_at").getAsString())
                    : null;
            Instant completedAt = jobJson.has("completed_at") && !jobJson.get("completed_at").isJsonNull()
                    ? Instant.parse(jobJson.get("completed_at").getAsString())
                    : null;

            // Parse steps from the job JSON
            List<Step> steps = new ArrayList<>();
            if (jobJson.has("steps") && !jobJson.get("steps").isJsonNull())
            {
                JsonArray stepsArray = jobJson.getAsJsonArray("steps");

                for (int j = 0; j < stepsArray.size(); j++)
                {
                    JsonObject stepJson = stepsArray.get(j).getAsJsonObject();

                    String stepName = stepJson.get("name").getAsString();
                    String stepStatus = stepJson.get("status").getAsString();
                    String stepConclusion = stepJson.has("conclusion") && !stepJson.get("conclusion").isJsonNull()
                            ? stepJson.get("conclusion").getAsString()
                            : null;

                    Instant stepsStartedAt = stepJson.has("started_at") && !stepJson.get("started_at").isJsonNull()
                            ? Instant.parse(stepJson.get("started_at").getAsString())
                            : null;
                    Instant stepCompletedAt = stepJson.has("completed_at") && !stepJson.get("completed_at").isJsonNull()
                            ? Instant.parse(stepJson.get("completed_at").getAsString())
                            : null;

                    Step step = new Step(stepName, stepStatus, stepConclusion, stepsStartedAt, stepCompletedAt);
                    steps.add(step);
                }
            }

            Job job = new Job(id, jobRunId, name, status, conclusion, startedAt, completedAt, steps);
            result.add(job);
        }

        return result;
    }

    /**
     * Helper method to make authenticated HTTP requests to GitHub API.
     */
    private String makeRequest(String url) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check for errors
        if (response.statusCode() != 200)
        {
            throw new RuntimeException("GitHub API error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }
}
