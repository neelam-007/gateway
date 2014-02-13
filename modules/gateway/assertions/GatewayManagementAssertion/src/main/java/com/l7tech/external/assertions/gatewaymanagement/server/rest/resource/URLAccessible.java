package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.gateway.api.Link;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface URLAccessible<M> {

    /**
     * Returns the resource type for this url accessible object
     *
     * @return The resource type for this url accessible object
     */
    @NotNull
    public String getResourceType();

    /**
     * Returns the full url string for the given entity.
     *
     * @param m The object to return the full url string for
     * @return The full url string that can be used to access the object
     */
    @NotNull
    public String getUrl(@NotNull M m);

    /**
     * Return the objects link
     *
     * @param m The object to return the link for.
     * @return The 'self' link for the given object
     */
    @NotNull
    public Link getLink(@NotNull M m);

    /**
     * Gets related links for the given object. These are usually the listing link and the template link
     *
     * @param m The object to return the related links for
     * @return The related links for the given object
     */
    @NotNull
    public List<Link> getRelatedLinks(@Nullable M m);

}
