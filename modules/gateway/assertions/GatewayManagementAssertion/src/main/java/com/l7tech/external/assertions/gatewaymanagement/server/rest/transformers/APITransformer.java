package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.Item;
import org.jetbrains.annotations.NotNull;


/**
 * This transformer is used to transform between a gateway object and a Managed object and from a managed object to an
 * Item.
 *
 * @param <M> The managed api object
 * @param <E> The gateway object (usually an entity)
 */
public interface APITransformer<M, E> {

    /**
     * Returns the ResourceType that this transformer is for.
     *
     * @return The ResourceType this transformer transforms
     */
    @NotNull
    public String getResourceType();

    /**
     * Converts a gateway object to it equivalent api object
     *
     * @param e The gateway object
     * @return The api object representing the gateway object
     */
    @NotNull
    public M convertToMO(@NotNull E e);

    /**
     * Converts the api object to its equivalent gateway object. This is the same as calling {@link
     * #convertFromMO(Object, boolean)} with strict true.
     * <p/>
     * If the api object contains a reference to an entity the referenced entity must exist in the gateway otherwise an
     * exception is thrown
     *
     * @param m The api object to convert
     * @return Returns the gateway object represented by the given api object.
     */
    @NotNull
    public E convertFromMO(@NotNull M m) throws ResourceFactory.InvalidResourceException;

    /**
     * Converts the api object to its equivalent gateway object.
     * <p/>
     * If strict is true and the api object contains a reference to an entity the referenced entity must exist in the
     * gateway otherwise an exception is thrown. If strict is false a dummy reference object will be created with the
     * same reference id set.
     *
     * @param m      The api object to convert
     * @param strict If true this will throw an exception if a referenced object cannot be found. Otherwise it creates a
     *               dummy reference object
     * @return Returns the gateway object represented by the given api object.
     */
    @NotNull
    public E convertFromMO(@NotNull M m, boolean strict) throws ResourceFactory.InvalidResourceException;

    /**
     * Converts the api object to an item properly populating all the item properties except links.
     *
     * @param m The api object to wrap in an {@link com.l7tech.gateway.api.Item}
     * @return The {@link com.l7tech.gateway.api.Item} wrapping the managed object.
     */
    @NotNull
    public Item<M> convertToItem(@NotNull M m);
}
