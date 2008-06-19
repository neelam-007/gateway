package com.l7tech.server.policy.assertion.jms;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.asynch.JmsTaskBag;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.NamingException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: vchan
 */
public class JmsResourceManager {

    private static final Logger _logger = Logger.getLogger(JmsResourceManager.class.getName());

    /** Singleton instance */
    private static JmsResourceManager _instance;

    // need to store one connection per endpoint
    private ConcurrentHashMap<String, CachedConnection> connectionHolder;

    /** singleton creation mutex */
    private static final Object createLock = new Object();

    /** connectionHolder mutex */
    private final Object syncLock = new Object();

    /** Task that handles the controlled closure of CachedConnections */
    private final ExecutorService closureTask;

    /**
     * Private constructor.
     */
    private JmsResourceManager() {
        this.connectionHolder = new ConcurrentHashMap<String, CachedConnection>();
        this.closureTask = Executors.newSingleThreadExecutor();
    }

   /**
    * Returns the singleton instance of the Jms resource manager.
    *
    * @return the singleton instance
    */
    public static JmsResourceManager getInstance() {

       synchronized(createLock) {
           if (_instance == null)
               _instance = new JmsResourceManager();
           return _instance;
       }
    }

    /**
     * Shutdown the singleton instance of the resource manager.
     */
    public static void shutdown() {

        if (_instance != null) {
            JmsResourceManager tobeClosed = _instance;

            // nullify the old instance
            synchronized(createLock) {
                _instance = null;
            }

            tobeClosed.shutdownInstance();
        }
    }

    /**
     * Returns a JmsBag for the given Jms endpoint.  Each call to this method for the
     * same endpoint will use the same Jms Connection instance with a new Session.
     * <br/>
     * Upon completion, the caller is expected to release the JmsBag resource by calling
     * release(cfg, jmsBag).
     *
     * @param endpoint the endpoint to build the JmsBag for
     * @return new JmsBag instance with a fresh Session
     */
    public JmsBag getJmsBag(JmsEndpointConfig endpoint) throws JmsRuntimeException {

        // check for null?
        CachedConnection cconn;
        final String key = getKey(endpoint);

        synchronized(syncLock) {

            cconn = connectionHolder.get(key);
            if (cconn == null || !cconn.matchVersion(endpoint) || cconn.isStale()) {
                cconn = newConnection(endpoint);
            }
        }

        try {
            return cconn.newTaskBag();

        } catch (JMSException jex) {
            // the Jms Connection may become stale, if a session cannot be created, then
            cconn.stale = true;
            throw new JmsRuntimeException("Unable to create session for endpoint (" + endpoint.getDisplayName() + "), conection might be stale.");
        }
    }


    public void release(JmsEndpointConfig cfg, JmsBag bag, boolean setStale) {

        CachedConnection conn = null;
        try {
            String key = getKey(cfg);
            if (connectionHolder.containsKey(key)) {
                conn = connectionHolder.get(key);

                if (conn.matchVersion(cfg)) {
                    conn.releaseCount++;
                } else {
                    unmatchedRelease.add(
                            new UnmatchedReleaseElement(key, conn.endpointVersion, conn.connectionVersion));
                }
            }
        } finally {
            // close off the session
            if (bag != null)
                bag.close();
            // set stale flag
            if (conn != null && setStale)
                conn.stale = true;
        }
    }

    /**
     * Close all Jms Connection
     *
     */
    private void shutdownInstance() {

        // shutdown any remaining tasks
        closureTask.shutdown();

        // close all regardless
        Collection<CachedConnection> connList = connectionHolder.values();
        for (CachedConnection c : connList) {
            c.close();

            _logger.log(Level.INFO, "Closing Jms connection ({0}), version {1}:{2}", new Object[] {
                    c.name, c.connectionVersion, c.endpointVersion
            });
        }

        if (!closureTask.isTerminated()) {
            closureTask.shutdownNow();
        }

        // remove singleton instance
        connectionHolder.clear();
    }

    /**
     * Build a key based on the endpoint configuration.  Used as the lookup key in the connectionHolder.
     *
     * @param cfg Jms endpoint config
     * @return the key String
     */
    private String getKey(JmsEndpointConfig cfg) {
        StringBuffer sb = new StringBuffer();
        if (cfg != null) {
            sb.append(cfg.getEndpoint().getOid()).append("-").append(cfg.getConnection().getOid());
        }
        return sb.toString();
    }


    private CachedConnection newConnection(JmsEndpointConfig endpoint) throws JmsRuntimeException {

        final String key = getKey(endpoint);

        try {
            // create the new JmsBag for the endpoint
            JmsBag newBag = JmsUtil.connect(endpoint, false, Session.CLIENT_ACKNOWLEDGE);
            newBag.getConnection().start();

            // create new cached connection wrapper
            CachedConnection newConn = new CachedConnection(endpoint, newBag);

            // replace connection if the endpoint already exists
            if (connectionHolder.containsKey(key)) {

                //
                // mark Connection for closure before replacing with new connection
                //
                CachedConnection existingConn = connectionHolder.get(key);
                markForClosure(existingConn);
                connectionHolder.replace(key, existingConn, newConn);

            } else {
                // otherwise, just add to the list
                connectionHolder.put(key, newConn);
            }


            _logger.log(Level.INFO, "New Jms connection created ({0}), version {1}:{2}", new Object[] {
                    newConn.name, newConn.connectionVersion, newConn.endpointVersion
            });

        } catch (JMSException jex) {
            throw new JmsRuntimeException(jex);
        } catch (JmsConfigException cex) {
            throw new JmsRuntimeException(cex);
        } catch (NamingException nex) {
            throw new JmsRuntimeException(nex);
        }

        return connectionHolder.get(key);
    }

    /**
     * Mark the connection for closure.  Use background thread to handle controlled
     * shutdown of the Jms Connection instance.
     *
     * @param oldConn the CachedConnection to shutdown
     */
    private void markForClosure(CachedConnection oldConn) {
        // add to tasklist for closure
        ConnectionClosureTask task = new ConnectionClosureTask(oldConn);
        closureTask.execute(task);
    }


    /**
     *
     */
    private class CachedConnection {

        final JmsBag bag;
        final String name;
        final int endpointVersion;
        final int connectionVersion;
        final String key;

        private long dispatchCount;
        private long releaseCount;

        boolean stale;

        CachedConnection(JmsEndpointConfig cfg, JmsBag bag) {
            this.bag = bag;
            this.name = cfg.getDisplayName().substring(cfg.getDisplayName().lastIndexOf("/"));
            this.endpointVersion = cfg.getEndpoint().getVersion();
            this.connectionVersion = cfg.getConnection().getVersion();
            this.key = getKey(cfg);
        }

        boolean matchVersion(JmsEndpointConfig cfg) {
            return matchVersion(cfg.getEndpoint().getVersion(), cfg.getConnection().getVersion());
        }

        boolean matchVersion(UnmatchedReleaseElement elem) {
            return matchVersion(elem.endpointVersion, elem.connectionVersion);
        }

        boolean matchVersion(int epVersion, int connVersion) {
            return (endpointVersion == epVersion) && (connectionVersion == connVersion);
        }

        boolean isStale() {
            return stale;
        }

        JmsBag newTaskBag() throws JMSException {

            // might be unable to create a new session due to stale connection.
            JmsTaskBag result = new JmsTaskBag(
                    bag.getJndiContext(),
                    bag.getConnectionFactory(),
                    bag.getConnection(),
                    bag.getConnection().createSession(false, Session.CLIENT_ACKNOWLEDGE)
            );

            dispatchCount++;
            return result;
        }

        void close() {
            /*
             * Assume that all sessions have been closed by the closure task
             */
            this.bag.close();
        }
    }



    private void staleConnection() {

    }



    private Queue<UnmatchedReleaseElement> unmatchedRelease = new ConcurrentLinkedQueue<UnmatchedReleaseElement>();
    private class UnmatchedReleaseElement {
        final String key;
        final int endpointVersion;
        final int connectionVersion;

        UnmatchedReleaseElement(String key, int endpointVersion, int connectionVersion) {
            this.key = key;
            this.endpointVersion = endpointVersion;
            this.connectionVersion = connectionVersion;
        }

        boolean matches(CachedConnection conn) {

            return (key.equals(conn.key) && conn.matchVersion(endpointVersion, connectionVersion));
        }
    }


    private static final long CLOSURE_TASK_RUNTIME = 3000L; // 3 sec
    private class ConnectionClosureTask implements Runnable {

        private CachedConnection conn;

        ConnectionClosureTask(CachedConnection conn) {
            this.conn = conn;
        }

        public void run() {

            /*
             * Allow for threads using existing sessions some time to
             * complete before closing the connection
             */
            try {
                long startTime = System.currentTimeMillis();
                long now = startTime;
                UnmatchedReleaseElement unmatched = null;
//                while ((now - startTime) <= CLOSURE_TASK_RUNTIME) {
//
//                    if (conn.dispatchCount == conn.releaseCount) {
//                        break;
//                    } else {
//                        // check for additional dispatches
////                        while (!unmatchedRelease.isEmpty()) {
////                            unmatched = unmatchedRelease.poll();
////                            if (unmatched.matches(conn)) {
////                                conn.releaseCount++;
////                            }
////                        }
//                    }

                    // sleep between checks
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException iex) {}
//
//                    now = System.currentTimeMillis();
//                }

            } finally {
                // close the main connection
                _logger.log(Level.INFO, "Closing Jms connection ({0}), version {1}:{2}", new Object[] {
                        conn.name, conn.connectionVersion, conn.endpointVersion
                });
                conn.close();
            }
        }
    }
}
