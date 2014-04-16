package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.*;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class DependencyTransformer implements APITransformer<DependencyListMO, DependencySearchResults>{

    @Inject
    private DependencyAnalyzer dependencyAnalyzer;

    @Inject
    private APIUtilityLocator apiUtilityLocator;

    @NotNull
    @Override
    public String getResourceType() {
        return "DEPENDENCY";
    }

    @Override
    public DependencyListMO convertToMO(DependencySearchResults dependencySearchResults) {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public EntityContainer<DependencySearchResults> convertFromMO(DependencyListMO DependencyListMO) throws ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public EntityContainer<DependencySearchResults> convertFromMO(DependencyListMO DependencyListMO, boolean strict) throws ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public EntityHeader convertToHeader(DependencyListMO DependencyListMO) throws ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public Item<DependencyListMO> convertToItem(DependencyListMO DependencyListMO) {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public Item<DependencyListMO> convertToItem(EntityHeader header) {
        throw new UnsupportedOperationException("TODO?");
    }

    public DependencyListMO toDependencyListObject(DependencySearchResults dependencySearchResults) {
        DependencyListMO dependencyAnalysisMO = ManagedObjectFactory.createDependencyListMO();
        dependencyAnalysisMO.setOptions(dependencySearchResults.getSearchOptions());
        dependencyAnalysisMO.setSearchObjectItem( toDependencyManagedObject(dependencySearchResults.getDependent(), dependencySearchResults.getDependencies()));
        dependencyAnalysisMO.setDependencies(Functions.map(dependencyAnalyzer.buildFlatDependencyList(dependencySearchResults, false), new Functions.Unary<DependencyMO, DependentObject>() {
            @Override
            public DependencyMO call(DependentObject dependentObject) {
                return toManagedObject(dependentObject);
            }
        }));
        return dependencyAnalysisMO;
    }

    private DependencyMO toDependencyManagedObject(DependentObject depObject , List<Dependency> dependencies) {
        List<DependencyMO> dependencyMOs = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            DependencyMO dependencyMO = ManagedObjectFactory.createDependencyMO();
            Item reference = toReference(dependency.getDependent());
            dependencyMO.setName(reference.getName());
            dependencyMO.setId(reference.getId());
            dependencyMO.setType(reference.getType());
            dependencyMOs.add(dependencyMO);
        }

        DependencyMO dependency = ManagedObjectFactory.createDependencyMO();
        Item reference = toReference(depObject);
        dependency.setName(reference.getName());
        dependency.setId(reference.getId());
        dependency.setType(reference.getType());
        dependency.setDependencies(dependencyMOs.isEmpty() ? null : dependencyMOs);
        return dependency;
    }

    private List<DependencyMO> toManagedObject(List<DependentObject> dependencies) {
        ArrayList<DependencyMO> dependencyMOs = new ArrayList<>();
        for (DependentObject dependency : dependencies) {
            dependencyMOs.add(toManagedObject(dependency));
        }
        return dependencyMOs.isEmpty() ? null : dependencyMOs;
    }

    private DependencyMO toManagedObject(DependentObject dependency) {
        DependencyMO dependencyMO = ManagedObjectFactory.createDependencyMO();
        dependencyMO.setName(toReference(dependency).getName());
        dependencyMO.setId(toReference(dependency).getId());
        dependencyMO.setType(toReference(dependency).getType());
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
