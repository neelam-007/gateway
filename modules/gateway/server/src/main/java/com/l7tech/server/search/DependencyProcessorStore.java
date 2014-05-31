package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
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

    //The processors map
    private Map<Dependency.DependencyType, DependencyProcessor> processors;

    /**
     * Creates a new dependency processor store with the given dependency processors.
     *
     * @param processors The dependency processors that this store has.
     */
    public DependencyProcessorStore(@NotNull final Map<Dependency.DependencyType, DependencyProcessor> processors) {
        this.processors = processors;
        //validates that the default processor is present in the processor map. This is required.
        if (!processors.containsKey(Dependency.DependencyType.ANY) || processors.get(Dependency.DependencyType.ANY) == null) {
            throw new IllegalArgumentException("The map of entity processors must contain a processor for a default object. Add a processor for EntityType.ANY");
        }
    }

    /**
     * Returns a Dependency processor given a DependencyType. If a mapped processor is not found a default dependency
     * processor is returned. Note this will not be able to return assertion specific dependency processors, if it is
     * given the Assertion type the default assertion processor will always be returned.
     *
     * @param type The DependencyType to return a processor for
     * @return The DependencyProcessor for the given dependency type. If a specific processor cannot be found a default
     * DependencyProcessor is returned.
     */
    @NotNull
    public DependencyProcessor getProcessor(@NotNull final Dependency.DependencyType type) {
        DependencyProcessor processor = processors.get(type);
        return processor != null ? processor : processors.get(Dependency.DependencyType.ANY);
    }

    /**
     * Returns a Dependency processor given an object. If a mapped processor is not found a generic dependency
     * processor is returned.
     *
     * @param object The object to return a processor for
     * @return The DependencyProcessor for the given dependency type. If a specific processor cannot be found a generic
     * DependencyProcessor is returned.
     */
    @NotNull
    public DependencyProcessor getProcessor(@NotNull final Object object) {
        return getProcessor(getTypeFromObject(object));
    }

    /**
     * Returns a dependency type given an object. If a specific type could not be found DependencyType.ANY is
     * returned.
     *
     * @param obj The object to find the dependency type of.
     * @return The dependency type of the given object
     */
    @NotNull
    private static com.l7tech.search.Dependency.DependencyType getTypeFromObject(@NotNull final Object obj) {
        if (obj instanceof Entity) {
            //if its an entity use the entity type to find the dependency type
            try {
                //noinspection unchecked
                return com.l7tech.search.Dependency.DependencyType.fromEntityType(EntityType.findTypeByEntity((Class<? extends Entity>) obj.getClass()));
            } catch (IllegalArgumentException e) {
                //Use the Generic dependency type for other entity types
                return com.l7tech.search.Dependency.DependencyType.ANY;
            }
        } else if (obj instanceof Assertion) {
            return com.l7tech.search.Dependency.DependencyType.ASSERTION;
        } else {
            return com.l7tech.search.Dependency.DependencyType.ANY;
        }
    }
}
