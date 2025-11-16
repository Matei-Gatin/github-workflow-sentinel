Project Description

"This tool is a continuous monitoring system that watches GitHub workflows. It checks the GitHub API every 30 seconds to see if any workflows, jobs, or steps have changed state (started, completed, failed, etc.).

On the first run, it only reports events that happen AFTER you start watching.

On subsequent runs, it catches you up on everything that happened since you last ran the tool, then continues monitoring.

It prints one line per event to the console. If nothing changed during a 30-second interval, it stays quiet and checks again after 30 seconds.

The state (last check time and seen events) is saved in a .sentinel-state.json file so it remembers between runs."

// ----------------------------------

WorkflowRun: A form about a workflow

What: "Build and Test Pipeline"
Status: "running" or "completed"
Branch: "main"
Commit: "abc123"
When started: "2025-11-15 10:30:00"
Job: A form about a job inside a workflow

What: "build-linux"
Status: "in_progress"
When started: "2025-11-15 10:30:15"
Step: A form about a step inside a job

What: "Run tests"
Status: "completed"
Result: "success"
Duration: "2 minutes"
MonitoringEvent: What we print to the screen

Type: "JOB_STARTED"
All the details formatted nicely