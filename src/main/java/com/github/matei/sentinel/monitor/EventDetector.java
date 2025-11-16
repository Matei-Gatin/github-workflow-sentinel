package com.github.matei.sentinel.monitor;

import com.github.matei.sentinel.model.*;
import com.github.matei.sentinel.util.Constants;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Detects state changes in GitHub workflows, jobs, and steps to generate monitoring events.
 * <p>
 * This class is the core of the event detection logic. It maintains the previous state
 * of workflows, jobs, and steps, and compares them with the current state to detect:
 * <ul>
 *   <li>Workflow state transitions (queued → in_progress → completed)</li>
 *   <li>Job state transitions with timing information</li>
 *   <li>Step state transitions with durations</li>
 * </ul>
 * </p>
 *
 * <h2>Detection Algorithm</h2>
 * For each entity (workflow, job, step):
 * <ol>
 *   <li>Check if we've seen it before (exists in previous state)</li>
 *   <li>If not seen before:
 *     <ul>
 *       <li>Generate event based on current status (queued, in_progress, completed)</li>
 *     </ul>
 *   </li>
 *   <li>If seen before:
 *     <ul>
 *       <li>Compare previous status with current status</li>
 *       <li>Generate event if status changed (e.g., queued → in_progress)</li>
 *     </ul>
 *   </li>
 *   <li>Calculate duration for completed events (end time - start time)</li>
 * </ol>
 *
 * <h2>State Management</h2>
 * The detector maintains three maps to track previous state:
 * <ul>
 *   <li><b>previousWorkflowRuns</b>: Keyed by workflow run ID</li>
 *   <li><b>previousJobs</b>: Keyed by job ID</li>
 *   <li><b>previousSteps</b>: Keyed by "jobId:stepName" (composite key)</li>
 * </ul>
 *
 * <h2>Memory Management</h2>
 * To prevent unbounded memory growth, the detector implements automatic cleanup:
 * <ul>
 *   <li>Tracks when each entity was last seen</li>
 *   <li>Removes entities not seen for more than 1 hour</li>
 *   <li>Cleanup runs on every call to {@link #detectEvents}</li>
 * </ul>
 *
 * <h2>Status Values</h2>
 * GitHub workflows/jobs/steps can have these statuses:
 * <ul>
 *   <li><b>queued</b>: Waiting to start ({@link Constants#STATUS_QUEUED})</li>
 *   <li><b>in_progress</b>: Currently running ({@link Constants#STATUS_IN_PROGRESS})</li>
 *   <li><b>completed</b>: Finished ({@link Constants#STATUS_COMPLETED})</li>
 * </ul>
 *
 * <h2>Conclusion Values</h2>
 * For completed entities, conclusion indicates the outcome:
 * <ul>
 *   <li><b>success</b>: Completed successfully</li>
 *   <li><b>failure</b>: Failed</li>
 *   <li><b>cancelled</b>: Manually cancelled</li>
 *   <li><b>skipped</b>: Skipped (e.g., due to conditions)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is NOT thread-safe. It should only be used from a single thread.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * EventDetector detector = new EventDetector("owner/repo");
 *
 * // First call - detects queued workflow
 * List<WorkflowRun> runs1 = List.of(new WorkflowRun(..., status="queued"));
 * List<MonitoringEvent> events1 = detector.detectEvents(runs1, Map.of());
 * // events1 contains: WORKFLOW_QUEUED
 *
 * // Second call - detects workflow started
 * List<WorkflowRun> runs2 = List.of(new WorkflowRun(..., status="in_progress"));
 * List<MonitoringEvent> events2 = detector.detectEvents(runs2, Map.of());
 * // events2 contains: WORKFLOW_STARTED
 *
 * // Third call - detects workflow completed
 * List<WorkflowRun> runs3 = List.of(new WorkflowRun(..., status="completed"));
 * List<MonitoringEvent> events3 = detector.detectEvents(runs3, Map.of());
 * // events3 contains: WORKFLOW_COMPLETED
 * }</pre>
 *
 * @see MonitoringEvent
 * @see EventType
 * @see WorkflowRun
 * @see Job
 * @see Step
 * @since 1.0
 */
public class EventDetector
{
    
    private final String repository;
    
    // Track previous state to detect changes
    private final Map<Long, WorkflowRun> previousWorkflowRuns = new HashMap<>();
    private final Map<Long, Job> previousJobs = new HashMap<>();
    private final Map<String, Step> previousSteps = new HashMap<>(); // jobId + stepName

    // Track when we last saw each item to clean up old entries
    private final Map<Long, Instant> workflowLastSeen = new HashMap<>();
    private final Map<Long, Instant> jobLastSeen = new HashMap<>();
    private final Map<String, Instant> stepLastSeen = new HashMap<>();

    public EventDetector(String repository)
    {
        this.repository = repository;
    }
    
    /**
     * Detects events by comparing current workflow runs with previous state.
     * 
     * @param currentWorkflowRuns current workflow runs from GitHub API
     * @param currentJobsMap map of runId -> list of jobs for that run
     * @return list of detected monitoring events
     */
    public List<MonitoringEvent> detectEvents(
            List<WorkflowRun> currentWorkflowRuns,
            Map<Long, List<Job>> currentJobsMap)
    {
        
        List<MonitoringEvent> events = new ArrayList<>();
        Instant now = Instant.now();

        for (WorkflowRun currentRun : currentWorkflowRuns)
        {
            WorkflowRun previousRun = previousWorkflowRuns.get(currentRun.getId());
            
            // Detect workflow-level events
            events.addAll(detectWorkflowEvents(currentRun, previousRun));
            
            // Detect job-level events
            List<Job> currentJobs = currentJobsMap.getOrDefault(currentRun.getId(), Collections.emptyList());
            events.addAll(detectJobEvents(currentRun, currentJobs));
            
            // Update previous state
            previousWorkflowRuns.put(currentRun.getId(), currentRun);

            workflowLastSeen.put(currentRun.getId(), now);
        }

        // Clean up here to prevent memory leak
        cleanupOldEntries(now);

        return events;
    }

    /**
     * Removes entries older than CLEANUP_THRESHOLD to prevent unbounded memory growth.
     */
    private void cleanupOldEntries(Instant now)
    {
        workflowLastSeen.entrySet().removeIf(entry ->
                {
                        if (Duration.between(entry.getValue(), now).compareTo(Constants.CLEANUP_THRESHOLD) > 0)
                        {
                            previousWorkflowRuns.remove(entry.getKey());
                            return true;
                        }
                        return false;
                }
        );

        // Remove old jobs
        jobLastSeen.entrySet().removeIf(entry -> {
            if (Duration.between(entry.getValue(), now).compareTo(Constants.CLEANUP_THRESHOLD) > 0)
            {
                previousJobs.remove(entry.getKey());
                return true;
            }
            return false;
        });

        // Remove old steps
        stepLastSeen.entrySet().removeIf(entry -> {
            if (Duration.between(entry.getValue(), now).compareTo(Constants.CLEANUP_THRESHOLD)  > 0)
            {
                previousSteps.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Detects workflow-level events (QUEUED, STARTED, COMPLETED).
     */
    private List<MonitoringEvent> detectWorkflowEvents(WorkflowRun currentRun, WorkflowRun previousRun)
    {
        List<MonitoringEvent> events = new ArrayList<>();
        
        // New workflow run detected
        if (previousRun == null)
        {
            // If status is "queued", report WORKFLOW_QUEUED
            if (Constants.STATUS_QUEUED.equals(currentRun.getStatus()))
            {
                events.add(new MonitoringEvent(
                    EventType.WORKFLOW_QUEUED,
                    currentRun.getUpdatedAt(),
                    repository,
                    currentRun.getHeadBranch(),
                    currentRun.getHeadSha(),
                    currentRun.getName(),
                    null, // no job
                    null, // no step
                    null, // no conclusion yet
                    null  // no duration yet
                ));
            }
            
            // If status is "in_progress", report WORKFLOW_STARTED
            if (Constants.STATUS_IN_PROGRESS.equals(currentRun.getStatus()))
            {
                events.add(new MonitoringEvent(
                    EventType.WORKFLOW_STARTED,
                    currentRun.getUpdatedAt(),
                    repository,
                    currentRun.getHeadBranch(),
                    currentRun.getHeadSha(),
                    currentRun.getName(),
                    null,
                    null,
                    null,
                    null
                ));
            }
            
            // If already completed when we first see it, report WORKFLOW_COMPLETED
            if (Constants.STATUS_COMPLETED.equals(currentRun.getStatus())
                    && currentRun.getConcludedAt() != null)
            {
                events.add(new MonitoringEvent(
                    EventType.WORKFLOW_COMPLETED,
                    currentRun.getConcludedAt(),
                    repository,
                    currentRun.getHeadBranch(),
                    currentRun.getHeadSha(),
                    currentRun.getName(),
                    null,
                    null,
                    currentRun.getConclusion(),
                    null // We don't have workflow start time, so can't calculate duration
                ));
            }
        } else
        {
            // Workflow state changed
            // Check if status changed from "queued" to "in_progress"
            if (Constants.STATUS_QUEUED.equals(previousRun.getStatus())
                    && Constants.STATUS_IN_PROGRESS.equals(currentRun.getStatus()))
            {
                events.add(new MonitoringEvent(
                    EventType.WORKFLOW_STARTED,
                    currentRun.getUpdatedAt(),
                    repository,
                    currentRun.getHeadBranch(),
                    currentRun.getHeadSha(),
                    currentRun.getName(),
                    null,
                    null,
                    null,
                    null
                ));
            }
            
            // Check if workflow completed
            if (!Constants.STATUS_COMPLETED.equals(previousRun.getStatus())
                    && Constants.STATUS_COMPLETED.equals(currentRun.getStatus()))
            {
                events.add(new MonitoringEvent(
                    EventType.WORKFLOW_COMPLETED,
                    currentRun.getConcludedAt() != null ? currentRun.getConcludedAt() : currentRun.getUpdatedAt(),
                    repository,
                    currentRun.getHeadBranch(),
                    currentRun.getHeadSha(),
                    currentRun.getName(),
                    null,
                    null,
                    currentRun.getConclusion(),
                    null
                ));
            }
        }
        
        return events;
    }
    
    /**
     * Detects job-level events (QUEUED, STARTED, COMPLETED) and step-level events.
     */
    private List<MonitoringEvent> detectJobEvents(WorkflowRun workflowRun, List<Job> currentJobs)
    {
        List<MonitoringEvent> events = new ArrayList<>();
        Instant now = Instant.now();

        for (Job currentJob : currentJobs)
        {
            Job previousJob = previousJobs.get(currentJob.getId());
            
            // New job detected
            if (previousJob == null)
            {
                // Job queued
                if (Constants.STATUS_QUEUED.equals(currentJob.getStatus()))
                {
                    events.add(new MonitoringEvent(
                        EventType.JOB_QUEUED,
                        Instant.now(), // GitHub API doesn't provide queue time, use current time
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        currentJob.getName(),
                        null,
                        null,
                        null
                    ));
                }
                
                // Job started
                if (Constants.STATUS_IN_PROGRESS.equals(currentJob.getStatus())
                        && currentJob.getStartedAt() != null)
                {
                    events.add(new MonitoringEvent(
                        EventType.JOB_STARTED,
                        currentJob.getStartedAt(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        currentJob.getName(),
                        null,
                        null,
                        null
                    ));
                }
                
                // Job already completed
                if (Constants.STATUS_COMPLETED.equals(currentJob.getStatus())
                        && currentJob.getCompletedAt() != null)
                {
                    Duration duration = null;
                    if (currentJob.getStartedAt() != null && currentJob.getCompletedAt() != null)
                    {
                        duration = Duration.between(currentJob.getStartedAt(), currentJob.getCompletedAt());
                    }
                    
                    events.add(new MonitoringEvent(
                        EventType.JOB_COMPLETED,
                        currentJob.getCompletedAt(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        currentJob.getName(),
                        null,
                        currentJob.getConclusion(),
                        duration
                    ));
                }
            } else
            {
                // Job state changed
                // Check if job started (queued -> in_progress)
                if (Constants.STATUS_QUEUED.equals(previousJob.getStatus())
                        && Constants.STATUS_IN_PROGRESS.equals(currentJob.getStatus()))
                {
                    events.add(new MonitoringEvent(
                        EventType.JOB_STARTED,
                        currentJob.getStartedAt() != null ? currentJob.getStartedAt() : Instant.now(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        currentJob.getName(),
                        null,
                        null,
                        null
                    ));
                }
                
                // Check if job completed
                if (!Constants.STATUS_COMPLETED.equals(previousJob.getStatus())
                        && Constants.STATUS_COMPLETED.equals(currentJob.getStatus()))
                {
                    Duration duration = null;
                    if (currentJob.getStartedAt() != null && currentJob.getCompletedAt() != null)
                    {
                        duration = Duration.between(currentJob.getStartedAt(), currentJob.getCompletedAt());
                    }
                    
                    events.add(new MonitoringEvent(
                        EventType.JOB_COMPLETED,
                        currentJob.getCompletedAt() != null ? currentJob.getCompletedAt() : Instant.now(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        currentJob.getName(),
                        null,
                        currentJob.getConclusion(),
                        duration
                    ));
                }
            }
            
            // Detect step-level events (if job has steps)
            events.addAll(detectStepEvents(workflowRun, currentJob));

            jobLastSeen.put(currentJob.getId(), now);

            // Update previous state
            previousJobs.put(currentJob.getId(), currentJob);
        }
        
        return events;
    }
    
    /**
     * Detects step-level events (STARTED, COMPLETED).
     * Note: GitHub API provides steps in Job details.
     */
    private List<MonitoringEvent> detectStepEvents(WorkflowRun workflowRun, Job job)
    {
        List<MonitoringEvent> events = new ArrayList<>();
        Instant now = Instant.now();

        List<Step> currentSteps = job.getSteps();
        if (currentSteps == null)
        {
            return events;
        }
        
        for (Step currentStep : currentSteps)
        {
            String stepKey = job.getId() + ":" + currentStep.getName();
            Step previousStep = previousSteps.get(stepKey);
            
            if (previousStep == null)
            {
                // New step started
                if (Constants.STATUS_IN_PROGRESS.equals(currentStep.getStatus())
                        && currentStep.getStartedAt() != null)
                {
                    events.add(new MonitoringEvent(
                        EventType.STEP_STARTED,
                        currentStep.getStartedAt(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        job.getName(),
                        currentStep.getName(),
                        null,
                        null
                    ));
                }
                
                // Step already completed
                if (Constants.STATUS_COMPLETED.equals(currentStep.getStatus())
                        && currentStep.getCompletedAt() != null)
                {
                    Duration duration = null;
                    if (currentStep.getStartedAt() != null && currentStep.getCompletedAt() != null)
                    {
                        duration = Duration.between(currentStep.getStartedAt(), currentStep.getCompletedAt());
                    }
                    
                    events.add(new MonitoringEvent(
                        EventType.STEP_COMPLETED,
                        currentStep.getCompletedAt(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        job.getName(),
                        currentStep.getName(),
                        currentStep.getConclusion(),
                        duration
                    ));
                }
            } else
            {
                // Step state changed
                if (!Constants.STATUS_COMPLETED.equals(previousStep.getStatus())
                        && Constants.STATUS_COMPLETED.equals(currentStep.getStatus()))
                {
                    Duration duration = null;
                    if (currentStep.getStartedAt() != null && currentStep.getCompletedAt() != null)
                    {
                        duration = Duration.between(currentStep.getStartedAt(), currentStep.getCompletedAt());
                    }
                    
                    events.add(new MonitoringEvent(
                        EventType.STEP_COMPLETED,
                        currentStep.getCompletedAt() != null ? currentStep.getCompletedAt() : Instant.now(),
                        repository,
                        workflowRun.getHeadBranch(),
                        workflowRun.getHeadSha(),
                        workflowRun.getName(),
                        job.getName(),
                        currentStep.getName(),
                        currentStep.getConclusion(),
                        duration
                    ));
                }
            }
            
            // Update previous state
            previousSteps.put(stepKey, currentStep);

            stepLastSeen.put(stepKey, now);
        }
        
        return events;
    }
}
