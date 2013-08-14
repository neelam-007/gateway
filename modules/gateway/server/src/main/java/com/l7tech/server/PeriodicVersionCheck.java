package com.l7tech.server;

import com.l7tech.objectmodel.PersistentEntity;
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
@Deprecated
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
        try {
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
            List<Long> creates = new ArrayList<Long>();
            List<Long> updates = new ArrayList<Long>();
            List<Long> deletions = new ArrayList<Long>();

            // 1. check that all that is in db is present in cache and that version is same
            for (Long oid : dbversions.keySet()) {
                // is it already in cache?
                Integer cachedVersion = cachedVersionMap.get(oid);

                if (cachedVersion == null) {
                    logger.fine("Entity " + oid + " is new.");
                    creates.add(oid);
                } else {
                    // check actual version
                    Integer dbversion = dbversions.get(oid);

                    if (!dbversion.equals(cachedVersion)) {
                        updates.add(oid);
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
            if (creates.isEmpty() && updates.isEmpty() && deletions.isEmpty()) {
                // nothing to do. we're done
            } else {
                for (Long createdOid : creates) {
                    if (checkAddOrUpdate(createdOid, 0)) {
                        PersistentEntity createdEntity;
                        try {
                            createdEntity = manager.findByPrimaryKey(createdOid);
                        } catch (FindException e) {
                            createdEntity = null;
                            logger.log(Level.WARNING, "Entity that was created cannot be retrieved", e);
                        }
                        if (createdEntity != null) {
                            create(createdEntity);
                        } // otherwise, next version check shall delete this service from cache
                    }
                }

                for (Long updatedOid : updates) {
                    Integer newVersion = dbversions.get(updatedOid);
                    if (checkAddOrUpdate(updatedOid, newVersion)) {
                        PersistentEntity updatedEntity;
                        try {
                            updatedEntity = manager.findByPrimaryKey(updatedOid.longValue());
                        } catch (FindException e) {
                            updatedEntity = null;
                            logger.log(Level.WARNING, "Entity that was updated cannot be retrieved", e);
                        }
                        if (updatedEntity != null) {
                            update(updatedEntity);
                        } // otherwise, next version check shall delete this service from cache
                    }
                }
                
                for (Long key : deletions) {
                    remove(key);
                }
            }
        } catch(Exception e) {
            //e.g. DataAccessResourceFailureException, CannotCreateTransactionException
            logger.log(Level.WARNING, "Error checking version information.", e);
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

    private void update(PersistentEntity updatedEntity) {
        cachedVersionMap.put(new Long(updatedEntity.getOid()), new Integer(updatedEntity.getVersion()));
        onUpdate(updatedEntity);
    }

    private void create(PersistentEntity createdEntity) {
        cachedVersionMap.put(createdEntity.getOid(), createdEntity.getVersion());
        onCreate(createdEntity);
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
    protected void onUpdate(PersistentEntity updatedEntity) {}

    /**
     * Override to be passed created entities.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param createdEntity the created Entity
     */
    protected void onCreate(PersistentEntity createdEntity) {}


    /**
     * Notify the task that the given entity update is known.
     *
     * <p>This can be used to prevent duplicate event notifications.</p>
     *
     * @param updatedOid The updated entity
     * @param version The latest known version
     * @return true if this notification is news to us
     */
    protected boolean notifyUpdate( long updatedOid, int version ) {
        Integer previousVersion = cachedVersionMap.put( updatedOid, version );
        return previousVersion == null || previousVersion != version;
    }

    /**
     * Notify the task that the given entity deletion is known.
     *
     * <p>This can be used to prevent duplicate event notifications.</p>
     *
     * @param deletedOid The deleted entity
     * @return true if this notification is news to us
     */
    protected boolean notifyDelete( long deletedOid ) {
        return cachedVersionMap.remove( deletedOid ) != null;      
    }

    public long getFrequency() {
        return DEFAULT_FREQUENCY;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private EntityManager manager;
    private Map<Long, Integer> cachedVersionMap;
    private static final long DEFAULT_FREQUENCY = 4 * 1000;
}
