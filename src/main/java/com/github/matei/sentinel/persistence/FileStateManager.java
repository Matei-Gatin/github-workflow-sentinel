package com.github.matei.sentinel.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.github.matei.sentinel.util.Constants;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;


/**
 * File-based implementation of StateManager.
 * Stores state in a JSON file (.sentinel-state.json) in the current directory.
 */
public class FileStateManager implements StateManager
{


    private final Gson gson;
    private final Map<String, RepositoryState> stateMap;

    public FileStateManager()
    {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.stateMap = new HashMap<>();
        loadState();
    }

    @Override
    public Optional<Instant> getLastCheckTime(String repository)
    {
        RepositoryState state = stateMap.get(repository);
        if (state == null || state.lastCheckTime == null)
        {
            return Optional.empty();
        }
        return Optional.of(Instant.parse(state.lastCheckTime));
    }

    @Override
    public void updateLastCheckTime(String repository, Instant timestamp)
    {
        RepositoryState state = stateMap.computeIfAbsent(repository, k -> new RepositoryState());
        state.lastCheckTime = timestamp.toString();
    }

    @Override
    public Set<String> getProcessedEventIds(String repository)
    {
        RepositoryState state = stateMap.get(repository);
        if (state == null || state.processedEventId == null)
        {
            return new HashSet<>();
        }

        return new HashSet<>(state.processedEventId);
    }

    @Override
    public void addProcessedEventId(String repository, String eventId)
    {
        RepositoryState state = stateMap.computeIfAbsent(repository, k -> new RepositoryState());
        if (state.processedEventId == null)
        {
            state.processedEventId = new LinkedHashSet<>();
        }
        state.processedEventId.add(eventId);

        // If we exceed the limit, remove the oldest entries
        if (state.processedEventId.size() > Constants.MAX_EVENT_IDS)
        {
            Iterator<String> iterator = state.processedEventId.iterator();
            int toRemove = state.processedEventId.size() - Constants.MAX_EVENT_IDS;
            for (int i = 0; i < toRemove && iterator.hasNext(); i++)
            {
                iterator.next();
                iterator.remove();
            }
        }
    }

    @Override
    public void save()
    {
        try (FileWriter writer = new FileWriter(Constants.STATE_FILE))
        {
            gson.toJson(stateMap, writer);
        } catch (IOException e)
        {
            System.err.println("Error saving state: " + e.getMessage());
        }
    }

    /**
     * Loads state from file if it exists.
     */
    private void loadState()
    {
        Path path = Path.of(Constants.STATE_FILE);
        if (!Files.exists(path))
        {
            return;
        }

        try (FileReader reader = new FileReader(path.toFile()))
        {
            TypeToken<Map<String, RepositoryState>> typeToken = new TypeToken<>() {};
            Map<String, RepositoryState> loaded = gson.fromJson(reader, typeToken.getType());
            if (loaded != null)
            {
                stateMap.putAll(loaded);
            }
        } catch (IOException e)
        {
            System.err.println("Error loading state: " + e.getMessage());
        }
    }

    /**
     * Inner class to hold state for a single repository.
     * This is what gets serialized to JSON.
     */
    private static class RepositoryState
    {
        String lastCheckTime; // ISO-8601 timestamp
        Set<String> processedEventId; // Event ID's we've seen
    }
}
