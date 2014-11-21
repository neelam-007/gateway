package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyCache;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * This is a provider for usages. It finds the usages of entities.
 */
@Since(RestManVersion.VERSION_1_0_1)
public class UsageResource {

    @SpringBean
    private DependencyCache dependencyCache;

    @SpringBean
    private DependencyTransformer transformer;

    @SpringBean
    private RbacAccessService rbacAccessService;

    @Context
    private UriInfo uriInfo;

    @NotNull
    private final EntityHeader entityHeader;

    public UsageResource(@NotNull final EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    /**
     * Returns the list of usages for this entity.
     *
     * @return The list of dependencies.
     * @throws com.l7tech.objectmodel.FindException
     *
     * @title Get Usages
     */
    @GET
    public Item getUsages() throws FindException, CannotRetrieveDependenciesException {
        rbacAccessService.validateFullAdministrator();
        DependencySearchResults dependencySearchResults = dependencyCache.findUsages(entityHeader);
        DependencyListMO dependencyListMO = transformer.convertToMO(dependencySearchResults);
        //hide the dependency search options, it is not usable in version 1.0 of the api
        dependencyListMO.setOptions(null);
        return new ItemBuilder<>(transformer.convertToItem(dependencyListMO))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }
}
