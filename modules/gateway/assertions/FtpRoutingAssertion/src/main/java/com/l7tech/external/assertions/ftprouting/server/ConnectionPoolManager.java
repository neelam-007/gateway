package com.l7tech.external.assertions.ftprouting.server;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * @author nilic
 */
public class ConnectionPoolManager {

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ConnectionPoolManager.class.getName());

    public static final int DEFAULT_BINDING_TIMEOUT = 30000;
    private static final int MIN_BINDING_TIMEOUT = 100;
    private static final int MAX_BINDING_TIMEOUT = 600000;

    public int bindingTimeout;
    public ReadWriteLock lock;
    public ThreadLocal<ThreadLocalInfo> info;

    /**
     * Create a manager with the default binding timeout / max age and default
     * stale connection cleanup.
     */
    public ConnectionPoolManager() {
        bindingTimeout = DEFAULT_BINDING_TIMEOUT;
        lock = new ReentrantReadWriteLock();
        info = new ThreadLocal<>();
    }

    /**
     * Get the binding timeout (milliseconds)
     *
     * <p>This is the per-usage limit for reuse of a bound Ftp Connection.</p>
     *
     * @return The current timeout
     */
    public int getBindingTimeout() {
        return bindingTimeout;
    }


    /**
     * Get the id that is in use for this thread.
     *
     * @return The identity of the current user (may be null).
     */
    public Object getId() {
        final ThreadLocalInfo tli = getInfo();
        return tli==null ?  null : tli.getId();
    }


    public ThreadLocalInfo getInfo() {
        return info.get();
    }


    /**
     * Set the binding timeout (milliseconds)
     *
     * <p>The minimum permitted value is 100ms the maximum is 300000ms (5 minutes)</p>
     *
     * <p>If the value given is out of range it is set to the nearest limit (and
     * a warning is logged).</p>
     *
     * @param bindingTimeout The timeout to use.
     */
    public void setBindingTimeout( int bindingTimeout) {
        if(bindingTimeout < MIN_BINDING_TIMEOUT) {
            logger.warning("Invalid binding timeout '"+bindingTimeout+"' using '"+MIN_BINDING_TIMEOUT+"' milliseconds.");
            bindingTimeout = MIN_BINDING_TIMEOUT;
        }
        else if(bindingTimeout > MAX_BINDING_TIMEOUT) {
            logger.warning("Invalid binding timeout '"+bindingTimeout+"' using '"+MAX_BINDING_TIMEOUT+"' milliseconds.");
            bindingTimeout = MAX_BINDING_TIMEOUT;
        }

        this.bindingTimeout = bindingTimeout;
    }

    /**
     * Holder for Ftp Connection data
     */
    public class Info  {
        public final Object syncLock = new Object();
        private final long allocationTime;
        private long lastUsageTime;
        public boolean inUse;
        public boolean disposed;

        public Info() {
            this.allocationTime = System.currentTimeMillis();
            this.lastUsageTime = this.allocationTime;
            this.inUse = true;
            this.disposed = false;
        }

        public long getAllocationTime() {
            return allocationTime;
        }

        public long getLastUsageTime() {
            return lastUsageTime;
        }

        public boolean isInUse() {
            return inUse;
        }

        public boolean isDisposed() {
            return disposed;
        }

        public void wrap() {
            synchronized ( syncLock ) {
                this.inUse = true;
                this.lastUsageTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Holder for ThreadLocal data
     */
    public static final class ThreadLocalInfo {
        private final Object id;
        private Boolean bind;

        public ThreadLocalInfo( final Object id ) {
            this.id = id;
        }

        public Object getId() {
            return id;
        }

        public void clearBindingStatus() {
            bind = null;
        }

        public boolean hasBindingStatus() {
            return bind != null;
        }

        public boolean bindingRequested() {
            return bind == Boolean.TRUE;
        }

        public boolean isBound() {
            return bind == Boolean.FALSE;
        }

        /**
         * If already bound just ignore the bind call.
         */
        public void bind() {
            if (bind == null) {
                bind = Boolean.TRUE;
            }
        }

        public void bound() {
            bind = Boolean.FALSE;
        }
    }
}
