package com.l7tech.server;

import com.l7tech.objectmodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
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
     * @throws com.l7tech.objectmodel.FindException
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
            Map<Goid, Integer> dbversions;

            // get db versions
            try {
                dbversions = manager.findVersionMap();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "error getting versions. " +
                  "this version check is stopping prematurely", e);
                return;
            }

            // actual check logic
            List<Goid> creates = new ArrayList<Goid>();
            List<Goid> updates = new ArrayList<Goid>();
            List<Goid> deletions = new ArrayList<Goid>();

            // 1. check that all that is in db is present in cache and that version is same
            for (Goid goid : dbversions.keySet()) {
                // is it already in cache?
                Integer cachedVersion = cachedVersionMap.get(goid);

                if (cachedVersion == null) {
                    logger.fine("Entity " + goid + " is new.");
                    creates.add(goid);
                } else {
                    // check actual version
                    Integer dbversion = dbversions.get(goid);

                    if (!dbversion.equals(cachedVersion)) {
                        updates.add(goid);
                        logger.fine("Entity " + goid + " has been updated.");
                    }
                }
            }

            // 2. check for things in cache not in db (deletions)
            for (Goid goid : cachedVersionMap.keySet()) {
                if (dbversions.get(goid) == null) {
                    deletions.add(goid);
                    logger.fine("Entity " + goid + " has been deleted.");
                }
            }

            // 3. make the updates
            if (creates.isEmpty() && updates.isEmpty() && deletions.isEmpty()) {
                // nothing to do. we're done
            } else {
                for (Goid createdGoid : creates) {
                    if (checkAddOrUpdate(createdGoid, 0)) {
                        PersistentEntity createdEntity;
                        try {
                            createdEntity = manager.findByPrimaryKey(createdGoid);
                        } catch (FindException e) {
                            createdEntity = null;
                            logger.log(Level.WARNING, "Entity that was created cannot be retrieved", e);
                        }
                        if (createdEntity != null) {
                            create(createdEntity);
                        } // otherwise, next version check shall delete this service from cache
                    }
                }

                for (Goid updatedGoid : updates) {
                    Integer newVersion = dbversions.get(updatedGoid);
                    if (checkAddOrUpdate(updatedGoid, newVersion)) {
                        PersistentEntity updatedEntity;
                        try {
                            updatedEntity = manager.findByPrimaryKey(updatedGoid);
                        } catch (FindException e) {
                            updatedEntity = null;
                            logger.log(Level.WARNING, "Entity that was updated cannot be retrieved", e);
                        }
                        if (updatedEntity != null) {
                            update(updatedEntity);
                        } // otherwise, next version check shall delete this service from cache
                    }
                }
                
                for (Goid key : deletions) {
                    remove(key);
                }
            }
        } catch(Exception e) {
            //e.g. DataAccessResourceFailureException, CannotCreateTransactionException
            logger.log(Level.WARNING, "Error checking version information.", e);
        }
    }

    public void markObjectAsStale(Goid goid) {
        cachedVersionMap.remove(goid);
    }

    private void remove(Goid goid) {
        cachedVersionMap.remove(goid);
        onDelete(goid);
    }

    private boolean checkAddOrUpdate(Goid goid, Integer version) {
        boolean loadEntity = preSave(goid, version.intValue());
        if(!loadEntity) {
            cachedVersionMap.put(goid, version); // If not loading then update now
        }
        return loadEntity;
    }

    private void update(PersistentEntity updatedEntity) {
        cachedVersionMap.put(updatedEntity.getGoid(), new Integer(updatedEntity.getVersion()));
        onUpdate(updatedEntity);
    }

    private void create(PersistentEntity createdEntity) {
        cachedVersionMap.put(createdEntity.getGoid(), createdEntity.getVersion());
        onCreate(createdEntity);
    }

    /**
     * Override to be notified of deleted entities.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param removedGoid The oid of the deleted Entity
     */
    protected abstract void onDelete(Goid removedGoid);

    /**
     * Override to receive notification of updated entities.
     *
     * <p>If this method returns true then onSave will be called
     * with the updated Entity.</p>
     *
     * <p>This implementation returns true.</p>
     *
     * @param updatedGoid     The id of the updated Entity
     * @param updatedVersion The version number of the updated Entity
     * @return true if the entity data is required
     */
    protected boolean preSave(Goid updatedGoid, int updatedVersion) {
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
     * @param updatedGoid The updated entity
     * @param version The latest known version
     * @return true if this notification is news to us
     */
    protected boolean notifyUpdate( Goid updatedGoid, int version ) {
        Integer previousVersion = cachedVersionMap.put( updatedGoid, version );
        return previousVersion == null || previousVersion != version;
    }

    /**
     * Notify the task that the given entity deletion is known.
     *
     * <p>This can be used to prevent duplicate event notifications.</p>
     *
     * @param deletedGoid The deleted entity
     * @return true if this notification is news to us
     */
    protected boolean notifyDelete( Goid deletedGoid ) {
        return cachedVersionMap.remove( deletedGoid ) != null;
    }

    public long getFrequency() {
        return DEFAULT_FREQUENCY;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private EntityManager manager;
    private Map<Goid, Integer> cachedVersionMap;
    private static final long DEFAULT_FREQUENCY = 4 * 1000;
}
