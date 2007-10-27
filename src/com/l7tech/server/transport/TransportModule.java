package com.l7tech.server.transport;

import com.l7tech.server.LifecycleBean;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.transport.SsgConnector;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;

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
     * Get the set of schema names recognized by this transport module.
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

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (SsgConnector.class.equals(event.getEntityClass())) {
                long[] ids = event.getEntityIds();
                char[] ops = event.getEntityOperations();
                for (int i = 0; i < ids.length; i++) {
                    long id = ids[i];
                    try {
                        switch (ops[i]) {
                        case EntityInvalidationEvent.CREATE:
                        case EntityInvalidationEvent.UPDATE:
                            SsgConnector c = ssgConnectorManager.findByPrimaryKey(id);
                            if (!connectorIsOwnedByThisModule(c))
                                break;
                            if (c.isEnabled())
                                addConnector(c);
                            else
                                removeConnector(id);
                            break;
                        case EntityInvalidationEvent.DELETE:
                            removeConnector(id);
                            break;
                        }
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Error processing change for connector oid " + id + ": " + ExceptionUtils.getMessage(t), t);
                        connectorErrors.put(id, t);
                    }
                }
            }
        }
    }
}
