package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.processors.DependencyFinder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private DependencyProcessorStore processorStore;

    /**
     * {@inheritDoc}
     */
    @Override
    public DependencySearchResults getDependencies(EntityHeader entityHeader) throws FindException {
        return getDependencies(entityHeader, DefaultSearchOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DependencySearchResults getDependencies(EntityHeader entityHeader, Map<String, String> searchOptions) throws FindException {
        List<DependencySearchResults> results = getDependencies(Arrays.asList(entityHeader), searchOptions);
        return results.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders) throws FindException {
        return getDependencies(entityHeaders, DefaultSearchOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders, Map<String, String> searchOptions) throws FindException {
        logger.log(Level.FINE, "Finding dependencies for {0}", entityHeaders);
        ArrayList<Entity> entities = new ArrayList<>(entityHeaders.size());
        for (EntityHeader entityHeader : entityHeaders) {
            entities.add(entityCrud.find(entityHeader));
        }

        DependencyFinder dependencyFinder = new DependencyFinder(searchOptions, processorStore);
        return dependencyFinder.process(entities);
    }
}
