package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.StaleUpdateException;
import com.l7tech.objectmodel.UpdateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Non-blocking only Lock implementation based on one cluster property entry.
 * Only tryLock() and unlock() are implemented.
 *
 * @author jbufu
 */
public class ClusterLock implements Lock {

    private static final Logger logger = Logger.getLogger(ClusterLock.class.getName());

    private final long id = new Random().nextLong(); // todo: come up with a friendlier id type

    private final ClusterPropertyManager clusterPropertyManager;
    protected final PlatformTransactionManager transactionManager; // required for TransactionTemplate

    private final String lockPropertyName;
    private final long staleTimeout; // millisesoncs

    public ClusterLock(ClusterPropertyManager cpm, PlatformTransactionManager transactionManager, String lockProp) {
        this(cpm, transactionManager, lockProp, Long.MAX_VALUE);
    }

    public ClusterLock(ClusterPropertyManager clusterPropertyManager, PlatformTransactionManager transactionManager,
                       String lockPropertyName, long staleMinutes) {
        if (clusterPropertyManager == null)
            throw new NullPointerException("ClusterPropertyManager parameter must not be null.");
        this.clusterPropertyManager = clusterPropertyManager;
        this.transactionManager = transactionManager;
        this.lockPropertyName = lockPropertyName;
        this.staleTimeout = staleMinutes * 60 * 1000;
    }

    public String toString() {
        return lockPropertyName;
    }

    public Condition newCondition() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void lock() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean tryLock() {
        boolean success = false;
        ClusterProperty lockProperty = null;
        ClusterLockEntry lockEntry = null;
        try {
            lockProperty = clusterPropertyManager.getCachedEntityByName(lockPropertyName, 0);
        } catch (FindException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Cluster lock entry for " + lockPropertyName + " not found.", e);
        }

        if (lockProperty == null)
            lockProperty = new ClusterProperty(lockPropertyName, null);
        else
            lockEntry = ClusterLockEntry.parseString(lockProperty.getValue());

        long now = System.currentTimeMillis();
        long age = Long.MIN_VALUE;

        if (lockEntry == null || ! lockEntry.isLocked() || ((age = now - lockEntry.getLockTime()) >= staleTimeout)) {
            if (age >= staleTimeout)
                logger.warning("Stale cluster lock found for " + lockPropertyName + " : " + lockEntry + " ; stealing lock.");

            // attempt to get the lock
            ClusterLockEntry newEntry = new ClusterLockEntry(true, now, id);
            lockProperty.setValue(newEntry.toString());
            if (success = update(lockProperty)) {
                if (logger.isLoggable(Level.FINE)) logger.fine(lockPropertyName + " locked by " + id);
            } else {
                logger.warning(lockPropertyName + " cluster lock update attempt by " + id + " FAILED!");
            }
        } else {
            if (logger.isLoggable(Level.FINE))
                logger.fine(lockPropertyName + " cluster lock update NOT attempted by " + id + "; currently owned by " + lockEntry.getOwnerId());
        }

        if (logger.isLoggable(Level.FINE)) logger.fine("tryLock() returning " + success);
        return success;
    }

    public void unlock() {
        ClusterProperty lockProperty = null;
        ClusterLockEntry lockEntry = null;
        try {
            lockProperty = clusterPropertyManager.getCachedEntityByName(lockPropertyName, 0);
        } catch (FindException e) {
            // if un unlock() is attempted, the property SHOULD be there
            logger.log(Level.WARNING, "Cluster lock entry for " + lockPropertyName + " not found.", e);
        }

        if (lockProperty == null)
            lockProperty = new ClusterProperty(lockPropertyName, null);
        else
            lockEntry = ClusterLockEntry.parseString(lockProperty.getValue());

        if (lockEntry == null || ! lockEntry.isLocked() || ! (lockEntry.getOwnerId() == id)) {
            logger.warning("Can't unlock " + lockPropertyName + "; unexpected entry found: " + lockEntry);
            return;
        }

        // unlock
        ClusterLockEntry newEntry = new ClusterLockEntry(false, System.currentTimeMillis(), id);
        lockProperty.setValue(newEntry.toString());
        if (update(lockProperty)) {
            if (logger.isLoggable(Level.FINE)) logger.fine(lockPropertyName + " unlocked by " + id);
        } else {
            logger.warning("Error unlocking " + lockPropertyName);
        }
    }

    private boolean update(final ClusterProperty clusterProperty) {
        return (Boolean) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                try {
                    clusterPropertyManager.update(clusterProperty);
                    return Boolean.TRUE;
                } catch (StaleUpdateException e) {
                    logger.log(Level.WARNING, "Cluster lock entry modified since last read.");
                    return Boolean.FALSE;
                } catch (UpdateException e) {
                    logger.log(Level.WARNING, "Error updating cluster lock property.", e);
                    return Boolean.FALSE;
                }
            }
        });
    }


    private static class ClusterLockEntry {
        private final boolean locked;
        private final long when;
        private final long owner;

        private ClusterLockEntry(boolean l, long w, long o) {
            locked = l;
            when = w;
            owner = o;
        }

        public boolean isLocked() { return locked; }

        public long getLockTime() { return when; }

        public long getOwnerId() { return owner; }

        /**
         * Returns ClusterLockEntry or null if the provided string is not a valid entry format.
         */
        public static ClusterLockEntry parseString(String entry) {
            if (entry == null) return null;

            String[] parts = entry.split(":");
            if (parts == null || parts.length != 3) {
                logger.warning("Invalid cluster lock entry: " + entry);
                return null;
            }

            boolean locked = Boolean.parseBoolean(parts[0]);
            long when;
            long owner;
            try {
                when = Long.parseLong(parts[1]);
                owner = Long.parseLong(parts[2]);
            } catch (NumberFormatException ne) {
                logger.warning("Invalid cluster lock entry: " + entry);
                return null;
            }
            
            return new ClusterLockEntry(locked, when, owner);

        }

        @Override
        public String toString() {
            return locked + ":" + when + ":" + owner;
        }
    }
}
