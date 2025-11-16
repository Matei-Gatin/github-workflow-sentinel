package com.github.matei.sentinel.config;

import com.github.matei.sentinel.util.Constants;
import lombok.Getter;

/**
 * Holds application configuration parsed from command-line arguments.
 * <p>
 * This class is immutable and validates configuration at construction time.
 * It provides convenient accessors for both the full repository string and
 * its components (owner and repo name).
 * </p>
 *
 * <h2>Repository Format</h2>
 * The repository must be in the format {@code owner/repo}, where:
 * <ul>
 *   <li><b>owner</b>: GitHub username or organization (e.g., "microsoft")</li>
 *   <li><b>repo</b>: Repository name (e.g., "vscode")</li>
 * </ul>
 *
 * <h2>Token Requirements</h2>
 * The GitHub Personal Access Token (PAT) must have the {@code repo} scope for
 * private repositories, or {@code public_repo} scope for public repositories only.
 *
 * <h2>Validation</h2>
 * The constructor validates:
 * <ul>
 *   <li>Repository is not null</li>
 *   <li>Repository contains exactly one forward slash</li>
 *   <li>Both owner and repo parts are non-empty</li>
 *   <li>Token is not null or empty</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is immutable and therefore thread-safe.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Configuration config = new Configuration("microsoft/vscode", "ghp_xxxxx");
 *     System.out.println("Owner: " + config.getOwner());      // "microsoft"
 *     System.out.println("Repo: " + config.getRepo());        // "vscode"
 *     System.out.println("Full: " + config.getRepository());  // "microsoft/vscode"
 * } catch (IllegalArgumentException e) {
 *     System.err.println("Invalid configuration: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see Constants#REPO_FORMAT_SEPARATOR
 * @see Constants#REPO_FORMAT_PARTS
 * @since 1.0
 */

@Getter
public class Configuration
{
    // repository in format "owner/repo" (e.g.: matei/sentinel)
    private final String repository;
    private final String owner;
    private final String repo;
    // GitHub Personal Access Token for API Auth
    private final String token;

    public Configuration(String repository, String token)
    {
        if (repository == null || !repository.contains(Constants.REPO_FORMAT_SEPARATOR))
        {
            throw new IllegalArgumentException("Repository must be in format 'owner/repo'");
        }

        String[] parts = repository.split(Constants.REPO_FORMAT_SEPARATOR);

        if (parts.length != Constants.REPO_FORMAT_PARTS)
        {
            throw new IllegalArgumentException("Repository must be in format 'owner/repo' (found " + parts.length +
                    " parts)");
        }

        if (parts[0].trim().isEmpty() || parts[1].trim().isEmpty())
        {
            throw new IllegalArgumentException("Repository owner and name cannot be empty");
        }

        this.repository = repository;
        this.token = token;
        this.owner = parts[0];
        this.repo = parts[1];
    }

    @Override
    public String toString()
    {
        return "Configuration{" +
                "repository='" + repository + '\'' +
                ", owner='" + owner + '\'' +
                ", repo='" + repo + '\'' +
                ", token='***hidden***'" +
                '}';
    }
}
