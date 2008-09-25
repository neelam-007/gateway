/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default {@link com.l7tech.objectmodel.EntityManager} implementation for Hibernate-managed {@link com.l7tech.objectmodel.PersistentEntity persistent entities}.
 * @author alex
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public abstract class HibernateEntityManager<ET extends PersistentEntity, HT extends EntityHeader>
        extends HibernateDaoSupport
        implements EntityManager<ET, HT>
{
    public static final String EMPTY_STRING = "";
    public static final String F_OID = "oid";
    public static final String F_NAME = "name";
    public static final String F_VERSION = "version";

    private final String HQL_FIND_ALL_OIDS_AND_VERSIONS =
            "SELECT " +
                    getTableName() + "." + F_OID + ", " +
                    getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName();

    private final String HQL_FIND_VERSION_BY_OID =
            "SELECT " + getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?";

    private final String HQL_FIND_BY_OID =
            "FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?";

    private final String HQL_DELETE_BY_OID =
            "FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?";

    protected PlatformTransactionManager transactionManager; // required for TransactionTemplate

    @Transactional(readOnly=true)
    @Secured(operation=OperationType.READ)
    public ET findByPrimaryKey(long oid) throws FindException {
        return findEntity(oid);
    }

    /**
     * Find the number of entities that are found for the given criterion.
     *
     * @param restrictions The restrictions (e.g before / after dates)
     * @return The number of entities matched
     * @throws FindException If an error occurs
     */
    protected int findCount( final Criterion... restrictions ) throws FindException {
        try {
            //noinspection unchecked
            return (Integer) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());

                    // Ensure manager specific criteria are added
                    addFindAllCriteria( crit );

                    // Add additional criteria
                    for (Criterion restriction : restrictions ) {
                        crit.add( restriction );
                    }

                    ScrollableResults results = crit.scroll( ScrollMode.SCROLL_SENSITIVE );
                    results.last();
                    return results.getRowNumber();
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }

    /**
     * Find a "page" worth of entities for the given sort, offset and count.
     *
     * <p>Additional restrictions may be passed in the form of hibernate Criterion.</p>
     *
     * @param sortProperty The property to sort by (e.g. name)
     * @param ascending True to sort in ascending order
     * @param offset The initial offset 0 for the first page
     * @param count The number of items to return
     * @param restrictions Additional restrictions (e.g before / after dates)
     * @return  The matching entities or an empty list if none found
     * @throws FindException If an error occurs
     */
    protected List<ET> findPage( final String sortProperty, final boolean ascending, final int offset, final int count, final Criterion... restrictions ) throws FindException {
        try {
            //noinspection unchecked
            return (List<ET>)getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());

                    // Ensure manager specific criteria are added
                    addFindAllCriteria( crit );

                    // Add additional criteria
                    for (Criterion restriction : restrictions ) {
                        crit.add( restriction );
                    }

                    if ( ascending ) {
                        crit.addOrder( Order.asc(sortProperty) );
                    } else {
                        crit.addOrder( Order.desc(sortProperty) );    
                    }

                    crit.setFirstResult( offset );
                    crit.setMaxResults( count );

                    return crit.list();
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }

    protected List<ET> findMatching(final Map<String, Object> map) throws FindException {
        for (Object o : map.values()) {
            if (o == null) return Collections.emptyList();
        }

        try {
            //noinspection unchecked
            return (List<ET>)getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    for (Map.Entry<String,?> entry: map.entrySet()) {
                        crit.add(Restrictions.eq(entry.getKey(), entry.getValue()));
                    }
                    return crit.list();
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }

    protected Map<String,Object> getUniqueAttributeMap(ET entity) {
        switch(getUniqueType()) {
            case NAME:
                if (entity instanceof NamedEntity ) {
                    NamedEntity namedEntity = (NamedEntity) entity;
                    Map<String,Object> map = new HashMap<String, Object>();
                    map.put("name", namedEntity.getName());
                    return map;
                } else {
                    throw new IllegalArgumentException("UniqueType is NAME, but entity is not a NamedEntity");
                }
            case NONE:
                return Collections.emptyMap();
            case OTHER:
            default:
                throw new IllegalArgumentException("UniqueType is OTHER, but getUniqueAttributeMap() not overridden");
        }
    }

    @Secured(operation=OperationType.CREATE)
    public long save(ET entity) throws SaveException {
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Saving {0} ({1})", new Object[] { getImpClass().getSimpleName(), entity.toString() });
        try {
            if (getUniqueType() != UniqueType.NONE) {
                final Map<String, Object> newMap = getUniqueAttributeMap(entity);
                List others;
                try {
                    others = findMatching(newMap);
                } catch (FindException e) {
                    throw new SaveException("Couldn't find previous version(s) to check uniqueness");
                }

                if (!others.isEmpty()) throw new DuplicateObjectException(describeAttributes(newMap) + " must be unique");
            }

            Object key = getHibernateTemplate().save(entity);
            if (!(key instanceof Long))
                throw new SaveException("Primary key was a " + key.getClass().getName() + ", not a Long");

            return ((Long)key);
        } catch (RuntimeException e) {
            throw new SaveException("Couldn't save " + entity.getClass().getSimpleName(), e);
        }
    }

    private String describeAttributes(Map<String, Object> newMap) {
        String what;
        if (newMap.size() == 1) {
            what = newMap.keySet().iterator().next();
        } else if (newMap.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> i = newMap.keySet().iterator(); i.hasNext();) {
                String key = i.next();
                sb.append(key);
                if (i.hasNext()) sb.append(", ");
            }
            what = sb.toString();
        } else {
            throw new IllegalStateException("Unique attribute map was empty");
        }
        return what;
    }

    public void update(ET entity) throws UpdateException {
        try {
            if (getUniqueType() != UniqueType.NONE) {
                final Map<String, Object> newMap = getUniqueAttributeMap(entity);
                List<ET> others;
                try {
                    others = findMatching(newMap);
                } catch (FindException e) {
                    throw new UpdateException("Couldn't find previous version(s) to check uniqueness");
                }

                if (!others.isEmpty()) {
                    for (ET other : others) {
                        if (!entity.getId().equals(other.getId()) || !entity.getClass().equals(other.getClass())) {
                            String message = describeAttributes(newMap) + " must be unique";
                            // nested since DuplicateObjectException is a SaveException not an UpdateException
                            throw new UpdateException(message, new DuplicateObjectException(message));
                        }
                    }
                }
            }
            getHibernateTemplate().merge(entity);
        } catch (RuntimeException e) {
            throw new UpdateException("Couldn't update " + entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Returns the current version (in the database) of the entity with the specified OID.
     *
     * @param oid the OID of the entity whose version should be retrieved
     * @return The version, or null if the entity does not exist.
     * @throws FindException
     */
    @Transactional(readOnly=true)
    public Integer getVersion(final long oid) throws FindException {
        try {
            return (Integer)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_VERSION_BY_OID);
                    q.setLong(0, oid);
                    return q.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Transactional(readOnly=true)
    @Secured(operation=OperationType.READ)
    public ET findEntity(final long oid) throws FindException {
        try {
            //noinspection unchecked
            return (ET)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_OID);
                    q.setLong(0, oid);
                    return q.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Transactional(readOnly=true)
    public Map<Long,Integer> findVersionMap() throws FindException {
        Map<Long, Integer> result = new HashMap<Long, Integer>();
        if (!PersistentEntity.class.isAssignableFrom(getImpClass())) throw new FindException("Can't find non-Entities!");

        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.NEVER);
            Query q = s.createQuery(HQL_FIND_ALL_OIDS_AND_VERSIONS);
            List results = q.list();
            if (results.size() > 0) {
                for (Object result1 : results) {
                    Object[] row = (Object[]) result1;
                    if (row[0]instanceof Long && row[1]instanceof Integer) {
                        result.put((Long)row[0], (Integer)row[1]);
                    } else {
                        throw new FindException("Got unexpected fields " + row[0] + " and " + row[1] + " from query!");
                    }
                }
            }
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
            releaseSession(s);
        }

        return result;
    }

    @Transactional(propagation=SUPPORTS)
    public EntityType getEntityType() {
        return EntityType.UNDEFINED;
    }

    @Transactional(readOnly=true)
    public Collection<HT> findAllHeaders() throws FindException {
        Collection<ET> entities = findAll();
        List<HT> headers = new ArrayList<HT>();
        for (ET entity : entities) {
            headers.add(newHeader(entity));
        }
        return Collections.unmodifiableList(headers);
    }

    @Transactional(readOnly=true)
    public Collection<HT> findAllHeaders(final int offset, final int windowSize) {
        //noinspection unchecked
        List<ET> entities = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            @Override
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(getImpClass());
                crit.setFirstResult(offset);
                crit.setFetchSize(windowSize);
                return crit.list();
            }
        });

        List<HT> headers = new ArrayList<HT>(entities.size());
        for (ET entity : entities) {
            headers.add(newHeader(entity));
        }
        return headers;
    }

    /**
     * Override this method to customize how EntityHeaders get created
     * (if {@link HT} is a subclass of {@link EntityHeader} it's mandatory)
     *
     * @param entity the PersistentEntity
     * @return a new EntityHeader based on the provided Entity ID and name 
     */
    protected HT newHeader(ET entity) {
        String name = null;
        if (entity instanceof NamedEntity) name = ((NamedEntity) entity).getName();
        if (name == null) name = "";

        //noinspection unchecked
        return (HT) new EntityHeader(
                entity.getId(),
                getEntityType(),
                name,
                EMPTY_STRING);
    }

    /**
     * Override this method to add additional criteria to findAll(), findAllHeaders(), findByName() etc.
     * @param allHeadersCriteria the Hibernate Criteria object that should be customized if some filtering is required 
     */
    protected void addFindAllCriteria(Criteria allHeadersCriteria) {
    }

    @Transactional(readOnly=true)
    @Secured(operation=OperationType.READ)
    public Collection<ET> findAll() throws FindException {
        try {
            //noinspection unchecked
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria allHeadersCriteria = session.createCriteria(getImpClass());
                    addFindAllCriteria(allHeadersCriteria);
                    return allHeadersCriteria.list();
                }
            });
        } catch (HibernateException e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Collection<ET> findAll(final int offset, final int windowSize) throws FindException {
        try {
            //noinspection unchecked
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria allCriteria = session.createCriteria(getImpClass());
                    addFindAllCriteria(allCriteria);
                    allCriteria.setFirstResult(offset);
                    allCriteria.setMaxResults(windowSize);
                    return allCriteria.list();
                }
            });
        } catch (HibernateException e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Transactional(propagation=SUPPORTS)
    public String getAllQuery() {
        String alias = getTableName();
        return "from " + alias + " in class " + getImpClass().getName();
    }

    @Secured(operation=OperationType.DELETE)
    public void delete(long oid) throws DeleteException, FindException {
        //getHibernateTemplate().d
        delete(getImpClass(), oid);
    }

    @Secured(operation=OperationType.DELETE)
    public void delete(final ET et) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    //noinspection unchecked
                    ET entity = (ET)session.get(et.getClass(), et.getOid());
                    if (entity == null) {
                        session.delete(et);
                    } else {
                        // Avoid NonUniqueObjectException if an older version of this is still in the Session
                        session.delete(entity);
                    }
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new DeleteException("Couldn't delete entity", e);
        }
    }

    @Transactional(propagation=SUPPORTS)
    public boolean isCacheCurrent(long objectid, int maxAge) {
        Lock read = cacheLock.readLock();
        CacheInfo cacheInfo;
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByOid.get(objectid);
            read.unlock(); read = null;
            cacheInfo = ref == null ? null : ref.get();
            return cacheInfo != null && cacheInfo.timestamp + maxAge >= System.currentTimeMillis();
        } finally {
            if (read != null) read.unlock();
        }

    }

    @SuppressWarnings({"unchecked"})
    @Transactional(propagation=SUPPORTS)
    @Secured(operation=OperationType.READ)
    public ET getCachedEntityByName(final String name, int maxAge) throws FindException {
        if (name == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");
        Lock read = cacheLock.readLock();
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByName.get(name);
            read.unlock(); read = null;
            CacheInfo<ET> cinfo = ref == null ? null : ref.get();

            if (cinfo != null) return freshen(cinfo, maxAge);

            return (ET) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus transactionStatus) {
                    try {
                        ET ent = findByUniqueName(name);
                        return ent == null ? null : checkAndCache(ent);
                    } catch (Exception e) {
//                        transactionStatus.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (RuntimeException e) {
            throw new FindException(ExceptionUtils.getMessage(e), e);
        } catch (CacheVeto e) {
            throw new FindException("Couldn't cache entity", e);
        } finally {
            if (read != null) read.unlock();
        }
    }

    @Transactional(readOnly=true)
    @Secured(operation=OperationType.READ)
    public ET findByUniqueName(final String name) throws FindException {
        if (name == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");

        try {
            //noinspection unchecked
            return (ET)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    addFindByNameCriteria(crit);
                    crit.add(Restrictions.eq(F_NAME, name));
                    return crit.uniqueResult();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    /**
     * Override to update the criteria used by findByUniqueName() before it's executed
     * @param crit criteria to be mutated
     */
    protected void addFindByNameCriteria(Criteria crit) {
    }

    /**
     * Gets the {@link PersistentEntity} with the specified name from a cache where possible.  If the
     * entity is not present in the cache, it will be retrieved from the database.  If the entity
     * is present in the cache but was cached too long ago, checks whether the cached entity
     * is stale by looking up its {@link PersistentEntity#getVersion}.  If the cached entity has the same
     * version as the database, the cached version is marked fresh.
     *
     * @param objectid the OID of the object to get
     * @param maxAge the age, in milliseconds, that a cached entity must attain before it is considered stale
     * @return the object with the specified ID, from a cache if possible.
     * @throws FindException
     * @throws CacheVeto
     */
    @Transactional(propagation=SUPPORTS, readOnly=true)
    @Secured(operation=OperationType.READ)
    public ET getCachedEntity(final long objectid, int maxAge) throws FindException, CacheVeto {
        ET entity;

        Lock read = cacheLock.readLock();
        CacheInfo<ET> cacheInfo;
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByOid.get(objectid);
            read.unlock(); read = null;
            cacheInfo = ref == null ? null : ref.get();
            if (cacheInfo == null) {
                // Might be new, or might be first run
                //noinspection unchecked
                entity = (ET)new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                    public Object doInTransaction(TransactionStatus transactionStatus) {
                        try {
                            return findEntity(objectid);
                        } catch (FindException e) {
//                            transactionStatus.setRollbackOnly();
                            throw new RuntimeException(e);
                        }
                    }
                });

                if (entity == null) return null; // Doesn't exist

                // New
                return checkAndCache(entity);
            } else {
                return freshen(cacheInfo, maxAge);
            }
        } finally {
            if (read != null) read.unlock();
        }
    }

    private ET freshen(CacheInfo<ET> cacheInfo, int maxAge) throws FindException, CacheVeto {
        if (cacheInfo.timestamp + maxAge < System.currentTimeMillis()) {
            // Time for a version check (getVersion() always goes to the database)
            Integer currentVersion = getVersion(cacheInfo.entity.getOid());
            if (currentVersion == null) {
                // BALEETED
                cacheRemove(cacheInfo.entity);
                return null;
            } else if (currentVersion != cacheInfo.version) {
                // Updated
                ET thing = findEntity(cacheInfo.entity.getOid());
                return thing == null ? null : checkAndCache(thing);
            }
        }

        return cacheInfo.entity;
    }

    protected void cacheRemove(PersistentEntity thing) {
        Lock write = cacheLock.writeLock();
        try {
            write.lock();
            cacheInfoByOid.remove(thing.getOid());
            if (thing instanceof NamedEntity) {
                cacheInfoByName.remove(((NamedEntity)thing).getName());
            }
            removedFromCache(thing);
            write.unlock();
            write = null;

        } finally {
            if (write != null) write.unlock();
        }
    }

    /**
     * Override this method to be notified when an Entity has been removed from the cache.
     * This method is called while the cache write lock is held, so it should avoid
     * taking any lengthy action before returning.
     *
     * @param ent the Entity that has been removed
     */
    protected void removedFromCache(Entity ent) { }

    /**
     * Override this method to check an Entity before it's added to the cache.
     * The cache lock is not held when this method is invoked.
     *
     * @param ent the Entity to check for suitability
     * @throws CacheVeto to prevent the Entity from being added.
     */
    protected void checkCachable(Entity ent) throws CacheVeto { }

    /**
     * Override this method to be notified when an Entity has been added to the cache.
     * This method is called while the cache write lock is held, so it should avoid
     * taking any lengthy action before returning.
     *
     * @param ent the Entity that has been added to the cache
     */
    protected void addedToCache(PersistentEntity ent) { }

    /**
     * Perform some action while holding the cache write lock.
     * It is an error to invoke this method if the current thread already
     * holds the cache read lock.
     *
     * @param stuff  a Callable to invoke with the lock held.  Required.
     * @return the value returned by the Callable.
     * @throws Exception if the Callable throws an exception
     */
    protected final <RT> RT doWithCacheWriteLock(Callable<RT> stuff) throws Exception {
        return doWithLock(cacheLock.writeLock(), stuff);
    }

    /**
     * Perform some action while holding the cache read lock.
     * It is safe to invoke this method while the current thread already
     * holds the cache write lock.
     *
     * @param stuff  a Callable to invoke with the lock held.  Required.
     * @return the value returned by the Callable.
     * @throws Exception if the Callable throws an exception
     */
    protected final <RT> RT doWithCacheReadLock(Callable<RT> stuff) throws Exception {
        return doWithLock(cacheLock.readLock(), stuff);
    }

    private <RT> RT doWithLock(Lock lock, Callable<RT> stuff) throws Exception {
        try {
            lock.lock();
            return stuff.call();
        } finally {
            lock.unlock();
        }
    }

    protected ET checkAndCache(ET thing) throws CacheVeto {
        final Long oid = thing.getOid();
        checkCachable(thing);

        CacheInfo<ET> info = null;

        // Get existing cache info
        Lock read = cacheLock.readLock();
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByOid.get(oid);
            info = ref == null ? null : ref.get();
        } finally {
            if (read != null) read.unlock();
        }

        Lock write = cacheLock.writeLock();
        try {
            write.lock();

            // new item to cache
            if (info == null) {
                info = new CacheInfo<ET>();
                WeakReference<CacheInfo<ET>> newref = new WeakReference<CacheInfo<ET>>(info);

                cacheInfoByOid.put(oid, newref);
                if (thing instanceof NamedEntity) {
                    cacheInfoByName.put(((NamedEntity)thing).getName(), newref);
                }
            }

            // set cached info 
            info.entity = thing;
            info.version = thing.getVersion();
            info.timestamp = System.currentTimeMillis();
            addedToCache(thing);

        } finally {
            if (write != null) write.unlock();
        }

        return thing;
    }


    /**
     * Retrieve the persistent object instance by it's primary key (object id). If the
     * object is not found returns <code>null</code>
     *
     * @param impClass the object class
     * @param oid      the object id
     * @return the object instance or <code>null</code> if no instance has been found
     * @throws FindException if there was an data access error
     */
    protected ET findByPrimaryKey(final Class impClass, final long oid) throws FindException {
        try {
            //noinspection unchecked
            return (ET) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    return session.get(impClass, oid);
                }
            });
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, org.hibernate.ObjectNotFoundException.class) ||
                ExceptionUtils.causedBy(e, ObjectDeletedException.class)) {
                return null;
            }
            throw new FindException("Data access error ", e);
        }
    }

    /**
     * Delete the persistent object of given class for
     *
     * @param entityClass the Class of Entity to delete
     * @param oid the OID of the Entity to be deleted
     * @throws DeleteException if the Entity cannot be deleted
     * @return true if the entity was deleted; false otherwise
     */
    protected boolean delete(Class entityClass, final long oid) throws DeleteException {
        try {
            return (Boolean)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_BY_OID);
                    q.setLong(0, oid);
                    List todelete = q.list();
                    if (todelete.size() == 0) {
                        return false;
                    } else if (todelete.size() == 1) {
                        session.delete(todelete.get(0));
                        return true;
                    } else {
                        throw new RuntimeException("More than one entity found with oid = " + oid);
                    }
                }
            });
        } catch (Exception he) {
            throw new DeleteException(he.toString(), he);
        }
    }

    /**
     * Lookup the entity by oid and delete(ET) it.
     *
     * @param oid The entity oid.
     * @return true if the entity was deleted; false otherwise
     * @throws com.l7tech.objectmodel.FindException if there is a problem finding the entity
     * @throws com.l7tech.objectmodel.DeleteException if there is a problem deleting the entity
     */
    protected boolean findAndDelete(final long oid) throws FindException, DeleteException {
        boolean deleted = false;

        ET entity = this.findByPrimaryKey( oid );
        if ( entity != null ) {
            delete(entity);
            deleted = true;
        }

        return deleted;
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private Map<Long, WeakReference<CacheInfo<ET>>> cacheInfoByOid = new HashMap<Long, WeakReference<CacheInfo<ET>>>();
    private Map<String, WeakReference<CacheInfo<ET>>> cacheInfoByName = new HashMap<String, WeakReference<CacheInfo<ET>>>();

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /** Holds information about a cached Entity. */
    static class CacheInfo<ET extends PersistentEntity> {
        ET entity;
        long timestamp;
        int version;
    }    
}
