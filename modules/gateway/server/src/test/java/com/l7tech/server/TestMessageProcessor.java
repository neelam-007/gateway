/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsManagerImpl;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.SoapFaultManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * TestMessageProcessor that allows access to the <code>PolicyEnforcementContext</code>
 *
 * @author emil
 * @version Apr 8, 2005
 */
public class TestMessageProcessor extends MessageProcessor {
    private Set<MessageProcessorListener> listeners = new HashSet<MessageProcessorListener>();

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sc           the service cache
     * @param wssd         the Wss Decorator
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public TestMessageProcessor(ServiceCache sc, PolicyCache pc, WssDecorator wssd)
      throws IllegalArgumentException {
        super(sc, pc, wssd, null, new TestLicenseManager(), new ServiceMetricsManagerImpl("yo",null), new AuditContextStub(), ServerConfig.getInstance(), new TrafficLogger(ServerConfig.getInstance(), null), new SoapFaultManager(ServerConfig.getInstance(), new ManagedTimer("Soap fault manager refresh")));
    }

    @Override
    public AssertionStatus processMessage(PolicyEnforcementContext context)
        throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException {

        try {
            notifyListenersBefore(context);
            return super.processMessage(context);
        } finally {
            notifyListenersAfter(context);
        }
    }

    /**
     * Add the <code>MessageProcessorListener</code> that will be
     * invoked before processing the message.
     *
     * @param l the message processor listener to register
     */
    public void addProcessorListener(MessageProcessorListener l) {
        listeners.add(l);
    }

    public void removeProcessorListener(MessageProcessorListener l) {
        listeners.remove(l);
    }

    private void notifyListenersBefore(PolicyEnforcementContext context) {
        for( MessageProcessorListener messageProcessorListener : listeners ) {
            messageProcessorListener.beforeProcessMessage( context );
        }
    }

    private void notifyListenersAfter(PolicyEnforcementContext context) {
        for( MessageProcessorListener messageProcessorListener : listeners ) {
            messageProcessorListener.afterProcessMessage( context );
        }
    }
}
