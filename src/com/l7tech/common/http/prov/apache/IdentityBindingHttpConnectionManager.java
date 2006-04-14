package com.l7tech.common.http.prov.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * Connection manager that manages connections with an implicit identity association.
 *
 * <p>It is essential that any binding / connection access / release occurs on a single thread.</p>
 *
 * <p>It is NOT guaranteed that bindings will be respected if you leave it up to GC to clean up
 * the HttpConnection.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class IdentityBindingHttpConnectionManager extends MultiThreadedHttpConnectionManager {

    //- PUBLIC

    public IdentityBindingHttpConnectionManager() {
        bindingTimeout = DEFAULT_BINDING_TIMEOUT;
        bindingMaxAge = DEFAULT_BINDING_MAX_AGE;
        lock = new ReaderPreferenceReadWriteLock();
        connectionsById = new HashMap();
        info = new ThreadLocal();
        cleanupTimer.schedule(new CleanupTimerTask(this), 5000, 5000);
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
            httpConnection = super.getConnection(hostConfiguration);
        }

        return httpConnection;
    }

    /**
     * Get a connection based on the current identity or from the unbound pool.
     *
     * @param hostConfiguration The connection descriptor.
     * @param timeout           The maximum wait time
     * @return the HttpConnection
     * @throws HttpException on timeout
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration, long timeout) throws HttpException {
        HttpConnection httpConnection = null;
        ThreadLocalInfo tli = getInfo();
        if (tli != null) {
            httpConnection = getBoundHttpConnection(tli.getId(), hostConfiguration, timeout);
            if (httpConnection != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (httpConnection == null) {
            httpConnection = super.getConnection(hostConfiguration, timeout);
        }

        return httpConnection;
    }

    /**
     * Release a connection.
     *
     * <p>Note that if the id is set this will NOT release the underlying connection.</p>
     *
     * @param httpConnection The connection to release
     */
    public void releaseConnection(HttpConnection httpConnection) {
        logger.info("Releasing connection.");

        ThreadLocalInfo tli = getInfo();
        Object identity = tli!=null ? tli.getId() : null;
        boolean bound = false;
        if (identity != null && tli.hasBindingStatus() && httpConnection != null && httpConnection.isOpen()) {
            if (tli.bindingRequested()) {
                bound = setBoundHttpConnection(identity, httpConnection);
            }
            else {
                logger.info("Connection is bound, retaining.");
                bound = true;
            }
        }

        if (!bound) {
            reallyReleaseConnection(httpConnection);
        }
    }

    //- PACKAGE

    void reallyReleaseConnection(HttpConnection httpConnection) {
        super.releaseConnection(unwrap(httpConnection));
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityBindingHttpConnectionManager.class.getName());
    private static final int DEFAULT_BINDING_MAX_AGE = 120000;
    private static final int MIN_BINDING_MAX_AGE = 100;
    private static final int MAX_BINDING_MAX_AGE = 900000;
    private static final int DEFAULT_BINDING_TIMEOUT = 30000;
    private static final int MIN_BINDING_TIMEOUT = 100;
    private static final int MAX_BINDING_TIMEOUT = 300000;
    private static final Timer cleanupTimer = new Timer("BoundConnectionCleanupTimer", true);

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
        catch(HttpException he) { // cant happen with 0 timeout
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
    private HttpConnection getBoundHttpConnection(Object identity, HostConfiguration hostConfiguration, long timeout) throws HttpException {
        HttpConnection httpConnection = null;

        boolean gotLock = false;
        while (!gotLock) {
            try {
                if (timeout == 0) lock.readLock().acquire();
                else if(!lock.readLock().attempt(timeout)) {
                    throw new HttpException("Timeout acquiring lock.");
                }
                gotLock = true;
                HttpConnectionInfo hci = (HttpConnectionInfo) connectionsById.get(identity);
                if (hci != null) {
                    if(isValid(hci)) {
                        httpConnection = hci.getHttpConnection();
                        if (httpConnection != null) {
                            httpConnection = new BoundHttpConnectionAdapter(httpConnection, hci);
                        }
                        else {
                            logger.info("Valid bound connection for identity '"+identity+"' is null?.");
                        }
                    }
                    else {
                        logger.info("Disposing invalid bound connection for identity '"+identity+"'.");
                        hci.dispose();
                    }
                }
                else {
                    logger.info("No bound connection info for identity '"+identity+"'.");
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
            if (!connectionHostConfiguration.equals(hostConfiguration)) {
                // Then release the connection and fall back to getting a new one from the pool
                HttpConnection connectionForClosing = httpConnection;
                httpConnection = null;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing bound HTTP connection for identity '"+identity+"', does not match required host configuration.");
                }

                connectionForClosing.close(); // Close it since the connection must not be reused
                reallyReleaseConnection(connectionForClosing);
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
        HostConfiguration connectionHostConfiguration = buildHostConfiguration(httpConnection);
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
        String virtualHost = httpConnection.getVirtualHost();
        int port = httpConnection.getPort();
        Protocol protocol = httpConnection.getProtocol();
        hostConfiguration.setHost(host, virtualHost, port, protocol);

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

    private HttpConnection unwrap(HttpConnection httpConnection) {
        if (httpConnection instanceof BoundHttpConnectionAdapter) {
            httpConnection = ((BoundHttpConnectionAdapter)httpConnection).getWrappedConnection();
        }

        return httpConnection;
    }

    /**
     * Holder for HttpConnection data
     */
    private static final class HttpConnectionInfo {
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

        public void recordUsage() {
            this.inUse = true;
            this.lastUsageTime = System.currentTimeMillis();
        }

        public void recordRelease() {
            this.inUse = false;
            //TODO update usage on release?
            //this.lastUsageTime = System.currentTimeMillis();
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

    /**
     *
     */
    public static class CleanupTimerTask extends TimerTask {

        private final WeakReference managerRef;

        CleanupTimerTask(final IdentityBindingHttpConnectionManager manager) {
            managerRef = new WeakReference(manager);
        }

        public void run() {
            IdentityBindingHttpConnectionManager manager =
                    (IdentityBindingHttpConnectionManager) managerRef.get();

            if (manager == null) {
                logger.info("Cancelled timer task for GC'd connection manager.");
                cancel();
            }
            else {
                // grab a copy of the MAP
                long timeNow = 0;
                Map connectionInfo = null;
                boolean gotLock = false;
                while (!gotLock) {
                    try {
                        manager.lock.writeLock().acquire();
                        gotLock = true;
                        timeNow = System.currentTimeMillis();
                        connectionInfo = new HashMap(manager.connectionsById);
                    }
                    catch(InterruptedException ie) {
                        logger.log(Level.WARNING, "Unexpected interruption acquiring read lock.", ie);
                    }
                    finally {
                        if (gotLock) manager.lock.writeLock().release();
                    }
                }

                List identitiesForRemoval = new ArrayList();
                if (connectionInfo != null) {
                    for(Iterator iterator = connectionInfo.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object identifier = entry.getKey();
                        HttpConnectionInfo httpConnectionInfo = (HttpConnectionInfo) entry.getValue();

                        if (!manager.isValid(httpConnectionInfo, timeNow)) {
                            if (httpConnectionInfo.isInUse()) {
                                logger.fine("Not releasing in use connection.");
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
                        manager.lock.writeLock().acquire();
                        gotLock = true;
                        manager.connectionsById.keySet().removeAll(identitiesForRemoval);
                    }
                    catch(InterruptedException ie) {
                        logger.log(Level.WARNING, "Unexpected interruption acquiring read lock.", ie);
                    }
                    finally {
                        if (gotLock) manager.lock.writeLock().release();
                    }
                }
                if(!identitiesForRemoval.isEmpty()) {
                    logger.info("Released connections with identifiers " + identitiesForRemoval);
                }
            }
        }
    }

    /**
     * Wrapper for HttpConnections (only used when bound).
     */
    private static class BoundHttpConnectionAdapter extends HttpConnection {

        // the wrapped connection
        private HttpConnection wrappedConnection;
        private final HttpConnectionInfo connectionInfo;

        /**
         * Creates a new HttpConnectionAdapter.
         * @param connection the connection to be wrapped
         */
        public BoundHttpConnectionAdapter(HttpConnection connection, HttpConnectionInfo connectionInfo) {
            super(connection.getHost(), connection.getPort(), connection.getProtocol());
            this.wrappedConnection = connection;
            this.connectionInfo = connectionInfo;
            this.connectionInfo.recordUsage();
        }

        /**
         * Tests if the wrapped connection is still available.
         * @return boolean
         */
        protected boolean hasConnection() {
            return wrappedConnection != null;
        }

        /**
         *
         */
        HttpConnection getWrappedConnection() {
            return wrappedConnection;
        }

        public void close() {
            if (hasConnection()) {
                wrappedConnection.close();
            } else {
                // do nothing
            }
        }

        public InetAddress getLocalAddress() {
            if (hasConnection()) {
                return wrappedConnection.getLocalAddress();
            } else {
                return null;
            }
        }

        public boolean isStaleCheckingEnabled() {
            if (hasConnection()) {
                return wrappedConnection.isStaleCheckingEnabled();
            } else {
                return false;
            }
        }

        public void setLocalAddress(InetAddress localAddress) {
            if (hasConnection()) {
                wrappedConnection.setLocalAddress(localAddress);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setStaleCheckingEnabled(boolean staleCheckEnabled) {
            if (hasConnection()) {
                wrappedConnection.setStaleCheckingEnabled(staleCheckEnabled);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String getHost() {
            if (hasConnection()) {
                return wrappedConnection.getHost();
            } else {
                return null;
            }
        }

        public HttpConnectionManager getHttpConnectionManager() {
            if (hasConnection()) {
                return wrappedConnection.getHttpConnectionManager();
            } else {
                return null;
            }
        }

        public InputStream getLastResponseInputStream() {
            if (hasConnection()) {
                return wrappedConnection.getLastResponseInputStream();
            } else {
                return null;
            }
        }

        public int getPort() {
            if (hasConnection()) {
                return wrappedConnection.getPort();
            } else {
                return -1;
            }
        }

        public Protocol getProtocol() {
            if (hasConnection()) {
                return wrappedConnection.getProtocol();
            } else {
                return null;
            }
        }

        public String getProxyHost() {
            if (hasConnection()) {
                return wrappedConnection.getProxyHost();
            } else {
                return null;
            }
        }

        public int getProxyPort() {
            if (hasConnection()) {
                return wrappedConnection.getProxyPort();
            } else {
                return -1;
            }
        }

        public OutputStream getRequestOutputStream()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getRequestOutputStream();
            } else {
                return null;
            }
        }

        /**
         * @deprecated
         */
        public OutputStream getRequestOutputStream(boolean useChunking)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getRequestOutputStream(useChunking);
            } else {
                return null;
            }
        }

        public InputStream getResponseInputStream()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getResponseInputStream();
            } else {
                return null;
            }
        }

        /**
         * @deprecated
         */
        public InputStream getResponseInputStream(HttpMethod method)
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getResponseInputStream(method);
            } else {
                return null;
            }
        }

        public boolean isOpen() {
            if (hasConnection()) {
                return wrappedConnection.isOpen();
            } else {
                return false;
            }
        }

        public boolean isProxied() {
            if (hasConnection()) {
                return wrappedConnection.isProxied();
            } else {
                return false;
            }
        }

        public boolean isResponseAvailable() throws IOException {
            if (hasConnection()) {
                return  wrappedConnection.isResponseAvailable();
            } else {
                return false;
            }
        }

        public boolean isResponseAvailable(int timeout) throws IOException {
            if (hasConnection()) {
                return  wrappedConnection.isResponseAvailable(timeout);
            } else {
                return false;
            }
        }

        public boolean isSecure() {
            if (hasConnection()) {
                return wrappedConnection.isSecure();
            } else {
                return false;
            }
        }

        public boolean isTransparent() {
            if (hasConnection()) {
                return wrappedConnection.isTransparent();
            } else {
                return false;
            }
        }

        public void open() throws IOException {
            if (hasConnection()) {
                wrappedConnection.open();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void print(String data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.print(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void printLine()
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.printLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void printLine(String data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.printLine(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String readLine() throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.readLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void releaseConnection() {
            if (hasConnection()) {
                HttpConnection wrappedConnection = this.wrappedConnection;
                this.wrappedConnection = null;
                this.connectionInfo.recordRelease();
                wrappedConnection.releaseConnection();
            } else {
                // do nothing
            }
        }

        public void setConnectionTimeout(int timeout) {
            if (hasConnection()) {
                wrappedConnection.setConnectionTimeout(timeout);
            } else {
                // do nothing
            }
        }

        public void setHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setHost(host);
            } else {
                // do nothing
            }
        }

        public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
            if (hasConnection()) {
                wrappedConnection.setHttpConnectionManager(httpConnectionManager);
            } else {
                // do nothing
            }
        }

        public void setLastResponseInputStream(InputStream inStream) {
            if (hasConnection()) {
                wrappedConnection.setLastResponseInputStream(inStream);
            } else {
                // do nothing
            }
        }

        public void setPort(int port) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setPort(port);
            } else {
                // do nothing
            }
        }

        public void setProtocol(Protocol protocol) {
            if (hasConnection()) {
                wrappedConnection.setProtocol(protocol);
            } else {
                // do nothing
            }
        }

        public void setProxyHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setProxyHost(host);
            } else {
                // do nothing
            }
        }

        public void setProxyPort(int port) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setProxyPort(port);
            } else {
                // do nothing
            }
        }

        /**
         * @deprecated
         */
        public void setSecure(boolean secure) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setSecure(secure);
            } else {
                // do nothing
            }
        }

        public void setSoTimeout(int timeout)
            throws SocketException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setSoTimeout(timeout);
            } else {
                // do nothing
            }
        }

        public void shutdownOutput() {
            if (hasConnection()) {
                wrappedConnection.shutdownOutput();
            } else {
                // do nothing
            }
        }

        public void tunnelCreated() throws IllegalStateException, IOException {
            if (hasConnection()) {
                wrappedConnection.tunnelCreated();
            } else {
                // do nothing
            }
        }

        public void write(byte[] data, int offset, int length)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.write(data, offset, length);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void write(byte[] data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.write(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void writeLine()
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.writeLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void writeLine(byte[] data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.writeLine(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void flushRequestOutputStream() throws IOException {
            if (hasConnection()) {
                wrappedConnection.flushRequestOutputStream();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public int getSoTimeout() throws SocketException {
            if (hasConnection()) {
                return wrappedConnection.getSoTimeout();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String getVirtualHost() {
            if (hasConnection()) {
                return wrappedConnection.getVirtualHost();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setVirtualHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setVirtualHost(host);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public int getSendBufferSize() throws SocketException {
            if (hasConnection()) {
                return wrappedConnection.getSendBufferSize();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setSendBufferSize(int sendBufferSize) throws SocketException {
            if (hasConnection()) {
                wrappedConnection.setSendBufferSize(sendBufferSize);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

    }
}
