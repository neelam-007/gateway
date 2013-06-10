package com.l7tech.server.search;

import com.l7tech.search.Dependency;
import com.l7tech.server.search.processors.DependencyProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The dependency processor store is used to retrieve the appropriate dependency processor for a given type.
 *
 * @author Victor Kazakov
 */
public class DependencyProcessorStore {

    private Map<Dependency.DependencyType, DependencyProcessor> processors;

    /**
     * Creates a new dependency processor store with the given dependency processors.
     *
     * @param processors The dependency processors that this store has.
     */
    public DependencyProcessorStore(@NotNull Map<Dependency.DependencyType, DependencyProcessor> processors) {
        this.processors = processors;
        //validates that the generic processor is present in the processor map. This is required.
        if (!processors.containsKey(Dependency.DependencyType.GENERIC)) {
            throw new IllegalArgumentException("The map of entity processors must contain a processor for a generic object. Add a processor for EntityType.ANY");
        }
    }

    /**
     * Returns a Dependency processor given a DependencyType. If a mapped processor is not found a generic dependency
     * processor is returned.
     *
     * @param type The DependencyType to return a processor for
     * @return The DependencyProcessor for the given dependency type. If a specific processor cannot be found a generic
     *         DependencyProcessor is returned.
     */
    @NotNull
    public DependencyProcessor getProcessor(@NotNull Dependency.DependencyType type) {
        DependencyProcessor processor = processors.get(type);
        return processor != null ? processor : processors.get(Dependency.DependencyType.GENERIC);
    }
}
