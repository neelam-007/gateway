package com.l7tech.server.bundling;

import com.l7tech.objectmodel.Entity;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This is an entity bundle used for migrating entities betreen gateways.
 */
public class EntityBundle {
    // The list of mappings for the entities
    private final List<EntityMappingInstructions> mappingInstructions;
    //the entities map. It is used to store the entities and quickly retrieve them by their ids.
    private final Map<String, Entity> idEntityMap;

    public EntityBundle(Collection<Entity> entities, List<EntityMappingInstructions> mappingInstructions) {
        this.mappingInstructions = mappingInstructions;

        //build entity map so that entities can be quickly retrieved.
        idEntityMap = Functions.toMap(entities, new Functions.Unary<Pair<String, Entity>, Entity>() {
            @Override
            public Pair<String, Entity> call(Entity entity) {
                return new Pair<>(entity.getId(), entity);
            }
        });
    }

    /**
     * The entities in the bundle.
     *
     * @return The entities in the bundle
     */
    public Collection<Entity> getEntities() {
        return idEntityMap.values();
    }

    /**
     * The list of mappings for the entities in the bundle
     *
     * @return The list of mappings for the entities in the bundle
     */
    public List<EntityMappingInstructions> getMappingInstructions() {
        return mappingInstructions;
    }

    /**
     * Returns the entity in this bundle with the given id. If there is no entity with that id null is returned.
     *
     * @param id The id of the entity to return
     * @return The entity in the bundle with the given id or null if there is no such entity with that id in the bundle
     */
    public Entity getEntity(@NotNull final String id) {
        return idEntityMap.get(id);
    }
}
