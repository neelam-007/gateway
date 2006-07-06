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
    private final JmsConnectionManager _connectionManager;
    private final JmsEndpointManager _endpointManager;

    private Set _activeReceivers = new HashSet();

    private final Logger _logger = Logger.getLogger(getClass().getName());
    private boolean started = false;
    private ConnectionVersionChecker connectionChecker;
    private PeriodicVersionCheck endpointChecker;


    public JmsBootProcess(ServerConfig serverConfig,
                          LicenseManager licenseManager,
                          JmsConnectionManager _connectionManager,
                          JmsEndpointManager _endpointManager)
    {
        this.serverConfig = serverConfig;
        this.licenseManager = licenseManager;
        this._connectionManager = _connectionManager;
        this._endpointManager = _endpointManager;
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
                            _logger.log(Level.SEVERE,
                                        "Unable to start JMS message input subsystem: " + ExceptionUtils.getMessage(e),
                                        e);
                        }
                    }
                }, 250);
            }
        }
    }

    public void destroy() throws Exception {
        stop();
    }

    public void afterPropertiesSet() throws Exception {
        start();
    }

    /**
     * Periodically checks for new, updated or deleted JMS endpoints
     */
    private class EndpointVersionChecker extends PeriodicVersionCheck {
        EndpointVersionChecker( JmsEndpointManager mgr ) throws FindException {
            super( mgr );
        }

        protected void onDelete( long removedOid ) {
            _logger.info( "Endpoint " + removedOid + " deleted!" );
            endpointDeleted( removedOid );
        }

        protected void onSave( Entity updatedEntity ) {
            _logger.info( "Endpoint " + updatedEntity.getOid() + " created or updated!" );
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
            _logger.info( "Connection " + removedOid + " deleted!" );
            connectionDeleted( removedOid );
        }

        protected void onSave( Entity updatedEntity ) {
            _logger.info( "Connection " + updatedEntity.getOid() + " created or updated!" );
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

        try {
            // Start up receivers for initial configuration
            Collection connections = _connectionManager.findAll();
            List staleEndpoints = new ArrayList();
            for ( Iterator i = connections.iterator(); i.hasNext(); ) {
                JmsConnection connection = (JmsConnection) i.next();
                JmsEndpoint[] endpoints = _endpointManager.findEndpointsForConnection( connection.getOid() );

                for ( int j = 0; j < endpoints.length; j++ ) {
                    JmsEndpoint endpoint = endpoints[j];
                    if ( !endpoint.isMessageSource() ) continue;
                    JmsReceiver receiver = makeReceiver( connection, endpoint );

                    try {
                        receiver.setApplicationContext(applicationContext);
                        receiver.setServerConfig(serverConfig);
                        receiver.start();
                        _activeReceivers.add( receiver );
                    } catch ( LifecycleException e ) {
                        _logger.log( Level.WARNING, "Couldn't start receiver for endpoint " + endpoint
                                                    + ".  Will retry periodically", e );
                        staleEndpoints.add( new Long( endpoint.getOid() ) );
                    }
                }
            }

            connectionChecker = new ConnectionVersionChecker( _connectionManager );
            endpointChecker = new EndpointVersionChecker( _endpointManager );

            for ( Iterator i = staleEndpoints.iterator(); i.hasNext(); ) {
                Long oid = (Long) i.next();
                endpointChecker.markObjectAsStale( oid );
            }
        } catch ( FindException e ) {
            String msg = "Couldn't start JMS subsystem!  JMS functionality will be disabled.";
            _logger.log( Level.SEVERE, msg, e );
            throw new LifecycleException( msg, e );
        }

        // Start periodic check timer
        Background.scheduleRepeated( connectionChecker, connectionChecker.getFrequency() * 2,
                                connectionChecker.getFrequency() );

        Background.scheduleRepeated( endpointChecker, endpointChecker.getFrequency() * 2,
                                endpointChecker.getFrequency() );
    }

    /**
     * Attempts to stop all running JMS receivers, then stops the {@link EndpointVersionChecker} and
     * {@link ConnectionVersionChecker}
     */
    // todo make this idempotent?
    private void stop() {
        connectionChecker.cancel();
        endpointChecker.cancel();
        for (Iterator i = _activeReceivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            _logger.info("Stopping and closing JMS receiver '" + receiver.toString() + "'");
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
            _logger.warning("Exception while stopping or closing " + component);
        }
    }

    /**
     * Handles the event fired by the deletion of a JmsConnection.
     */
    private synchronized void connectionDeleted( long deletedConnectionOid ) {
        for (Iterator i = _activeReceivers.iterator(); i.hasNext();) {
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

            EntityHeader[] endpoints = _endpointManager.findEndpointHeadersForConnection( updatedOid );
            for ( int i = 0; i < endpoints.length; i++ ) {
                EntityHeader header = endpoints[i];
                JmsEndpoint endpoint = _endpointManager.findByPrimaryKey( header.getOid() );
                endpointUpdated( endpoint );
            }
        } catch ( FindException e ) {
            _logger.log( Level.SEVERE, "Caught exception finding endpoints for a connection!", e );
        }
    }

    /**
     * Handles the event generated by the discovery of the deletion of a JmsEndpoint.
     *
     * @param deletedEndpointOid the OID of the endpoint that has been deleted.
     */
    private synchronized void endpointDeleted( long deletedEndpointOid ) {
        for (Iterator i = _activeReceivers.iterator(); i.hasNext();) {
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
                JmsConnection connection = _connectionManager.findConnectionByPrimaryKey( updatedEndpoint.getConnectionOid() );
                receiver = makeReceiver( connection, updatedEndpoint);
                receiver.setApplicationContext(applicationContext);
                receiver.setServerConfig(serverConfig);
                receiver.start();
                _activeReceivers.add(receiver);
            } catch (LifecycleException e) {
                _logger.warning("Exception while initializing receiver " + receiver +
                                "; will try again later: " + e.toString());
                try {
                    Thread.sleep(500);
                } catch ( InterruptedException e1 ) {
                    Thread.currentThread().interrupt();
                }
                if (endpointChecker != null)
                    endpointChecker.markObjectAsStale( new Long(updatedEndpoint.getOid()));
            } catch ( FindException e ) {
                _logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }

    private JmsReceiver makeReceiver( JmsConnection connection, JmsEndpoint endpoint) {
        JmsReceiver receiver = new JmsReceiver(
                connection,
                endpoint,
                endpoint.getReplyType());
        return receiver;
    }
}
