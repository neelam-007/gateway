/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.PeriodicVersionCheck;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.event.system.LicenseEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsBootProcess implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener {
    private ApplicationContext applicationContext;
    private final ServerConfig serverConfig;
    private final LicenseManager licenseManager;
    private final JmsConnectionManager connectionManager;
    private final JmsEndpointManager endpointManager;

    private Set<JmsReceiver> activeReceivers = new HashSet<JmsReceiver>();

    private static final Logger logger = Logger.getLogger(JmsBootProcess.class.getName());
    private boolean started = false;
    private ConnectionVersionChecker connectionChecker;
    private PeriodicVersionCheck endpointChecker;


    public JmsBootProcess(ServerConfig serverConfig,
                          LicenseManager licenseManager,
                          JmsConnectionManager connectionManager,
                          JmsEndpointManager endpointManager)
    {
        this.serverConfig = serverConfig;
        this.licenseManager = licenseManager;
        this.connectionManager = connectionManager;
        this.endpointManager = endpointManager;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext!=null) throw new IllegalStateException("applicationContext already initialized!");
        this.applicationContext = applicationContext;
    }

    public String toString() {
        return "JMS Boot Process";
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof LicenseEvent) {
            // If the JMS input subsystem becomes licensed after bootup, start it now
            // We do not, however, support de-licensing an already-active JMS subsystem without a reboot
            synchronized (this) {
                if (started)
                    return;  //avoid cost of scheduling oneshot timertask if we have already started
            }

            if (licenseManager.isFeatureEnabled(GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT)) {
                Background.scheduleOneShot(new TimerTask() {
                    public void run() {
                        try {
                            start();
                        } catch (LifecycleException e) {
                            logger.log(Level.SEVERE,
                                        "Unable to start JMS message input subsystem: " + ExceptionUtils.getMessage(e),
                                        e);
                        }
                    }
                }, 250);
            }
        }
        else if (applicationEvent instanceof ContextRefreshedEvent) {
            // Start on the refresh event since the auditing system won't work before the initial
            // refresh is completed
            try {
                start();
            } catch (LifecycleException e) {
                    logger.log(Level.SEVERE,
                                "Unable to start JMS message input subsystem: " + ExceptionUtils.getMessage(e),
                                e);
                }
            }
    }

    public void destroy() throws Exception {
        stop();
    }

    public void afterPropertiesSet() throws Exception {
        if(applicationContext == null) throw new IllegalStateException("applicationContext is required.");
        if(serverConfig == null) throw new IllegalStateException("serverConfig is required.");
        if(licenseManager == null) throw new IllegalStateException("licenseManager is required.");
        if(connectionManager == null) throw new IllegalStateException("connectionManager is required.");
        if(endpointManager == null) throw new IllegalStateException("endpointManager is required.");
    }

    /**
     * Periodically checks for new, updated or deleted JMS endpoints
     */
    private class EndpointVersionChecker extends PeriodicVersionCheck {
        EndpointVersionChecker( JmsEndpointManager mgr ) throws FindException {
            super( mgr );
        }

        protected void onDelete( long removedOid ) {
            logger.info( "Endpoint " + removedOid + " deleted!" );
            endpointDeleted( removedOid );
        }

        protected void onSave( Entity updatedEntity ) {
            logger.info( "Endpoint " + updatedEntity.getOid() + " created or updated!" );
            endpointUpdated( (JmsEndpoint)updatedEntity );
        }
    }

    /**
     * Periodically checks for new, updated or deleted JMS connections
     */
    private class ConnectionVersionChecker extends PeriodicVersionCheck {
        ConnectionVersionChecker( JmsConnectionManager mgr ) throws FindException {
            super(mgr);
        }

        protected void onDelete( long removedOid ) {
            logger.info( "Connection " + removedOid + " deleted!" );
            connectionDeleted( removedOid );
        }

        protected void onSave( Entity updatedEntity ) {
            logger.info( "Connection " + updatedEntity.getOid() + " created or updated!" );
            connectionUpdated( (JmsConnection)updatedEntity );
        }
    }


    /**
     * Starts {@link JmsReceiver}s for the initial configuration.  Also starts the {@link EndpointVersionChecker} and {@link ConnectionVersionChecker} to periodically
     * check whether endpoints or connections have been created, updated or deleted.
     * <p/>
     * Any exception that is thrown in a JmsReceiver's start() method will be logged but not propagated.
     */
    private void start() throws LifecycleException {
        if (!licenseManager.isFeatureEnabled(GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT))
            return; // should we log here?  currently (per req.) we just pretend there is no JMS code present at all

        synchronized (this) {
            if (started) return;
            started = true;  // "started" just means that we have already once attempted to start the JMS listener subsystem
        }

        logger.info("JMS starting.");

        try {
            // Start up receivers for initial configuration
            Collection<JmsConnection> connections = connectionManager.findAll();
            List<Long> staleEndpoints = new ArrayList<Long>();
            for (JmsConnection connection : connections) {
                JmsEndpoint[] endpoints = endpointManager.findEndpointsForConnection(connection.getOid());

                for (JmsEndpoint endpoint : endpoints) {
                    if (!endpoint.isMessageSource()) continue;
                    JmsReceiver receiver = new JmsReceiver(connection, endpoint, endpoint.getReplyType());

                    try {
                        receiver.setApplicationContext(applicationContext);
                        receiver.setServerConfig(serverConfig);
                        receiver.start();
                        activeReceivers.add(receiver);
                    } catch (LifecycleException e) {
                        logger.log(Level.WARNING, "Couldn't start receiver for endpoint " + endpoint
                                + ".  Will retry periodically", e);
                        staleEndpoints.add(new Long(endpoint.getOid()));
                    }
                }
            }

            connectionChecker = new ConnectionVersionChecker(connectionManager);
            endpointChecker = new EndpointVersionChecker(endpointManager);

            for (Long oid : staleEndpoints) {
                endpointChecker.markObjectAsStale(oid);
            }
        } catch ( FindException e ) {
            String msg = "Couldn't start JMS subsystem!  JMS functionality will be disabled.";
            logger.log( Level.SEVERE, msg, e );
            throw new LifecycleException( msg, e );
        }

        // Start periodic check timer
        Background.scheduleRepeated( connectionChecker, connectionChecker.getFrequency() * 2,
                                connectionChecker.getFrequency() );

        Background.scheduleRepeated( endpointChecker, endpointChecker.getFrequency() * 2,
                                endpointChecker.getFrequency() );

        logger.info("JMS started.");
    }

    /**
     * Attempts to stop all running JMS receivers, then stops the {@link EndpointVersionChecker} and
     * {@link ConnectionVersionChecker}
     */
    // todo make this idempotent?
    private void stop() {
        connectionChecker.cancel();
        endpointChecker.cancel();
        for (JmsReceiver receiver : activeReceivers) {
            logger.info("Stopping and closing JMS receiver '" + receiver.toString() + "'");
            stopAndClose(receiver);
        }
    }

    /**
     * Stops a JmsReceiver. Logs but does not propagate any exception that might be thrown.
     */
    private void stopAndClose(JmsReceiver component) {
        try {
            component.stop();
            component.close();
        } catch (LifecycleException e) {
            logger.warning("Exception while stopping or closing " + component);
        }
    }

    /**
     * Handles the event fired by the deletion of a JmsConnection.
     */
    private synchronized void connectionDeleted( long deletedConnectionOid ) {
        for (Iterator i = activeReceivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            if (receiver.getConnection().getOid() == deletedConnectionOid ) {
                stopAndClose(receiver);
                i.remove();
            }
        }
    }

    /**
     * Handles the event fired by the update of a JmsConnection.
     */
    private synchronized void connectionUpdated(JmsConnection updatedConnection) {
        long updatedOid = updatedConnection.getOid();

        try {
            // Stop and remove any existing receivers for this connection
            connectionDeleted( updatedOid );

            EntityHeader[] endpoints = endpointManager.findEndpointHeadersForConnection( updatedOid );
            for (EntityHeader header : endpoints) {
                JmsEndpoint endpoint = endpointManager.findByPrimaryKey(header.getOid());
                endpointUpdated(endpoint);
            }
        } catch ( FindException e ) {
            logger.log( Level.SEVERE, "Caught exception finding endpoints for a connection!", e );
        }
    }

    /**
     * Handles the event generated by the discovery of the deletion of a JmsEndpoint.
     *
     * @param deletedEndpointOid the OID of the endpoint that has been deleted.
     */
    private synchronized void endpointDeleted( long deletedEndpointOid ) {
        for (Iterator i = activeReceivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            JmsEndpoint existingEndpoint = receiver.getInboundRequestEndpoint();
            if (existingEndpoint.getOid() == deletedEndpointOid ) {
                stopAndClose(receiver);
                i.remove();
            }
        }
    }

    /**
     * Handles the event fired by the update or creation of a JmsEndpoint.
     *
     * Calls endpointDeleted to shut down any receiver(s) that might already
     * be listening to that endpoint.
     *
     * @param updatedEndpoint the JmsEndpoint that has been created or updated.
     */
    private synchronized void endpointUpdated( JmsEndpoint updatedEndpoint ) {
        // Stop any existing receivers for this endpoint
        endpointDeleted( updatedEndpoint.getOid() );

        if (updatedEndpoint.isMessageSource()) {
            JmsReceiver receiver = null;
            try {
                JmsConnection connection = connectionManager.findByPrimaryKey( updatedEndpoint.getConnectionOid() );
                receiver = new JmsReceiver(connection, updatedEndpoint, updatedEndpoint.getReplyType());
                receiver.setApplicationContext(applicationContext);
                receiver.setServerConfig(serverConfig);
                receiver.start();
                activeReceivers.add(receiver);
            } catch (LifecycleException e) {
                logger.warning("Exception while initializing receiver " + receiver +
                                "; will try again later: " + e.toString());
                try {
                    Thread.sleep(500);
                } catch ( InterruptedException e1 ) {
                    Thread.currentThread().interrupt();
                }
                if (endpointChecker != null)
                    endpointChecker.markObjectAsStale( new Long(updatedEndpoint.getOid()));
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }

}
