/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.objectmodel;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.util.ExceptionUtils;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public abstract class HibernateEntityManager extends HibernateDaoSupport implements EntityManager {
    public static final String EMPTY_STRING = "";
    public static final String F_OID = "oid";
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

    /**
     * Returns the current version (in the database) of the entity with the specified OID.
     *
     * @param oid the OID of the entity whose version should be retrieved
     * @return The version, or null if the entity does not exist.
     * @throws FindException
     */
    public Integer getVersion(long oid) throws FindException {
        try {
            Query q = getSession().createQuery(HQL_FIND_VERSION_BY_OID);
            q.setLong(0, oid);
            q.setFlushMode(FlushMode.NEVER);
            List results = q.list();
            if (results.size() == 0) return null;
            if (results.size() > 1) throw new FindException("Multiple results found");
            Object result = results.get(0);
            if (!(result instanceof Integer)) throw new FindException("Found " + result.getClass().getName() + " when looking for Integer!");
            return (Integer)result;
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Entity findEntity(long oid) throws FindException {
        try {
            Query q = getSession().createQuery(HQL_FIND_BY_OID);
            q.setFlushMode(FlushMode.NEVER);
            q.setLong(0, oid);
            List results = q.list();
            if (results.size() == 0) return null;
            if (results.size() > 1) throw new FindException("Multiple results found!");
            Object result = results.get(0);
            if (!(result instanceof Entity)) throw new FindException("Found " + result.getClass().getName() + " when looking for Entity!");
            return (Entity)results.get(0);
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Map findVersionMap() throws FindException {
        Map result = new HashMap();
        if (!Entity.class.isAssignableFrom(getImpClass())) throw new FindException("Can't find non-Entities!");

        try {
            Query q = getSession().createQuery(HQL_FIND_ALL_OIDS_AND_VERSIONS).setFlushMode(FlushMode.NEVER);
            List results = q.list();
            if (results.size() > 0) {
                for (Iterator i = results.iterator(); i.hasNext();) {
                    Object[] row = (Object[])i.next();
                    if (row[0] instanceof Long && row[1] instanceof Integer) {
                        result.put(row[0], row[1]);
                    } else {
                        throw new FindException("Got unexpected fields " + row[0] + " and " + row[1] + " from query!");
                    }
                }
            }
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }

        return result;
    }

    public abstract Class getImpClass();

    public abstract Class getInterfaceClass();

    public abstract String getTableName();

    public Collection findAllHeaders() throws FindException {
        Collection entities = findAll();
        List headers = new ArrayList();
        for (Iterator i = entities.iterator(); i.hasNext();) {
            Entity entity = (Entity)i.next();
            String name = null;
            if (entity instanceof NamedEntity) name = ((NamedEntity)entity).getName();
            if (name == null) name = "";
            final long id = entity.getOid();
            headers.add(new EntityHeader(Long.toString(id), EntityType.fromInterface(getInterfaceClass()), name, EMPTY_STRING));
        }
        return Collections.unmodifiableList(headers);
    }

    /**
     * Override this method to add additional criteria to findAll(), findAllHeaders(), findByName() etc.
     *
     * @param allHeadersCriteria
     */
    protected void addFindAllCriteria(Criteria allHeadersCriteria) {
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public Collection findAll() throws FindException {
        try {
            Session s = getSession();
            Criteria allHeadersCriteria = s.createCriteria(getImpClass());
            addFindAllCriteria(allHeadersCriteria);
            return allHeadersCriteria.list();
        } catch (HibernateException e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public String getAllQuery() {
        String alias = getTableName();
        return "from " + alias + " in class " + getImpClass().getName();
    }

    public void delete(long oid) throws DeleteException, FindException {
        //getHibernateTemplate().d
        delete(getImpClass(), oid);
    }

    public boolean isCacheCurrent(long objectid, int maxAge) throws FindException {
        Long oid = new Long(objectid);
        Sync read = cacheLock.readLock();
        CacheInfo cacheInfo;
        try {
            read.acquire();
            cacheInfo = (CacheInfo)cacheInfoByOid.get(oid);
            read.release(); read = null;

            if (cacheInfo == null) return false;

            return cacheInfo.timestamp + maxAge >= System.currentTimeMillis();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while acquiring cache lock", e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (read != null) read.release();
        }

    }

    public NamedEntity getCachedEntityByName(String name, int maxAge) throws FindException {
        if (name == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");
        Sync read = cacheLock.readLock();
        try {
            read.acquire();
            CacheInfo cinfo = (CacheInfo)cacheInfoByName.get(name);
            read.release(); read = null;

            NamedEntity ne;
            if (cinfo == null) {
                final Session session = getSession();

                Criteria findByName = session.createCriteria(getInterfaceClass());
                findByName.add(Restrictions.eq("name", name));
                findByName.setFlushMode(FlushMode.NEVER);
                List entities = findByName.list();

                if (entities.size() > 1) {
                    throw new FindException("Found multiple entities with name '" + name + "'");
                } else if (entities.size() == 0) {
                    return null;
                }
                ne = (NamedEntity)entities.get(0);

                return (NamedEntity)checkAndCache(ne);
            } else {
                return (NamedEntity)freshen(cinfo, maxAge);
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for cache lock", e);
            Thread.currentThread().interrupt();
            return null;
        } catch (HibernateException e) {
            throw new FindException("Couldn't find entity by name", e);
        } catch (CacheVeto e) {
            throw new FindException("Couldn't cache entity", e);
        } finally {
            if (read != null) read.release();
        }
    }

    /**
     * Gets the {@link Entity} with the specified name from a cache where possible.  If the
     * entity is not present in the cache, it will be retrieved from the database.  If the entity
     * is present in the cache but was cached too long ago, checks whether the cached entity
     * is stale by looking up its {@link Entity#getVersion}.  If the cached entity has the same
     * version as the database, the cached version is marked fresh.
     *
     * @param objectid the OID of the object to get
     * @param maxAge the age, in milliseconds, that a cached entity must attain before it is considered stale
     * @return the object with the specified ID, from a cache if possible.
     * @throws FindException
     * @throws CacheVeto
     */
    public Entity getCachedEntity(long objectid, int maxAge) throws FindException, CacheVeto {
        Long oid = new Long(objectid);
        Entity entity;

        Sync read = cacheLock.readLock();
        CacheInfo cacheInfo;
        try {
            read.acquire();
            cacheInfo = (CacheInfo)cacheInfoByOid.get(oid);
            read.release(); read = null;

            if (cacheInfo == null) {
                // Might be new, or might be first run
                entity = findEntity(objectid);
                if (entity == null) return null; // Doesn't exist

                // New
                return checkAndCache(entity);
            } else {
                return freshen(cacheInfo, maxAge);
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while acquiring cache lock", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (read != null) read.release();
        }

    }

    private Entity freshen(CacheInfo cacheInfo, int maxAge) throws FindException, CacheVeto {
        if (cacheInfo.timestamp + maxAge < System.currentTimeMillis()) {
            // Time for a version check (getVersion() always goes to the database)
            Integer currentVersion = getVersion(cacheInfo.entity.getOid());
            if (currentVersion == null) {
                // BALEETED
                cacheRemove(cacheInfo.entity);
                return null;
            } else if (currentVersion.intValue() != cacheInfo.version) {
                // Updated
                return checkAndCache(findEntity(cacheInfo.entity.getOid()));
            }
        }

        return cacheInfo.entity;
    }

    protected void cacheRemove(Entity thing) {
        final Long oid = new Long(thing.getOid());

        Sync write = cacheLock.writeLock();
        try {
            write.acquire();
            cacheInfoByOid.remove(oid);
            if (thing instanceof NamedEntity) {
                cacheInfoByName.remove(((NamedEntity)thing).getName());
            }
            write.release();
            write = null;

            removedFromCache(thing);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Interrupted waiting for cache lock", e);
            Thread.currentThread().interrupt();
        } finally {
            if (write != null) write.release();
        }
    }

    /**
     * Override this method to be notified when an Entity has been removed from the cache.
     */
    protected void removedFromCache(Entity ent) { }

    /**
     * Override this method to check an Entity before it's added to the cache.
     *
     * @throws CacheVeto to prevent the Entity from being added.
     */
    protected void checkCachable(Entity ent) throws CacheVeto { }

    /**
     * Override this method to be notified when an Entity has been added to the cache.
     */
    protected void addedToCache(Entity ent) { }

    protected Entity checkAndCache(Entity thing) throws CacheVeto {
        final Long oid = new Long(thing.getOid());
        checkCachable(thing);

        Sync read = cacheLock.readLock();
        Sync write = cacheLock.writeLock();
        try {
            read.acquire();
            CacheInfo info = (CacheInfo)cacheInfoByOid.get(oid);
            read.release(); read = null;

            if (info == null) {
                info = new CacheInfo();

                write.acquire();
                cacheInfoByOid.put(oid, info);
                if (thing instanceof NamedEntity) {
                    cacheInfoByName.put(((NamedEntity)thing).getName(), info);
                }
                write.release(); write = null;
            }

            info.entity = thing;
            info.version = thing.getVersion();
            info.timestamp = System.currentTimeMillis();

            addedToCache(thing);

            return thing;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for cache lock", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (write != null) write.release();
            if (read != null) read.release();
        }
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
    protected Object findByPrimaryKey(Class impClass, long oid) throws FindException {
        try {
            Session s = getSession();
            FlushMode beforeMode = s.getFlushMode();
            s.setFlushMode(FlushMode.COMMIT);
            Object thing = s.load(impClass, new Long(oid));
            s.setFlushMode(beforeMode);
            return thing;
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
     * @param entityClass
     * @param oid
     * @throws DeleteException
     */
    protected boolean delete(Class entityClass, long oid) throws DeleteException {
        try {
            Query q = getSession().createQuery(HQL_DELETE_BY_OID);
            q.setLong(0,oid);
            List todelete = q.list();
            if (todelete.size() == 0) {
                return false;
            } else if (todelete.size() == 1) {
                getHibernateTemplate().delete(todelete.get(0));
                return true;
            } else {
                throw new DeleteException("More than one entity found with oid = " + oid);
            }
        } catch (DataAccessException he) {
            throw new DeleteException(he.toString(), he);
        }
    }


    protected final Logger logger = Logger.getLogger(getClass().getName());

    private ReadWriteLock cacheLock = new ReentrantWriterPreferenceReadWriteLock();
    private Map cacheInfoByOid = new HashMap();
    private Map cacheInfoByName = new HashMap();
}
