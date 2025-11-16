package com.github.matei.sentinel.client;

import com.github.matei.sentinel.model.Job;
import com.github.matei.sentinel.model.WorkflowRun;

import java.time.Instant;
import java.util.List;

/**
 * Interface for communicating with GitHub Actions API.
 * Abstracts away the HTTP/JSON details.
 */

public interface GitHubApiClient
{
    /**
     * Fetches workflow runs for a repository since a given timestamp.
     *
     * @param owner Repository owner (e.g., "microsoft")
     * @param repo Repository name (e.g., "vscode")
     * @param since Only fetch runs updated after this timestamp (can be null for all runs)
     * @return List of workflow runs
     * @throws Exception if API call fails
     */

    List<WorkflowRun> getWorkflowRuns(String owner, String repo, Instant since) throws Exception;

    /**
     * Fetches jobs for a specific workflow run.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param runId Workflow run ID
     * @return List of jobs for this run
     * @throws Exception if API call fails
     */

    List<Job> getJobsForRun(String owner, String repo, long runId) throws Exception;
}
