/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.util.ExceptionUtils;
import net.sf.hibernate.Criteria;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HibernateEntityManager extends HibernateDaoSupport implements EntityManager {
    public static final String EMPTY_STRING = "";
    public static final String F_OID = "oid";
    public static final String F_VERSION = "version";


    /**
     * Returns the current version (in the database) of the entity with the specified OID.
     *
     * @param oid the OID of the entity whose version should be retrieved
     * @return The version, or null if the entity does not exist.
     * @throws FindException
     */
    public Integer getVersion(long oid) throws FindException {
        String alias = getTableName();
        String query = "SELECT " + alias + ".version"
          + " FROM " + alias + " IN CLASS " + getImpClass().getName()
          + " WHERE " + alias + ".oid = ?";
        try {
            List results = getHibernateTemplate().find(query, new Long(oid));
            if (results.size() == 0) return null;
            if (results.size() > 1) throw new FindException("Multiple results found");
            Object result = results.get(0);
            if (!(result instanceof Integer)) throw new FindException("Found " + result.getClass().getName() + " when looking for Integer!");
            return (Integer)result;
        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Entity findEntity(long oid) throws FindException {
        String alias = getTableName();
        String query = "FROM " + alias +
          " IN CLASS " + getImpClass() +
          " WHERE " + alias + ".oid = ?";
        try {
            List results = getHibernateTemplate().find(query, new Long(oid));
            if (results.size() == 0) return null;
            if (results.size() > 1) throw new FindException("Multiple results found!");
            Object result = results.get(0);
            if (!(result instanceof Entity)) throw new FindException("Found " + result.getClass().getName() + " when looking for Entity!");
            return (Entity)results.get(0);
        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Map findVersionMap() throws FindException {
        Map result = new HashMap();
        Class impClass = getImpClass();
        String alias = getTableName();
        if (!Entity.class.isAssignableFrom(impClass)) throw new FindException("Can't find non-Entities!");

        String query = "SELECT " + alias + ".oid, " + alias + ".version" +
          " FROM " + alias +
          " IN CLASS " + getImpClass();

        try {
            List results = getHibernateTemplate().find(query);
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
        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }

        return result;
    }

    /**
     * Generates a Hibernate query string for retrieving a single field from a User.
     *
     * @param oid      The objectId of the User to query
     * @param getfield the (aliased) name of the field to return
     * @return
     */
    protected String getFieldQuery(String oid, String getfield) {
        String alias = getTableName();
        StringBuffer sqlBuffer = new StringBuffer("SELECT ");
        sqlBuffer.append(alias);
        sqlBuffer.append(".");
        sqlBuffer.append(getfield);
        sqlBuffer.append(" FROM ");
        sqlBuffer.append(alias);
        sqlBuffer.append(" in class ");
        sqlBuffer.append(getImpClass().getName());
        sqlBuffer.append(" WHERE ");
        sqlBuffer.append(alias);
        sqlBuffer.append(".");
        sqlBuffer.append(F_OID);
        sqlBuffer.append(" = '");
        sqlBuffer.append(oid);
        sqlBuffer.append("'");
        return sqlBuffer.toString();
    }

    public void checkUpdate(Entity ent) throws UpdateException {
        String stmt = getFieldQuery(new Long(ent.getOid()).toString(), F_VERSION);

        try {
            Session s = getSession();
            List results = s.find(stmt);
            if (results.size() == 0) {
                String err = "Object to be updated does not exist!";
                logger.log(Level.WARNING, err);
                throw new UpdateException(err);
            }

            int savedVersion = ((Integer)results.get(0)).intValue();

            if (savedVersion != ent.getVersion()) {
                String err = "Object to be updated is stale (a later version exists in the database)!";
                logger.log(Level.WARNING, err);
                throw new StaleUpdateException(err);
            }
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.getMessage(), e);
        }
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
            List entities = allHeadersCriteria.list();
            return entities;
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

    /**
     * @param o
     * @param maxAge
     * @return
     * @throws FindException
     * @throws CacheVeto
     */
    public Entity getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
        Long oid = new Long(o);
        Entity entity;

        Sync read = cacheLock.readLock();
        Sync write = cacheLock.writeLock();
        CacheInfo cacheInfo = null;
        try {
            read.acquire();
            cacheInfo = (CacheInfo)cache.get(oid);
            read.release();
            read = null;
            if (cacheInfo == null) {
                // Might be new, or might be first run
                entity = findEntity(o);
                if (entity == null) return null; // Doesn't exist

                // New
                write.acquire();
                checkAndCache(entity);
                write.release();
                write = null;
                return entity;
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while acquiring cache lock", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (read != null) read.release();
            if (write != null) write.release();
        }

        try {
            if (cacheInfo.timestamp + maxAge < System.currentTimeMillis()) {
                // Time for a version check (getVersion() always goes to the database)
                Integer currentVersion = getVersion(o);
                if (currentVersion == null) {
                    // BALEETED
                    write.acquire();
                    cacheRemove(cacheInfo.entity);
                    cacheInfo = null;
                    write.release();
                    write = null;
                    return null;
                } else if (currentVersion.intValue() != cacheInfo.version) {
                    // Updated
                    entity = findEntity(o);
                    write.acquire();
                    cacheInfo = checkAndCache(entity);
                    write.release();
                    write = null;
                }
            }

            return cacheInfo.entity;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while acquiring cache lock", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (read != null) read.release();
            if (write != null) write.release();
        }
    }

    protected void cacheRemove(Entity thing) {
        final Long oid = new Long(thing.getOid());
        cache.remove(oid);
        removedFromCache(thing);
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

    protected CacheInfo checkAndCache(Entity thing) throws CacheVeto {
        final Long oid = new Long(thing.getOid());
        checkCachable(thing);

        CacheInfo info = (CacheInfo)cache.get(oid);
        if (info == null) {
            info = new CacheInfo();
            cache.put(oid, info);
        }

        info.entity = thing;
        info.version = thing.getVersion();
        info.timestamp = System.currentTimeMillis();

        addedToCache(thing);

        return info;
    }


    /**
     * Retrieve the persistent object instance by it's primary key (object id). If the
     * object is not found returns <code>null</code>
     *
     * @param impClass the oobject class
     * @param oid      the object id
     * @return the object instance or <code>null</code> if no instance has been found
     * @throws FindException if there was an data access error
     */
    protected Object findByPrimaryKey(Class impClass, long oid) throws FindException {
        try {
            return getHibernateTemplate().load(impClass, new Long(oid));
        } catch (DataAccessException e) {
            if (ExceptionUtils.causedBy(e, net.sf.hibernate.ObjectNotFoundException.class)) {
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
            String deleteQuery = "from temp in class " +
              entityClass.getName() +
              " where temp.oid = ?";
            return getHibernateTemplate().delete(deleteQuery, new Long(oid), Hibernate.LONG) == 1;
        } catch (DataAccessException he) {
            throw new DeleteException(he.toString(), he);
        }
    }


    protected final Logger logger = Logger.getLogger(getClass().getName());

    private ReadWriteLock cacheLock = new ReaderPreferenceReadWriteLock();
    private Map cache = new HashMap();

}
