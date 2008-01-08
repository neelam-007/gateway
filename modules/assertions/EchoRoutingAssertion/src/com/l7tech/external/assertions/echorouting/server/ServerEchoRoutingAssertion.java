/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.echorouting.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.external.assertions.echorouting.EchoRoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assertion that echoes the request into the response.
 */
public class ServerEchoRoutingAssertion extends ServerRoutingAssertion<EchoRoutingAssertion> {
    //- PUBLIC

    public ServerEchoRoutingAssertion(EchoRoutingAssertion ea, ApplicationContext applicationContext) {
        super(ea, applicationContext, logger);
        auditor = new Auditor(this, applicationContext, logger);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();

        final MimeKnob mimeKnob = (MimeKnob) request.getKnob(MimeKnob.class);
        final ContentTypeHeader cth = mimeKnob == null ? null : mimeKnob.getOuterContentType();

        if (cth == null) {
            auditor.logAndAudit(AssertionMessages.CANNOT_ECHO_NO_CTYPE);
            return AssertionStatus.NOT_APPLICABLE;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        if (request.isXml()) {
            try {
                handleProcessedSecurityHeader(context,
                                              data.getCurrentSecurityHeaderHandling(),
                                              data.getXmlSecurityActorToPromote());
            } catch(SAXException se) {
                logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
            }
        }

        // Initialize request
        try {
            response.initialize(stashManagerFactory.createStashManager(),
                                cth,
                                mimeKnob.getEntireMessageBodyAsInputStream());
            context.setRoutingStatus(RoutingStatus.ROUTED); // Ensure routing status set (Bug #4570)
            return AssertionStatus.NONE;
        } catch (NoSuchPartException nspe) {
            throw new CausedIOException("Unable copy request to response.", nspe);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerEchoRoutingAssertion.class.getName());
    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;
}
