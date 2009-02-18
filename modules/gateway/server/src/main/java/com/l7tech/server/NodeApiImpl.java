/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.boot.ShutdownWatcher;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.node.EventSubscription;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.audit.AuditRecordManager;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of the Cluster Node API hosted by the Cluster Node and used by the Process Controller.
 *
 * @author alex
 */
public class NodeApiImpl implements NodeApi {
    private static final Logger logger = Logger.getLogger(NodeApiImpl.class.getName());

    @Resource
    private ServerConfig serverConfig;

    @Resource
    private SsgConnectorManager ssgConnectorManager;

    @Resource
    private ShutdownWatcher shutdowner;

    @Resource
    private AuditRecordManager auditRecordManager;

    @SuppressWarnings({"SpringJavaAutowiringInspection"})
    @Resource
    private WebServiceContext wscontext; // Injected by CXF to get access to request metadata (e.g. HttpServletRequest)

    @PostConstruct
    public void start() {
        if (serverConfig == null || ssgConnectorManager == null || shutdowner == null) throw new NullPointerException("A required component is missing");
    }

    private boolean isProcessControllerPresent() {
        return serverConfig.getBooleanPropertyCached(ServerConfig.PARAM_PROCESS_CONTROLLER_PRESENT, false, 30000);
    }

    public void shutdown() {
        checkRequest();
        logger.warning("Node Shutdown requested");
        shutdowner.shutdownNow();
    }

    private void checkRequest() {
        if (!isProcessControllerPresent()) throw new IllegalStateException(NODE_NOT_CONFIGURED_FOR_PC);
        final HttpServletRequest hsr = (HttpServletRequest)wscontext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        if (hsr == null) throw new IllegalStateException("Request received outside of expected servlet context");
        try {
            HttpTransportModule.requireEndpoint(hsr, SsgConnector.Endpoint.PC_NODE_API);

            if ( !InetAddress.getByName(hsr.getRemoteAddr()).isLoopbackAddress()) {
                throw new IllegalStateException("Request denied for non-local address.");
            }
        } catch (TransportModule.ListenerException e) {
            // TODO come up with a friendlier way to throw a SOAP fault here
            throw new IllegalStateException(e);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException("Request denied for non-local address.", uhe);
        }
    }

    public void ping() {
        checkRequest();
        logger.fine("ping");
    }

    public Set<SsgConnector> getConnectors() throws FindException {
        checkRequest();
        return new HashSet<SsgConnector>(ssgConnectorManager.findAll());
    }

    public NodeStatus getNodeStatus() {
        checkRequest();
        logger.fine("getNodeStatus");
        return new NodeStatus(NodeStateType.RUNNING, new Date(), new Date()); // TODO
    }

    public Set<EventSubscription> subscribeEvents(Set<String> eventIds) throws UnsupportedEventException, SaveException {
        checkRequest();
        logger.fine("subscribeEvents");
        return null;
    }

    public Set<EventSubscription> renewEventSubscriptions(Set<String> subscriptionIds) throws UpdateException {
        checkRequest();
        logger.fine("renewEventSubscriptions");
        return null;
    }

    public void releaseEventSubscriptions(Set<String> subscriptionIds) throws DeleteException {
        checkRequest();
        logger.fine("releaseEventSubscriptions");
    }

    public String getProperty(String propertyId) throws UnsupportedPropertyException, FindException {
        checkRequest();
        logger.fine("getProperty");
        if (propertyId.equals(BuiltinMonitorables.AUDIT_SIZE.getName())) {
            return Long.toString(auditRecordManager.findCount(new AuditSearchCriteria.Builder().build()));
        } else {
            throw new UnsupportedPropertyException("propertyId");
        }
    }
}
