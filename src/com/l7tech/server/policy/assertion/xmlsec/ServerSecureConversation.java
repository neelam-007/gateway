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
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.identity.User;

import java.io.IOException;
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
        WssProcessor.ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null) {
            throw new IOException("This request was not processed for WSS message level security.");
        }
        WssProcessor.SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken token = tokens[i];
            if (token instanceof WssProcessor.SecurityContextToken) {
                WssProcessor.SecurityContextToken secConTok = (WssProcessor.SecurityContextToken)token;
                String contextId = secConTok.getContextIdentifier();
                SecureConversationSession session = SecureConversationContextManager.getInstance().getSession(contextId);
                User authenticatedUser = session.getUsedBy();
                request.setAuthenticated(true);
                request.setUser(authenticatedUser);
                return AssertionStatus.NONE;
            }
        }
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
