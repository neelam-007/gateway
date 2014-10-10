package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.List;

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
     * Returns the list of dependencies for this entity.
     *
     * @param resourceTypes   the resource types to search dependency for
     * @param level   how deep to search for the dependencies. 0 for none, 1 for immediate dependencies
     * @return The list of dependencies.
     * @throws FindException
     *
     * @title Get Dependencies
     */
    @GET
    public Item getDependencies(@QueryParam("searchEntityType") @Since(RestManVersion.VERSION_1_0_1) List<String> resourceTypes,
                                @QueryParam("level") @DefaultValue("-1") @Since(RestManVersion.VERSION_1_0_1) Integer level) throws FindException, CannotRetrieveDependenciesException {
        rbacAccessService.validateFullAdministrator();
        final DependencySearchResults dependencySearchResults = dependencyAnalyzer.getDependencies(entityHeader,
                CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false)
                        .put(DependencyAnalyzer.SearchEntityTypeOptionKey, Functions.map(resourceTypes,new Functions.Unary<EntityType, String>() {
                            @Override
                            public EntityType call(String s) {
                                return EntityType.valueOf(s);
                            }
                        }))
                        .put(DependencyAnalyzer.SearchDepthOptionKey, level).map());
        DependencyListMO dependencyListMO = transformer.convertToMO(dependencySearchResults);
        //hide the dependency search options, it is not usable in version 1.0 of the api
        dependencyListMO.setOptions(null);
        return new ItemBuilder<>(transformer.convertToItem(dependencyListMO))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }
}
