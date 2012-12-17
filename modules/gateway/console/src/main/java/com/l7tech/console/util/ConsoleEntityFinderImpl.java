package com.l7tech.console.util;

import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;

/**
 * A utility that provides read-only lookup services for entities, backed by the actual admin APIs for the
 * corresponding entities.  This can be used within the SSM.  RBAC enforcement is done on the actual entity APIs.
 */
public class ConsoleEntityFinderImpl implements HeaderBasedEntityFinder<Entity,EntityHeader> {

    @NotNull
    public Entity findByEntityTypeAndPrimaryId(@NotNull EntityType entityType, @NotNull String id) throws FindException {
        Registry registry = registry();
        switch (entityType) {
            case ENCAPSULATED_ASSERTION:
                return registry.getEncapsulatedAssertionAdmin().findByPrimaryKey(asLongOid(id));

            // add new entity types here as needed
            // case WHATEVER:
            //     return registry.getWhateverAdmin().findByPrimaryKey(id);

            default:
                throw new UnsupportedEntityTypeException("Entity type currently not supported: " + entityType);
        }
    }

    private long asLongOid(String id) throws FindException {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new FindException("ID string not a valid OID: " + id, e);
        }
    }

    @NotNull
    @Override
    public Entity findByHeader(@NotNull EntityHeader header) throws FindException {
        return findByEntityTypeAndPrimaryId(header.getType(), header.getStrId());
    }

    /**
     * Overridden in tests.
     */
    @NotNull
    Registry registry() throws FindException {
        Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent())
            throw new FindException("Admin context not present");
        return registry;
    }
}
