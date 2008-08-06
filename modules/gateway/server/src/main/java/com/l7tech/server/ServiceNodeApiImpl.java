/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.node.EventSubscription;
import com.l7tech.server.management.api.node.ServiceNodeApi;
import com.l7tech.server.management.config.monitoring.MonitoringScheme;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.transport.http.HttpTransportModule;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;

import javax.annotation.Resource;
import javax.jws.WebService;
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
@WebService(name="ServiceNodeAPI", targetNamespace = "http://ns.l7tech.com/secureSpan/5.0/component/processController/serviceNodeApi")
public class ServiceNodeApiImpl implements ServiceNodeApi, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(ServiceNodeApiImpl.class.getName());

    private final SsgConnectorManager ssgConnectorManager;

    private AbstractApplicationContext spring;

    @Resource
    private WebServiceContext context;

    public ServiceNodeApiImpl(SsgConnectorManager ssgConnectorManager) {
        this.ssgConnectorManager = ssgConnectorManager;
    }

    public void shutdown() {
        checkEndpoint();
        logger.warning("shutdown");
        spring.stop();
        spring.destroy();
        System.exit(0);
    }

    private void checkEndpoint() {
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
        checkEndpoint();
        logger.info("ping");
    }

    public Set<SsgConnector> getConnectors() throws FindException {
        checkEndpoint();
        return new HashSet(ssgConnectorManager.findAll());
    }

    public NodeStatus getNodeStatus() {
        checkEndpoint();
        logger.info("getNodeStatus");
        return new NodeStatus();
    }

    public void pushMonitoringScheme(MonitoringScheme scheme) throws UpdateException {
        checkEndpoint();
        logger.info("pushMonitoringScheme");
    }

    public MonitoringScheme getMonitoringScheme() throws FindException {
        checkEndpoint();
        logger.info("getMonitoringScheme");
        return null;
    }

    public Set<EventSubscription> subscribeEvents(Set<String> eventIds) throws UnsupportedEventException, SaveException {
        checkEndpoint();
        logger.info("subscribeEvents");
        return null;
    }

    public Set<EventSubscription> renewEventSubscriptions(Set<String> subscriptionIds) throws UpdateException {
        checkEndpoint();
        logger.info("renewEventSubscriptions");
        return null;
    }

    public void releaseEventSubscriptions(Set<String> subscriptionIds) throws DeleteException {
        checkEndpoint();
        logger.info("releaseEventSubscriptions");
    }

    public Object getProperty(String propertyId) throws UnsupportedPropertyException, FindException {
        checkEndpoint();
        logger.info("getProperty");
        return null;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.spring = (AbstractApplicationContext)applicationContext;
    }
}
