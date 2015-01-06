package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers;

import com.l7tech.objectmodel.Entity;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.util.MasterPasswordManager;
import org.jetbrains.annotations.NotNull;

/**
 * This is an entity transformer that will transform a managed object to an entity container containing the specific
 * entity.
 *
 * @param <M>
 * @param <E>
 */
public interface EntityAPITransformer<M, E extends Entity> extends APITransformer<M, EntityContainer<E>> {

    /**
     * Transforms the entity to a managed object. Not this may miss some info that may be present in an entity container
     * containing this entity.
     *
     * @param e The entity to transform
     * @param passwordManager To encrypt passwords. Null to not include password
     * @return The returned managed object
     */
    @NotNull
    public M convertToMO(@NotNull E e,  MasterPasswordManager passwordManager);
}
