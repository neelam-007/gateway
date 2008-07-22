package com.l7tech.server.transport;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.objectmodel.FindException;
import org.springframework.context.ApplicationEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for transport modules for connectors for incoming FTP, HTTP, etc.
 * 
 * @see com.l7tech.server.transport.http.HttpTransportModule
 * @see com.l7tech.server.transport.ftp.FtpServerManager
 */
public abstract class TransportModule extends LifecycleBean {
    protected final Logger logger;
    protected final SsgConnectorManager ssgConnectorManager;
    protected final Map<Long, Throwable> connectorErrors = new ConcurrentHashMap<Long, Throwable>();

    public static final class ListenerException extends Exception {
        public ListenerException(String message) {
            super(message);
        }

        public ListenerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    protected TransportModule(String name, Logger logger, String licenseFeature, LicenseManager licenseManager, SsgConnectorManager ssgConnectorManager)
    {
        super(name, logger, licenseFeature, licenseManager);
        this.logger = logger;
        this.ssgConnectorManager = ssgConnectorManager;
    }

    /**
     * Attempt to add and spin up the specified connector on this transport module.
     *
     * @param connector  the connector to add.  Required.
     * @throws ListenerException if a proble is immediately detected while starting the new connector.
     */
    protected abstract void addConnector(SsgConnector connector) throws ListenerException;

    /**
     * Shut down and remove any connector from this transport module with the specified oid.
     * <p/>
     * If the specified oid is not recognized as a previously-added connector,
     * this method silently does nothing.
     *
     * @param oid the oid of the connector to remove.
     */
    protected abstract void removeConnector(long oid);

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

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (TransportModule.isEventIgnorable(applicationEvent)) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if (!isStarted())
            return;

        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (SsgConnector.class.equals(event.getEntityClass()))
                handleSsgConnectorInvalidationEvent(event);
        }
    }

    private void handleSsgConnectorInvalidationEvent(EntityInvalidationEvent event) {
        long[] ids = event.getEntityIds();
        char[] operations = event.getEntityOperations();
        for (int i = 0; i < ids.length; i++)
            handleSsgConnectorOperation(operations[i], ids[i]);
    }

    private void handleSsgConnectorOperation(char operation, long connectorId) {
        try {
            switch (operation) {
                case EntityInvalidationEvent.CREATE:
                case EntityInvalidationEvent.UPDATE:
                    createOrUpdateConnector(connectorId);
                    break;
                case EntityInvalidationEvent.DELETE:
                    removeConnector(connectorId);
                    break;
                default:
                    logger.warning("Unrecognized entity operation: " + operation);
                    break;
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error processing change for connector oid " + connectorId + ": " + ExceptionUtils.getMessage(t), t);
            connectorErrors.put(connectorId, t);
        }
    }

    private void createOrUpdateConnector(long connectorId) throws FindException, ListenerException {
        SsgConnector c = ssgConnectorManager.findByPrimaryKey(connectorId);
        if (c == null) {
            // Already removed
            return;
        }
        if (c.isEnabled() && connectorIsOwnedByThisModule(c) && isValidConnectorConfig(c))
            addConnector(c);
        else
            removeConnector(connectorId);
    }
}
