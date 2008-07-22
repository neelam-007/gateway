package com.l7tech.common.http.prov.apache;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.WaitFreeQueue;

import com.l7tech.util.ShutdownExceptionHandler;

/**
 * HttpConnectionManager that eliminates connection pool contention with a thread local cache.
 *
 * @author Steve Jones
 */
public class CachingHttpConnectionManager extends StaleCheckingHttpConnectionManager {

    //- PUBLIC

    /**
     * Get a connection, use one from the thread local cache if available.
     */
    public HttpConnection getConnectionWithTimeout(HostConfiguration hostConfiguration, long timeout) throws ConnectionPoolTimeoutException {
        CachedConnectionInfo cachedConnectionInfo = getCachedConnectionInfo(hostConfiguration);

        if (cachedConnectionInfo == null) {
            HttpConnection httpConnection = super.getConnectionWithTimeout(hostConfiguration, timeout);
            cachedConnectionInfo = setCachedConnectionInfo(hostConfiguration, httpConnection);
        }

        return new HttpConnectionWrapper(cachedConnectionInfo.httpConnection, cachedConnectionInfo);
    }

    /**
     * Get a connection, use one from the thread local cache if available.
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration) {
        CachedConnectionInfo cachedConnectionInfo = getCachedConnectionInfo(hostConfiguration);

        if (cachedConnectionInfo == null) {
            HttpConnection httpConnection = super.getConnection(hostConfiguration);
            cachedConnectionInfo = setCachedConnectionInfo(hostConfiguration, httpConnection);
        }

        return new HttpConnectionWrapper(cachedConnectionInfo.httpConnection, cachedConnectionInfo);
    }

    /**
     * Release the connection (goes back to the pool).
     */
    public void releaseConnection(HttpConnection conn) {
        super.releaseConnection(unwrap(conn));
    }

    //- PRIVATE

    /**
     * Logger for the class
     */
    private static final Logger logger = Logger.getLogger(CachingHttpConnectionManager.class.getName());

    /**
     *
     */
    private static final long CACHE_TIME = 1000L;

    /**
     * Queue into which connections to re-pool are dropped (non-blocking)
     */
    private static final Channel timeoutQueue = new WaitFreeQueue();

    // Create and start the timer daemon
    static {
        Thread timer = new Thread(new CachedConnectionCleanup());
        timer.setDaemon(true);
        timer.setName("CachedHttpConnectionCleanupThread");
        timer.setUncaughtExceptionHandler(ShutdownExceptionHandler.getInstance());
        timer.start();
    }

    /**
     *
     */
    private ThreadLocal localConnection = new ThreadLocal();

    /**
     * Get the cached connection info if it matches the given host configuration.
     *
     * This will release the http connection if it cannot be reused.
     */
    private CachedConnectionInfo getCachedConnectionInfo(HostConfiguration hostConfiguration) {
        CachedConnectionInfo connectionInfoFromCache = (CachedConnectionInfo) localConnection.get();
        CachedConnectionInfo cachedConnectionInfo = null;

        if (connectionInfoFromCache != null && connectionInfoFromCache.consume()) {
            // now we own the connection
            if (!connectionInfoFromCache.hostConfiguration.equals(hostConfiguration)) {
                // release connection since we don't want it
                connectionInfoFromCache.httpConnection.releaseConnection();
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Releasing cached HttpConnection");
            } else if (connectionInfoFromCache.httpConnection.isOpen()) {
                // create new connection info if usable
                cachedConnectionInfo = new CachedConnectionInfo(connectionInfoFromCache);
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Using cached HttpConnection");
            }
            // else already closed by another thread
        }

        localConnection.set(cachedConnectionInfo);

        return cachedConnectionInfo;
    }

    /**
     * Cache the given http connection / host configuration
     */
    private CachedConnectionInfo setCachedConnectionInfo(HostConfiguration hostConfiguration, HttpConnection httpConnection) {
        CachedConnectionInfo cachedConnectionInfo = new CachedConnectionInfo(hostConfiguration, httpConnection);
        localConnection.set(cachedConnectionInfo);
        return cachedConnectionInfo;
    }

    /**
     * Remove the wrapper from an http connection
     */
    private HttpConnection unwrap(HttpConnection httpConnection) {
        while (httpConnection instanceof HttpConnectionWrapper &&
            ((HttpConnectionWrapper)httpConnection).getConnectionListener() instanceof CachedConnectionInfo) {
            httpConnection = ((HttpConnectionWrapper)httpConnection).getWrappedConnection();
        }

        return httpConnection;
    }

    /**
     * Data object for cached connections.
     *
     * These objects are single use and can be "consumed" either by the request
     * handler thread or the cleanup thread
     */
    private final class CachedConnectionInfo implements HttpConnectionWrapper.ConnectionListener {
        private final HostConfiguration hostConfiguration;
        private final HttpConnection httpConnection;
        private final Object consmedLock = new Object();
        private final long created;
        private boolean consumed;

        CachedConnectionInfo(HostConfiguration hostConfiguration, HttpConnection httpConnection) {
            this.hostConfiguration = new HostConfiguration(hostConfiguration);
            this.httpConnection = httpConnection;
            this.created = System.currentTimeMillis();
            this.consumed = false;
        }

        CachedConnectionInfo(CachedConnectionInfo cachedConnectionInfo) {
            this(cachedConnectionInfo.hostConfiguration,
                 cachedConnectionInfo.httpConnection);
        }

        public boolean release() {
            boolean release = false;
            try {
                if( httpConnection.isOpen() )
                    timeoutQueue.put(this);
                else
                    release = true;
            }
            catch(InterruptedException ie) {
                release = true;
            }

            if (release) {
                localConnection.set(null);    
            }

            return release; // release underlying connection?
        }

        public void wrap() {
        }

        // attempt to consume the connection
        boolean consume() {
            boolean success = false;
            synchronized(consmedLock) {
                success = !consumed;
                consumed = true;
            }
            return success;
        }

        boolean isConsumed() {
            synchronized(consmedLock) {
                return consumed;
            }
        }
    }

    /**
     * Cleanup task for cached http connections.
     *
     * This reads from the queue of cached connections and is responsible for
     * releasing timed out connections.
     */
    private static final class CachedConnectionCleanup implements Runnable {
        private Set cached = new HashSet(100);

        private CachedConnectionInfo safePoll() throws InterruptedException {
            try {
                return (CachedConnectionInfo) timeoutQueue.poll(0);
            }
            catch(NullPointerException npe) {
                // npe can be thrown on shutdown, pretend it was an interruption
                throw new InterruptedException();
            }
        }

        public void run() {
            logger.info("Cached HttpConnection cleanup thread starting.");
            try {
                while(true) {
                    Thread.sleep(100);

                    // process from queue
                    CachedConnectionInfo cachedConnectionInfo = null;
                    while ((cachedConnectionInfo = safePoll()) != null) {
                        if (!cachedConnectionInfo.isConsumed()) {
                            cached.add(cachedConnectionInfo);
                        }
                    }

                    // process old
                    long time = System.currentTimeMillis();
                    for (Iterator cacheIter = cached.iterator(); cacheIter.hasNext(); ) {
                        cachedConnectionInfo = (CachedConnectionInfo) cacheIter.next();
                        if (cachedConnectionInfo.isConsumed()) {
                            cacheIter.remove();
                        } else if ((time-cachedConnectionInfo.created) > CACHE_TIME) {
                            if (cachedConnectionInfo.consume()) {
                                // release to pool
                                cachedConnectionInfo.httpConnection.releaseConnection();
                                if (logger.isLoggable(Level.FINER))
                                    logger.log(Level.FINER, "Releasing cached HttpConnection (timeout)");
                            } else {
                                // consumed by someone else so remove
                                cacheIter.remove();
                            }
                        }
                    }
                }
            }
            catch(InterruptedException ie) {
                // shutdown
            }
            logger.info("Cached HttpConnection cleanup thread stopping.");
        }
    }
}
