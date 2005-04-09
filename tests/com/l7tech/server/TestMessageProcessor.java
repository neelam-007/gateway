/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.io.IOException;

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
     * @param sm           the service manager
     * @param wssd         the Wss Decorator
     * @param pkey         the server private key
     * @param pkey         the server certificate
     * @param auditContext the audit context
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public TestMessageProcessor(ServiceManager sm, WssDecorator wssd, PrivateKey pkey, X509Certificate cert, AuditContext auditContext)
      throws IllegalArgumentException {
        super(sm, wssd, pkey, cert, auditContext);
    }

    public AssertionStatus processMessage(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException, PolicyVersionException {

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