package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.FtpException;
import com.jscape.inet.ftps.Ftps;
import com.jscape.inet.ftps.FtpsCertificateVerifier;
import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nilic
 */
public class FtpsConnectionPoolManager extends ConnectionPoolManager{   //- PUBLIC

    /**
     * Create a manager with the default binding timeout / max age and default
     * stale connection cleanup.
     */
    public FtpsConnectionPoolManager() {
        super();
        connectionsById = new ConcurrentHashMap<>();

    }

    /**
     * Set the id that will be used from this thread.
     *
     * @param identity The identity of the current user.
     */
    public void setId( final Object identity,
                       final X509TrustManager trustManager,
                       final HostnameVerifier hostnameVerifier ) throws FtpException {
        if ( BINDING_ENABLED ) {
            setInfo(identity, trustManager, hostnameVerifier);
        }
    }

    /**
     * Bind the current connection.
     */
    public void bind(final X509TrustManager trustManager,
                     final HostnameVerifier hostnameVerifier) throws FtpException{
        if ( BINDING_ENABLED ) {
            ThreadLocalInfo tli = getInfo();

            if (tli == null) {
                logger.warning("Attempt to bind with no id set"); // using fake id!
                tli = setInfo(new Object(), trustManager, hostnameVerifier);
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

    public Ftps getConnection( final FtpClientConfig config,
                               final DefaultKey keyFinder,
                               final X509TrustManager trustManager,
                               final HostnameVerifier hostnameVerifier) throws FtpException{

        if ( !BINDING_ENABLED ) return FtpClientUtils.newFtpsClient(config, keyFinder, trustManager, hostnameVerifier);

        Ftps ftps = null;
        final ThreadLocalInfo tli = getInfo();   //just use the bean
        if (tli != null) {
            ftps = getBoundFtps(tli.getId(), config, trustManager, hostnameVerifier);
            if (ftps != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (ftps == null) {
            ftps = FtpClientUtils.newFtpsClient(config, keyFinder, trustManager, hostnameVerifier);
        }

        return ftps;
    }

    public Ftps getConnectionWithTimeout( final FtpClientConfig config,
                                          final DefaultKey keyFinder,
                                          final X509TrustManager trustManager,
                                          final HostnameVerifier hostnameVerifier,
                                          final long timeout ) throws ConnectionPoolTimeoutException, FtpException {
        if ( !BINDING_ENABLED ) return getConnectionWithTimeout(config, keyFinder, trustManager, hostnameVerifier, timeout);

        Ftps ftps = null;
        final ThreadLocalInfo tli = getInfo();
        if (tli != null) {
            ftps = getBoundFtps(tli.getId(), config, timeout, trustManager, hostnameVerifier);
            if (ftps != null) tli.bound();
            else if(tli.isBound()) tli.clearBindingStatus();
        }

        if (ftps == null) {
            ftps = getConnectionWithTimeout(config, keyFinder, trustManager, hostnameVerifier, timeout);
        }

        return ftps;
    }

    public void releaseConnection( final Ftps conn ) {
        conn.disconnect();
    }

    //- PROTECTED

    /**
     * Periodic cleanup task (releases stale bound connections).
     */
    protected void doCleanup() {
        // grab a copy of the MAP
        long timeNow = 0L;
        Map<Object,FtpsInfo> ftpsInfo = null;
        boolean gotLock = false;
        while (!gotLock) {
            try {
                lock.writeLock().lock();
                gotLock = true;
                timeNow = System.currentTimeMillis();
                ftpsInfo = new HashMap<>(connectionsById);
            }
            finally {
                if (gotLock) lock.writeLock().unlock();
            }
        }

        final List<Pair<Object,FtpsInfo>> identitiesForRemoval = new ArrayList<>();
        for ( final Map.Entry<Object,FtpsInfo> entry : ftpsInfo.entrySet() ) {
            final Object identifier = entry.getKey();
            final FtpsInfo ftpsConnectionInfo = entry.getValue();

            synchronized ( ftpsConnectionInfo.syncLock ) {
                if ( !isValid( ftpsConnectionInfo, timeNow ) ) {
                    if ( ftpsConnectionInfo.isInUse() ) {
                        if ( logger.isLoggable( Level.FINE ) ) {
                            logger.fine( "Not releasing in use connection identity '" + identifier + "'." );
                        }
                    } else {
                        identitiesForRemoval.add( new Pair<>(identifier,ftpsConnectionInfo) );
                        ftpsConnectionInfo.dispose();
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

    private static final Logger logger = Logger.getLogger(FtpsConnectionPoolManager.class.getName());

    private static final boolean BINDING_ENABLED = true;

    private ConcurrentHashMap<Object,FtpsInfo> connectionsById;

    public Map<Object,FtpsInfo> getConnectionsById() {
        return connectionsById;
    }

    /**
     * Get the Ftps Connection bound to the given identity.
     *
     * If there is a bound connection that matches the given host configuration it will be re-used.
     *
     * @param identity          The identity for the bound connection.
     * @param config            The host configuration to match
     * @return The Ftp connection or null
     */
    private Ftps getBoundFtps( final Object identity,
                               @Nullable final FtpClientConfig config,
                               final X509TrustManager trustManager,
                               final HostnameVerifier hostnameVerifier) throws FtpException{
        try {
            return getBoundFtps(identity, config, 0L, trustManager, hostnameVerifier);
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
    private Ftps getBoundFtps( final Object identity,
                               final FtpClientConfig config,
                               final long timeout,
                               final X509TrustManager trustManager,
                               final HostnameVerifier hostnameVerifier) throws ConnectionPoolTimeoutException, FtpException {
        Ftps ftps = null;
        FtpsInfo fi = null;

        boolean gotLock = false;
        while (!gotLock) {
            try {
                if (timeout == 0L) lock.readLock().lock();
                else if(!lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    throw new ConnectionPoolTimeoutException("Timeout acquiring lock.");
                }
                gotLock = true;
                if (connectionsById.size() != 0) {
                    fi = connectionsById.get(identity);
                    if (fi != null) {
                        synchronized ( fi.syncLock ) {
                            if( config != null && isValid(fi) && !fi.isDisposed() && fi.ftps.isConnected() ) {
                                ftps = fi.getFtps();
                                if (ftps == null) {
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
            }
            catch(InterruptedException ie) {
                logger.log(Level.WARNING, "Unexpected interruption acquiring read lock.", ie);
            }
            finally {
                if (gotLock) lock.readLock().unlock();
            }
        }

        if ( fi != null && ftps != null ) {
            if ( !buildHostConfiguration(ftps, config, trustManager, hostnameVerifier) ) {
                // Then release the connection and fall back to getting a new one from the pool
                Ftps connectionForClosing = ftps;
                ftps = null;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Releasing bound FTPS connection for identity '"+identity+"', does not match required host configuration.");
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
        return ftps;
    }

    public boolean setBoundFtp( final Object identity,
                                final Ftps ftps,
                                boolean reuse) {
        boolean bound = false;
        boolean gotLock = false;
        try {
            lock.writeLock().lock();
            gotLock = true;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Binding FTP connection for identity '"+identity+"'.");
            }

            FtpsInfo previouslyBoundFtpInfo = connectionsById.put(identity, new FtpsInfo(ftps));

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
                ftps.disconnect(); // close to ensure connection is not re-used
            }
        }

        return bound;
    }

    /**
     * Construct a host configuration based on the given connection.
     *
     * @param ftps The FTPS Client that describes the HostConfiguration
     * @return boolean true if constructed HostConfiguration matches the given Ftp connection configuration
     */
    private boolean buildHostConfiguration( final Ftps ftps ,
                                            FtpClientConfig config,
                                            final X509TrustManager trustManager,
                                            final HostnameVerifier hostnameVerifier) throws FtpException{

        boolean isEqual = false;
        CertificateVerifier configCertificateVerifier = null;

        if (trustManager != null && hostnameVerifier != null)  {
            configCertificateVerifier = new CertificateVerifier(trustManager, hostnameVerifier, config.getHost());
        }

        FtpClientConfig hostConfiguration = FtpClientConfigImpl.newFtpConfig(ftps.getHostname());

        String host = ftps.getHostname();
        int port = ftps.getPort();
        String user = ftps.getUsername();
        String pass = ftps.getPassword();
        String dir = ftps.getDir();
        hostConfiguration.setHost(host);
        hostConfiguration.setUser(user);
        hostConfiguration.setPass(pass);
        hostConfiguration.setPort(port);
        hostConfiguration.setDirectory(dir);
        CertificateVerifier hostCertificateVerifier = (CertificateVerifier) ftps.getFtpsCertificateVerifier();

        if (config.getHost().equals(hostConfiguration.getHost()) &&
            config.getUser().equals(hostConfiguration.getUser()) &&
            config.getPass().equals(hostConfiguration.getPass()) &&
            config.getPort() == hostConfiguration.getPort() &&
            config.getDirectory().equals(hostConfiguration.getDirectory()) &&
            configCertificateVerifier  == hostCertificateVerifier) {
            return true;
        }

        return isEqual;
    }


    private ThreadLocalInfo setInfo( final Object identity,
                                     final X509TrustManager trustManager,
                                     final HostnameVerifier hostnameVerifier ) throws FtpException{
        ThreadLocalInfo newInfo = info.get();
        if (identity == null) {
            newInfo = null;
        } else if (newInfo == null || !newInfo.getId().equals(identity)) {
            if (newInfo != null) {
                getBoundFtps(newInfo.getId(), null, trustManager, hostnameVerifier); // ensure any currently bound connection is closed.
            }

            newInfo = new ThreadLocalInfo(identity);
        }

        info.set(newInfo);

        return newInfo;
    }

    private boolean isValid( final FtpsInfo ftpsInfo ) {
        return isValid(ftpsInfo, System.currentTimeMillis());
    }

    private boolean isValid( final FtpsInfo ftpsInfo,
                             final long atTime ) {
        boolean valid = true;

        synchronized ( ftpsInfo.syncLock ) {
            if ( atTime - ftpsInfo.getLastUsageTime()>((long)bindingTimeout)) {
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Holder for Ftps Connection data
     */
    private final class FtpsInfo extends Info {

        private Ftps ftps;

        private FtpsInfo( final Ftps ftps ) {
            super();
            this.ftps = ftps;
        }

        public Ftps getFtps() {
            return ftps;
        }


        /**
         * Release a connection.
         *
         * <p>Note that if the id is set this will NOT release the underlying connection.</p>
         */
        public boolean release() {
            synchronized ( syncLock ) {
                final ThreadLocalInfo tli = getInfo();
                final Ftps ftps = getFtps();
                final Object identity = tli!=null ? tli.getId() : null;

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Releasing connection for " + identity);
                }

                boolean bound = false;
                if (identity != null && tli.hasBindingStatus() && ftps != null && ftps.isConnected()) {
                    if (tli.bindingRequested()) {
                        bound = setBoundFtp(identity, ftps, false);
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
                Ftps disposeMe = ftps;
                ftps = null;
                inUse = false;
                disposed = true;

                if (disposeMe != null) {
                    disposeMe.disconnect();
                }
            }
        }
    }

    /**
     * FTPS certificate verifier.
     *
     * <p>This is the way a verifier works: During {@link Ftps#connect}, the
     * authorized() method gets invoked first. If authorized() returns false,
     * then the verify() method will be invoked to verify the certificate,
     * after which the authorized() method is invoked again. If it still
     * returns false, an FtpException is thrown with the message
     * "Could not authenticate when has not been authorized". It's not a very
     * clear message so we provide a {@link #throwIfFailed} method to return a
     * better exception message.
     */
    private static class CertificateVerifier implements FtpsCertificateVerifier {
        private final X509TrustManager _trustManager;
        private final HostnameVerifier _hostnameVerifier;
        private final String _hostName;
        private boolean _authorized = false;
        private FtpException _exception;

        public CertificateVerifier(X509TrustManager trustManager, HostnameVerifier hostnameVerifier, String hostName) {
            _trustManager = trustManager;
            _hostnameVerifier = hostnameVerifier;
            _hostName = hostName;
        }

        @Override
        public boolean authorized() {
            return _authorized;
        }

        @Override
        public void verify(SSLSession sslSession) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Verifying FTP server (" + _hostName + ") SSL certificate using trusted certificate store.");
            }
            Certificate[] certs;
            try {
                certs = sslSession.getPeerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_NO_CERT.getMessage(), _hostName, e.getMessage()));
                return;
            }
            final X509Certificate[] x509certs = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; ++ i) {
                if (certs[i] instanceof X509Certificate) {
                    x509certs[i] = (X509Certificate)certs[i];
                } else {
                    _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_NOT_X509.getMessage(), _hostName));
                    return;
                }
            }

            try {
                _trustManager.checkServerTrusted(x509certs, CertUtils.extractAuthType(sslSession.getCipherSuite()));
                if ( _hostnameVerifier == null || _hostnameVerifier.verify( _hostName, sslSession ) ) {
                    _authorized = true;
                    _exception = null;
                } else {
                    _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_UNTRUSTED.getMessage(), _hostName, "Hostname verification failed"));
                }
            } catch (CertificateException e) {
                _exception = new FtpException(MessageFormat.format(SystemMessages.FTP_SSL_UNTRUSTED.getMessage(), _hostName, ExceptionUtils.getMessage(e)));
            }
        }

        /**
         * @throws FtpException if an exception was encountered during {@link #verify}.
         */
        public void throwIfFailed() throws FtpException {
            if (_exception != null) throw _exception;
        }
    }
}
