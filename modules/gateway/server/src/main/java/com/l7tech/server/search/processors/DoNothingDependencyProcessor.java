package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is the Do Nothing dependency processor. It is does nothing for all the method implementations, either returning
 * null or the empty list. This is used by SsgActiveConnector dependency processors and SsgConnector dependency
 * processors.
 *
 * @author Victor Kazakov
 */
public class DoNothingDependencyProcessor<O> implements DependencyProcessor<O> {

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull O object, @NotNull DependencyFinder finder) throws FindException {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<O> find(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull O dependent) {
        return new DependentObject(null, com.l7tech.search.Dependency.DependencyType.ANY) {
        };
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        return Collections.emptyList();
    }

    @Override
    public void replaceDependencies(@NotNull O object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
    }
}
