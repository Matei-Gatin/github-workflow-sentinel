# 🔍 GitHub Workflow Sentinel

A command-line tool that monitors GitHub Actions workflows and reports updates in real-time.

## 🎯 Features

- ✅ Monitor GitHub Actions workflow runs
- ✅ Track jobs and steps in real-time
- ✅ Report new workflows being queued
- ✅ Track job/step start and completion
- ✅ Persistent state across restarts
- ✅ Graceful interruption handling
- ✅ Concise, human-readable output

## 🚀 Usage

```bash
java -jar github-workflow-sentinel.jar \
  --repo "owner/repository" \
  --token "ghp_xxxxxxxxxxxx" \
  --interval 30
```

### Parameters

- `--repo, -r`: Repository in format `owner/repo` (required)
- `--token, -t`: GitHub personal access token (required)
- `--interval, -i`: Polling interval in seconds (default: 30)

### Example Output

```
2025-11-15 16:05:55 | QUEUED  | workflow: Build and Test | branch: main | commit: abc123f
2025-11-15 16:06:00 | STARTED | workflow: Build and Test | run: 123456
2025-11-15 16:06:05 | STARTED | job: build | run: 123456
2025-11-15 16:06:10 | STARTED | step: Checkout | job: build
2025-11-15 16:06:12 | SUCCESS | step: Checkout | job: build | duration: 2s
```

## 🔧 Building

```bash
mvn clean package
```

## 📚 Requirements

- Java 21+
- GitHub Personal Access Token with `repo` and `workflow` permissions

## 🎓 Created By

Matei Gatin - JetBrains Internship Application Task

## 📝 License

MIT