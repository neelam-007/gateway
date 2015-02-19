package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.l7tech.util.Option.optional;

/**
 * APIResourceFactory implementation that uses an EntityManager to access Entity resources.
 */
@Component
public abstract class EntityManagerAPIResourceFactory<R extends ManagedObject, E extends PersistentEntityImp, EH extends EntityHeader> implements APIResourceFactory<R> {

    @Inject
    private RbacAccessService rbacAccessService;

    @Inject
    private PlatformTransactionManager transactionManager;

    protected abstract EntityManager<E, EH> getEntityManager();

    /**
     * Convert from managed resource object to entity
     *
     * @param resource the managed resource object
     * @return the entity
     * @throws ResourceFactory.InvalidResourceException
     */
    protected abstract E convertFromMO(R resource) throws ResourceFactory.InvalidResourceException;

    /**
     * Convert from entity to managed resource object
     *
     * @param entity the entity to convert
     * @return the managed object
     */
    protected abstract R convertToMO(E entity);

    /**
     * Entity update callback before the primary entity is persisted.
     * <p/>
     * <p>This implementation does nothing.</p>
     *
     * @param entity The entity to update
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void beforeUpdateEntity(E entity) throws ObjectModelException {
    }

    /**
     * Entity update callback after the primary entity is persisted.
     * <p/>
     * <p>This implementation does nothing.</p>
     *
     * @param newEntity The new updated entity.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void afterUpdateEntity(E newEntity) throws ObjectModelException {
    }

    /**
     * Entity create callback after the primary entity is persisted.
     * <p/>
     * <p>This implementation does nothing.</p>
     *
     * @param entity The created entity.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void afterCreateEntity(E entity) throws ObjectModelException {
    }

    /**
     * Entity create callback before the primary entity is persisted.
     * <p/>
     * <p>This implementation does nothing.</p>
     *
     * @param entity The entity to create
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void beforeCreateEntity(E entity) throws ObjectModelException {
    }


    /**
     * Entity delete callback after the primary entity is deleted.
     * <p/>
     * <p>This implementation does nothing.</p>
     *
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void afterDeleteEntity() throws ObjectModelException {
    }


    /**
     * Entity delete callback before the primary entity is deleted.
     * <p/>
     * <p>This implementation does nothing.</p>
     *
     * @param entityToDelete The entity to delete
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void beforeDeleteEntity(E entityToDelete) throws ObjectModelException {
    }

    @NotNull
    @Override
    public String getResourceType() {
        return getResourceEntityType().toString();
    }

    /**
     * Returns the entity type of the resource
     *
     * @return The resource entity type
     */
    protected abstract EntityType getResourceEntityType();

    @Override
    public String createResource(final @NotNull R resource) throws ResourceFactory.InvalidResourceException {

        //validate that the resource is appropriate for create.
        validateCreateResource(null, resource);

        return RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryThrows<String, ResourceFactory.InvalidResourceException>() {
            @Override
            public String call() throws ResourceFactory.InvalidResourceException {

                try {
                    E entity = convertFromMO(resource);
                    rbacAccessService.validatePermitted(entity, OperationType.CREATE);
                    RestResourceFactoryUtils.validate(entity, Collections.<String, String>emptyMap());

                    beforeCreateEntity(entity);
                    Goid id = getEntityManager().save(entity);
                    afterCreateEntity(entity);

                    resource.setId(id.toString());

                    return id.toString();


                } catch (ObjectModelException ome) {
                    throw new ResourceFactory.ResourceAccessException("Unable to create entity.", ome);
                }

            }
        });
    }


    @Override
    public void createResource(@NotNull final String id, @NotNull final R resource) throws ResourceFactory.ResourceFactoryException {
        //validate that the resource is appropriate for create.
        validateCreateResource(id, resource);
        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceFactoryException>() {
            @Override
            public void call() throws ResourceFactory.ResourceFactoryException {

                try {
                    E entity = convertFromMO(resource);
                    rbacAccessService.validatePermitted(entity, OperationType.CREATE);
                    RestResourceFactoryUtils.validate(entity, Collections.<String, String>emptyMap());

                    Goid goid = Goid.parseGoid(id);

                    beforeCreateEntity(entity);
                    getEntityManager().save(goid, entity);
                    afterCreateEntity(entity);

                    resource.setId(id.toString());


                } catch (ObjectModelException ome) {
                    throw new ResourceFactory.ResourceAccessException("Unable to create entity.", ome);
                }

                resource.setId(id);
            }
        });
    }

    @Override
    public void updateResource(@NotNull final String id, @NotNull final R resource) throws ResourceFactory.ResourceFactoryException {
        validateUpdateResource(id, resource);
        final Goid goid = Goid.parseGoid(id);
        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceFactoryException>() {
            @Override
            public void call() throws ResourceFactory.ResourceFactoryException {
                try {
                    E oldEntity = getEntityManager().findByPrimaryKey(goid);
                    if (oldEntity == null) {
                        throw new ResourceFactory.ResourceNotFoundException("Resource not found " + id);
                    }
                    rbacAccessService.validatePermitted(oldEntity, OperationType.UPDATE);

                    E updateEntity = convertFromMO(resource);

                    updateEntity.setGoid(goid);
                    updateEntity.setVersion(optional(oldEntity.getVersion()).orSome(VERSION_NOT_PRESENT));

                    rbacAccessService.validatePermitted(updateEntity, OperationType.UPDATE);
                    RestResourceFactoryUtils.validate(updateEntity, Collections.<String, String>emptyMap());

                    beforeUpdateEntity(updateEntity);
                    getEntityManager().update(updateEntity);
                    afterUpdateEntity(updateEntity);

                } catch (ObjectModelException ome) {
                    throw new ResourceFactory.ResourceAccessException("Unable to update entity.", ome);
                }
                resource.setId(id);
            }
        });
    }


    @Override
    public R getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {

        Goid goid = Goid.parseGoid(id);
        try {
            E entity = getEntityManager().findByPrimaryKey(goid);
            if (entity == null) {
                throw new ResourceFactory.ResourceNotFoundException("Resource not found " + id);
            }
            rbacAccessService.validatePermitted(entity, OperationType.READ);
            return convertToMO(entity);
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to find entity.", ome);
        }
    }


    public boolean resourceExists(@NotNull String id) {
        Goid goid = Goid.parseGoid(id);
        try {
            return getEntityManager().findByPrimaryKey(goid) != null;
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to find entity.", ome);
        }
    }


    @Override
    public List<R> listResources(@Nullable String sort, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> filters) {
        try {
            List<E> entities = getEntityManager().findPagedMatching(0, -1, sort, ascending, filters);
            entities = rbacAccessService.accessFilter(entities, getResourceEntityType(), OperationType.READ, null);

            return Functions.map(entities, new Functions.UnaryThrows<R, E, ObjectModelException>() {
                @Override
                public R call(E e) throws ObjectModelException {
                    return convertToMO(e);
                }
            });
        } catch (ObjectModelException e) {
            throw new ResourceFactory.ResourceAccessException("Unable to list entities.", e);
        }
    }

    @Override
    public void deleteResource(@NotNull final String id) throws ResourceFactory.ResourceNotFoundException {
        final Goid goid = Goid.parseGoid(id);
        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                try {
                    E entity = getEntityManager().findByPrimaryKey(goid);
                    if (entity == null) {
                        throw new ResourceFactory.ResourceNotFoundException("Resource not found " + id);
                    }
                    rbacAccessService.validatePermitted(entity, OperationType.DELETE);
                    beforeDeleteEntity(entity);
                    getEntityManager().delete(entity);

                    afterDeleteEntity();
                } catch (ObjectModelException ome) {
                    throw new ResourceFactory.ResourceAccessException("Unable to delete entity.", ome);
                }
            }
        });
    }


    protected static final int VERSION_NOT_PRESENT = Integer.MIN_VALUE;

    /**
     * Validates that a resource can be used for create. Checks to see if the id is set correctly
     *
     * @param id       The id to create the resource with.
     * @param resource The resource to create
     */
    private void validateCreateResource(@Nullable String id, R resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new InvalidArgumentException("id", "Must not specify an ID when creating a new entity, or id must equal new entity id");
        }

        if (resource.getVersion() != null) {
            throw new InvalidArgumentException("version", "Must not specify a version when creating a new entity");
        }
    }

    /**
     * Validates that a resource can be used for update. Checks to see if the id is set correctly
     *
     * @param id       The id of the resource to update.
     * @param resource The resource to update
     */
    private void validateUpdateResource(String id, R resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new InvalidArgumentException("id", "Must not specify an ID when updating a new entity, or id must equal entity id");
        }
    }

    @Override
    public Mapping buildMapping(@NotNull R resource, @Nullable Mapping.Action defaultAction, @Nullable String defaultMapBy) {
        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setType(getResourceType());
        mapping.setAction(defaultAction);
        mapping.setSrcId(resource.getId());
        if (!"id".equals(defaultMapBy)) {
            mapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("MapBy", defaultMapBy).map());
        }
        return mapping;
    }

}
