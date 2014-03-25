package com.l7tech.server.search;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.*;
import com.l7tech.server.search.processors.DependencyFinder;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This service is used to find dependencies of different entities.
 *
 * @author Victor Kazakov
 */
public class DependencyAnalyzerImpl implements DependencyAnalyzer {
    private static final Logger logger = Logger.getLogger(DependencyAnalyzerImpl.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private IdentityProviderConfigManager identityProviderConfigManager;

    @Inject
    private DependencyProcessorStore processorStore;

    /**
     * {@inheritDoc}
     */
    @Override
    public DependencySearchResults getDependencies(EntityHeader entityHeader) throws FindException {
        return getDependencies(entityHeader, Collections.<String,Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DependencySearchResults getDependencies(EntityHeader entityHeader, Map<String, Object> searchOptions) throws FindException {
        List<DependencySearchResults> results = getDependencies(Arrays.asList(entityHeader), searchOptions);
        return results.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders) throws FindException {
        return getDependencies(entityHeaders, Collections.<String,Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders, Map<String, Object> searchOptions) throws FindException {
        logger.log(Level.FINE, "Finding dependencies for {0}", entityHeaders);
        ArrayList<Entity> entities = new ArrayList<>(entityHeaders.size());
        for (EntityHeader entityHeader : entityHeaders) {
            entities.add(loadEntity(entityHeader));
        }

        DependencyFinder dependencyFinder = new DependencyFinder(searchOptions, processorStore);
        return dependencyFinder.process(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Entity> void replaceDependencies(E entity, Map<EntityHeader, EntityHeader> replacementMap) throws CannotReplaceDependenciesException, CannotRetrieveDependenciesException {
        if(replacementMap.isEmpty())
            //nothing to replace
            return;
        DependencyFinder dependencyFinder = new DependencyFinder(Collections.<String,Object>emptyMap(), processorStore);
        dependencyFinder.replaceDependencies(entity, replacementMap);
    }

    public List<DependentObject> buildFlatDependencyList(DependencySearchResults dependencySearchResult) {
        return buildFlatDependencyList(Arrays.asList(dependencySearchResult));
    }

    public List<DependentObject> buildFlatDependencyList(List<DependencySearchResults> dependencySearchResults) {
        List<DependentObject> dependentObjects = new ArrayList<>();
        for (DependencySearchResults dependencySearchResult : dependencySearchResults) {
            buildDependentObjectsList(dependentObjects, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies(),new ArrayList<DependentObject>());
        }
        return dependentObjects;
    }

    private void buildDependentObjectsList(List<DependentObject> dependentObjects, final DependentObject dependent, List<Dependency> dependencies, List<DependentObject> processed ) {
        // check circular dependency
        if(processed.contains(dependent)){
            return;
        }
        processed.add(dependent);

        if (dependent != null && com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            //if it is a folder we still need to put security zone dependencies first.
            for (Dependency dependency : dependencies) {
                if(com.l7tech.search.Dependency.DependencyType.SECURITY_ZONE.equals(dependency.getDependent().getDependencyType())) {
                    buildDependentObjectsList(dependentObjects, dependency.getDependent(), dependency.getDependencies(),processed);
                }
            }
            buildDependentObjectsList(dependentObjects, dependent, dependencies);
        }
        for (Dependency dependency : dependencies) {
            buildDependentObjectsList(dependentObjects, dependency.getDependent(), dependency.getDependencies(),processed);
        }
        if (dependent != null && !com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            buildDependentObjectsList(dependentObjects, dependent, dependencies);
        }
    }

    private void buildDependentObjectsList(List<DependentObject> dependentObjects, final DependentObject dependent,final List<Dependency> dependencies) {
        if (!dependentObjects.contains(dependent)) {
            dependentObjects.add(dependent);
        }
        final DependentObject current = dependentObjects.get(dependentObjects.indexOf(dependent));
        for(Dependency dep: dependencies){
            if (dep.getDependent() instanceof DependentAssertion) {
                DependentAssertion assDep = new DependentAssertion((DependentAssertion)dep.getDependent());
                assDep.setDependencies(Collections.EMPTY_LIST);
                current.addDependency(assDep);
            } else if (dep.getDependent() instanceof DependentEntity) {
                DependentEntity entityDep = new DependentEntity((DependentEntity)dep.getDependent());
                entityDep.setDependencies(Collections.EMPTY_LIST);
                current.addDependency(entityDep);
            }
        }
    }


    private Entity loadEntity(EntityHeader entityHeader) throws FindException {
        if (EntityType.ID_PROVIDER_CONFIG.equals(entityHeader.getType())) {
            return identityProviderConfigManager.findByHeader(entityHeader);
        } else
            return entityCrud.find(entityHeader);
    }
}
