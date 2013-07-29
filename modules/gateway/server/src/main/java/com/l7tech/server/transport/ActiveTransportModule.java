package com.l7tech.server.transport;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import javax.inject.Inject;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.audit.SystemMessages.CONNECTOR_ERROR;

/**
 * Abstract superclass for active connector transport modules.
 */
public abstract class ActiveTransportModule extends LifecycleBean {
    protected final Logger logger;
    protected final Component component;
    private Audit audit;

    @Inject
    protected SsgActiveConnectorManager ssgActiveConnectorManager;

    protected ActiveTransportModule( @NotNull final String name,
                                     @NotNull  final Component component,
                                     @NotNull final Logger logger,
                                     @NotNull final String licenseFeature )
    {
        super(name, logger, licenseFeature, null);
        this.component = component;
        this.logger = logger;
    }

    @Inject
    @Override
    protected void setLicenseManager( final LicenseManager licenseManager ) {
        super.setLicenseManager( licenseManager );
    }

    /**
     * Override this method to indicate whether the transport module is initialized.
     *
     * <p>If a module is not initialized but is started then <code>doStart</code>
     * will be called when the Gateway is ready to process messages</p>
     *
     * @return True if initialized.
     */
    protected boolean isInitialized() {
        return isStarted();
    }

    /**
     * Check whether the specified SsgActiveConnector oid and version is unchanged from the last version seen.
     *
     * If so, a possibly expensive removal and re-addition of the SsgActiveConnector will be bypassed.
     * <p/>
     * This method always returns false.  Subclasses that cache active SsgActiveConnector entities can override this
     * method with a cleverer implementation.
     *
     * @param oid      the SsgActiveConnector object ID.
     * @param version  the SsgActiveConnector entity version.
     * @return true if this SsgActiveConnector version is already current, and any associated entity update event should be ignored.
     *         false if this SsgActiveConnector version may not be current and the SsgActiveConnector should be removed and re-added.
     */
    protected boolean isCurrent( final Goid oid, final int version ) {
        return false;
    }

    /**
     * Attempt to add and spin up the specified SsgActiveConnector on this transport module.
     *
     * @param ssgActiveConnector  the SsgActiveConnector to add.  Required.
     * @throws com.l7tech.server.transport.ListenerException if a problem is immediately detected while starting the new SsgActiveConnector.
     */
    protected abstract void addConnector( @NotNull SsgActiveConnector ssgActiveConnector ) throws ListenerException;

    /**
     * Shut down and remove any SsgActiveConnector from this transport module with the specified oid.
     * <p/>
     * If the specified oid is not recognized as a previously-added SsgActiveConnector,
     * this method silently does nothing.
     *
     * @param oid the oid of the SsgActiveConnector to remove.
     */
    protected abstract void removeConnector( Goid oid );

    /**
     * Get the set of connector types recognized by this transport module.
     *
     * @return a set of type names, e.g. "SFTP".  Never null.
     */
    protected abstract Set<String> getSupportedTypes();

    /**
     * Check if the specified type is recognized by this transport module.
     * <p/>
     * This method just checks if the named type is listed in {@link #getSupportedTypes}.
     *
     * @param type  the type being considered, e.g. "SFTP".  Required.
     * @return true if SsgActiveConnector with this type will be added by this transport module.
     */
    protected boolean typeIsOwnedByThisModule( @NotNull final String type ) {
        return getSupportedTypes().contains( type );
    }

    /**
     * Check if the specified SsgActiveConnector should be recognized by this transport module.
     * <p/>
     * This method just checks the type with {@link #typeIsOwnedByThisModule(String)}.
     *
     * @param ssgActiveConnector the SsgActiveConnector being considered.  Required.
     * @return true if this conector should be added by this transport module.
     */
    protected boolean connectorIsOwnedByThisModule( @NotNull final SsgActiveConnector ssgActiveConnector ) {
        return typeIsOwnedByThisModule( ssgActiveConnector.getType() );
    }

    /**
     * Check if the specified SsgActiveConnector is configured correctly for this transport module.
     * <p/>
     * The general contract is that for SsgActiveConnector owned by this module, they will only be put
     * in service if they are enabled and this method returns true; otherwise they will
     * be taken out of service.
     * <p/>
     * This method always returns true.
     *
     * @param ssgActiveConnector the SsgActiveConnector to examine.  Required.
     * @return true if the SsgActiveConnector configuration looks valid for this transport module;
     *         false if it is invalid and should be treated as though it were disabled
     */
    protected boolean isValidConnectorConfig( @NotNull final SsgActiveConnector ssgActiveConnector ) {
        return true;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if ( TransportModule.isEventIgnorable( applicationEvent )) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if (!isStarted())
            return;

        if (applicationEvent instanceof GoidEntityInvalidationEvent) {
            GoidEntityInvalidationEvent event = (GoidEntityInvalidationEvent)applicationEvent;
            if (SsgActiveConnector.class.equals(event.getEntityClass()))
                handleConnectorInvalidationEvent( event );
        } else if (applicationEvent instanceof ReadyForMessages && isStarted() && !isInitialized() ) {
            try {
                doStart();
            } catch ( LifecycleException e ) {
                logger.log(Level.WARNING, "Error starting transport module: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    private void handleConnectorInvalidationEvent( final GoidEntityInvalidationEvent event ) {
        Goid[] ids = event.getEntityIds();
        char[] operations = event.getEntityOperations();
        for (int i = 0; i < ids.length; i++) {
            handleConnectorOperation( operations[i], ids[i] );
        }
    }

    private void handleConnectorOperation( final char operation, final Goid connectorId ) {
        try {
            switch (operation) {
                case GoidEntityInvalidationEvent.CREATE:
                case GoidEntityInvalidationEvent.UPDATE:
                    createOrUpdateConnector( connectorId );
                    break;
                case GoidEntityInvalidationEvent.DELETE:
                    removeConnector( connectorId );
                    break;
                default:
                    logger.warning("Unrecognized entity operation: " + operation);
                    break;
            }
        } catch ( final Throwable t ) {
            logger.log(Level.WARNING, "Error processing change for active connector oid " + connectorId + ": " + ExceptionUtils.getMessage(t), t);
        }
    }

    private void createOrUpdateConnector( final Goid connectorId ) throws FindException, ListenerException {
        final SsgActiveConnector c = ssgActiveConnectorManager.findByPrimaryKey(connectorId);
        if (c == null) {
            // Already removed
            return;
        }

        // If this module keeps track of active entities, and can tell this update has already been processed,
        // skip the expensive removal and re-add.
        if (isCurrent(c.getGoid(), c.getVersion()))
            return;

        removeConnector( connectorId );
        if (c.isEnabled() && connectorIsOwnedByThisModule( c ) && isValidConnectorConfig( c )) {
            final SsgActiveConnector roc = c.getReadOnlyCopy();
            try {
                addConnector( roc );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to start " + roc.getType() + " active connector " + roc.getName() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    protected final void auditError( final String schemes, final String message, @Nullable final Throwable exception ) {
        getAudit().logAndAudit( CONNECTOR_ERROR, new String[]{schemes, message}, exception);
    }

    protected Audit getAudit() {
        Audit audit = this.audit;

        if (audit == null) {
            audit = new Auditor(this, getApplicationContext(), logger);
            this.audit = audit;
        }

        return audit;
    }
}
