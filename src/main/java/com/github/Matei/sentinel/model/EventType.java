package com.github.Matei.sentinel.model;

/**
 * Types of events we can monitor and report.
 */

public enum EventType
{
    WORKFLOW_QUEUED, // workflow was just queued
    WORKFLOW_STARTED, // workflow just started running
    WORKFLOW_COMPLETED, // workflow finished (success or failure)

    JOB_QUEUED, // job was queued
    JOB_STARTED, // job started running
    JOB_COMPLETED, // job finished (success or failure)

    STEP_STARTED, // step started running
    STEP_COMPLETED // step finished (success or failure)
}
