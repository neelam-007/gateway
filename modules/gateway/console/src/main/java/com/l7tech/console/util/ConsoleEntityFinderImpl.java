package com.l7tech.console.util;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * A utility that provides read-only lookup services for entities, backed by the actual admin APIs for the
 * corresponding entities.  This can be used within the SSM.  RBAC enforcement is done on the actual entity APIs.
 */
public class ConsoleEntityFinderImpl implements HeaderBasedEntityFinder {

    @NotNull
    Entity findByEntityTypeAndGuid(EntityType type, String guid) throws FindException {
        Registry registry = registry();
        switch (type) {
            case ENCAPSULATED_ASSERTION:
                final EncapsulatedAssertionConfig found = registry.getEncapsulatedAssertionAdmin().findByGuid(guid);
                attachPolicies(found);
                return found;

            // add new entity types here as needed
            // case WHATEVER:
            //     return registry.getWhateverAdmin().findByGuid(id);

            default:
                throw new UnsupportedEntityTypeException("Entity type currently not supported: " + type);
        }
    }

    @NotNull
    @Override
    public Entity find(@NotNull EntityHeader header) throws FindException {
        if (header instanceof GuidEntityHeader) {
            GuidEntityHeader guidHeader = (GuidEntityHeader) header;
            String guid = guidHeader.getGuid();
            if (guid != null)
                return findByEntityTypeAndGuid(header.getType(), guid);
        }
        throw new UnsupportedEntityTypeException("Entity type currently not supported: " + header.getType());
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

    /**
     * Overridden in tests.
     */
    void attachPolicies(@NotNull final EncapsulatedAssertionConfig found) throws FindException {
        EncapsulatedAssertionConsoleUtil.attachPolicies(Collections.singletonList(found));
    }
}
