package com.github.matei.sentinel.client;

import com.github.matei.sentinel.model.Job;
import com.github.matei.sentinel.model.WorkflowRun;

import java.time.Instant;
import java.util.List;

/**
 * Client interface for interacting with the GitHub Actions API.
 * <p>
 * This interface abstracts the HTTP communication with GitHub's REST API,
 * allowing for easy testing and alternative implementations (e.g., mock client for tests).
 * </p>
 *
 * <h2>GitHub API Rate Limits</h2>
 * GitHub enforces the following rate limits:
 * <ul>
 *   <li><b>Authenticated requests</b>: 5,000 requests per hour</li>
 *   <li><b>Unauthenticated requests</b>: 60 requests per hour (not supported by this tool)</li>
 * </ul>
 * <p>
 * This client does not implement rate limit handling or backoff strategies.
 * If rate limits are exceeded, the API will return HTTP 403 with rate limit headers.
 * </p>
 *
 * <h2>Authentication</h2>
 * All requests must be authenticated using a GitHub Personal Access Token (PAT)
 * with the {@code repo} scope for private repositories, or {@code public_repo}
 * scope for public repositories only.
 *
 * <h2>Thread Safety</h2>
 * Implementations of this interface are not required to be thread-safe.
 * The tool uses a single-threaded polling model.
 *
 * @see <a href="https://docs.github.com/en/rest/actions">GitHub Actions API</a>
 * @see <a href="https://docs.github.com/en/rest/overview/resources-in-the-rest-api#rate-limiting">GitHub Rate Limiting</a>
 * @since 1.0
 */
public interface GitHubApiClient
{
    /**
     * Fetches workflow runs for a repository, optionally filtered by update time.
     * <p>
     * This method retrieves workflow runs from the GitHub Actions API. Runs are returned
     * in descending order by update time (most recent first). The result is limited to
     * {@link com.github.matei.sentinel.util.Constants#MAX_RESULTS_PER_PAGE} runs per request.
     * </p>
     *
     * <h3>API Endpoint</h3>
     * {@code GET /repos/{owner}/{repo}/actions/runs}
     *
     * <h3>Filtering</h3>
     * <ul>
     *   <li>If {@code since} is {@code null}, returns all recent workflow runs</li>
     *   <li>If {@code since} is provided, only returns runs updated after that timestamp</li>
     * </ul>
     *
     * <h3>Response</h3>
     * Each {@link WorkflowRun} includes:
     * <ul>
     *   <li>Workflow run ID, name, and status</li>
     *   <li>Branch name and commit SHA</li>
     *   <li>Creation, update, and conclusion timestamps</li>
     *   <li>Conclusion status (success, failure, cancelled, etc.)</li>
     * </ul>
     *
     * @param owner the repository owner (e.g., "microsoft")
     * @param repo the repository name (e.g., "vscode")
     * @param since only include runs updated after this timestamp; if {@code null}, returns all recent runs
     * @return list of workflow runs, may be empty but never {@code null}
     * @throws IllegalArgumentException if {@code owner} or {@code repo} is null or empty
     * @throws RuntimeException if the API request fails due to:
     *         <ul>
     *           <li>HTTP 401: Invalid or expired authentication token</li>
     *           <li>HTTP 403: Rate limit exceeded or insufficient permissions</li>
     *           <li>HTTP 404: Repository not found or not accessible</li>
     *           <li>Network errors or timeouts</li>
     *         </ul>
     * @see WorkflowRun
     */
    List<WorkflowRun> getWorkflowRuns(String owner, String repo, Instant since) throws Exception;

    /**
     * Fetches all jobs for a specific workflow run, including their steps.
     * <p>
     * This method retrieves the jobs that are part of a workflow run. Each job
     * represents a unit of work within the workflow, and contains multiple steps
     * that are executed sequentially.
     * </p>
     *
     * <h3>API Endpoint</h3>
     * {@code GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs}
     *
     * <h3>Response</h3>
     * Each {@link Job} includes:
     * <ul>
     *   <li>Job ID, name, and status</li>
     *   <li>Start and completion timestamps</li>
     *   <li>Conclusion status (success, failure, cancelled, skipped)</li>
     *   <li>List of {@link com.github.matei.sentinel.model.Step}s executed by the job</li>
     * </ul>
     *
     * <h3>Job Status Values</h3>
     * <ul>
     *   <li>{@code queued}: Job is waiting to be executed</li>
     *   <li>{@code in_progress}: Job is currently running</li>
     *   <li>{@code completed}: Job has finished (check conclusion for outcome)</li>
     * </ul>
     *
     * @param owner the repository owner
     * @param repo the repository name
     * @param runId the unique identifier of the workflow run
     * @return list of jobs with their steps, may be empty but never {@code null}
     * @throws IllegalArgumentException if {@code owner} or {@code repo} is null/empty, or {@code runId <= 0}
     * @throws RuntimeException if the API request fails or the run doesn't exist
     * @see Job
     * @see com.github.matei.sentinel.model.Step
     */
    List<Job> getJobsForRun(String owner, String repo, long runId) throws Exception;
}
