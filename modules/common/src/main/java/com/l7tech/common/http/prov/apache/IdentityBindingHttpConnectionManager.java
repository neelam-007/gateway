package com.l7tech.common.http.prov.apache;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.protocol.Protocol;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 */
public class IdentityBindingHttpConnectionManager extends StaleCheckingHttpConnectionManager {

    //- PUBLIC

    /**
     * Create a manager with the default binding timeout / max age and default
     * stale connection cleanup.
     */
    public IdentityBindingHttpConnectionManager() {
        bindingTimeout = DEFAULT_BINDING_TIMEOUT;
        bindingMaxAge = DEFAULT_BINDING_MAX_AGE;
        lock = new ReentrantReadWriteLock();
        connectionsById = new HashMap<Object,HttpConnectionInfo>();
        info = new ThreadLocal<ThreadLocalInfo>();
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
        final ThreadLocalInfo tli = getInfo();
        return tli==null ?  null : tli.getId();
    }

    /**
     * Set the id that will be used from this thread.
     *
     * @param identity The identity of the current user.
     */
    public void setId( final Object identity ) {
        if ( BINDING_ENABLED ) {
            setInfo(identity);
        }
    }

    /**
     * Bind the current connection.
     */
    public void bind() {
        if ( BINDING_ENABLED ) {
            ThreadLocalInfo tli = getInfo();

            if (tli == null) {
                logger.warning("Attempt to bind with no id set"); // using fake id!
                tli = setInfo(new Object());
            }

            tli.bind();
        }
    }

    /**
     * Get a connection based on the current identity or from the unbound pool.
     *
     * @param hostConfiguration The connection descriptor.
     * @return the HttpConnection.
     */
    @Override
    public HttpConnection getConnection( final HostConfiguration hostConfiguration ) {
        if ( !BINDING_ENABLED ) return super.getConnection(hostConfiguration);

        HttpConnection httpConnection = null;
        final ThreadLocalInfo tli = getInfo();
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

    @Override
    public HttpConnection getConnectionWithTimeout( final HostConfiguration hostConfiguration,
                                                    final long timeout ) throws ConnectionPoolTimeoutException {
        if ( !BINDING_ENABLED ) return super.getConnectionWithTimeout(hostConfiguration, timeout);

        HttpConnection httpConnection = null;
        final ThreadLocalInfo tli = getInfo();
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

    @Override
    public void releaseConnection( final HttpConnection conn ) {
        super.releaseConnection( BINDING_ENABLED ? unwrapit(conn) : conn );
    }

    //- PROTECTED

    /**
     * Periodic cleanup task (releases stale bound connections).
     */
    @Override
    protected void doCleanup() {
        // grab a copy of the MAP
        long timeNow = 0L;
        Map<Object,HttpConnectionInfo> connectionInfo = null;
        boolean gotLock = false;
        while (!gotLock) {
            try {
                lock.writeLock().lock();
                gotLock = true;
                timeNow = System.currentTimeMillis();
                connectionInfo = new HashMap<Object,HttpConnectionInfo>(connectionsById);
            }
            finally {
                if (gotLock) lock.writeLock().unlock();
            }
        }

        final List<Pair<Object,HttpConnectionInfo>> identitiesForRemoval = new ArrayList<Pair<Object,HttpConnectionInfo>>();
        for ( final Map.Entry<Object,HttpConnectionInfo> entry : connectionInfo.entrySet() ) {
            final Object identifier = entry.getKey();
            final HttpConnectionInfo httpConnectionInfo = entry.getValue();

            synchronized ( httpConnectionInfo.syncLock ) {
                if ( !isValid( httpConnectionInfo, timeNow ) ) {
                    if ( httpConnectionInfo.isInUse() ) {
                        if ( logger.isLoggable( Level.FINE ) ) {
                            logger.fine( "Not releasing in use connection identity '" + identifier + "'." );
                        }
                    } else {
                        identitiesForRemoval.add( new Pair<Object,HttpConnectionInfo>(identifier,httpConnectionInfo) );
                        httpConnectionInfo.dispose();
                    }
                }
            }
        }

        gotLock = false;
        while (!gotLock) {
            try {
                lock.writeLock().lock();
                gotLock = true;
                connectionsById.entrySet().removeAll(identitiesForRemoval);
            }
            finally {
                if (gotLock) lock.writeLock().unlock();
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

    private static final boolean BINDING_ENABLED = ConfigFactory.getBooleanProperty( "com.l7tech.common.http.prov.apache.identityBindingEnabled", true );

    private static final int DEFAULT_BINDING_MAX_AGE = ConfigFactory.getIntProperty( "com.l7tech.common.http.prov.apache.identityBindingMaxAge", 120000 );
    private static final int MIN_BINDING_MAX_AGE = 100;
    private static final int MAX_BINDING_MAX_AGE = 900000;
    private static final int DEFAULT_BINDING_TIMEOUT = ConfigFactory.getIntProperty( "com.l7tech.common.http.prov.apache.identityBindingTimeout", 30000 );
    private static final int MIN_BINDING_TIMEOUT = 100;
    private static final int MAX_BINDING_TIMEOUT = 300000;

    private int bindingTimeout;
    private int bindingMaxAge;
    private ReadWriteLock lock;
    private Map<Object,HttpConnectionInfo> connectionsById;
    private ThreadLocal<ThreadLocalInfo> info;

    public Map<Object,HttpConnectionInfo> getConnectionsById() {
        return connectionsById;
    }

    /**
     * Get the HttpConnection bound to the given identity.
     *
     * If there is a bound connection that matches the given host configuration it will be re-used.
     *
     * @param identity          The identity for the bound connection.
     * @param hostConfiguration The host configuration to match
     * @return The HttpConnection or null
     */
    private HttpConnection getBoundHttpConnection( final Object identity,
                                                   final HostConfiguration hostConfiguration ) {
        try {
            return getBoundHttpConnection(identity, hostConfiguration, 0L);
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
    private HttpConnection getBoundHttpConnection( final Object identity,
                                                   final HostConfiguration hostConfiguration,
                                                   final long timeout ) throws ConnectionPoolTimeoutException {
        HttpConnection httpConnection = null;
        HttpConnectionInfo hci = null;

        boolean gotLock = false;
        while (!gotLock) {
            try {
                if (timeout == 0L) lock.readLock().lock();
                else if(!lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    throw new ConnectionPoolTimeoutException("Timeout acquiring lock.");
                }
                gotLock = true;
                hci = connectionsById.get(identity);
                if (hci != null) {
                    synchronized ( hci.syncLock ) {
                        if( hostConfiguration != null && isValid(hci) && !hci.isDisposed() && hci.httpConnection.isOpen() ) {
                            httpConnection = hci.getHttpConnection();
                            if (httpConnection != null) {
                                httpConnection = new HttpConnectionWrapper(httpConnection, hci);
                            }
                            else {
                                logger.fine("Valid bound connection for identity '"+identity+"' is null?.");
                            }
                        } else {
                            hci.dispose();
                        }
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
                if (gotLock) lock.readLock().unlock();
            }
        }

        if ( hci != null && httpConnection != null ) {
            HostConfiguration connectionHostConfiguration = buildHostConfiguration(httpConnection);
            if ( !connectionHostConfiguration.equals(hostConfiguration) ) {
                // Then release the connection and fall back to getting a new one from the pool
                HttpConnection connectionForClosing = httpConnection;
                httpConnection = null;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing bound HTTP connection for identity '"+identity+"', does not match required host configuration.");
                }

                connectionForClosing.close(); // Close it since the connection must not be reused
                releaseConnection(connectionForClosing);
                hci.dispose();
            }
            else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Re-using bound HTTP connection for identity '"+identity+"'.");
                }
            }
        }

        return httpConnection;
    }

    private boolean setBoundHttpConnection( final Object identity,
                                            final HttpConnection httpConnection ) {
        boolean bound = false;
        boolean gotLock = false;
        try {
            lock.writeLock().lock();
            gotLock = true;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Binding HTTP connection for identity '"+identity+"'.");
            }

            HttpConnectionInfo previouslyBoundHttpConnectionInfo =
                    connectionsById.put(identity, new HttpConnectionInfo(httpConnection));

            bound = true;
            if(previouslyBoundHttpConnectionInfo!=null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing replaced bound HTTP connection for identity '"+identity+"'.");
                }
                synchronized ( previouslyBoundHttpConnectionInfo.syncLock ) {
                    previouslyBoundHttpConnectionInfo.dispose();
                }
            }
        }
        finally {
            if (gotLock) lock.writeLock().unlock();

            if (!bound) {
                httpConnection.close(); // close to ensure connection is not re-used
            }
        }

        return bound;
    }

    /**
     * Construct a host configuration based on the given connection.
     *
     * @param httpConnection The HttpConnection that describes the HostConfiguration
     * @return a newly constructed HostConfiguration that matches the given HttpConnection
     */
    private HostConfiguration buildHostConfiguration( final HttpConnection httpConnection ) {

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
        return info.get();
    }

    private ThreadLocalInfo setInfo( final Object identity ) {
        ThreadLocalInfo newInfo = info.get();
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

    private boolean isValid( final HttpConnectionInfo httpConnectionInfo ) {
        return isValid(httpConnectionInfo, System.currentTimeMillis());
    }

    private boolean isValid( final HttpConnectionInfo httpConnectionInfo,
                             final long atTime ) {
        boolean valid = true;

        synchronized ( httpConnectionInfo.syncLock ) {
            if ((httpConnectionInfo.getAllocationTime()+((long)bindingMaxAge)<atTime) ||
                (httpConnectionInfo.getLastUsageTime()+((long)bindingTimeout)<atTime)) {
                valid = false;
            }
        }

        return valid;
    }

    private HttpConnection unwrapit( HttpConnection httpConnection ) {
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
        private final Object syncLock = new Object();
        private final long allocationTime;
        private long lastUsageTime;
        private HttpConnection httpConnection;
        private boolean inUse;
        private boolean disposed;

        private HttpConnectionInfo( final HttpConnection httpConnection ) {
            this.allocationTime = System.currentTimeMillis();
            this.lastUsageTime = this.allocationTime;
            this.httpConnection = httpConnection;
            this.inUse = false;
            this.disposed = false;
        }

        public long getAllocationTime() {
            return allocationTime;
        }

        public long getLastUsageTime() {
            return lastUsageTime;
        }

        public HttpConnection getHttpConnection() {
            return httpConnection;
        }

        public boolean isInUse() {
            return inUse;
        }

        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void wrap() {
            synchronized ( syncLock ) {
                this.inUse = true;
                this.lastUsageTime = System.currentTimeMillis();
            }
        }

        /**
         * Release a connection.
         *
         * <p>Note that if the id is set this will NOT release the underlying connection.</p>
         */
        @Override
        public boolean release() {
            synchronized ( syncLock ) {
                final ThreadLocalInfo tli = getInfo();
                final HttpConnection httpConnection = getHttpConnection();
                final Object identity = tli!=null ? tli.getId() : null;

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Releasing connection for " + identity);
                }

                boolean bound = false;
                if (identity != null && tli.hasBindingStatus() && httpConnection != null && httpConnection.isOpen()) {
                    if (tli.bindingRequested()) {
                        bound = setBoundHttpConnection(identity, httpConnection);
                    }
                    else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Connection is bound, retaining " + identity);
                        }
                        bound = true;
                    }
                }

                this.inUse = false;
                return !bound;
            }
        }

        public void dispose() {
            if ( !disposed ) {
                HttpConnection disposeMe = httpConnection;
                httpConnection = null;
                inUse = false;
                disposed = true;

                if (disposeMe != null) {
                    disposeMe.close();
                    disposeMe.releaseConnection();
                }
            }
        }
    }

    /**
     * Holder for ThreadLocal data
     */
    private static final class ThreadLocalInfo {
        private final Object id;
        private Boolean bind;

        private ThreadLocalInfo( final Object id ) {
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
