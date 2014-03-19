package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.*;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a provider for dependencies. It finds the dependencies of entities that can have dependencies.
 */
@Provider
@Path(DependencyResource.Version_URI + "dependencies")
public class DependencyResource {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private URLAccessibleLocator URLAccessibleLocator;

    @SpringBean
    private APIUtilityLocator apiUtilityLocator;

    @Context
    private UriInfo uriInfo;

    private EntityHeader entityHeader;

    public static enum ReturnType {
        List, Tree
    }

    public DependencyResource() {
        //TODO: need a way to specify all gateway dependencies.
    }

    public DependencyResource(EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    /**
     * Returns the dependency analysis for the resource
     *
     * @param returnType The way to return the dependency information. Either as a tree or a list.
     * @return The dependencies of this entity.
     * @throws FindException
     */
    @GET
    public Item get(@QueryParam("returnType") @DefaultValue("Tree") ReturnType returnType) throws FindException {
        if (entityHeader == null) {
            throw new IllegalStateException("Cannot find dependencies, no entity set.");
        }
        switch (returnType) {
            case Tree:
                return new ItemBuilder<DependencyTreeMO>(entityHeader.toString() + " dependencies", "Dependency")
                        .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                        .setContent(toDependencyTreeObject(dependencyAnalyzer.getDependencies(entityHeader, CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false).map())))
                        .build();
            case List:
                return new ItemBuilder<DependencyListMO>(entityHeader.toString() + " dependencies", "Dependency")
                        .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                        .setContent(toDependencyListObject(dependencyAnalyzer.getDependencies(entityHeader, CollectionUtils.MapBuilder.<String, Object>builder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false).map())))
                        .build();
            default:
                throw new IllegalArgumentException("Unknown return type: " + returnType);
        }
    }

    //TODO: move entity transformation work to a transformer class
    public DependencyTreeMO toDependencyTreeObject(DependencySearchResults dependencySearchResults) {
        DependencyTreeMO dependencyAnalysisMO = ManagedObjectFactory.createDependencyTreeMO();
        dependencyAnalysisMO.setOptions(dependencySearchResults.getSearchOptions());
        dependencyAnalysisMO.setSearchObjectItem(toReference(dependencySearchResults.getDependent()));
        dependencyAnalysisMO.setDependencies(toManagedObject(dependencySearchResults.getDependencies()));
        return dependencyAnalysisMO;
    }

    public DependencyListMO toDependencyListObject(DependencySearchResults dependencySearchResults) {
        DependencyListMO dependencyAnalysisMO = ManagedObjectFactory.createDependencyListMO();
        dependencyAnalysisMO.setOptions(dependencySearchResults.getSearchOptions());
        dependencyAnalysisMO.setSearchObjectItem(toReference(dependencySearchResults.getDependent()));
        dependencyAnalysisMO.setDependencies(Functions.map(dependencyAnalyzer.buildFlatDependencyList(dependencySearchResults), new Functions.Unary<Item, DependentObject>() {
            @Override
            public Item call(DependentObject dependentObject) {
                return toReference(dependentObject);
            }
        }));
        return dependencyAnalysisMO;
    }

    private List<DependencyMO> toManagedObject(List<Dependency> dependencies) {
        ArrayList<DependencyMO> dependencyMOs = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            dependencyMOs.add(toManagedObject(dependency));
        }
        return dependencyMOs.isEmpty() ? null : dependencyMOs;
    }

    private DependencyMO toManagedObject(Dependency dependency) {
        DependencyMO dependencyMO = ManagedObjectFactory.createDependencyMO();
        dependencyMO.setDependentObject(toReference(dependency.getDependent()));
        dependencyMO.setDependencies(toManagedObject(dependency.getDependencies()));
        return dependencyMO;
    }

    private Item toReference(DependentObject dependent) {
        if (dependent instanceof DependentAssertion) {
            return new ItemBuilder<>(dependent.getName(), null, "Assertion").build();
        } else if (dependent instanceof DependentEntity) {
            return buildReferenceFromEntityHeader(((DependentEntity) dependent).getEntityHeader());
        } else {
            throw new IllegalArgumentException("Unknown dependency type: " + dependent.getClass());
        }
    }

    private Item buildReferenceFromEntityHeader(EntityHeader entityHeader) {
        APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(entityHeader.getType().toString());
        if (transformer != null) {
            return transformer.convertToItem(entityHeader);
        }
        throw new IllegalArgumentException("Could not find resource for entity type: " + entityHeader.getType());
    }
}
