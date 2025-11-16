# GitHub Workflow Sentinel 🔍

A Java command-line tool that monitors GitHub Actions workflow runs in real-time and reports events as they happen.

## Features

- **Real-time Monitoring**: Polls GitHub API every 30 seconds for workflow updates
- **Comprehensive Event Tracking**: Reports workflow, job, and step events
- **Stateful Persistence**: Remembers last check time between runs
- **Graceful Shutdown**: Handles CTRL+C cleanly
- **Memory Efficient**: Automatic cleanup of old event data

## Requirements

- Java 21 or higher
- Maven 3.6+ (for building)
- GitHub Personal Access Token with `repo` scope

## Installation

### Clone the Repository
```bash
git clone https://github.com/Matei-Gatin/github-workflow-sentinel.git
cd github-workflow-sentinel
```

### Build the Project
```bash
mvn clean package
```

This creates an executable JAR: `target/sentinel.jar`

## Usage

```bash
java -jar target/sentinel.jar --repo owner/repo --token ghp_xxxxx
```

### Arguments

| Argument | Short | Description | Required |
|----------|-------|-------------|----------|
| `--repo` | `-r` | Repository in format `owner/repo` | Yes |
| `--token` | `-t` | GitHub Personal Access Token | Yes |

### Example

```bash
java -jar target/sentinel.jar --repo microsoft/vscode --token ghp_abc123def456
```

## Getting a GitHub Token

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Give it a name (e.g., "Sentinel Monitor")
4. Select scope: **`repo`** (Full control of private repositories)
5. Click "Generate token"
6. Copy the token (starts with `ghp_`)

**⚠️ Security Note**: Never commit your token to a repository!

## Output Format

The tool outputs one line per event:

```
[TIMESTAMP] [EVENT_TYPE] [STATUS] repo:owner/repo branch:main sha:abc123 workflow:"CI" job:build step:compile - Duration: 2m 30s
```

### Event Types

- `WORKFLOW_QUEUED` - Workflow is queued
- `WORKFLOW_STARTED` - Workflow execution started
- `WORKFLOW_COMPLETED` - Workflow finished (with conclusion)
- `JOB_QUEUED` - Job is queued
- `JOB_STARTED` - Job execution started
- `JOB_COMPLETED` - Job finished (with duration)
- `STEP_STARTED` - Step execution started
- `STEP_COMPLETED` - Step finished (with duration)

### Example Output

```
Starting monitoring for repository: microsoft/vscode
Press CTRL+C to stop.

[2025-11-15T10:30:00Z] [WORKFLOW_STARTED] repo:microsoft/vscode branch:main sha:abc123d workflow:"CI Pipeline"
[2025-11-15T10:30:15Z] [JOB_STARTED] repo:microsoft/vscode branch:main sha:abc123d workflow:"CI Pipeline" job:build
[2025-11-15T10:31:45Z] [STEP_COMPLETED] [SUCCESS] repo:microsoft/vscode branch:main sha:abc123d workflow:"CI Pipeline" job:build step:"Compile" - Duration: 1m 30s
[2025-11-15T10:32:00Z] [JOB_COMPLETED] [SUCCESS] repo:microsoft/vscode branch:main sha:abc123d workflow:"CI Pipeline" job:build - Duration: 2m 15s
```

## How It Works

### First Run
- Reports only **NEW** events that occur after the tool starts
- Creates a state file `.sentinel-state.json`

### Subsequent Runs
- Reports **ALL** events since the last run (catches up on missed events)
- Updates the state file

### State File
The tool stores its state in `.sentinel-state.json`:
```json
{
  "owner/repo": {
    "lastCheckTime": "2025-11-15T10:30:00Z",
    "processedEventId": ["event1", "event2", "..."]
  }
}
```

**Note**: This file is automatically managed. Add it to `.gitignore`.

## Architecture

### Design Patterns Used

- **Strategy Pattern** - Event formatting (`EventFormatter`)
- **Repository Pattern** - State persistence (`StateManager`)
- **Dependency Injection** - Component initialization
- **Observer Pattern** - Shutdown hooks

### Package Structure

```
com.github.matei.sentinel/
├── Main.java                    # Entry point
├── client/                      # GitHub API communication
├── config/                      # Configuration
├── formatter/                   # Event output formatting
├── model/                       # Domain models
├── monitor/                     # Monitoring logic
├── persistence/                 # State management
└── util/                        # Constants and utilities
```

## Error Handling

The tool handles common errors gracefully:

- **401 Unauthorized**: Invalid or expired token
- **403 Forbidden**: Rate limit exceeded or insufficient permissions
- **404 Not Found**: Repository not found
- **Timeout**: Network timeout (retries automatically)

## Testing

Run the test suite:
```bash
mvn test
```

Test coverage includes:
- Model classes
- Event formatting
- State persistence
- Configuration validation

## Troubleshooting

### "Invalid or expired GitHub token"
- Check that your token is correct
- Ensure it has `repo` scope
- Generate a new token if needed

### "Repository not found"
- Verify repository name format: `owner/repo`
- Check token has access to the repository
- For private repos, token needs full `repo` scope

### "Request timed out"
- Check your internet connection
- GitHub API might be slow - tool will retry automatically

## Performance & Scalability

- **Memory Management**: Automatically cleans up events older than 1 hour
- **State File**: Limits stored event IDs to 1000 (FIFO)
- **API Calls**: Polls every 30 seconds (within GitHub rate limits)
- **Rate Limit**: 5000 requests/hour (authenticated)

## License

MIT License - See [LICENSE](LICENSE) for details

## Author

**Matei Gatin**
- GitHub: [@Matei-Gatin](https://github.com/Matei-Gatin)

## Acknowledgments

- Built with Java 21+ HttpClient (no external HTTP libraries!)
- Uses Gson for JSON parsing
- JUnit 5 for testing

---