/**
 * GitHub Workflow Sentinel - Real-time GitHub Actions Monitoring Tool.
 * <p>
 * This application monitors GitHub Actions workflow runs and reports events in real-time.
 * It uses a polling mechanism to query the GitHub API every 30 seconds and detects
 * state changes in workflows, jobs, and steps.
 * </p>
 *
 * <h2>Architecture Overview</h2>
 * The application is organized into the following packages:
 * <ul>
 *   <li><b>{@link com.github.matei.sentinel.client}</b> - GitHub API communication layer</li>
 *   <li><b>{@link com.github.matei.sentinel.model}</b> - Domain models representing workflows, jobs, and steps</li>
 *   <li><b>{@link com.github.matei.sentinel.monitor}</b> - Core monitoring logic and event detection</li>
 *   <li><b>{@link com.github.matei.sentinel.persistence}</b> - State management and persistence layer</li>
 *   <li><b>{@link com.github.matei.sentinel.formatter}</b> - Event output formatting</li>
 *   <li><b>{@link com.github.matei.sentinel.config}</b> - Application configuration</li>
 *   <li><b>{@link com.github.matei.sentinel.util}</b> - Utility classes and constants</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * The application employs several design patterns:
 * <ul>
 *   <li><b>Strategy Pattern</b> - {@link com.github.matei.sentinel.formatter.EventFormatter} allows pluggable formatters</li>
 *   <li><b>Repository Pattern</b> - {@link com.github.matei.sentinel.persistence.StateManager} abstracts persistence</li>
 *   <li><b>Dependency Injection</b> - Constructor-based injection in {@link com.github.matei.sentinel.monitor.WorkflowMonitor}</li>
 *   <li><b>Observer Pattern</b> - Shutdown hooks for graceful termination</li>
 * </ul>
 *
 * <h2>Core Workflow</h2>
 * <ol>
 *   <li>Parse command-line arguments (repository and GitHub token)</li>
 *   <li>Initialize components (API client, state manager, event detector, formatter)</li>
 *   <li>Check for previous state (first run vs. subsequent runs)</li>
 *   <li>Enter monitoring loop:
 *     <ul>
 *       <li>Poll GitHub API for workflow runs</li>
 *       <li>For each run, fetch jobs and steps</li>
 *       <li>Detect state changes and generate events</li>
 *       <li>Format and output events to console</li>
 *       <li>Update state with new timestamp and processed event IDs</li>
 *       <li>Sleep for 30 seconds, then repeat</li>
 *     </ul>
 *   </li>
 *   <li>On CTRL+C, shutdown gracefully and save state</li>
 * </ol>
 *
 * <h2>State Management</h2>
 * The tool maintains state between runs in a JSON file ({@code .sentinel-state.json}):
 * <ul>
 *   <li><b>First Run</b>: Reports only NEW events that occur after the tool starts</li>
 *   <li><b>Subsequent Runs</b>: Reports ALL events since the last check time (catches up on missed events)</li>
 * </ul>
 *
 * <h2>Memory Management</h2>
 * To prevent unbounded memory growth:
 * <ul>
 *   <li>Old workflow/job/step data is cleaned up after 1 hour</li>
 *   <li>Event IDs in state file are limited to 1000 (FIFO removal)</li>
 *   <li>HTTP responses are cached for 10 seconds to reduce API calls</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * The tool handles common error scenarios:
 * <ul>
 *   <li><b>401 Unauthorized</b>: Invalid or expired GitHub token</li>
 *   <li><b>403 Forbidden</b>: Rate limit exceeded or insufficient permissions</li>
 *   <li><b>404 Not Found</b>: Repository not found or not accessible</li>
 *   <li><b>Timeout</b>: Network timeout with automatic retry</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * java -jar sentinel.jar --repo microsoft/vscode --token ghp_xxxxx
 * }</pre>
 *
 * @author Matei Gatin
 * @version 1.0
 * @since 1.0
 * @see com.github.matei.sentinel.Main
 * @see <a href="https://docs.github.com/en/rest/actions">GitHub Actions API Documentation</a>
 */
package com.github.matei.sentinel;