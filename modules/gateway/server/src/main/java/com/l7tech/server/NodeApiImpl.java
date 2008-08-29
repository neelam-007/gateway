/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.boot.ShutdownWatcher;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.node.EventSubscription;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.MonitoringScheme;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.transport.http.HttpTransportModule;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of the Service Node API used by the Process Controller.
 *
 * TODO authentication/trust?
 * @author alex
 */
public class NodeApiImpl implements NodeApi, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(NodeApiImpl.class.getName());

    /** Injected by Spring post-construct */
    @Resource
    private ServerConfig serverConfig;

    /** Injected by Spring post-construct */
    @Resource
    private SsgConnectorManager ssgConnectorManager;

    /** Injected by Spring post-construct */
    @Resource
    private ShutdownWatcher shutdowner;

    /** Injected by CXF to get access to request metadata */
    @Resource
    private WebServiceContext context;

    @PostConstruct
    private void start() {
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
        final HttpServletRequest hsr = (HttpServletRequest)context.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        if (hsr == null) throw new IllegalStateException("Request received outside of expected servlet context");
        try {
            HttpTransportModule.requireEndpoint(hsr, SsgConnector.Endpoint.PC_NODE_API);
        } catch (TransportModule.ListenerException e) {
            // TODO come up with a friendlier way to throw a SOAP fault here
            throw new IllegalStateException(e);
        }
    }

    public void ping() {
        checkRequest();
        logger.info("ping");
    }

    public Set<SsgConnector> getConnectors() throws FindException {
        checkRequest();
        return new HashSet(ssgConnectorManager.findAll());
    }

    public NodeStatus getNodeStatus() {
        checkRequest();
        logger.info("getNodeStatus");
        return new NodeStatus();
    }

    public void pushMonitoringScheme(MonitoringScheme scheme) throws UpdateException {
        checkRequest();
        logger.info("pushMonitoringScheme");
    }

    public MonitoringScheme getMonitoringScheme() throws FindException {
        checkRequest();
        logger.info("getMonitoringScheme");
        return null;
    }

    public Set<EventSubscription> subscribeEvents(Set<String> eventIds) throws UnsupportedEventException, SaveException {
        checkRequest();
        logger.info("subscribeEvents");
        return null;
    }

    public Set<EventSubscription> renewEventSubscriptions(Set<String> subscriptionIds) throws UpdateException {
        checkRequest();
        logger.info("renewEventSubscriptions");
        return null;
    }

    public void releaseEventSubscriptions(Set<String> subscriptionIds) throws DeleteException {
        checkRequest();
        logger.info("releaseEventSubscriptions");
    }

    public Object getProperty(String propertyId) throws UnsupportedPropertyException, FindException {
        checkRequest();
        logger.info("getProperty");
        return null;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }
}
