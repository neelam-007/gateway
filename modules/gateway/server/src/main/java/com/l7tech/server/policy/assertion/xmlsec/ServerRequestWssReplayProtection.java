package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.message.Message;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.*;
import com.l7tech.util.*;
import com.l7tech.common.io.CertUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * This assertion asserts that this message had a signed timestamp, and that no message with this timestamp signed
 * by one of the same signing tokens has been seen recently.
 */
public class ServerRequestWssReplayProtection extends AbstractServerAssertion<RequestWssReplayProtection> {
    private static final long EXPIRY_GRACE_TIME_MILLIS = 1000L * 60 * 1; // allow messages expired up to 1 minute ago
    private static final long MAXIMUM_MESSAGE_AGE_MILLIS = 1000L * 60 * 60 * 24 * 30; // hard cap of 30 days old
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60 * 5; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY_TIME = 1000L * 60 * 10; // if no Expires, assume expiry after 10 min

    private final Auditor auditor;
    private final MessageIdManager messageIdManager;
    private final SecurityTokenResolver securityTokenResolver;

    public ServerRequestWssReplayProtection(RequestWssReplayProtection subject, ApplicationContext spring) {
        super(subject);
        this.auditor = new Auditor(this, spring, logger);
        this.messageIdManager = (MessageIdManager)spring.getBean("distributedMessageIdManager");
        this.securityTokenResolver = (SecurityTokenResolver)spring.getBean("securityTokenResolver");
    }

    private static final class MultipleSenderIdException extends Exception { }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
        final Message msg;
        try {
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.FAILED;
        }
        final String what = assertion.getTargetName();

        ProcessorResult wssResults;
        try {
            if (!msg.isSoap()) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_NOT_SOAP, what);
                return AssertionStatus.NOT_APPLICABLE;
            }

            wssResults = WSSecurityProcessorUtils.getWssResults(msg, what, securityTokenResolver, auditor);

            if (wssResults == null) {
                // WssProcessorUtil.getWssResults already audited any error messages
                if (assertion.getTarget() == TargetMessageType.REQUEST) {
                    // If we're dealing with something other than the request, there's no point sending a challenge or
                    // policy URL to the original requestor.
                    context.setRequestPolicyViolated();
                    context.setAuthenticationMissing();
                }
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            // In practice, this can only happen if a mutating assertion (e.g. XSLT or Regex) has changed this message
            // from SOAP to non-SOAP--if it was originally SOAP, Trogdor will have already run, and the message will
            // have been parsed before we get here.
            throw (IOException)new IOException(ExceptionUtils.getMessage(e)).initCause(e);
        }

        // See if there's a wsa:MessageID
        String wsaMessageId = null;
        for (SignedElement signedElement : wssResults.getElementsThatWereSigned()) {
            Element el = signedElement.asElement();
            if (DomUtils.elementInNamespace(el, SoapConstants.WSA_NAMESPACE_ARRAY) && SoapConstants.MESSAGEID_EL_NAME.equals(el.getLocalName())) {
                if (wsaMessageId != null) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MULTIPLE_MESSAGE_IDS, what);
                    return AssertionStatus.BAD_REQUEST;
                } else {
                    wsaMessageId = DomUtils.getTextValue(el);
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_GOT_SIGNED_MESSAGE_ID, what, wsaMessageId);
                    // continue in order to detect multiple MessageIDs
                }
            }
        }

        if (wsaMessageId == null) auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_SIGNED_MESSAGE_ID, what);
        // OK to proceed with timestamp alone

        // Validate timestamp
        WssTimestamp timestamp = wssResults.getTimestamp();
        if (timestamp == null) {
            context.setRequestPolicyViolated();
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_TIMESTAMP, what);
            return AssertionStatus.BAD_REQUEST;
        }
        if (!timestamp.isSigned()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NOT_SIGNED, what);
            return AssertionStatus.BAD_REQUEST;
        }

        final WssTimestampDate createdTimestamp = timestamp.getCreated();
        if (createdTimestamp == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT, what);
            return AssertionStatus.BAD_REQUEST;
        }

        final String createdIsoString = createdTimestamp.asIsoString().trim();
        final long created = createdTimestamp.asTime();
        final long now = System.currentTimeMillis();
        long expires;
        if (timestamp.getExpires() != null) {
            expires = timestamp.getExpires().asTime();
        } else {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT, what, String.valueOf(DEFAULT_EXPIRY_TIME));
            expires = created + DEFAULT_EXPIRY_TIME;
        }

        if (expires <= (now - EXPIRY_GRACE_TIME_MILLIS)) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_STALE_TIMESTAMP, what);
            return AssertionStatus.BAD_REQUEST;
        }

        if (created > now)
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CLOCK_SKEW, what, String.valueOf(created));

        if (created <= (now - MAXIMUM_MESSAGE_AGE_MILLIS)) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CREATED_TOO_OLD, what);
            return AssertionStatus.BAD_REQUEST;
        }

        final String messageIdStr;
        if (wsaMessageId != null) {
            messageIdStr = wsaMessageId;
        } else {
            try {
                String senderId = getSenderId(wssResults.getSigningTokens(timestamp.asElement()), createdIsoString, null);
                if (senderId == null) return AssertionStatus.BAD_REQUEST;
                messageIdStr = senderId;
            } catch (MultipleSenderIdException e) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MULTIPLE_SENDER_IDS, what);
                return AssertionStatus.BAD_REQUEST;
            }
        }

        MessageId messageId = new MessageId(messageIdStr, expires + CACHE_ID_EXTRA_TIME_MILLIS);
        try {
            messageIdManager.assertMessageIdIsUnique(messageId);
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED, messageIdStr, what);
        } catch (MessageIdManager.DuplicateMessageIdException e) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_REPLAY, messageIdStr, what);
            return AssertionStatus.BAD_REQUEST;
        }

        return AssertionStatus.NONE;
    }

    private String getSenderId(XmlSecurityToken[] signingTokens, String createdIsoString, String what)
        throws MultipleSenderIdException, UnsupportedEncodingException
    {
        String senderId = null;

        for (XmlSecurityToken signingToken : signingTokens) {
            if (signingToken instanceof X509SigningSecurityToken) {
                if (senderId != null) throw new MultipleSenderIdException();
                X509Certificate signingCert = ((X509SigningSecurityToken)signingToken).getMessageSigningCertificate();
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT, what);

                // Use cert info as sender id
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(createdIsoString.getBytes("UTF-8"));
                    md.update(signingCert.getSubjectDN().toString().getBytes("UTF-8"));
                    md.update(signingCert.getIssuerDN().toString().getBytes("UTF-8"));
                    md.update(skiToString(signingCert).getBytes("UTF-8"));
                    byte[] digest = md.digest();
                    senderId = HexUtils.hexDump(digest);
                } catch (CertificateEncodingException e) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_SKI, what, signingCert.getSubjectDN().getName());
                    return null;
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e); // can't happen, misconfigured VM
                }
            } else if (signingToken instanceof SecurityContextToken) {
                if (senderId != null) throw new MultipleSenderIdException();
                // It was signed by a WS-SecureConversation session's derived key
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SC_KEY, what);
                String sessionID = ((SecurityContextToken)signingToken).getContextIdentifier();

                // Use session ID as sender ID
                StringBuffer sb = new StringBuffer();
                sb.append(createdIsoString);
                sb.append(";");
                sb.append("SessionID=");
                sb.append(sessionID);
                senderId = sb.toString();
            } else if (signingToken instanceof EncryptedKey) {
                if (senderId != null) throw new MultipleSenderIdException();
                // It was signed by an EncryptedKey
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_ENC_KEY, what);
                final String encryptedKeySha1;
                // Since it's a signing token, we can assume it must already have been unwrapped
                encryptedKeySha1 = ((EncryptedKey)signingToken).getEncryptedKeySHA1();

                StringBuffer sb = new StringBuffer();
                sb.append(createdIsoString);
                sb.append(";");
                sb.append("EncryptedKeySHA1=");
                sb.append(encryptedKeySha1);
                senderId = sb.toString();
            } else {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_UNSUPPORTED_TOKEN_TYPE, what, signingToken.getClass().getName());
                return null;
            }
            }
        return senderId;
        }

    private String skiToString(X509Certificate signingCert) throws CertificateEncodingException {
        byte[] ski = CertUtils.getSKIBytesFromCert(signingCert);
        return ski == null ? "" : HexUtils.hexDump(ski);
    }

    private static final Logger logger = Logger.getLogger(ServerRequestWssReplayProtection.class.getName());
}
