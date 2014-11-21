package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.search.Dependency;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The dependency cache maintians a graph of the gateway dependencies. It is used to increase the speed of getting
 * dependencies and allows for reverse dependency searches (entity usages).
 *
 * @See https://wiki.l7tech.com/mediawiki/index.php/Reverse_Dependency_Analysis
 */
public class DependencyCacheStub implements DependencyCache, PropertyChangeListener {
    @NotNull
    @Override
    public DependencySearchResults findUsages(@NotNull EntityHeader dependentEntityHeader) {
        return new DependencySearchResults(new DependentObject("Stub", Dependency.DependencyType.ANY) {}, null, Collections.<String, Object>emptyMap());
    }

    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull EntityHeader entity) throws FindException, CannotRetrieveDependenciesException {
        return new DependencySearchResults(new DependentObject("Stub", Dependency.DependencyType.ANY) {}, null, Collections.<String, Object>emptyMap());
    }

    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull EntityHeader entity, @NotNull Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        return new DependencySearchResults(new DependentObject("Stub", Dependency.DependencyType.ANY) {}, null, Collections.<String, Object>emptyMap());
    }

    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull List<EntityHeader> entityHeaders) throws FindException, CannotRetrieveDependenciesException {
        return Arrays.asList(new DependencySearchResults(new DependentObject("Stub", Dependency.DependencyType.ANY) {}, null, Collections.<String, Object>emptyMap()));
    }

    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull List<EntityHeader> entityHeaders, @NotNull Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        return Arrays.asList(new DependencySearchResults(new DependentObject("Stub", Dependency.DependencyType.ANY) {}, null, Collections.<String, Object>emptyMap()));
    }

    @Override
    public <E extends Entity> void replaceDependencies(@NotNull E entity, @NotNull Map<EntityHeader, EntityHeader> replacementMap, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

    }
}
