package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.identity.User;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

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
        WssProcessor.ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null) {
            throw new IOException("This request was not processed for WSS message level security.");
        }
        WssProcessor.SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken token = tokens[i];
            if (token instanceof WssProcessor.SecurityContextToken) {
                WssProcessor.SecurityContextToken secConTok = (WssProcessor.SecurityContextToken)token;
                if (!secConTok.isPossessionProved()) {
                    logger.log(Level.FINE, "Ignoring SecurityContextToken with no proof-of-possession");
                    continue;
                }
                String contextId = secConTok.getContextIdentifier();
                SecureConversationSession session = SecureConversationContextManager.getInstance().getSession(contextId);
                if (session == null) {
                    logger.warning("The request referred to a SecureConversation token that is not recognized " +
                                   "on this server. Perhaps the session has expired. Returning AUTH_FAILED.");
                    return AssertionStatus.AUTH_FAILED;
                }
                User authenticatedUser = session.getUsedBy();
                request.setAuthenticated(true);
                request.setUser(authenticatedUser);
                response.addDeferredAssertion(this, deferredSecureConversationResponseDecoration(session));
                logger.fine("Secure COnversation session recognized for user " + authenticatedUser.getLogin());
                return AssertionStatus.NONE;
            }
        }
        logger.info("This request did not seem to refer to a Secure Conversation token.");
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final ServerAssertion deferredSecureConversationResponseDecoration(final SecureConversationSession session) {
        return new ServerAssertion() {
            public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
                if (!(response instanceof SoapResponse))
                    throw new PolicyAssertionException("This type of assertion is only supported with SOAP responses");
                SoapResponse soapResponse = (SoapResponse)response;
                WssDecorator.DecorationRequirements wssReq = soapResponse.getOrMakeDecorationRequirements();
                wssReq.setSecureConversationSession(new WssDecorator.DecorationRequirements.SecureConversationSession() {
                    public String getId() {
                        return session.getIdentifier();
                    }
                    public SecretKey getSecretKey() {
                        return session.getSharedSecret();
                    }
                });
                return AssertionStatus.NONE;
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
