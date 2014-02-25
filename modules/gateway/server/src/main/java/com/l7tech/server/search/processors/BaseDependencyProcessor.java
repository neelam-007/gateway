package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public List<Dependency> findDependencies(O object, DependencyFinder finder) throws FindException {
        throw new NotImplementedException("The findDependencies method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @Override
    public <E extends Entity> List<E> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        throw new NotImplementedException("The find method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @Override
    public DependentObject createDependentObject(O dependent) {
        throw new NotImplementedException("The createDependentObject method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @Nullable
    @Override
    public List<DependentObject> createDependentObject(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        throw new NotImplementedException("The createDependentObject method is not yet implemented for this dependency processor: " + this.getClass());
    }

    @Override
    public void replaceDependencies(@NotNull O entity, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        throw new NotImplementedException("The replaceDependencies method is not yet implemented for this dependency processor: " + this.getClass());
    }
}
