package com.l7tech.server.search;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.search.objects.DependencySearchResults;
import org.jetbrains.annotations.NotNull;

/**
 * The Dependency Analyzer is used to find the dependencies of an Entity.
 *
 * @author Victor Kazakov
 */
public interface DependencyCache extends DependencyAnalyzer {

    @NotNull
    public DependencySearchResults findUsages(@NotNull EntityHeader dependentEntityHeader);
}
