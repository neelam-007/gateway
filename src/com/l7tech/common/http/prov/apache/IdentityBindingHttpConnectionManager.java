package com.l7tech.common.http.prov.apache;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * Connection manager that manages connections with an implicit identity association.
 *
 * <p>It is essential that any binding / connection access / release occurs on a single thread.</p>
 *
 * <p>It is NOT guaranteed that bindings will be respected if you leave it up to GC to clean up
 * the HttpConnection.</p>
 *
 * <p>This connection manager is not intended for use connecting to a large number of different
 * hosts.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class IdentityBindingHttpConnectionManager extends CachingHttpConnectionManager {

    //- PUBLIC

    /**
     * Create a manager with the default binding timeout / max age and default
     * stale connection cleanup.
     */
    public IdentityBindingHttpConnectionManager() {
        bindingTimeout = DEFAULT_BINDING_TIMEOUT;
        bindingMaxAge = DEFAULT_BINDING_MAX_AGE;
        lock = new ReaderPreferenceReadWriteLock();
        connectionsById = new HashMap();
        info = new ThreadLocal();
    }

    /**
     * Get the binding timeout (milliseconds)
     *
     * <p>This is the per-usage limit for reuse of a bound HttpConnection.</p>
     *
     * @return The current timeout
     */
    public int getBindingTimeout() {
        return bindingTimeout;
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
    public void setBindingTimeout(int bindingTimeout) {
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
     * Get the binding maximum age (milliseconds)
     *
     * <p>This is the overall limit for reuse of a bound HttpConnection.</p>
     *
     * @return The current timeout
     */
    public int getBindingMaxAge() {
        return bindingTimeout;
    }

    /**
     * Set the binding maximum age (milliseconds)
     *
     * <p>The minimum permitted value is 100ms the maximum is 900000ms (15 minutes)</p>
     *
     * <p>If the value given is out of range it is set to the nearest limit (and
     * a warning is logged).</p>
     *
     * @param bindingMaxAge The timeout to use.
     */
    public void setBindingMaxAge(int bindingMaxAge) {
        if(bindingMaxAge < MIN_BINDING_MAX_AGE) {
            logger.warning("Invalid binding maximum age '"+bindingMaxAge+"' using '"+MIN_BINDING_MAX_AGE+"' milliseconds.");
            bindingMaxAge = MIN_BINDING_MAX_AGE;
        }
        else if(bindingMaxAge > MAX_BINDING_MAX_AGE) {
            logger.warning("Invalid binding maximum age '"+bindingMaxAge+"' using '"+MAX_BINDING_MAX_AGE+"' milliseconds.");
            bindingMaxAge = MAX_BINDING_MAX_AGE;
        }

        this.bindingMaxAge = bindingMaxAge;
    }

    /**
     * Get the id that is in use for this thread.
     *
     * @return The identity of the current user (may be null).
     */
    public Object getId() {
        ThreadLocalInfo tli = getInfo();
        return tli==null ?  null : tli.getId();
    }

    /**
     * Set the id that will be used from this thread.
     *
     * @param identity The identity of the current user.
     */
    public void setId(Object identity) {
        setInfo(identity);
    }

    /**
     * Bind the current connection.
     */
    public void bind() {
        ThreadLocalInfo tli = getInfo();

        if (tli == null) {
            logger.warning("Attempt to bind with no id set, using fake id!");
            tli = setInfo(new Object());
        }

        tli.bind();
    }

    /**
     * Get a connection based on the current identity or from the unbound pool.
     *
     * @param hostConfiguration The connection descriptor.
     * @return the HttpConnection.
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration) {
        HttpConnection httpConnection = null;
        ThreadLocalInfo tli = getInfo();
        if (tli != null) {
            httpConnection = getBoundHttpConnection(tli.getId(), hostConfiguration);
            if (httpConnection != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (httpConnection == null) {
            HttpConnection newConnection = super.getConnection(hostConfiguration);
            httpConnection = new HttpConnectionWrapper(newConnection, new HttpConnectionInfo(newConnection));
        }

        return httpConnection;
    }

    public HttpConnection getConnectionWithTimeout(HostConfiguration hostConfiguration, long timeout) throws ConnectionPoolTimeoutException {
        HttpConnection httpConnection = null;
        ThreadLocalInfo tli = getInfo();
        if (tli != null) {
            httpConnection = getBoundHttpConnection(tli.getId(), hostConfiguration, timeout);
            if (httpConnection != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (httpConnection == null) {
            HttpConnection newConnection = super.getConnectionWithTimeout(hostConfiguration, timeout);
            httpConnection = new HttpConnectionWrapper(newConnection, new HttpConnectionInfo(newConnection));
        }

        return httpConnection;
    }

    public void releaseConnection(HttpConnection conn) {
        super.releaseConnection(unwrapit(conn));
    }

    //- PROTECTED

    /**
     * Periodic cleanup task (releases stale bound connections).
     */
    protected void doCleanup() {
        // grab a copy of the MAP
        long timeNow = 0;
        Map connectionInfo = null;
        boolean gotLock = false;
        while (!gotLock) {
            try {
                lock.writeLock().acquire();
                gotLock = true;
                timeNow = System.currentTimeMillis();
                connectionInfo = new HashMap(connectionsById);
            }
            catch(InterruptedException ie) {
                logger.log(Level.WARNING, "Unexpected interruption acquiring read lock.", ie);
            }
            finally {
                if (gotLock) lock.writeLock().release();
            }
        }

        List identitiesForRemoval = new ArrayList();
        if (connectionInfo != null) {
            for(Iterator iterator = connectionInfo.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object identifier = entry.getKey();
                HttpConnectionInfo httpConnectionInfo = (HttpConnectionInfo) entry.getValue();

                if (!isValid(httpConnectionInfo, timeNow)) {
                    if (httpConnectionInfo.isInUse()) {
                        if(logger.isLoggable(Level.FINE)) {
                            logger.fine("Not releasing in use connection identity '"+identifier+"'.");
                        }
                    }
                    else {
                        identitiesForRemoval.add(identifier);
                        httpConnectionInfo.dispose();
                    }
                }
            }
        }

        gotLock = false;
        while (!gotLock) {
            try {
                lock.writeLock().acquire();
                gotLock = true;
                connectionsById.keySet().removeAll(identitiesForRemoval);
            }
            catch(InterruptedException ie) {
                logger.log(Level.WARNING, "Unexpected interruption acquiring read lock.", ie);
            }
            finally {
                if (gotLock) lock.writeLock().release();
            }
        }
        if (!identitiesForRemoval.isEmpty()) {
            if(logger.isLoggable(Level.FINE)) {
                logger.fine("Released connections with identifiers " + identitiesForRemoval);
            }
        }

        // stale cleanup
        super.doCleanup();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityBindingHttpConnectionManager.class.getName());
    private static final int DEFAULT_BINDING_MAX_AGE = 120000;
    private static final int MIN_BINDING_MAX_AGE = 100;
    private static final int MAX_BINDING_MAX_AGE = 900000;
    private static final int DEFAULT_BINDING_TIMEOUT = 30000;
    private static final int MIN_BINDING_TIMEOUT = 100;
    private static final int MAX_BINDING_TIMEOUT = 300000;

    private int bindingTimeout;
    private int bindingMaxAge;
    private ReadWriteLock lock;
    private Map connectionsById;
    private ThreadLocal info;

    /**
     * Get the HttpConnection bound to the given identity.
     *
     * If there is a bound connection that matches the given host configuration it will be re-used.
     *
     * @param identity          The identity for the bound connection.
     * @param hostConfiguration The host configuration to match
     * @return The HttpConnection or null
     */
    private HttpConnection getBoundHttpConnection(Object identity, HostConfiguration hostConfiguration) {
        try {
            return getBoundHttpConnection(identity, hostConfiguration, 0);
        }
        catch(ConnectionPoolTimeoutException he) { // cant happen with 0 timeout
            logger.log(Level.WARNING, "Unexpected timeout looking for bound connection", he);
            return null;
        }
    }

    /**
     * Get the HttpConnection bound to the given identity.
     *
     * If there is a bound connection that matches the given host configuration it will be re-used.
     *
     * @param identity          The identity for the bound connection.
     * @param hostConfiguration The host configuration to match
     * @param timeout           The maximum time to wait in milliseconds
     * @return The HttpConnection or null
     */
    private HttpConnection getBoundHttpConnection(Object identity, HostConfiguration hostConfiguration, long timeout) throws ConnectionPoolTimeoutException {
        HttpConnection httpConnection = null;
        HttpConnectionInfo hci = null;

        boolean gotLock = false;
        while (!gotLock) {
            try {
                if (timeout == 0) lock.readLock().acquire();
                else if(!lock.readLock().attempt(timeout)) {
                    throw new ConnectionPoolTimeoutException("Timeout acquiring lock.");
                }
                gotLock = true;
                hci = (HttpConnectionInfo) connectionsById.get(identity);
                if (hci != null) {
                    if(isValid(hci)) {
                        httpConnection = hci.getHttpConnection();
                        if (httpConnection != null) {
                            httpConnection = new HttpConnectionWrapper(httpConnection, hci);
                        }
                        else {
                            logger.info("Valid bound connection for identity '"+identity+"' is null?.");
                        }
                    }
                    else {
                        if(logger.isLoggable(Level.FINE)) {
                            logger.fine("Disposing invalid bound connection for identity '"+identity+"'.");
                        }
                        hci.dispose();
                    }
                }
                else {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine("No bound connection info for identity '"+identity+"'.");
                    }
                }
            }
            catch(InterruptedException ie) {
                logger.log(Level.WARNING, "Unexpected interruption acquiring read lock.", ie);
            }
            finally {
                if (gotLock) lock.readLock().release();
            }
        }

        if (httpConnection != null) {
            HostConfiguration connectionHostConfiguration = buildHostConfiguration(httpConnection);
            if (hostConfiguration==null || !connectionHostConfiguration.equals(hostConfiguration)) {
                // Then release the connection and fall back to getting a new one from the pool
                HttpConnection connectionForClosing = httpConnection;
                httpConnection = null;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing bound HTTP connection for identity '"+identity+"', does not match required host configuration.");
                }

                connectionForClosing.close(); // Close it since the connection must not be reused
                releaseConnection(connectionForClosing);
                 if (hci != null) {
                     hci.dispose();
                 }
            }
            else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Re-using bound HTTP connection for identity '"+identity+"'.");
                }
            }
        }

        return httpConnection;
    }

    private boolean setBoundHttpConnection(Object identity, HttpConnection httpConnection) {
        boolean bound = false;
        boolean gotLock = false;
        try {
            lock.writeLock().acquire();
            gotLock = true;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Binding HTTP connection for identity '"+identity+"'.");
            }

            HttpConnectionInfo previouslyBoundHttpConnectionInfo =
                    (HttpConnectionInfo) connectionsById.put(identity, new HttpConnectionInfo(httpConnection));

            bound = true;
            if(previouslyBoundHttpConnectionInfo!=null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing replaced bound HTTP connection for identity '"+identity+"'.");
                }
                previouslyBoundHttpConnectionInfo.dispose();
            }
        }
        catch(InterruptedException ie) {
            logger.log(Level.WARNING, "Interrupted waiting for write lock, NOT binding connection for identity '"+identity+"'.", ie);
        }
        finally {
            if (gotLock) lock.writeLock().release();
        }

        if (!bound) {
            httpConnection.close(); // close to ensure connection is not re-used
        }

        return bound;
    }

    /**
     * Construct a host configuration based on the given connection.
     *
     * @param httpConnection The HttpConnection that describes the HostConfiguration
     * @return a newly constructed HostConfiguration that matches the given HttpConnection
     */
    private HostConfiguration buildHostConfiguration(HttpConnection httpConnection) {

        HostConfiguration hostConfiguration = new HostConfiguration();

        String host = httpConnection.getHost();
        int port = httpConnection.getPort();
        Protocol protocol = httpConnection.getProtocol();
        hostConfiguration.setHost(host, port, protocol);

        if (httpConnection.getLocalAddress() != null) {
            InetAddress localAddress = httpConnection.getLocalAddress();
            hostConfiguration.setLocalAddress(localAddress);
        }

        if (httpConnection.getProxyHost() != null) {
            String proxyHost = httpConnection.getProxyHost();
            int proxyPort = httpConnection.getProxyPort();
            hostConfiguration.setProxy(proxyHost, proxyPort);
        }

        return hostConfiguration;
    }

    private ThreadLocalInfo getInfo() {
        return (ThreadLocalInfo) info.get();
    }

    private ThreadLocalInfo setInfo(Object identity) {
        ThreadLocalInfo newInfo = (ThreadLocalInfo) info.get();
        if (identity == null) {
            newInfo = null;
        }
        else if (newInfo == null || !newInfo.getId().equals(identity)) {
            if (newInfo != null) {
                getBoundHttpConnection(newInfo.getId(), null); // ensure any currently bound connection is closed.
            }
            newInfo = new ThreadLocalInfo(identity);
        }
        info.set(newInfo);
        return newInfo;
    }

    private boolean isValid(HttpConnectionInfo httpConnectionInfo) {
        return isValid(httpConnectionInfo, System.currentTimeMillis());
    }

    private boolean isValid(HttpConnectionInfo httpConnectionInfo, long atTime) {
        boolean valid = true;

        if ((httpConnectionInfo.getAllocationTime()+bindingMaxAge<atTime) ||
            (httpConnectionInfo.getLastUsageTime()+bindingTimeout<atTime)) {
            valid = false;
        }

        return valid;
    }

    private HttpConnection unwrapit(HttpConnection httpConnection) {
        while (httpConnection instanceof HttpConnectionWrapper && 
            ((HttpConnectionWrapper)httpConnection).getConnectionListener() instanceof HttpConnectionInfo) {
            httpConnection = ((HttpConnectionWrapper)httpConnection).getWrappedConnection();
        }

        return httpConnection;
    }

    /**
     * Holder for HttpConnection data
     */
    private final class HttpConnectionInfo implements HttpConnectionWrapper.ConnectionListener {
        private final Object lock = new Object();
        private final long allocationTime;
        private long lastUsageTime;
        private HttpConnection httpConnection;
        private boolean inUse;

        public HttpConnectionInfo(HttpConnection httpConnection) {
            this.allocationTime = System.currentTimeMillis();
            this.lastUsageTime = this.allocationTime;
            this.httpConnection = httpConnection;
            this.inUse = false;
        }

        public long getAllocationTime() {
            return allocationTime;
        }

        public long getLastUsageTime() {
            return lastUsageTime;
        }

        public HttpConnection getHttpConnection() {
            synchronized(lock) {
                return httpConnection;
            }
        }

        public boolean isInUse() {
            return inUse;
        }

        public void wrap() {
            this.inUse = true;
            this.lastUsageTime = System.currentTimeMillis();
        }

        /**
         * Release a connection.
         *
         * <p>Note that if the id is set this will NOT release the underlying connection.</p>
         */
        public boolean release() {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Releasing connection.");
            }

            ThreadLocalInfo tli = getInfo();
            HttpConnection httpConnection = getHttpConnection();
            Object identity = tli!=null ? tli.getId() : null;
            boolean bound = false;
            if (identity != null && tli.hasBindingStatus() && httpConnection != null && httpConnection.isOpen()) {
                if (tli.bindingRequested()) {
                    bound = setBoundHttpConnection(identity, httpConnection);
                }
                else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Connection is bound, retaining.");
                    }
                    bound = true;
                }
            }

            this.inUse = false;
            return !bound; // release underlying connection?
        }

        public void dispose() {
            HttpConnection disposeMe = null;
            synchronized(lock) {
                disposeMe = httpConnection;
                httpConnection = null;
            }
            if (disposeMe != null) {
                disposeMe.close();
                disposeMe.releaseConnection();
            }
            this.inUse = false;
        }
    }

    /**
     * Holder for ThreadLocal data
     */
    private static final class ThreadLocalInfo {
        private final Object id;
        private Boolean bind;

        public ThreadLocalInfo(Object id) {
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
