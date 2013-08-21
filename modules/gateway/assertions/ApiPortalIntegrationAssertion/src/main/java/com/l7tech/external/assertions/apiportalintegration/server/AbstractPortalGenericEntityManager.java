package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Background;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parent class for all AbstractPortalGenericEntity Managers that uses a generic entity manager to store/retrieve generic entities.
 * <p/>
 * Loaded generic entities will be cached in RAM.  The cache is cleared occasionally (daily by default), and specific
 * entries are removed whenever their corresponding generic entity is updated (anywhere on the cluster).
 * <p/>
 * No caching of failed lookups (negative caching) is currently performed.
 */
public abstract class AbstractPortalGenericEntityManager<T extends AbstractPortalGenericEntity> implements PortalGenericEntityManager<T>, ApplicationListener {
    /**
     * @return the EntityManager to use for CRUD operations.
     */
    public abstract EntityManager<T, GenericEntityHeader> getEntityManager();

    /**
     * @return an array of objects to use for update locking (minimize race conditions when updating a generic entity).
     */
    public abstract Object[] getUpdateLocks();

    /**
     * @return the name of the config property that stores the cache wipe interval in milliseconds.
     */
    public abstract String getCacheWipeIntervalConfigProperty();

    public AbstractPortalGenericEntityManager(final ApplicationContext applicationContext) {
        final ApplicationEventProxy applicationEventProxy = applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);

        // Clear cache daily to prevent it from growing without bound
        long wipeTime = ConfigFactory.getLongProperty(getCacheWipeIntervalConfigProperty(), TimeUnit.DAYS.toMillis(1));
        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                nameCache.clear(); // This is the only time entries are ever removed from the nameCache
                cache.clear();
            }
        }, wipeTime, wipeTime);
    }

    @Override
    public T add(final T genericEntity) throws SaveException {
        if (!AbstractPortalGenericEntity.DEFAULT_GOID.equals(genericEntity.getGoid())) {
            throw new SaveException("Specified GenericEntity has already been saved (it has goid " + genericEntity.getGoid() + ")");
        }
        final String name = genericEntity.getName();
        try {
            doAsSystem(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    final Goid goid = getEntityManager().save(genericEntity);
                    nameCache.put(goid, name);
                    genericEntity.setGoid(goid);
                    cache.put(name, genericEntity.getReadOnlyCopy());
                    return null;
                }
            });
        } catch (final SaveException e) {
            throw e;
        } catch (final ObjectModelException e) {
            throw new SaveException(e);
        }
        return genericEntity;
    }

    @Override
    public T update(final T genericEntity) throws FindException, UpdateException {
        final String name = genericEntity.getName();
        if (name != null) {
            final int lockNum = Math.abs(genericEntity.getName().hashCode() % getUpdateLocks().length);
            final Object lockObj = getUpdateLocks()[lockNum];
            try {
                doAsSystem(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        // locking to minimize race conditions when the same GenericEntity is updated
                        synchronized (lockObj) {
                            final T found = findByUniqueName(name);
                            if (found == null) {
                                throw new ObjectNotFoundException("GenericEntity with name=" + name + " not found");
                            }
                            // only update if it's necessary
                            if (!found.equals(genericEntity)) {
                                //we want to keep the original oid and version
                                genericEntity.setGoid(found.getGoid());
                                genericEntity.setVersion(found.getVersion());
                                getEntityManager().update(genericEntity);
                                nameCache.put(genericEntity.getGoid(), name);
                                cache.put(name, genericEntity.getReadOnlyCopy());
                            } else {
                                genericEntity.setGoid(found.getGoid());
                                genericEntity.setVersion(found.getVersion());
                                nameCache.put(found.getGoid(), name);
                                cache.put(name, found.getReadOnlyCopy());
                                LOGGER.log(Level.FINE, "Skipping update of generic entity with name=" + genericEntity.getName() +
                                        " because it is equal to an existing generic entity.");
                            }
                        }
                        return null;
                    }
                });
            } catch (final FindException e) {
                throw e;
            } catch (final UpdateException e) {
                throw e;
            } catch (final ObjectModelException e) {
                throw new UpdateException(e);
            }
        } else {
            throw new UpdateException("Missing name for GenericEntity.");
        }
        return genericEntity;
    }

    @Override
    public void delete(final String name) throws FindException, DeleteException {
        try {
            doAsSystem(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    final T found = findByUniqueName(name);
                    if (found == null) {
                        throw new ObjectNotFoundException("GenericEntity with name=" + name + " not found");
                    }
                    getEntityManager().delete(found);
                    // only remove from cache, not named cache to minimize race conditions between the two concurrent maps
                    cache.remove(name);
                    return null;
                }
            });
        } catch (final FindException e) {
            throw e;
        } catch (final DeleteException e) {
            throw e;
        } catch (final ObjectModelException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    @Override
    public T find(final String name) throws FindException {
        return find(name, false);
    }

    @Override
    public T find(final String name, final boolean nocache) throws FindException {
        /**
         * if nocache=true:
         * - don't use cache, always retrieve from the db
         * - clears cache for the particular name if any
         * - will not store the result into cache
         */
        if (!nocache) {
            final T cached = (T) cache.get(name);
            if (cached != null) {
                // protect the cache
                return (T) cached.getReadOnlyCopy();
            }
        } else {
            cache.remove(name);//this make sure cache is cleaned up just in case the find below fails
        }
        final T found = findByUniqueName(name);
        if (found != null && !nocache) {
            nameCache.put(found.getGoid(), name);
            cache.put(name, found.getReadOnlyCopy());
        }
        return found;
    }

    @Override
    public List<T> findAll() throws FindException {
        final Collection<T> all = getEntityManager().findAll();
        return all == null ? Collections.<T>emptyList() : new ArrayList<T>(all);
    }

    /**
     * Listen for any changes to a GenericEntity (which can be triggered by any node on a cluster) and remove from cache.
     */
    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        // if a PortalManagedService has been modified, remove it from the cache
        if (event instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
            if (GenericEntity.class.equals(entityInvalidationEvent.getEntityClass())) {
                final Goid[] ids = entityInvalidationEvent.getEntityIds();
                for (final Goid id : ids) {
                    final String name = nameCache.get(id);
                    if (name != null) {
                        cache.remove(name);
                    }
                }
            }
        }
    }
    
    @Override
    public int getCacheItem(){
        return cache.size();
    }

    /**
     * Key = entity name.
     * <p/>
     * Value = entity.
     */
    protected final ConcurrentMap<String, AbstractPortalGenericEntity> cache = new ConcurrentHashMap<String, AbstractPortalGenericEntity>();
    /**
     * Key = entity oid.
     * <p/>
     * Value = entity name.
     */
    protected final ConcurrentMap<Goid, String> nameCache = new ConcurrentHashMap<Goid, String>();
    protected static final int DEFAULT_NUM_UPDATE_LOCKS = 1000;

    /**
     * Suppresses auditing of generic entity changes.
     */
    protected static void doAsSystem(final Callable<Void> task) throws ObjectModelException {
        boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            task.call();
        } catch (final ObjectModelException e) {
            throw e;
        } catch (final Exception e) {
            throw new ObjectModelException(e);
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(AbstractPortalGenericEntityManager.class.getName());

    /**
     * Temporary workaround for bug http://sarek.l7tech.com/bugzilla/show_bug.cgi?id=12334
     */
    private T findByUniqueName(final String name) throws FindException {
        T found = null;
        try {
            found = getEntityManager().findByUniqueName(name);
        } catch (final InvalidGenericEntityException e) {
            if (e.getMessage().contains("generic entity is not of expected class")) {
                // temporary workaround for bug
                // see http://sarek.l7tech.com/bugzilla/show_bug.cgi?id=12334
                LOGGER.log(Level.FINE, "Generic entity with name=" + name + " was found but has the different classname.", ExceptionUtils.getDebugException(e));
            } else {
                throw e;
            }
        }
        return found;
    }
}
