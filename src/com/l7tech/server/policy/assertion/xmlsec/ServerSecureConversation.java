package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.SecurityContextToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import org.xml.sax.SAXException;

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
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                logger.info("Request not SOAP; unable to check for WS-SecureConversation token");
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if (wssResults == null) {
            logger.info("This request did not contain any WSS level security.");
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
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
                    context.setRequestPolicyViolated();
                    return AssertionStatus.AUTH_FAILED;
                }
                User authenticatedUser = session.getUsedBy();
                context.setAuthenticated(true);
                context.setAuthenticatedUser(authenticatedUser);
                context.addDeferredAssertion(this, deferredSecureConversationResponseDecoration(session));
                logger.fine("Secure Conversation session recognized for user " + authenticatedUser.getLogin());
                return AssertionStatus.NONE;
            }
        }
        logger.info("This request did not seem to refer to a Secure Conversation token.");
        context.setAuthenticationMissing();
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final ServerAssertion deferredSecureConversationResponseDecoration(final SecureConversationSession session) {
        return new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
                DecorationRequirements wssReq;
                try {
                    if (!context.getResponse().isSoap()) {
                        logger.warning("Response not SOAP; unable to attach WS-SecureConversation token");
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                    wssReq = context.getResponse().getXmlKnob().getOrMakeDecorationRequirements();
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }
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
