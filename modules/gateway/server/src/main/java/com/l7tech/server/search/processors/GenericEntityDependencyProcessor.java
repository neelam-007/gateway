package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.entity.GenericEntityUtils;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * This is the processor for demo generic entities. For findDependencies() and replaceDependencies() it will delegate a
 * registered genericEntityDependencyProcessor if one is registered. Otherwise it will use the default dependency
 * processor.
 * <p/>
 * Custom Generic entity dependency processors can be registered on the {@link DependencyProcessorRegistry} with name
 * "genericEntityDependencyProcessorRegistry"
 */
public class GenericEntityDependencyProcessor extends DefaultDependencyProcessor<GenericEntity> implements DependencyProcessor<GenericEntity> {

    /**
     * This is used to locate generic entity specific dependency processors.
     */
    @Inject
    @Named("genericEntityDependencyProcessorRegistry")
    private DependencyProcessorRegistry<GenericEntity> genericEntityDependencyProcessorRegistry;

    @Inject
    private GenericEntityManager genericEntityManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final GenericEntity object, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //need to up-cast the generic entity to the concrete version.
        final GenericEntity concreteGenericEntity = getConcreteGenericEntity(object);
        //find the default dependencies
        final List<com.l7tech.server.search.objects.Dependency> dependencies = super.findDependencies(concreteGenericEntity, finder);
        //delegate to the custom dependency processor for the generic entity type.
        final DependencyProcessor<GenericEntity> genericEntityProcessor = genericEntityDependencyProcessorRegistry.get(concreteGenericEntity.getClass().getName());
        if (genericEntityProcessor != null) {
            dependencies.addAll(CollectionUtils.subtract(genericEntityProcessor.findDependencies(concreteGenericEntity, finder), dependencies));
        }
        return dependencies;
    }

    @NotNull
    @Override
    public List<GenericEntity> find(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        final List<GenericEntity> entitiesList = super.find(searchValue, dependencyType, searchValueType);
        //need to up cast the generic entities to the concrete versions.
        return Functions.map(entitiesList, new Functions.UnaryThrows<GenericEntity, GenericEntity, FindException>() {
            @Override
            public GenericEntity call(final GenericEntity genericEntity) throws FindException {
                return getConcreteGenericEntity(genericEntity);
            }
        });
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull final GenericEntity dependent) {
        return super.createDependentObject(dependent);
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        return super.createDependentObjects(searchValue, dependencyType, searchValueType);
    }

    @Override
    public void replaceDependencies(@NotNull final GenericEntity object, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        final GenericEntity concreteGenericEntity;
        try {
            //need to up-cast the generic entity to the concrete version.
            concreteGenericEntity = getConcreteGenericEntity(object);
        } catch (FindException e) {
            throw new CannotReplaceDependenciesException(object.getClass(), "Unable to load concrete entity class: " + ExceptionUtils.getMessage(e), e);
        }

        super.replaceDependencies(concreteGenericEntity, replacementMap, finder, replaceAssertionsDependencies);
        final DependencyProcessor<GenericEntity> genericEntityProcessor = genericEntityDependencyProcessorRegistry.get(concreteGenericEntity.getClass().getName());
        if (genericEntityProcessor != null) {
            //noinspection unchecked
            genericEntityProcessor.replaceDependencies(object, replacementMap, finder, replaceAssertionsDependencies);
        }
        //need to regenerate the xml and set it on the given object so that it can be properly saved.
        GenericEntityUtils.regenerateValueXml(concreteGenericEntity);
        object.setValueXml(concreteGenericEntity.getValueXml());
    }

    /**
     * This will up cast a generic entity to its concrete object.
     *
     * @param genericEntity The generic entity to up-cast
     * @return The concrete version of the generic entity.
     * @throws FindException This is thrown if there was a problem converting to a concrete generic entity
     */
    @NotNull
    private GenericEntity getConcreteGenericEntity(@NotNull final GenericEntity genericEntity) throws FindException {
        //This will convert the general generic entity to the concrete version.
        return genericEntityManager.asConcreteEntity(genericEntity);
    }
}
