package com.l7tech.server.search;

import com.l7tech.server.search.processors.DependencyProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a registry used to store dependency processors.
 *
 * @author Victor Kazakov
 */
public class DependencyProcessorRegistry {

    @NotNull
    private final Map<String, DependencyProcessor> processors;

    public DependencyProcessorRegistry() {
        processors = new HashMap<>();
    }

    public DependencyProcessorRegistry(@NotNull final Map<String, DependencyProcessor> processors) {
        this.processors = new HashMap<>(processors);
    }

    /**
     * Retrieve a dependency processor by key
     *
     * @param key The key of the dependency processor to get.
     * @return The associated dependency processor or null if there is not dependency processor for this key
     */
    @Nullable
    public DependencyProcessor get(@NotNull final String key) {
        return processors.get(key);
    }

    /**
     * Register a dependency processor. If a dependency processor is already registered with this key then an
     * IllegalArgumentException is thrown.
     *
     * @param key                 the key for this dependency processor
     * @param dependencyProcessor The dependency processor
     */
    public void register(@NotNull final String key, @NotNull final DependencyProcessor dependencyProcessor) {
        if (processors.containsKey(key))
            throw new IllegalArgumentException("There is already a dependency processor registered for key: " + key);
        processors.put(key, dependencyProcessor);
    }

    /**
     * Remove a dependency processor with the given key.
     *
     * @param key the key of the dependency processor to remove.
     * @return The dependency processor that was removed. Or null if there is no such dependency processor.
     */
    @Nullable
    public DependencyProcessor remove(@NotNull final String key) {
        return processors.remove(key);
    }
}
