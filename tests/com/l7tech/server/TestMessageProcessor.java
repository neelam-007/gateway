/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.common.LicenseException;
import com.l7tech.common.TestLicenseManager;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.log.TrafficLogger;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * TestMessageProcessor that allows access to the <code>PolicyEnforcementContext</code>
 *
 * @author emil
 * @version Apr 8, 2005
 */
public class TestMessageProcessor extends MessageProcessor {
    private Set listeners = new HashSet();

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sc           the service cache
     * @param wssd         the Wss Decorator
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public TestMessageProcessor(ServiceCache sc, WssDecorator wssd)
      throws IllegalArgumentException {
        super(sc , wssd, null, new TestLicenseManager(), new ServiceMetricsManager("yo",null), new AuditContextStub(), ServerConfig.getInstance(), new TrafficLogger(ServerConfig.getInstance(), null));
    }

    public AssertionStatus processMessage(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException {

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
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            MessageProcessorListener messageProcessorListener = (MessageProcessorListener)iterator.next();
            messageProcessorListener.beforeProcessMessage(context);
        }
    }

    private void notifyListenersAfter(PolicyEnforcementContext context) {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            MessageProcessorListener messageProcessorListener = (MessageProcessorListener)iterator.next();
            messageProcessorListener.afterProcessMessage(context);
        }
    }
}