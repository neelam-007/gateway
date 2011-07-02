package com.l7tech.external.assertions.echorouting.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.MessageRole;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.util.CausedIOException;
import com.l7tech.external.assertions.echorouting.EchoRoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Assertion that echoes the request into the response.
 */
public class ServerEchoRoutingAssertion extends ServerRoutingAssertion<EchoRoutingAssertion> {

    //- PUBLIC

    public ServerEchoRoutingAssertion(EchoRoutingAssertion ea, ApplicationContext applicationContext) {
        super(ea, applicationContext);
        stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();

        // todo: move to abstract routing assertion
        request.notifyMessage(response, MessageRole.RESPONSE);
        response.notifyMessage(request, MessageRole.REQUEST);

        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);
        final ContentTypeHeader cth = (mimeKnob == null || !request.isInitialized()) ? null : mimeKnob.getOuterContentType();

        if (cth == null) {
            logAndAudit(AssertionMessages.CANNOT_ECHO_NO_CTYPE);
            return AssertionStatus.NOT_APPLICABLE;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(request);
        } catch(SAXException se) {
            logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
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

    private final StashManagerFactory stashManagerFactory;
}
