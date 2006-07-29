package com.l7tech.server;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

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
 * @author alex, $Author$
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
    public PeriodicVersionCheck(EntityManager manager) throws FindException {
        this.manager = manager;
        cachedVersionMap = manager.findVersionMap();
    }

    /**
     * todo: review whether to make into transactional checker
     */
    public synchronized void run() {

        Map<Long, Integer> dbversions;

        // get db versions
        try {
            dbversions = manager.findVersionMap();
        } catch (FindException e) {
            logger.log(Level.SEVERE, "error getting versions. " +
              "this version check is stopping prematurely", e);
            return;
        }

        // actual check logic
        List<Long> updatesAndAdditions = new ArrayList<Long>();
        List<Long> deletions = new ArrayList<Long>();

        // 1. check that all that is in db is present in cache and that version is same
        for (Long oid : dbversions.keySet()) {
            // is it already in cache?
            Integer cachedVersion = cachedVersionMap.get(oid);

            if (cachedVersion == null) {
                logger.fine("Entity " + oid + " is new.");
                updatesAndAdditions.add(oid);
            } else {
                // check actual version
                Integer dbversion = dbversions.get(oid);

                if (!dbversion.equals(cachedVersion)) {
                    updatesAndAdditions.add(oid);
                    logger.fine("Entity " + oid + " has been updated.");
                }
            }
        }

        // 2. check for things in cache not in db (deletions)
        for (Long oid : cachedVersionMap.keySet()) {
            if (dbversions.get(oid) == null) {
                deletions.add(oid);
                logger.fine("Entity " + oid + " has been deleted.");
            }
        }

        // 3. make the updates
        if (updatesAndAdditions.isEmpty() && deletions.isEmpty()) {
            // nothing to do. we're done
        } else {
            for (Long updatedOid : updatesAndAdditions) {
                Integer newVersion = dbversions.get(updatedOid);
                if (checkAddOrUpdate(updatedOid, newVersion)) {
                    Entity updatedEntity;
                    try {
                        updatedEntity = manager.findEntity(updatedOid.longValue());
                    } catch (FindException e) {
                        updatedEntity = null;
                        logger.log(Level.WARNING, "Entity that was updated or created " +
                                "cannot be retrieved", e);
                    }
                    if (updatedEntity != null) {
                        addOrUpdate(updatedEntity);
                    } // otherwise, next version check shall delete this service from cache
                }
            }
            for (Long key : deletions) {
                remove(key);
            }
        }
    }

    public void markObjectAsStale(Long oid) {
        cachedVersionMap.remove(oid);
    }

    private void remove(Long oid) {
        cachedVersionMap.remove(oid);
        onDelete(oid.longValue());
    }

    private boolean checkAddOrUpdate(Long oid, Integer version) {
        boolean loadEntity = preSave(oid.longValue(), version.intValue());
        if(!loadEntity) {
            cachedVersionMap.put(oid, version); // If not loading then update now
        }
        return loadEntity;
    }

    private void addOrUpdate(Entity updatedEntity) {
        cachedVersionMap.put(new Long(updatedEntity.getOid()), new Integer(updatedEntity.getVersion()));
        onSave(updatedEntity);
    }


    /**
     * Override to be notified of deleted entities.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param removedOid The oid of the deleted Entity
     */
    protected abstract void onDelete(long removedOid);

    /**
     * Override to receive notification of updated entities.
     *
     * <p>If this method returns true then onSave will be called
     * with the updated Entity.</p>
     *
     * <p>This implementation returns true.</p>
     *
     * @param updatedOid     The id of the updated Entity
     * @param updatedVersion The version number of the updated Entity
     * @return true if the entity data is required
     */
    protected boolean preSave(long updatedOid, int updatedVersion) {
        return true;
    }

    /**
     * Override to be passed updated entities.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param updatedEntity the updated Entity
     */
    protected void onSave(Entity updatedEntity) {}

    public long getFrequency() {
        return DEFAULT_FREQUENCY;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private EntityManager manager;
    private Map<Long, Integer> cachedVersionMap;
    private static final long DEFAULT_FREQUENCY = 4 * 1000;
}
