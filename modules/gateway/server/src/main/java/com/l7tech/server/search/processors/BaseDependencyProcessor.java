package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * This is a base dependency processor. It implements all methods but throws NotImplementedException if an are called.
 * This is here to make implementation of dependency processors more compact
 *
 * @author Victor Kazakov
 */
public abstract class BaseDependencyProcessor<O> implements DependencyProcessor<O> {
    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull O object, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        throw new NotImplementedException("The findDependencies method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @NotNull
    @Override
    public List<O> find(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        throw new NotImplementedException("The find method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull O dependent) {
        throw new NotImplementedException("The createDependentObject method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        throw new NotImplementedException("The createDependentObjects method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @Override
    public void replaceDependencies(@NotNull O entity, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder) throws CannotReplaceDependenciesException {
        throw new NotImplementedException("The replaceDependencies method is not yet implemented for this dependency processor: " + this.getClass());
    }
}
