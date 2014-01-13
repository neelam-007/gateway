package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.gateway.api.Item;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * The template resource interface. All resources that allow retrieving a template should implement this in order to
 * support consistent rest calls.
 *
 * @author Victor Kazakov
 */
public interface TemplatingResource<R> {
    /**
     * This will return a template, example entity that can be used as a base to creating a new entity.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Item<R> getResourceTemplate();
}
