package com.l7tech.server.transport;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.util.Background;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.audit.SystemMessages.*;

/**
 * Abstract superclass for transport modules for connectors for incoming FTP, HTTP, etc.
 * 
 * @see com.l7tech.server.transport.http.HttpTransportModule
 * @see com.l7tech.server.transport.ftp.FtpServerManager
 */
public abstract class TransportModule extends LifecycleBean {
    protected final Logger logger;
    protected final Component component;
    protected final SsgConnectorManager ssgConnectorManager;
    private final TrustedCertServices trustedCertServices;
    private final DefaultKey defaultKey;
    private final Config config;
    private final Object invalidationSync = new Object();

    protected TransportModule( @NotNull  final String name,
                               @NotNull  final Component component,
                               @NotNull  final Logger logger,
                               @NotNull  final String licenseFeature,
                               @Nullable final LicenseManager licenseManager,
                               @NotNull  final SsgConnectorManager ssgConnectorManager,
                               @NotNull  final TrustedCertServices trustedCertServices,
                               @NotNull  final DefaultKey defaultKey,
                               @NotNull  final Config config )
    {
        super(name, logger, licenseFeature, licenseManager);
        this.component = component;
        this.logger = logger;
        this.ssgConnectorManager = ssgConnectorManager;
        this.trustedCertServices = trustedCertServices;
        this.defaultKey = defaultKey;
        this.config = config;
    }

    /**
     * Check whether the specified SsgConnector oid and version is unchanged from the last version seen by this connector.
     * If so, a possibly expensive removal and re-addition of the connector will be bypassed.
     * <p/>
     * This method always returns false.  Subclasses that cache active connector entities can override this
     * method with a cleverer implementation.
     *
     * @param goid      the SsgConnector object GOID.
     * @param version  the SsgConnector entity version.
     * @return true if this connector version is already current, and any associated entity update event should be ignored.
     *         false if this connector version may not be current and the connector should be removed and re-added.
     */
    protected boolean isCurrent( Goid goid, int version ) {
        return false;
    }

    /**
     * Attempt to add and spin up the specified connector on this transport module.
     *
     * @param connector  the connector to add.  Required.
     * @throws ListenerException if a proble is immediately detected while starting the new connector.
     */
    protected abstract void addConnector(SsgConnector connector) throws ListenerException;

    /**
     * Shut down and remove any connector from this transport module with the specified goid.
     * <p/>
     * If the specified goid is not recognized as a previously-added connector,
     * this method silently does nothing.
     *
     * @param goid the goid of the connector to remove.
     */
    protected abstract void removeConnector(Goid goid);

    /**
     * Get the set of scheme names recognized by this transport module.
     *
     * @return a set of scheme names, ie "HTTP" and "HTTPS".  Never null.
     */
    protected abstract Set<String> getSupportedSchemes();

    /**
     * Check if the specified scheme is recognized by this transport module.
     * <p/>
     * This method just checks if the named scheme is listed in {@link #getSupportedSchemes}.
     *
     * @param scheme  the scheme being considered, ie "HTTP" or "FTPS".  Required.
     * @return true if connectors with this scheme will be added by this transport module.
     */
    protected boolean schemeIsOwnedByThisModule(String scheme) {
        return getSupportedSchemes().contains(scheme);
    }

    /**
     * Check if the specified connector should be recognized by this transport module.
     * <p/>
     * This method just checks the scheme with {@link #schemeIsOwnedByThisModule(String)}.
     *
     * @param connector the connector being considered.  Required.
     * @return true if this conector should be added by this transport module.
     */
    protected boolean connectorIsOwnedByThisModule(SsgConnector connector) {
        return schemeIsOwnedByThisModule(connector.getScheme());
    }

    /**
     * Check if the specified connector is configured correctly for this transport module.
     * <p/>
     * The general contract is that for connectors owned by this module, they will only be put
     * in service if they are enabled and this method returns true; otherwise they will
     * be taken out of service.
     * <p/>
     * This method always returns true.
     *
     * @param connector the connector to examine.  Required.
     * @return true if this connector configuration looks valid for this transport module; 
     *         false if it is invalid and should be treated as though it were disabled
     */
    protected boolean isValidConnectorConfig(SsgConnector connector) {
        return true;
    }

    protected static boolean isEventIgnorable(ApplicationEvent applicationEvent) {
        return applicationEvent instanceof AuditDetailEvent ||
                applicationEvent instanceof MessageProcessed ||
                applicationEvent instanceof FaultProcessed;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if (TransportModule.isEventIgnorable(applicationEvent)) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if (!isStarted())
            return;

        if (applicationEvent instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (SsgConnector.class.equals(event.getEntityClass())) {
                Background.scheduleOneShot( new TimerTask() {
                    @Override
                    public void run() {
                        // Run invalidation on another thread in case it does
                        // it's own auditing
                        handleSsgConnectorInvalidationEvent( event );
                    }
                }, 0L );
            }
        }
    }

    private void handleSsgConnectorInvalidationEvent(EntityInvalidationEvent event) {
        synchronized ( invalidationSync ) {
            Goid[] ids = event.getEntityIds();
            char[] operations = event.getEntityOperations();
            for (int i = 0; i < ids.length; i++) {
                handleSsgConnectorOperation(operations[i], ids[i]);
            }
        }
    }

    private void handleSsgConnectorOperation(char operation, Goid connectorGoid) {
        try {
            switch (operation) {
                case EntityInvalidationEvent.CREATE:
                case EntityInvalidationEvent.UPDATE:
                    createOrUpdateConnector(connectorGoid);
                    break;
                case EntityInvalidationEvent.DELETE:
                    removeConnector(connectorGoid);
                    break;
                default:
                    logger.warning("Unrecognized entity operation: " + operation);
                    break;
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error processing change for connector oid " + connectorGoid + ": " + ExceptionUtils.getMessage(t), t);
        }
    }

    private void createOrUpdateConnector(Goid connectorGoid) throws FindException, ListenerException {
        SsgConnector c = ssgConnectorManager.findByPrimaryKey(connectorGoid);
        if (c == null) {
            // Already removed
            return;
        }

        // If this module keeps track of active entities, and can tell this update has already been processed,
        // skip the expensive removal and re-add.
        if (isCurrent(c.getGoid(), c.getVersion()))
            return;

        removeConnector(connectorGoid);
        if (c.isEnabled() && connectorIsOwnedByThisModule(c) && isValidConnectorConfig(c)) {
            final SsgConnector roc = c.getReadOnlyCopy();
            // TODO replace this background timer hack with something that won't fail if a foreign transport module takes longer than 200ms to relinquish the socket
            Background.scheduleOneShot(new TimerTask() {
                @Override
                public void run() {
                    try {
                        addConnector(roc);
                    } catch (Exception e) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        logger.log(Level.WARNING, "Unable to start " + roc.getScheme() + " connector on port " + roc.getPort() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
            }, 200L);
        }
    }

    /**
     * Get access to ServerConfig.  Provided for the benefit of transport drivers that may be created
     * via reflection by third-party code.
     *
     * @return the serverConfig instance.  Never null.
     */
    public Config getServerConfig() {
        return config;
    }

    /**
     * Get the private key entry that should be used for the specified connector, assuming that a TLS listen port
     * were to be created for it.
     *
     * @param c  the connector whose SSL server private key to locate.  Required.
     * @return an SsgKeyEntry for the specified connector.  Never null; will use the default SSL key if no other key is configured.
     * @throws ListenerException if no default SSL key is available.  This normally can't happen.
     */
    public SsgKeyEntry getKeyEntry(SsgConnector c) throws ListenerException {
        try {
            return c.getKeystoreGoid() == null ? defaultKey.getSslInfo() : defaultKey.lookupKeyByKeyAlias(c.getKeyAlias(), c.getKeystoreGoid());
        } catch (IOException e) {
            throw new ListenerException("No default SSL key is currently available: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new ListenerException("Unable to access private key for connector: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new ListenerException("Unable to access private key for connector: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Utility method that looks up accepted issuer certificates that should be advertised by an X509TrustManager
     * acting on behalf of the specified SsgConnector for this TransportModule.
     *
     * @param connector the connector to inquire about.  Required.
     * @return an array of X509Certificate instances that should be accepted as issuers at TLS handshake time for this connector.
     *         Never null or empty.
     * @throws com.l7tech.objectmodel.FindException if there is an error while attempting to look up the required information.
     */
    public X509Certificate[] getAcceptedIssuersForConnector(SsgConnector connector) throws FindException {
        // Suppress inclusion of accepted issuers list if so configured for this connector
        if (connector.getBooleanProperty("noAcceptedIssuers"))
            return new X509Certificate[0];

        // There's no point worrying about the accepted issuers list if we don't plan to ever send a client challenge.
        if (connector.getClientAuth() == SsgConnector.CLIENT_AUTH_NEVER)
            return new X509Certificate[0];

        String protocols = connector.getProperty(SsgConnector.PROP_TLS_OVERRIDE_PROTOCOLS);
        if (protocols == null) protocols = connector.getProperty(SsgConnector.PROP_TLS_PROTOCOLS);
        boolean onlyTls10 = protocols == null || (!protocols.contains("TLSv1.1") && !protocols.contains("TLSv1.2"));

        // If only TLS 1.0 is enabled, behave as we did pre-5.3, unless "acceptedIssuers" is forced to "true" (Bug #8727)
        // "acceptedIssuers" connector property not to be documented -- will be removed in future release
        if (onlyTls10 && !connector.getBooleanProperty("acceptedIssuers"))
            return new X509Certificate[0];

        Collection<TrustedCert> trustedCerts = trustedCertServices.getAllCertsByTrustFlags(EnumSet.of(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS));
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        for (TrustedCert trustedCert : trustedCerts) {
            certs.add(trustedCert.getCertificate());
        }

        // SSL-J requires at least one accepted issuer when client auth is set to anything but NEVER.  If using SSL-J with client auth,
        // and the list would otherwise be empty, we'll add our own server cert to the list to keep it from failing.
        if (!onlyTls10 && certs.isEmpty()) {
            try {
                certs.add( defaultKey.getSslInfo().getCertificate() );
            } catch (IOException e) {
                // This is non-fatal, at least for this purpose, since many TLS handshakes will succeed even with
                // an empty issuers list -- the purpose of this hack was just to work around an SSL-J issue.
                logger.log(Level.WARNING, "Unable to get default certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return certs.toArray(new X509Certificate[certs.size()]);
    }

    /**
     * Called by transport level code to notify that a particular connector has a configuration problem that prevents it from being opened in its current configuration.
     *
     * @param connectorGoid the GOID of the connector that won't open.
     */
    public abstract void reportMisconfiguredConnector(Goid connectorGoid);
    

    protected String describe( final SsgConnector connector ) {
        return connector.getName() + " (#" + connector.getGoid() + ",v" + connector.getVersion() + ") on port " + connector.getPort();
    }

    protected final void auditStart( final String scheme, final String connectorDescription ) {
        auditListenerStateChanged(CONNECTOR_START, new String[]{scheme, connectorDescription}, null);
    }

    protected final void auditStop( final String scheme, String connectorDescription) {
        auditListenerStateChanged(CONNECTOR_STOP, new String[]{scheme, connectorDescription}, null);
    }

    protected final void auditError( final String schemes, final String message, @Nullable final Throwable exception ) {
        auditListenerStateChanged(CONNECTOR_ERROR, new String[]{schemes, message}, exception);
    }

    /**
     * Immediately emit a system audit record containing the specified detail message.
     * Also sends the detail message to the log sinks.
     *
     * @param msg detail message.  Required.
     * @param params detail params. May be null or empty.
     * @param e  exception.  May be null.
     */
    protected final void auditListenerStateChanged(@NotNull AuditDetailMessage msg, @Nullable String[] params, @Nullable Throwable e) {
        final TransportEvent event = new TransportEvent(
            this,
            component,
            null,
            Level.INFO,
            "State Changed",
            "Listener state changed");

        event.setAuditDetails(Arrays.asList(new AuditDetail(msg, params, e)));
        getApplicationContext().publishEvent(event);
    }
}
