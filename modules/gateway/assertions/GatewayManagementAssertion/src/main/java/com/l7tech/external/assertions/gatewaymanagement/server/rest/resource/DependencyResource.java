package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyResultsMO;
import com.l7tech.gateway.api.DependentObjectMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a provider for dependencies. It finds the dependencies of entities that can have dependencies.
 */
@Provider
@Path("dependencies")
public class DependencyResource {

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    private EntityHeader entityHeader;

    public DependencyResource() {
    }

    public DependencyResource(EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    @GET
    public DependencyResultsMO get() throws FindException {
        return toManagedObject(dependencyAnalyzer.getDependencies(entityHeader));
    }

    private DependencyResultsMO toManagedObject(DependencySearchResults dependencySearchResults) {
        DependencyResultsMO dependencyResultsMO = ManagedObjectFactory.createDependencyResultsMO();
        dependencyResultsMO.setOptions(dependencySearchResults.getSearchOptions());
        dependencyResultsMO.setSearchObject(toManagedObject(dependencySearchResults.getDependent()));
        dependencyResultsMO.setDependencies(toManagedObject(dependencySearchResults.getDependencies()));
        return dependencyResultsMO;
    }

    private List<DependencyMO> toManagedObject(List<Dependency> dependencies) {
        ArrayList<DependencyMO> dependencyMOs = new ArrayList<>();
        for(Dependency dependency : dependencies){
            dependencyMOs.add(toManagedObject(dependency));
        }
        return dependencyMOs;
    }

    private DependencyMO toManagedObject(Dependency dependency) {
        DependencyMO dependencyMO = ManagedObjectFactory.createDependencyMO();
        dependencyMO.setDependentObject(toManagedObject(dependency.getDependent()));
        dependencyMO.setDependencies(toManagedObject(dependency.getDependencies()));
        return dependencyMO;
    }

    private DependentObjectMO toManagedObject(DependentObject dependent) {
        DependentObjectMO dependentObjectMO = ManagedObjectFactory.createDependentObjectMO();
        dependentObjectMO.setName(dependent.getName());
        dependentObjectMO.setObjectType(dependent.getDependencyType().name());
        if(dependent instanceof DependentAssertion){
            dependentObjectMO.setObjectType("Assertion");
        } else if(dependent instanceof DependentEntity) {
            dependentObjectMO.setId(((DependentEntity)dependent).getId());
        }
        return dependentObjectMO;
    }
}
