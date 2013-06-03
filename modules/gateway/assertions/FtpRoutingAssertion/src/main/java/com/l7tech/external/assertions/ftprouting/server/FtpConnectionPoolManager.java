package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.Ftp;
import com.jscape.inet.ftp.FtpException;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.l7tech.util.Pair;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nilic
 */
public class FtpConnectionPoolManager extends ConnectionPoolManager{   //- PUBLIC

    /**
     * Create a manager with the default binding timeout / max age and default
     * stale connection cleanup.
     */
    public FtpConnectionPoolManager() {
        super();
        connectionsById = new ConcurrentHashMap<>();
    }


    /**
     * Set the id that will be used from this thread.
     *
     * @param identity The identity of the current user.
     */
    public void setId( final Object identity ) throws FtpException{
        if ( BINDING_ENABLED ) {
            setInfo(identity);
        }
    }

    /**
     * Bind the current connection.
     */
    public void bind() throws FtpException{
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
     * @param config The connection descriptor.
     * @return the Ftp client.
     */

    public Ftp getConnection( final FtpClientConfig config ) throws FtpException{

        if ( !BINDING_ENABLED ) return FtpUtils.newFtpClient(config);

        Ftp ftp = null;
        final ThreadLocalInfo tli = getInfo();   //just use the bean
        if (tli != null) {
            ftp = getBoundFtp(tli.getId(), config);
            if (ftp != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (ftp == null) {
            ftp = FtpUtils.newFtpClient(config);
        }

        return ftp;
    }

    public Ftp getConnectionWithTimeout( final FtpClientConfig config,
                                         final long timeout ) throws ConnectionPoolTimeoutException, FtpException {
        if ( !BINDING_ENABLED ) return getConnectionWithTimeout(config, timeout);

        Ftp ftp = null;
        final ThreadLocalInfo tli = getInfo();
        if (tli != null) {
            ftp = getBoundFtp(tli.getId(), config, timeout);
            if (ftp != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (ftp == null) {
            ftp = getConnectionWithTimeout(config, timeout);
        }

        return ftp;
    }

    public void releaseConnection( final Ftp conn ) {
        conn.disconnect();
    }

    //- PROTECTED

    /**
     * Periodic cleanup task (releases stale bound connections).
     */
    protected void doCleanup() {
        // grab a copy of the MAP
        long timeNow = 0L;
        Map<Object,FtpInfo> ftpInfo = null;
        boolean gotLock = false;
        while (!gotLock) {
            try {
                lock.writeLock().lock();
                gotLock = true;
                timeNow = System.currentTimeMillis();
                ftpInfo = new ConcurrentHashMap<>(connectionsById);
            }
            finally {
                if (gotLock) lock.writeLock().unlock();
            }
        }

        final List<Pair<Object,FtpInfo>> identitiesForRemoval = new ArrayList<>();
        for ( final Map.Entry<Object,FtpInfo> entry : ftpInfo.entrySet() ) {
            final Object identifier = entry.getKey();
            final FtpInfo ftpConnectionInfo = entry.getValue();

            synchronized ( ftpConnectionInfo.syncLock ) {
                if ( !isValid( ftpConnectionInfo, timeNow ) ) {
                    if ( ftpConnectionInfo.isInUse() ) {
                        if ( logger.isLoggable( Level.FINE ) ) {
                            logger.fine( "Not releasing in use connection identity '" + identifier + "'." );
                        }
                    } else {
                        identitiesForRemoval.add( new Pair<>(identifier,ftpConnectionInfo) );
                        ftpConnectionInfo.dispose();
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
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FtpConnectionPoolManager.class.getName());

    private static final boolean BINDING_ENABLED = true;
    private ConcurrentHashMap<Object,FtpInfo> connectionsById;

    public Map<Object,FtpInfo> getConnectionsById() {
        return connectionsById;
    }

    /**
     * Get the Ftp Connection bound to the given identity.
     *
     * If there is a bound connection that matches the given host configuration it will be re-used.
     *
     * @param identity          The identity for the bound connection.
     * @param config            The host configuration to match
     * @return The Ftp connection or null
     */
    private Ftp getBoundFtp( final Object identity,
                             @Nullable final FtpClientConfig config  ) throws FtpException{
        try {
            return getBoundFtp(identity, config, 0L);
        }
        catch(ConnectionPoolTimeoutException he) { // cant happen with 0 timeout
            logger.log(Level.WARNING, "Unexpected timeout looking for bound connection", he);
            return null;
        }
    }

    /**
     * Get the Ftp connection bound to the given identity.
     *
     * If there is a bound connection that matches the given host configuration it will be re-used.
     *
     * @param identity          The identity for the bound connection.
     * @param config            The host configuration to match
     * @param timeout           The maximum time to wait in milliseconds
     * @return The Ftp connection or null
     */
    private Ftp getBoundFtp( final Object identity,
                             final FtpClientConfig config,
                             final long timeout ) throws ConnectionPoolTimeoutException, FtpException {
        Ftp ftp = null;
        FtpInfo fi = null;

        boolean gotLock = false;
        while (!gotLock) {
            try {
                if (timeout == 0L) lock.readLock().lock();
                else if(!lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    throw new ConnectionPoolTimeoutException("Timeout acquiring lock.");
                }
                gotLock = true;
                fi = connectionsById.get(identity);
                if (fi != null) {
                    synchronized ( fi.syncLock ) {
                        if( config != null && isValid(fi) && !fi.isDisposed() && fi.ftp.isConnected() ) {
                            ftp = fi.getFtp();
                            if (ftp == null) {
                                logger.fine("Valid bound connection for identity '"+identity+"' is null?.");
                            }
                        } else {
                            fi.dispose();
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

        if ( fi != null && ftp != null ) {
            if ( !buildHostConfiguration(ftp, config) ) {
                // Then release the connection and fall back to getting a new one from the pool
                Ftp connectionForClosing = ftp;
                ftp = null;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing bound FTP connection for identity '"+identity+"', does not match required host configuration.");
                }

                connectionForClosing.disconnect(); // Close it since the connection must not be reused
                releaseConnection(connectionForClosing);
                fi.dispose();
            }
            else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Re-using bound FTP connection for identity '"+identity+"'.");
                }
            }
        }
        return ftp;
    }

    public boolean setBoundFtp( final Object identity,
                                 final Ftp ftp,
                                 boolean reuse) {
        boolean bound = false;
        boolean gotLock = false;
        try {
            lock.writeLock().lock();
            gotLock = true;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Binding FTP connection for identity '"+identity+"'.");
            }

            FtpInfo previouslyBoundFtpInfo = connectionsById.put(identity, new FtpInfo(ftp));

            bound = true;
            if (previouslyBoundFtpInfo != null && !reuse){
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing replaced bound FTP connection for identity '"+identity+"'.");
                }
                synchronized ( previouslyBoundFtpInfo.syncLock ) {
                    previouslyBoundFtpInfo.dispose();
                }
            }
        }
        finally {
            if (gotLock) lock.writeLock().unlock();

            if (!bound) {
                ftp.disconnect(); // close to ensure connection is not re-used
            }
        }

        return bound;
    }

    /**
     * Construct a host configuration based on the given connection.
     *
     * @param ftp The FTP Client that describes the HostConfiguration
     * @return boolean true if constructed HostConfiguration matches the given Ftp connection configuration
     */
    private boolean buildHostConfiguration( final Ftp ftp , FtpClientConfig config) throws FtpException{

        boolean isEqual = false;
        FtpClientConfig hostConfiguration = FtpClientConfigImpl.newFtpConfig(ftp.getHostname());

        String host = ftp.getHostname();
        int port = ftp.getPort();
        String user = ftp.getUsername();
        String pass = ftp.getPassword();
        String dir = ftp.getDir();
        hostConfiguration.setHost(host);
        hostConfiguration.setUser(user);
        hostConfiguration.setPass(pass);
        hostConfiguration.setPort(port);
        hostConfiguration.setDirectory(dir);

        if (config.getHost().equals(hostConfiguration.getHost()) &&
            config.getUser().equals(hostConfiguration.getUser()) &&
            config.getPass().equals(hostConfiguration.getPass()) &&
            config.getPort() == hostConfiguration.getPort() &&
            config.getDirectory().equals(hostConfiguration.getDirectory())){
            return true;
        }

        return isEqual;
    }

    private ThreadLocalInfo setInfo( final Object identity ) throws FtpException{
        ThreadLocalInfo newInfo = info.get();
        if (identity == null) {
            newInfo = null;
        }
        else if (newInfo == null || !newInfo.getId().equals(identity)) {
            if (newInfo != null) {
                getBoundFtp(newInfo.getId(), null); // ensure any currently bound connection is closed.
            }

            newInfo = new ThreadLocalInfo(identity);
        }

        info.set(newInfo);

        return newInfo;
    }

    private boolean isValid( final FtpInfo ftpInfo ) {
        return isValid(ftpInfo, System.currentTimeMillis());
    }

    private boolean isValid( final FtpInfo ftpInfo,
                             final long atTime ) {
        boolean valid = true;

        synchronized ( ftpInfo.syncLock ) {
            if ( atTime - ftpInfo.getLastUsageTime()>((long)bindingTimeout)) {
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Holder for Ftp Connection data
     */
    private final class FtpInfo extends Info{
        private final Object syncLock = new Object();

        private Ftp ftp;

        private FtpInfo( final Ftp ftp ) {
            super();
            this.ftp = ftp;
        }

        public Ftp getFtp() {
            return ftp;
        }

        /**
         * Release a connection.
         *
         * <p>Note that if the id is set this will NOT release the underlying connection.</p>
         */
        public boolean release() {
            synchronized ( syncLock ) {
                final ThreadLocalInfo tli = getInfo();
                final Ftp ftp = getFtp();
                final Object identity = tli!=null ? tli.getId() : null;

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Releasing connection for " + identity);
                }

                boolean bound = false;
                if (identity != null && tli.hasBindingStatus() && ftp != null && ftp.isConnected()) {
                    if (tli.bindingRequested()) {
                        bound = setBoundFtp(identity, ftp, false);
                    }
                    else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Ftp is bound, retaining " + identity);
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
                Ftp disposeMe = ftp;
                ftp = null;
                inUse = false;
                disposed = true;

                if (disposeMe != null) {
                    disposeMe.disconnect();
                }
            }
        }
    }
}
