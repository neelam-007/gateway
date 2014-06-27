package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by assertions that use require entities at design time, for metadata and/or validation
 * purposes.
 * <p/>
 * Assertions implementing this interface may additionally need to override the {@link Assertion#updateTemporaryData(Assertion)} method
 * to preserve cached transient entity references during policy validation.
 */
public interface UsesEntitiesAtDesignTime extends UsesEntities {
    /**
     * Get descriptions of entities used at design time.
     *
     * @return an array of entity headers of entities this assertion needs at design time in order to provide
     *         full validation/metadata capabilities.  May be empty but should not be null.
     */
    EntityHeader[] getEntitiesUsedAtDesignTime();

    /**
     * Check whether the specified entity, which is needed at design time, is already loaded into this
     * assertion bean.
     *
     * @param header an entity header that was recently returned from {@link #getEntitiesUsedAtDesignTime()}.  Required.
     * @return true if this entity needs to be provided to this assertion bean using {@link #provideEntity}.  false if it is already available.
     */
    boolean needsProvideEntity(@NotNull EntityHeader header);

    /**
     * Called by users of the assertion in order to provide access to a full instance of an entity described by calling
     * {@link #getEntitiesUsedAtDesignTime()}.  The assertion bean can use the information from this entity at design
     * time in order to provide services like metadata, UsesVariables, SetsVariables, and validation.
     *
     * @param header an entity header that was recently returned from {@link #getEntitiesUsedAtDesignTime()}.  Required.
     * @param entity a complete copy of the entity corresponding to this entity header.  Required.
     */
    void provideEntity(@NotNull EntityHeader header, @NotNull Entity entity);

    /**
     * Get an error handler to use if there is a problem obtaining an entity declared as needed as design time.
     * <p/>
     * On the Gateway, the default error handler will just log a WARNING.
     *
     * @return an error handler to use when providing entities at design time, or null to use the default error handling behavior.
     */
    @Nullable
    Functions.BinaryVoid<EntityHeader,FindException> getProvideEntitiesErrorHandler();
}
