package com.l7tech.server.log.syslog.impl;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSL extension to the Mina Syslog IoHandler implementation.
 *
 * @author vchan
 */
public class MinaSecureSyslogHandler extends MinaSyslogHandler {

    /** Logger */
    private static final Logger logger = Logger.getLogger(MinaSecureSyslogHandler.class.getName());
    /** Separator char for errorMessages */
    private static final String ERROR_SEP = "; ";
    /** The idle time set for each IoSession managed by this handler */
    private static final int IDLE_TIME = 60 * 2; // seconds

    /** SSL filter */
    private SslFilter SSL_FILTER;
    /** SSL with client auth keystore alias (optional) */
    private String sslKeystoreAlias;
    /** SSL with client auth keystore id (optional) */
    private Goid sslKeystoreId;
    /** List of errors encountered by the handler before the connection is terminated */
    private StringBuffer errorMessages;
    /** Flag indicating whether the currently held session has been open */
    private boolean sessionOpened;
    /** Callback function to the syslog to check to check the IoSession */
    private final Functions.Unary<Boolean, IoSession> currentSessionCheckCallback;

    /**
     * Default constructor.
     *
     * @param sessionCallback the callback function to perform
     * @param currentSessionChecker the callback function to check for the referenced IoSession
     * @param sslKeystoreAlias the keystore alias name to use for the SSL KeyManager
     * @param sslKeystoreId the keystore Id to use for the SSL KeyManager
     */
    MinaSecureSyslogHandler(Functions.BinaryVoid<IoSession, String> sessionCallback,
                            Functions.Unary<Boolean, IoSession> currentSessionChecker,
                            String sslKeystoreAlias, Goid sslKeystoreId) {
        super(sessionCallback);
        this.currentSessionCheckCallback = currentSessionChecker;
        this.errorMessages = new StringBuffer();

        if (SSL_FILTER == null) {
            this.sslKeystoreAlias = sslKeystoreAlias;
            this.sslKeystoreId = sslKeystoreId;

            SslFilter filter = new SslFilter( getSSLContext() );
            filter.setUseClientMode(true);
            SSL_FILTER = filter;
        }
    }

    /**
     * Returns an SSLContext instance based on the arguments passed in the constructor.  When specified,
     * the keystore alias/id is used to query the keyManager.  Otherwise, the SSG default SSL MeyManager
     * will be used to establish the SSLContext.
     *
     * @return an SSLContext instance
     */
    private SSLContext getSSLContext() {

        try {
            KeyManager[] km;
            TrustManager tm = SyslogSslClientSupport.getTrustManager();
            if (sslKeystoreAlias != null && sslKeystoreId != null && !Goid.isDefault(sslKeystoreId)) {
                // use the configured sslKeystore
                try {
                    if (SyslogSslClientSupport.isInitialized()) {
                        SsgKeyEntry entry = SyslogSslClientSupport.getSsgKeyStoreManager().lookupKeyByKeyAlias(sslKeystoreAlias, sslKeystoreId);
                        KeyManager keyManager = new SingleCertX509KeyManager(entry.getCertificateChain(), entry.getPrivateKey());
                        km = new KeyManager[] { keyManager };
                    } else {
                        return null;
                    }
                } catch (GeneralSecurityException e) {
                    return null;
                } catch (ObjectModelException e) {
                    return null;
                }
            } else {
                // use default SSL keystore
                km = SyslogSslClientSupport.getDefaultKeyManagers();
            }

            // Create the SSLContext
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(km, new TrustManager[] { tm }, null);
            return ctx;

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.log(Level.WARNING, "Unable to instantiate SSLContext for Secure Syslog: {0}", ExceptionUtils.getMessage(e));
        }
        return null;
    }

    /**
     * Appends the SSL_FILTER to the connector's filter chain to enable SSL.
     *
     * @param connector the connector to initialize
     */
    public void setupConnectorForSSL(IoConnector connector) {
        connector.getFilterChain().addLast("ssl", SSL_FILTER);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable throwable) throws Exception {
        // capture the error message
        if (logger.isLoggable(Level.WARNING))
            this.errorMessages.append(ExceptionUtils.getMessage(throwable)).append(ERROR_SEP);
        super.exceptionCaught(session, throwable);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        this.sessionOpened = (session.isConnected() && !session.isClosing());
        // set the idle time
        session.getConfig().setIdleTime(IdleStatus.WRITER_IDLE, IDLE_TIME);
        super.sessionOpened(session);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        if (currentSessionCheckCallback.call(session))
            this.sessionOpened = false;
        if (errorMessages.length() > 0) {
            logger.log(Level.WARNING, "SSL session closed with errors: {0}", errorMessages.toString());
            errorMessages = new StringBuffer();
        }
        logger.log(Level.FINE, "Session closed after (ms): {0}", System.currentTimeMillis() - session.getCreationTime());
        super.sessionClosed(session);
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {

        // Close any IoSessions that is not currently referenced by the ManagedSyslog
        if (IdleStatus.WRITER_IDLE.equals(idleStatus) && !currentSessionCheckCallback.call(ioSession)) {
//            StringBuffer sb = new StringBuffer("closing idle session: ").append("\n");
//            sb.append("Idle count: ").append(ioSession.getIdleCount(IdleStatus.WRITER_IDLE)).append("\n");
//            sb.append("Idle time (sec): ").append(ioSession.getIdleTime(IdleStatus.WRITER_IDLE)).append("\n");
//            sb.append("Attribs: ").append(ioSession.getAttributeKeys()).append("\n");
//            sb.append("Remote Addr: ").append(ioSession.getRemoteAddress()).append("\n");
//            sb.append("Local Addr: ").append(ioSession.getLocalAddress()).append("\n");
//            sb.append("Create time: ").append(ioSession.getCreationTime()).append("\n");
//            sb.append("Last IO: ").append(ioSession.getLastIoTime()).append("\n");
//            sb.append("Since last IO: ").append(System.currentTimeMillis() - ioSession.getLastIoTime()).append("\n");
//            sb.append("Written Req: ").append(ioSession.getWrittenWriteRequests()).append("\n");
//            sb.append("written Msg: ").append(ioSession.getWrittenMessages());
//            System.out.println(sb.toString());

            ioSession.close(true);
        }

        super.sessionIdle(ioSession, idleStatus);
    }

    @Override
    public boolean verifySession(IoSession session) {
        boolean check = super.verifySession(session);
        if (check) {
            if (SSL_FILTER.isSslStarted(session) && sessionOpened) {
                return (SSL_FILTER.getSslSession(session) != null);
            }
        }
        return false;
    }

    /*
     * This method is only used for debug purposes
     */
    SSLSession getSSLSession(IoSession session) {
        return SSL_FILTER.getSslSession(session);
    }
}
