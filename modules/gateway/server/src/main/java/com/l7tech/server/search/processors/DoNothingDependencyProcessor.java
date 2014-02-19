package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public List<Dependency> findDependencies(O object, DependencyFinder finder) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public <E extends Entity> List<E> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        return null;
    }

    @Override
    public DependentObject createDependentObject(O dependent) {
        return null;
    }

    @Nullable
    @Override
    public List<DependentObject> createDependentObject(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        return null;
    }

    @Override
    public void replaceDependencies(@NotNull O object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws FindException {
    }
}
