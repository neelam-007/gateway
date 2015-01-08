package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.search.DependencySearchResultsUtils;
import com.l7tech.server.search.objects.*;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class DependencyTransformer implements APITransformer<DependencyListMO, DependencySearchResults>{

    @Inject
    private ServiceManager serviceManager;
    @Inject
    private PolicyManager policyManager;

    @NotNull
    @Override
    public String getResourceType() {
        return "DEPENDENCY";
    }


    @NotNull
    @Override
    public DependencyListMO convertToMO(@NotNull DependencySearchResults dependencySearchResults, SecretsEncryptor secretsEncryptor) {
        return convertToMO(dependencySearchResults);
    }

    @NotNull
    public DependencyListMO convertToMO(@NotNull DependencySearchResults dependencySearchResults) {
        DependencyListMO dependencyAnalysisMO = ManagedObjectFactory.createDependencyListMO();
        dependencyAnalysisMO.setOptions(dependencySearchResults.getSearchOptions());
        dependencyAnalysisMO.setSearchObjectItem( toDependencyManagedObject(dependencySearchResults.getDependent(), dependencySearchResults.getDependencies()));
        List<Dependency> dependencyList = DependencySearchResultsUtils.flattenDependencySearchResults(dependencySearchResults, false);
        dependencyAnalysisMO.setDependencies(new ArrayList<DependencyMO>());
        dependencyAnalysisMO.setMissingDependencies(new ArrayList<DependencyMO>());
        for(Dependency dependency: dependencyList){
            if(dependency instanceof BrokenDependency){
                dependencyAnalysisMO.getMissingDependencies().add(toManagedObject(dependency));
            }else{
                dependencyAnalysisMO.getDependencies().add(toManagedObject(dependency));
            }
        }

        return dependencyAnalysisMO;
    }


    @NotNull
    public DependencyListMO convertToMO(@NotNull List<DependencySearchResults> dependencySearchResultsList) {
        DependencyListMO dependencyAnalysisMO = ManagedObjectFactory.createDependencyListMO();
        if(dependencySearchResultsList.isEmpty()){
            return dependencyAnalysisMO;
        }
//        dependencyAnalysisMO.setOptions(dependencySearchResults.getSearchOptions());
//        dependencyAnalysisMO.setSearchObjectItem(toDependencyManagedObject(dependencySearchResultsList.get(0).getDependent(), dependencySearchResultsList.get(0).getDependencies()));
        List<Dependency> dependencyList = new ArrayList<>();
        for (DependencySearchResults results : dependencySearchResultsList){
            dependencyList.addAll(DependencySearchResultsUtils.flattenDependencySearchResults(results, false));
        }
        dependencyAnalysisMO.setDependencies(new ArrayList<DependencyMO>());
        dependencyAnalysisMO.setMissingDependencies(new ArrayList<DependencyMO>());
        for(Dependency dependency: dependencyList){
            if(dependency instanceof BrokenDependency){
                dependencyAnalysisMO.getMissingDependencies().add(toManagedObject(dependency));
            }else{
                dependencyAnalysisMO.getDependencies().add(toManagedObject(dependency));
            }
        }

        return dependencyAnalysisMO;
    }

    @NotNull
    @Override
    public DependencySearchResults convertFromMO(@NotNull DependencyListMO dependencyListMO, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(dependencyListMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public DependencySearchResults convertFromMO(@NotNull DependencyListMO dependencyListMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("Converting DependencyListMO to an internal DependencySearchResults is not supported.");
    }

    @NotNull
    @Override
    public Item<DependencyListMO> convertToItem(@NotNull DependencyListMO dependencyListMO) {
        return new ItemBuilder<DependencyListMO>(dependencyListMO.getSearchObjectItem().getName() + " dependencies", getResourceType())
                .setContent(dependencyListMO)
                .build();
    }

    private DependencyMO toDependencyManagedObject(DependentObject depObject, List<Dependency> dependencies) {
        List<DependencyMO> dependencyMOs = new ArrayList<>();
        if(dependencies != null) {
            for (Dependency dependency : dependencies) {
                DependencyMO dependencyMO = ManagedObjectFactory.createDependencyMO();
                Item reference = toReference(dependency.getDependent());
                dependencyMO.setName(reference.getName());
                dependencyMO.setId(reference.getId());
                dependencyMO.setType(reference.getType());
                dependencyMOs.add(dependencyMO);
            }
        }

        DependencyMO dependency = ManagedObjectFactory.createDependencyMO();
        Item reference = toReference(depObject);
        dependency.setName(reference.getName());
        dependency.setId(reference.getId());
        dependency.setType(reference.getType());
        dependency.setDependencies(dependencyMOs.isEmpty() ? null : dependencyMOs);
        return dependency;
    }

    private List<DependencyMO> toManagedObject(List<Dependency> dependencies) {
        if(dependencies == null || dependencies.isEmpty()){
            return null;
        } else {
            ArrayList<DependencyMO> dependencyMOs = new ArrayList<>();
            for (Dependency dependency : dependencies) {
                dependencyMOs.add(toManagedObject(dependency));
            }
            return dependencyMOs.isEmpty() ? null : dependencyMOs;
        }
    }

    private DependencyMO toManagedObject(Dependency dependency) {
        DependencyMO dependencyMO = ManagedObjectFactory.createDependencyMO();
        Item dependencyItem = toReference(dependency.getDependent());
        dependencyMO.setName(dependencyItem.getName());
        dependencyMO.setId(dependencyItem.getId());
        dependencyMO.setType(dependencyItem.getType());
        dependencyMO.setDependencies(toManagedObject(dependency.getDependencies()));
        return dependencyMO;
    }

    private Item toReference(DependentObject dependent) {
        if (dependent instanceof DependentAssertion) {
            return new ItemBuilder<>(dependent.getName(), null, "Assertion").build();
        } else if (dependent instanceof BrokenDependentEntity) {
            EntityHeader header = ((BrokenDependentEntity) dependent).getEntityHeader();
            if( header.getName() == null && header.getStrId() == null ){
                // if no name and id, use GUID
                if(header instanceof GuidEntityHeader ) {
                    return new ItemBuilder<>(((GuidEntityHeader) header).getGuid(), ((GuidEntityHeader) header).getGuid(), header.getType().name())
                            .build();
                }
                throw new IllegalArgumentException("Unknown dependent: " + header.toStringVerbose());
            }
            if(header.getName() == null ) {
                return new ItemBuilder<>(header.getStrId(), header.getStrId(), header.getType().name())
                        .build();
            }
            if(header.getStrId() == null ) {
                return new ItemBuilder<>(header.getName(), header.getName(), header.getType().name())
                        .build();
            }
            return new ItemBuilder<>(header.getName(), header.getStrId(), header.getType().name())
                    .build();
        } else if (dependent instanceof DependentEntity) {
            return buildReferenceFromEntityHeader(((DependentEntity) dependent).getEntityHeader());
        } else {
            throw new IllegalArgumentException("Unknown dependency type: " + dependent.getClass());
        }
    }

    /**
     * This method builds an Item from an entity header.
     *
     * @param header The entity header to build the item from
     * @return The item created from the entity header.
     */
    private Item buildReferenceFromEntityHeader(EntityHeader header) {
        //need to check some special cases
        if (header instanceof ResourceEntryHeader) {
            //document resources should use the uri as their name
            return new ItemBuilder<ResourceDocumentMO>(((ResourceEntryHeader) header).getUri(), header.getStrId(), header.getType().name())
                    .build();
        } else if (header instanceof AliasHeader) {
            //aliases should be their name from their backing policy or service.
            return new ItemBuilder<PolicyAliasMO>(findAliasName(((AliasHeader) header).getAliasedEntityId(), header.getType()), header.getStrId(), header.getType().name())
                    .build();
        } else {
            //the default item
            return new ItemBuilder<>(header.getName(), header.getStrId(), header.getType().name())
                    .build();
        }
    }

    /**
     * Finds the alias name by looking for the policy or service with the given id. If this policy or service cannot be
     * found the id is returned.
     *
     * @param aliasID The id of the policy or service to search for
     * @param type    The type of alias. Either Policy or service.
     * @return The name of the alias
     */
    private String findAliasName(final Goid aliasID, final EntityType type) {
        try {
            final NamedEntity backingEntity;
            if (EntityType.POLICY_ALIAS.equals(type)) {
                backingEntity = policyManager.findByPrimaryKey(aliasID);
            } else if (EntityType.SERVICE_ALIAS.equals(type)) {
                backingEntity = serviceManager.findByPrimaryKey(aliasID);
            } else {
                backingEntity = null;
            }
            if (backingEntity != null) {
                return backingEntity.getName() + " alias";
            }
        } catch (Throwable t) {
            //we do not want to throw here and default to using the id if a policy or service cannot be found
        }
        return aliasID.toString();
    }
}
