package com.l7tech.server;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HibernateEntityManager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link java.util.TimerTask} that periodically checks for the creation, deletion
 * or update of persistent entities.
 * <p/>
 * This is a generic version of functionality that was previously implemented in
 * {@link com.l7tech.server.service.ServiceCache}.  ServiceCache should extend this class
 * and implement its abstract methods.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class PeriodicVersionCheck extends TimerTask {

    /**
     * Loads an initial version map automatically.
     * <p/>
     * Don't rely on this class to "discover" pre-existing entities anymore!
     *
     * @param manager
     * @throws FindException
     */
    public PeriodicVersionCheck(HibernateEntityManager manager) throws FindException {
        _manager = manager;
        _cachedVersionMap = manager.findVersionMap();
    }

    /**
     * todo: review whether to make into transactional checker
     */
    public synchronized void run() {

        Map dbversions = null;

        // get db versions
        try {
            dbversions = _manager.findVersionMap();
        } catch (FindException e) {
            logger.log(Level.SEVERE, "error getting versions. " +
              "this version check is stopping prematurely", e);
            return;
        }

        // actual check logic
        List updatesAndAdditions = new ArrayList();
        List deletions = new ArrayList();

        // 1. check that all that is in db is present in cache and that version is same
        for (Iterator i = dbversions.keySet().iterator(); i.hasNext();) {
            Long oid = (Long)i.next();

            // is it already in cache?
            Integer cachedVersion = (Integer)_cachedVersionMap.get(oid);

            if (cachedVersion == null) {
                logger.fine("Entity " + oid + " is new.");
                updatesAndAdditions.add(oid);
            } else {
                // check actual version
                Integer dbversion = (Integer)dbversions.get(oid);

                if (!dbversion.equals(cachedVersion)) {
                    updatesAndAdditions.add(oid);
                    logger.fine("Entity " + oid + " has been updated.");
                }
            }
        }

        // 2. check for things in cache not in db (deletions)
        for (Iterator i = _cachedVersionMap.keySet().iterator(); i.hasNext();) {
            Long oid = (Long)i.next();
            if (dbversions.get(oid) == null) {
                deletions.add(oid);
                logger.fine("Entity " + oid + " has been deleted.");
            }
        }

        // 3. make the updates
        if (updatesAndAdditions.isEmpty() && deletions.isEmpty()) {
            // nothing to do. we're done
        } else {
            for (Iterator i = updatesAndAdditions.iterator(); i.hasNext();) {
                Long updatedOid = (Long)i.next();
                Entity updatedEntity = null;
                try {
                    updatedEntity = _manager.findEntity(updatedOid.longValue());
                } catch (FindException e) {
                    updatedEntity = null;
                    logger.log(Level.WARNING, "Entity that was updated or created " +
                      "cannot be retrieved", e);
                }
                if (updatedEntity != null) {
                    addOrUpdate(updatedEntity);
                } // otherwise, next version check shall delete this service from cache
            }
            for (Iterator i = deletions.iterator(); i.hasNext();) {
                Long key = (Long)i.next();
                remove(key);
            }
        }
    }

    public void markObjectAsStale(Long oid) {
        _cachedVersionMap.remove(oid);
    }

    private void remove(Long oid) {
        _cachedVersionMap.remove(oid);
        onDelete(oid.longValue());
    }

    private void addOrUpdate(Entity updatedEntity) {
        _cachedVersionMap.put(new Long(updatedEntity.getOid()), new Integer(updatedEntity.getVersion()));
        onSave(updatedEntity);
    }

    protected abstract void onDelete(long removedOid);

    protected abstract void onSave(Entity updatedEntity);

    public long getFrequency() {
        return DEFAULT_FREQUENCY;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private HibernateEntityManager _manager;
    private Map _cachedVersionMap;
    private static final long DEFAULT_FREQUENCY = 4 * 1000;
}
