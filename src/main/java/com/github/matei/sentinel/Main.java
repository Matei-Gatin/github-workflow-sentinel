package com.github.matei.sentinel;


import com.github.matei.sentinel.client.GitHubApiClient;
import com.github.matei.sentinel.client.GitHubApiClientImpl;
import com.github.matei.sentinel.config.Configuration;
import com.github.matei.sentinel.formatter.ConsoleEventFormatter;
import com.github.matei.sentinel.formatter.EventFormatter;
import com.github.matei.sentinel.monitor.EventDetector;
import com.github.matei.sentinel.monitor.WorkflowMonitor;
import com.github.matei.sentinel.persistence.FileStateManager;
import com.github.matei.sentinel.persistence.StateManager;
import com.github.matei.sentinel.util.Constants;

public class Main {
    public static void main(String[] args) {
        if (args.length < Constants.MIN_ARGS_COUNT)
        {
            printUsage();
            System.exit(1);
        }

        String repository = null;
        String token = null;

        // Parse arguments
        for (int i = 0; i < args.length; i++)
        {
            if ((Constants.ARG_REPO_LONG.equals(args[i]) || Constants.ARG_REPO_SHORT.equals(args[i]))
                    && i + 1 < args.length)
            {
                repository = args[i + 1];
                i++;
            } else if ((Constants.ARG_TOKEN_LONG.equals(args[i]) || Constants.ARG_TOKEN_SHORT.equals(args[i]))
                    && i + 1 < args.length)
            {
                token = args[i + 1];
                i++;
            }
        }

        // Validate arguments
        if (repository == null || token == null)
        {
            System.err.println("Error: Both --repo and --token are required.");
            printUsage();
            System.exit(1);
        }

        if (!repository.contains(Constants.REPO_FORMAT_SEPARATOR)
                || repository.split(Constants.REPO_FORMAT_SEPARATOR).length != Constants.REPO_FORMAT_PARTS) {
            System.err.println("Error: Repository must be in format 'owner/repo'");
            System.exit(1);
        }

        if (token.trim().isEmpty())
        {
            System.err.println("Error: Token cannot be empty");
            System.exit(1);
        }

        try
        {
            Configuration cofig = new Configuration(repository, token);

            // Initialize all components
            GitHubApiClient apiClient = new GitHubApiClientImpl(token);
            StateManager stateManager = new FileStateManager();
            EventDetector eventDetector = new EventDetector(repository);
            EventFormatter eventFormatter = new ConsoleEventFormatter();
            WorkflowMonitor monitor = new WorkflowMonitor(
                    apiClient,
                    stateManager,
                    eventDetector,
                    eventFormatter,
                    cofig
            );

            // Register shutdown hook for graceful termination (CTRL+C handling)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("\nShutting down gracefully...");
                monitor.stop();
            }));

            // start monitoring
            monitor.start();
        } catch (Exception e)
        {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage()
    {
        System.err.println("Usage: java -jar sentinel.jar --repo owner/repo --token ghp_xxxxx");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --repo, -r    Repository in format 'owner/repo' (required)");
        System.err.println("  --token, -t   GitHub Personal Access Token (required)");
        System.err.println();
        System.err.println("Example:");
        System.err.println("  java -jar sentinel.jar --repo microsoft/vscode --token ghp_abc123");
    }
}