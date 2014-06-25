package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * This is a provider for dependencies. It finds the dependencies of entities that can have dependencies.
 */
public class DependencyResource {

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private DependencyTransformer transformer;

    @SpringBean
    private RbacAccessService rbacAccessService;

    @Context
    private UriInfo uriInfo;

    @NotNull
    private final EntityHeader entityHeader;

    public DependencyResource(@NotNull final EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    /**
     * Returns the dependency analysis for the resource
     *
     * @return The dependencies of this entity.
     * @throws FindException
     *
     * @title Get Dependencies
     */
    @GET
    public Item get() throws FindException, CannotRetrieveDependenciesException {
        rbacAccessService.validateFullAdministrator();
        final DependencySearchResults dependencySearchResults = dependencyAnalyzer.getDependencies(entityHeader, CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false).map());
        DependencyListMO dependencyListMO = transformer.convertToMO(dependencySearchResults);
        return new ItemBuilder<>(transformer.convertToItem(dependencyListMO))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }
}
