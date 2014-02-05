package com.l7tech.server.search;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentObject;
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

    public List<DependentObject> buildFlatDependencyList(DependencySearchResults dependencySearchResult) {
        return buildFlatDependencyList(Arrays.asList(dependencySearchResult));
    }

    public List<DependentObject> buildFlatDependencyList(List<DependencySearchResults> dependencySearchResults) {
        List<DependentObject> dependentObjects = new ArrayList<>();
        for (DependencySearchResults dependencySearchResult : dependencySearchResults) {
            buildDependentObjectsList(dependentObjects, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies());
        }
        return dependentObjects;
    }

    private void buildDependentObjectsList(List<DependentObject> dependentObjects, final DependentObject dependent, List<Dependency> dependencies) {
        if (dependent != null && com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            buildDependentObjectsList(dependentObjects, dependent);
        }
        for (Dependency dependency : dependencies) {
            buildDependentObjectsList(dependentObjects, dependency.getDependent(), dependency.getDependencies());
        }
        if (dependent != null && !com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            buildDependentObjectsList(dependentObjects, dependent);
        }
    }

    private void buildDependentObjectsList(List<DependentObject> dependentObjects, final DependentObject dependent) {
        if (!dependentObjects.contains(dependent)) {
            dependentObjects.add(dependent);
        }
    }


    private Entity loadEntity(EntityHeader entityHeader) throws FindException {
        if (EntityType.ID_PROVIDER_CONFIG.equals(entityHeader.getType())) {
            return identityProviderConfigManager.findByHeader(entityHeader);
        } else
            return entityCrud.find(entityHeader);
    }
}
