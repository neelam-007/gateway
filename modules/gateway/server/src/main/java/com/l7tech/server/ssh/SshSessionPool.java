package com.l7tech.server.ssh;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This extends GenericKeyedObjectPool so that we can add property changes listeners and an initializer.
 *
 * @author Victor Kazakov
 */
public class SshSessionPool extends GenericKeyedObjectPool implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(SshSessionPool.class.getName());

    // PROPERTIES
    public static final String SSH_SESSION_POOL_MAX_ACTIVE = "ssh.session.pool.maxActive";
    public static final String SSH_SESSION_POOL_WHEN_EXHAUSTED_ACTION = "ssh.session.pool.whenExhaustedAction";
    public static final String SSH_SESSION_POOL_MAX_WAIT = "ssh.session.pool.maxWait";
    public static final String SSH_SESSION_POOL_MAX_IDLE = "ssh.session.pool.maxIdle";
    public static final String SSH_SESSION_POOL_MAX_TOTAL = "ssh.session.pool.maxTotal";
    public static final String SSH_SESSION_POOL_MIN_IDLE = "ssh.session.pool.minIdle";
    public static final String SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS = "ssh.session.pool.timeBetweenEvictionRunsMillis";
    public static final String SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN = "ssh.session.pool.numTestsPerEvictionRun";
    public static final String SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS = "ssh.session.pool.minEvictableIdleTimeMillis";

    // DEFAULTS
    public static final int DEFAULT_SSH_SESSION_POOL_MAX_ACTIVE = 10;
    public static final String DEFAULT_SSH_SESSION_POOL_WHEN_EXHAUSTED_ACTION = "FAIL";
    public static final int DEFAULT_SSH_SESSION_POOL_MAX_WAIT = 60000;
    public static final int DEFAULT_SSH_SESSION_POOL_MAX_IDLE = -1;
    public static final int DEFAULT_SSH_SESSION_POOL_MAX_TOTAL = -1;
    public static final int DEFAULT_SSH_SESSION_POOL_MIN_IDLE = 1;
    public static final long DEFAULT_SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS = 30 * 60 * 1000L;
    public static final int DEFAULT_SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN = -1;
    public static final long DEFAULT_SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS = 10 * 60 * 1000L;

    private com.l7tech.util.Config config;

    public SshSessionPool(SshSessionFactory factory, com.l7tech.util.Config config) {
        super(factory,
                config.getIntProperty(SSH_SESSION_POOL_MAX_ACTIVE, DEFAULT_SSH_SESSION_POOL_MAX_ACTIVE),
                getWhenExhaustedAction(config),
                config.getLongProperty(SSH_SESSION_POOL_MAX_WAIT, DEFAULT_SSH_SESSION_POOL_MAX_WAIT),
                config.getIntProperty(SSH_SESSION_POOL_MAX_IDLE, DEFAULT_SSH_SESSION_POOL_MAX_IDLE),
                config.getIntProperty(SSH_SESSION_POOL_MAX_TOTAL, DEFAULT_SSH_SESSION_POOL_MAX_TOTAL),
                config.getIntProperty(SSH_SESSION_POOL_MIN_IDLE, DEFAULT_SSH_SESSION_POOL_MIN_IDLE), true, true,
                config.getLongProperty(SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS, DEFAULT_SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS),
                config.getIntProperty(SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN, DEFAULT_SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN),
                config.getLongProperty(SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, DEFAULT_SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS),
                false);
        this.config = config;
    }

    private static byte getWhenExhaustedAction(final com.l7tech.util.Config config) {
        final String action = config.getProperty(SSH_SESSION_POOL_WHEN_EXHAUSTED_ACTION, DEFAULT_SSH_SESSION_POOL_WHEN_EXHAUSTED_ACTION);
        switch (action) {
            case "FAIL":
                return GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL;
            case "GROW":
                return GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW;
            case "BLOCK":
            default:
                return GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        synchronized (this) {
            switch (evt.getPropertyName()) {
                case SSH_SESSION_POOL_MAX_ACTIVE:
                    setMaxActive(config.getIntProperty(SSH_SESSION_POOL_MAX_ACTIVE, DEFAULT_SSH_SESSION_POOL_MAX_ACTIVE));
                    break;
                case SSH_SESSION_POOL_WHEN_EXHAUSTED_ACTION:
                    setWhenExhaustedAction(getWhenExhaustedAction(config));
                    break;
                case SSH_SESSION_POOL_MAX_WAIT:
                    setMaxWait(config.getLongProperty(SSH_SESSION_POOL_MAX_WAIT, DEFAULT_SSH_SESSION_POOL_MAX_WAIT));
                    break;
                case SSH_SESSION_POOL_MAX_IDLE:
                    setMaxIdle(config.getIntProperty(SSH_SESSION_POOL_MAX_IDLE, DEFAULT_SSH_SESSION_POOL_MAX_IDLE));
                    break;
                case SSH_SESSION_POOL_MAX_TOTAL:
                    setMaxTotal(config.getIntProperty(SSH_SESSION_POOL_MAX_TOTAL, DEFAULT_SSH_SESSION_POOL_MAX_TOTAL));
                    break;
                case SSH_SESSION_POOL_MIN_IDLE:
                    /**
                     * Note the min idle property does not work as expected in the GenericKeyedObjectPool. It gets checked after an eviction run is made.
                     * If during the eviction run all sessions for a given key are evicted the key will be removed from the pool and the min idle will not apply to that key as it will no longer be in the pool.
                     */
                    setMinIdle(config.getIntProperty(SSH_SESSION_POOL_MIN_IDLE, DEFAULT_SSH_SESSION_POOL_MIN_IDLE));
                    break;
                case SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS:
                    setTimeBetweenEvictionRunsMillis(config.getLongProperty(SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS, DEFAULT_SSH_SESSION_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS));
                    break;
                case SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN:
                    setNumTestsPerEvictionRun(config.getIntProperty(SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN, DEFAULT_SSH_SESSION_POOL_NUM_TESTS_PER_EVICTION_RUN));
                    break;
                case SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS:
                    setMinEvictableIdleTimeMillis(config.getLongProperty(SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, DEFAULT_SSH_SESSION_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS));
                    break;
            }
        }
    }

    @Override
    public Object borrowObject(Object key) throws Exception {
        logger.log(Level.FINE, "Borrowing session for key: {0}", key.toString());
        final Object session = super.borrowObject(key);
        logger.log(Level.FINER, "Total active = {0}, Total idle = {1}, Total active for key = {2}, Total idle for key = {3}, Key {4}", new Object[]{this.getNumActive(), this.getNumIdle(), this.getNumActive(key), this.getNumIdle(key), key});
        return session;
    }

    @Override
    public void returnObject(Object key, Object session) throws Exception {
        logger.log(Level.FINE, "Returning session for key: {0}", key.toString());
        super.returnObject(key, session);
        logger.log(Level.FINER, "Total active = {0}, Total idle = {1}, Total active for key = {2}, Total idle for key = {3}, Key {4}", new Object[]{this.getNumActive(), this.getNumIdle(), this.getNumActive(key), this.getNumIdle(key), key});
    }
}
