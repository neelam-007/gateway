package com.l7tech.server;

import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link java.util.TimerTask} that periodically checks for the creation, deletion
 * or update of persistent entities.
 *
 * This is a generic version of functionality that was previously implemented in
 * {@link com.l7tech.service.ServiceCache}.  ServiceCache should extend this class
 * and implement its abstract methods.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class PeriodicVersionCheck extends TimerTask {
    public PeriodicVersionCheck( HibernateEntityManager manager ) {
        _manager = manager;
    }

    public synchronized void run() {
        PersistenceContext context = null;

        try {
            context = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting persistence context. " +
                                     "this version check is stopping prematurely", e);
            return;
        }

        try {
            Map dbversions = null;

            try {
                context.beginTransaction();
            } catch (TransactionException e) {
                logger.log(Level.SEVERE, "error begining transaction. " +
                                         "this version check is stopping prematurely", e);
                return;
            }

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
                    logger.fine( "Entity " + oid + " is new." );
                    updatesAndAdditions.add(oid);
                } else {
                    // check actual version
                    Integer dbversion = (Integer)dbversions.get(oid);

                    if ( !dbversion.equals(cachedVersion) ) {
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

            // close hib transaction
            try {
                context.rollbackTransaction();
            } catch (TransactionException e) {
                logger.log(Level.WARNING, "error rollbacking transaction", e);
            }
        } finally {
            context.close();
        }
    }

    public void markObjectAsStale( Long oid ) {
        _cachedVersionMap.remove( oid );
    }

    private void remove( Long oid ) {
        _cachedVersionMap.remove( oid );
        onDelete( oid.longValue() );
    }

    private void addOrUpdate( Entity updatedEntity ) {
        _cachedVersionMap.put( new Long( updatedEntity.getOid() ), new Integer( updatedEntity.getVersion() ) );
        onSave( updatedEntity );
    }

    protected abstract void onDelete( long removedOid );
    protected abstract void onSave( Entity updatedEntity );

    public long getFrequency() {
        return DEFAULT_FREQUENCY;
    }

    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private final HibernateEntityManager _manager;
    private final Map _cachedVersionMap = new HashMap();
    private static final long DEFAULT_FREQUENCY = 4 * 1000;
}
