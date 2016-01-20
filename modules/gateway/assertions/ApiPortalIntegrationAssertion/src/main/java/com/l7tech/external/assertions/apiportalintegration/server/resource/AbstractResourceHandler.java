package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.objectmodel.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parent class for all resource handlers.
 *
 * @param <R> the type of portal resource.
 * @param <E> the type of portal generic entity.
 */
public abstract class AbstractResourceHandler<R extends Resource, E extends AbstractPortalGenericEntity> implements ResourceHandler<R> {
    public static final String ID = "id";

    /**
     * Determine whether the given entity should be filtered.
     *
     * @param entity  the entity in question.
     * @param filters a map of filters. Can be null.
     * @return true if the entity should be filtered. False otherwise.
     */
    public abstract boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters);

    public R doGet(@NotNull final String id) throws FindException {
        R result = null;
        final E found = manager.find(id);
        if (found != null) {
            result = transformer.entityToResource(found);
        }
        return result;
    }

    public List<R> doGet(@Nullable final Map<String, String> filters) throws FindException {
        final List<R> resources = new ArrayList<R>();
        if (filters == null || !filters.containsKey(ID)) {
            final List<E> all = manager.findAll();
            for (final E entity : all) {
                if (doFilter(entity, filters)) {
                    resources.add(transformer.entityToResource(entity));
                }
            }
        } else {
            final E found = manager.find(filters.get(ID));
            if (found != null) {
                resources.add(transformer.entityToResource(found));
            }
        }
        return resources;
    }

    public void doDelete(@NotNull final String id) throws DeleteException, FindException {
        manager.delete(id);
    }

    public R doPut(@NotNull R resource) throws FindException, UpdateException, SaveException {
        final E entity = transformer.resourceToEntity(resource);
        final E found = manager.find(entity.getName());
        E result = null;
        if (found != null) {
            result = manager.update(entity);
        } else {
            result = manager.add(entity);
        }
        return transformer.entityToResource(result);
    }

    public List<R> doPut(@NotNull final List<R> resources, final boolean removeOmitted) throws ObjectModelException {
        final Map<String, E> entityMap = new HashMap<String, E>();
        for (final R resource : resources) {
            final E entity = transformer.resourceToEntity(resource);
            Validate.notEmpty(entity.getName(), "Resource id missing");
            entityMap.put(entity.getName(), entity);
        }
        final List<R> outputList = new ArrayList<R>();
        if (!removeOmitted) {
            for (final E entity : entityMap.values()) {
                final E found = manager.find(entity.getName());
                E persisted = null;
                if (found == null) {
                    persisted = manager.add(entity);
                } else {
                    persisted = manager.update(entity);
                }
                outputList.add(transformer.entityToResource(persisted));
            }
        } else {
            // delete all entities that are not included in the list to add/update
            final List<E> all = manager.findAll();
            final Set<String> existingNames = new HashSet<String>();
            for (final E existingEntity : all) {
                existingNames.add(existingEntity.getName());
            }
            final Collection<String> toAdd = CollectionUtils.subtract(entityMap.keySet(), existingNames);
            for (final String add : toAdd) {
                final E added = manager.add(entityMap.get(add));
                outputList.add(transformer.entityToResource(added));
            }
            final Collection<String> toUpdate = CollectionUtils.intersection(entityMap.keySet(), existingNames);
            for (final String update : toUpdate) {
                final E updated = manager.update(entityMap.get(update));
                outputList.add(transformer.entityToResource(updated));
            }
            final Collection<String> toDelete = CollectionUtils.subtract(existingNames, entityMap.keySet());
            for (final String delete : toDelete) {
                LOGGER.log(Level.FINE, "Deleting portal resource with id=" + delete);
                manager.delete(delete);
            }
        }
        return outputList;
    }
    
    public int getCacheItems(){
        return manager.getCacheItem();
    }

    protected AbstractResourceHandler(@NotNull final PortalGenericEntityManager manager, @NotNull final ResourceTransformer transformer) {
        this.manager = manager;
        this.transformer = transformer;
    }

    final PortalGenericEntityManager<E> manager;
    final ResourceTransformer<R, E> transformer;

    private static final Logger LOGGER = Logger.getLogger(AbstractResourceHandler.class.getName());
}
