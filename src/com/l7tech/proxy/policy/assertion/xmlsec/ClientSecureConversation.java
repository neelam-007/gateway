package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.message.Message;
import com.l7tech.common.security.token.SecurityContextToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecorationRequirements.SimpleSecureConversationSession;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
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

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException,
            GeneralSecurityException, ClientCertificateException, IOException, SAXException,
            KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException,
            PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException
    {
        // Establish session, if possible
        final String sessionId = context.getOrCreateSecureConversationId();
        final byte[] sessionKey = context.getSecureConversationSharedSecret();

        // Configure outbound decoration to use WS-SecureConversation
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) {
                DecorationRequirements wssReqs = context.getDefaultWssRequirements();
                wssReqs.setSignTimestamp();
                wssReqs.setSecureConversationSession(new SimpleSecureConversationSession(sessionId, sessionKey, SoapUtil.WSSC_NAMESPACE));
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            IOException, SAXException, ResponseValidationException, KeyStoreCorruptException,
            PolicyAssertionException, InvalidDocumentFormatException
    {
        final Message response = context.getResponse();
        if (!response.isSoap()) {
            log.info("Response is not SOAP; SecureConversation is therefore not applicable");
            return AssertionStatus.NOT_APPLICABLE;
        }
        // Make sure the response's WssProcessor.Results contain a reference to the Secure Conversation
        ProcessorResult pr = context.getResponse().getSecurityKnob().getProcessorResult();
        if (pr == null) {
            log.info("WSS processing was not done on this response.");
            return AssertionStatus.FAILED;
        }
        XmlSecurityToken[] tokens = pr.getXmlSecurityTokens();
        SecurityContextToken sct = null;
        for (int i = 0; i < tokens.length; i++) {
            XmlSecurityToken token = tokens[i];
            if (token instanceof SecurityContextToken) {
                SecurityContextToken checkSct = (SecurityContextToken)token;
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

        String expectedSessionId = context.getSecureConversationId();
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

    private static final Logger log = Logger.getLogger(ClientSecureConversation.class.getName());
}
