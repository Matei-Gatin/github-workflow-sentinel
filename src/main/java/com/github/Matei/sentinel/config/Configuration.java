package com.github.Matei.sentinel.config;

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
        if (repository == null || !repository.contains("/"))
        {
            throw new IllegalArgumentException("Repository must be in format 'owner/repo'");
        }

        this.repository = repository;
        this.token = token;

        String[] parts = repository.split("/");
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
