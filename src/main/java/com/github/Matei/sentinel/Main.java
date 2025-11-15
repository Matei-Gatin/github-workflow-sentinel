package com.github.Matei.sentinel;


public class Main {
    public static void main(String[] args) {

    }

    private static void printUsage() {
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