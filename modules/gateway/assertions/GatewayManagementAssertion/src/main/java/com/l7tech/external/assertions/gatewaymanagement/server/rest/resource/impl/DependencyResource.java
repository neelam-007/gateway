package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.RestResourceLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.DependencyAnalysisMO;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is a provider for dependencies. It finds the dependencies of entities that can have dependencies.
 */
@Provider
@Path("dependencies")
public class DependencyResource {

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private RestResourceLocator restResourceLocator;

    private EntityHeader entityHeader;

    public DependencyResource() {
        //TODO: need a way to specify all gateway dependencies.
    }

    public DependencyResource(EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    @GET
    public DependencyAnalysisMO get() throws FindException {
        if(entityHeader == null) {
            throw new IllegalStateException("Cannot find dependencies, no entity set.");
        }
        final HashMap<String, String> searchOptions = new HashMap<>(DependencyAnalyzer.DefaultSearchOptions);
        searchOptions.put("returnAssertionsAsDependencies", "false");
        return toManagedObject(dependencyAnalyzer.getDependencies(entityHeader, searchOptions));
    }

    //TODO: move entity transformation work to a transformer class
    private DependencyAnalysisMO toManagedObject(DependencySearchResults dependencySearchResults) {
        DependencyAnalysisMO dependencyAnalysisMO = ManagedObjectFactory.createDependencyResultsMO();
        dependencyAnalysisMO.setOptions(dependencySearchResults.getSearchOptions());
        dependencyAnalysisMO.setSearchObjectReference(toReference(dependencySearchResults.getDependent()));
        dependencyAnalysisMO.setDependencies(toManagedObject(dependencySearchResults.getDependencies()));
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

    private Reference toReference(DependentObject dependent) {
        final Reference reference;
        if (dependent instanceof DependentAssertion) {
            reference = ManagedObjectFactory.createReference();
            reference.setContent(dependent.getName());
            reference.setEntityType(dependent.getDependencyType().name());
            reference.setEntityType("Assertion");
        } else if (dependent instanceof DependentEntity) {
            reference = buildReferenceFromEntityHeader(((DependentEntity) dependent).getEntityHeader());
        } else {
            throw new IllegalArgumentException("Unknown dependency type: " + dependent.getClass());
        }
        return reference;
    }

    private Reference buildReferenceFromEntityHeader(EntityHeader entityHeader) {
        RestEntityResource restEntityResource = restResourceLocator.findByEntityType(entityHeader.getType());
        if(restEntityResource != null) {
            return restEntityResource.toReference(entityHeader);
        }
        throw new IllegalArgumentException("Could not find resource for entity type: " + entityHeader.getType());
    }
}
