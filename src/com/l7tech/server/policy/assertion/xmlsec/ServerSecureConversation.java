package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.SecurityContextToken;
import com.l7tech.common.security.xml.processor.SecurityToken;
import com.l7tech.identity.User;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SSG-side processing of the SecureConversation assertion.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 */
public class ServerSecureConversation implements ServerAssertion {
    public ServerSecureConversation(SecureConversation assertion) {
        // nothing to remember from the passed assertion
    }
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest)) {
            logger.info("This type of assertion is only supported with SOAP type of messages");
            return AssertionStatus.BAD_REQUEST;
        }
        SoapRequest soapreq = (SoapRequest)request;
        ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null) {
            throw new IOException("This request was not processed for WSS message level security.");
        }
        SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token instanceof SecurityContextToken) {
                SecurityContextToken secConTok = (SecurityContextToken)token;
                if (!secConTok.isPossessionProved()) {
                    logger.log(Level.FINE, "Ignoring SecurityContextToken with no proof-of-possession");
                    continue;
                }
                String contextId = secConTok.getContextIdentifier();
                SecureConversationSession session = SecureConversationContextManager.getInstance().getSession(contextId);
                if (session == null) {
                    logger.warning("The request referred to a SecureConversation token that is not recognized " +
                                   "on this server. Perhaps the session has expired. Returning AUTH_FAILED.");
                    response.setPolicyViolated(true);
                    return AssertionStatus.AUTH_FAILED;
                }
                User authenticatedUser = session.getUsedBy();
                request.setAuthenticated(true);
                request.setUser(authenticatedUser);
                response.addDeferredAssertion(this, deferredSecureConversationResponseDecoration(session));
                logger.fine("Secure Conversation session recognized for user " + authenticatedUser.getLogin());
                return AssertionStatus.NONE;
            }
        }
        logger.info("This request did not seem to refer to a Secure Conversation token.");
        response.setAuthenticationMissing(true);
        response.setPolicyViolated(true);
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final ServerAssertion deferredSecureConversationResponseDecoration(final SecureConversationSession session) {
        return new ServerAssertion() {
            public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
                if (!(response instanceof SoapResponse))
                    throw new PolicyAssertionException("This type of assertion is only supported with SOAP responses");
                SoapResponse soapResponse = (SoapResponse)response;
                DecorationRequirements wssReq = soapResponse.getOrMakeDecorationRequirements();
                wssReq.setSignTimestamp(true);
                wssReq.setSecureConversationSession(new DecorationRequirements.SecureConversationSession() {
                    public String getId() {
                        return session.getIdentifier();
                    }
                    public byte[] getSecretKey() {
                        return session.getSharedSecret().getEncoded();
                    }
                });
                return AssertionStatus.NONE;
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
