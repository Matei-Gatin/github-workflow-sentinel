package com.github.matei.sentinel.config;

import com.github.matei.sentinel.util.Constants;
import lombok.Getter;

/**
 * Configuration holder for the application.
 * Contains repository and GitHub token from command-line arguments.
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
