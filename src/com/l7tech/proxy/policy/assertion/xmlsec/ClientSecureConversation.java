package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The client side processing of the SecureConversation assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 */
public class ClientSecureConversation extends ClientAssertion {
    public ClientSecureConversation(SecureConversation assertion) {
        // nothing in assertion we need to remember
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            IOException, SAXException, ResponseValidationException, KeyStoreCorruptException,
            PolicyAssertionException, InvalidDocumentFormatException
    {
        // Make sure the response's WssProcessor.Results contain a reference to the Secure Conversation
        WssProcessor.ProcessorResult pr = response.getProcessorResult();
        WssProcessor.SecurityToken[] tokens = pr.getSecurityTokens();
        WssProcessor.SecurityContextToken sct = null;
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken token = tokens[i];
            if (token instanceof WssProcessor.SecurityContextToken) {
                WssProcessor.SecurityContextToken checkSct = (WssProcessor.SecurityContextToken)token;
                if (!checkSct.isPossessionProved()) {
                    log.log(Level.FINE, "Ignoring SecurityContextToken that was not used to sign anything");
                    continue;
                }
                sct = checkSct;
            }
        }

        if (sct == null) {
            log.log(Level.INFO, "Response did not contain a proven SecurityContextToken; assertion fails");
            return AssertionStatus.FALSIFIED;
        }

        String expectedSessionId = request.getSecureConversationId();
        if (expectedSessionId == null) {
            // can't actually happen; decorateRequest should have made one
            final String msg = "Request did not have a session ID set.";
            log.log(Level.SEVERE, msg);
            throw new IllegalStateException(msg);
        }

        if (!expectedSessionId.equals(sct.getContextIdentifier())) {
            log.log(Level.WARNING, "Response contained a proven SecurityContextToken, " +
                                   "but the session ID did not match the one we used in the request.");
            return AssertionStatus.FALSIFIED;
        }

        // Response contained a proven token for this conversation.
        log.log(Level.FINE, "Response contained a proven SecurityContextToken for this session; assertion succeeds.");
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Require valid WS-SecureConversation session";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    public AssertionStatus decorateRequest(PendingRequest request)
            throws BadCredentialsException, OperationCanceledException,
            GeneralSecurityException, ClientCertificateException, IOException, SAXException,
            KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException,
            PolicyAssertionException, InvalidDocumentFormatException
    {
        // Establish session, if possible
        final String sessionId = request.getOrCreateSecureConversationId();
        final byte[] sessionKey = request.getSecureConversationSharedSecret();

        // Configure outbound decoration to use WS-SecureConversation
        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request) {
                WssDecorator.DecorationRequirements wssReqs = request.getWssRequirements();
                wssReqs.setSignTimestamp(true);
                wssReqs.setSecureConversationSession(new WssDecorator.DecorationRequirements.SecureConversationSession() {
                    public String getId() {
                        return sessionId;
                    }

                    public byte[] getSecretKey() {
                        return sessionKey;
                    }
                });
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    private static final Logger log = Logger.getLogger(ClientSecureConversation.class.getName());
}
