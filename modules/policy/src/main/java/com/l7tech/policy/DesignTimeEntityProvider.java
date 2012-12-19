package com.l7tech.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.UsesEntitiesAtDesignTime;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by services that can satisfy the needs of a UsesEntitiesAtDesignTime implementer.
 */
public interface DesignTimeEntityProvider {

    /**
     * Provide the specified entity user with all entities it currently needs.
     *
     * @param entityUser an entity user that may need to exchange one or more entity headers for full entity instances.
     * @param errorHandler an error handler to be notified about missing/unavailable entities, or null.  If an error handler is provided, this method will not throw a FindException.
     * @throws FindException if errorHandler is null and at least one sought-after entity could not be provided.
     * @throws RuntimeException if an errorHandler is provided and it throws an unchecked exception.
     */
    void provideNeededEntities(@NotNull UsesEntitiesAtDesignTime entityUser, @Nullable Functions.BinaryVoid<EntityHeader,FindException> errorHandler) throws FindException;
}
