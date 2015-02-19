package com.l7tech.server.bundling;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This is an entity bundle used for migrating entities between gateways.
 */
public class EntityBundle {
    // The list of mappings for the entities
    @NotNull
    private final List<EntityMappingInstructions> mappingInstructions;
    //the entities map. It is used to store the entities and quickly retrieve them by their ids.
    @NotNull
    private final Map<Pair<String, EntityType>, EntityContainer> idEntityMap;
    private final List<DependencySearchResults> dependencySearchResults;

    /**
     * Creates a new Entity bundle with the given entity containers and mapping instructions
     *  @param entities            The entity containers that are part of this bundle
     * @param mappingInstructions The mapping instructions.
     * @param dependencySearchResults  The dependency analysis results used to create bundle
     */
    public EntityBundle(@NotNull final Collection<EntityContainer> entities, @NotNull final List<EntityMappingInstructions> mappingInstructions, @NotNull final List<DependencySearchResults> dependencySearchResults) {
        this.mappingInstructions = mappingInstructions;
        this.dependencySearchResults = dependencySearchResults;

        //build entity map so that entities can be quickly retrieved. Use a pair of id and type as the key because it is possible for some id's to collide (Goid(0,2) for example)
        idEntityMap = Functions.toMap(entities, new Functions.Unary<Pair<Pair<String, EntityType>, EntityContainer>, EntityContainer>() {
            @Override
            public Pair<Pair<String, EntityType>, EntityContainer> call(final EntityContainer entityContainer) {
                return new Pair<Pair<String, EntityType>, EntityContainer>(entityContainer.getId(), entityContainer);
            }
        });
    }

    /**
     * The entities in the bundle.
     *
     * @return The entities in the bundle
     */
    @NotNull
    public Collection<EntityContainer> getEntities() {
        return idEntityMap.values();
    }

    /**
     * The list of mappings for the entities in the bundle
     *
     * @return The list of mappings for the entities in the bundle
     */
    @NotNull
    public List<EntityMappingInstructions> getMappingInstructions() {
        return mappingInstructions;
    }

    /**
     * The list of dependency search results
     *
     * @return The list of dependency search results in the bundle
     */
    public List<DependencySearchResults> getDependencySearchResults() {
        return dependencySearchResults;
    }

    /**
     * Returns the entity in this bundle with the given id and type. If there is no entity with that id null is
     * returned.
     *
     * @param id         The id of the entity to return
     * @param entityType The entity type of the entity to retrieve.
     * @return The entity in the bundle with the given id and entity type or null if there is no such entity with that
     * id and type in the bundle
     */
    @Nullable
    public EntityContainer getEntity(@NotNull final String id, @NotNull final EntityType entityType) {
        return idEntityMap.get(new Pair<>(id, entityType));
    }
}
