package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * This is a base class that used a wsman factory to transform an MO
 * @param <M> The MO that is being transformed
 * @param <E> The Entity that that MO transforms into
 * @param <F> The wsman resource factory that can transform the MO
 */
public abstract class APIResourceWsmanBaseTransformer<M extends ManagedObject, E, F extends ResourceFactory<M,E>> implements APITransformer<M, E> {

    /**
     * The wiseman resource factory
     */
    protected F factory;

    /**
     * Sets the wiseman resource factory. Likely set using injection.
     *
     * @param factory The wiseman resource factory
     */
    protected abstract void setFactory(F factory);

    @Override
    @NotNull
    public EntityType getEntityType(){
        return factory.getType();
    }

    @Override
    public M convertToMO(E e) {
        return factory.asResource(e);
    }

    @Override
    public E convertFromMO(M m) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m, true);
    }

    @Override
    public E convertFromMO(M m, boolean strict) throws ResourceFactory.InvalidResourceException {
        return factory.fromResource(m, strict);
    }

    @Override
    public Item<M> convertToItem(EntityHeader header){
        return new ItemBuilder<M>(header.getName(), header.getStrId(), factory.getType().name())
                .build();
    }
}
