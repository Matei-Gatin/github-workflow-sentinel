package com.github.matei.sentinel.monitor;

import com.github.matei.sentinel.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Detects new/changed events by comparing current workflow state with previous state.
 * This is where the magic happens - figuring out what changed and generating MonitoringEvents.
 */
public class EventDetector
{
    
    private final String repository;
    
    // Track previous state to detect changes
    private final Map<Long, WorkflowRun> previousWorkflowRuns = new HashMap<>();
    private final Map<Long, Job> previousJobs = new HashMap<>();
    private final Map<String, Step> previousSteps = new HashMap<>(); // jobId + stepName
    
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
        }
        
        return events;
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
            if ("queued".equals(currentRun.getStatus()))
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
            if ("in_progress".equals(currentRun.getStatus()))
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
            if ("completed".equals(currentRun.getStatus()) && currentRun.getConcludedAt() != null)
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
            if ("queued".equals(previousRun.getStatus()) && "in_progress".equals(currentRun.getStatus()))
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
            if (!"completed".equals(previousRun.getStatus()) && "completed".equals(currentRun.getStatus()))
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
        
        for (Job currentJob : currentJobs)
        {
            Job previousJob = previousJobs.get(currentJob.getId());
            
            // New job detected
            if (previousJob == null)
            {
                // Job queued
                if ("queued".equals(currentJob.getStatus()))
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
                if ("in_progress".equals(currentJob.getStatus()) && currentJob.getStartedAt() != null)
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
                if ("completed".equals(currentJob.getStatus()) && currentJob.getCompletedAt() != null)
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
                if ("queued".equals(previousJob.getStatus()) && "in_progress".equals(currentJob.getStatus()))
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
                if (!"completed".equals(previousJob.getStatus()) && "completed".equals(currentJob.getStatus()))
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
        
        // Note: In the real implementation, you'll get steps from the Job object
        // For now, we'll assume Job has a getSteps() method
        // This is a placeholder - you'll need to add steps to Job model
        // and parse them from the GitHub API response
        
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
                if ("in_progress".equals(currentStep.getStatus()) && currentStep.getStartedAt() != null)
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
                if ("completed".equals(currentStep.getStatus()) && currentStep.getCompletedAt() != null)
                {
                    Duration duration = null;
                    if (currentStep.getStartedAt() != null && currentStep.getCompletedAt() != null) {
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
                if (!"completed".equals(previousStep.getStatus()) && "completed".equals(currentStep.getStatus()))
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
        }
        
        return events;
    }
}
