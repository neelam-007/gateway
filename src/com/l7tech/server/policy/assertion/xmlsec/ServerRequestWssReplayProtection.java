package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssTimestamp;
import com.l7tech.common.security.xml.processor.WssTimestampDate;
import com.l7tech.common.util.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
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

    public ServerRequestWssReplayProtection(RequestWssReplayProtection subject, ApplicationContext ctx) {
        super(subject);
        this.auditor = new Auditor(this, ctx, logger);
        this.messageIdManager = (MessageIdManager)ctx.getBean("distributedMessageIdManager");
    }

    private static final class MultipleSenderIdException extends Exception { }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
        ProcessorResult wssResults;

        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NON_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
            if (wssResults == null) {
                auditor.logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
                context.setRequestPolicyViolated();
                context.setAuthenticationMissing();
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
            if (XmlUtil.elementInNamespace(el, SoapUtil.WSA_NAMESPACE_ARRAY) && SoapUtil.MESSAGEID_EL_NAME.equals(el.getLocalName())) {
                if (wsaMessageId != null) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MULTIPLE_MESSAGE_IDS);
                    return AssertionStatus.BAD_REQUEST;
                } else {
                    wsaMessageId = XmlUtil.getTextValue(el);
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_GOT_SIGNED_MESSAGE_ID, wsaMessageId);
                    // continue in order to detect multiple MessageIDs
                }
            }
        }

        if (wsaMessageId == null) auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_SIGNED_MESSAGE_ID);
        // OK to proceed with timestamp alone

        // Validate timestamp
        WssTimestamp timestamp = wssResults.getTimestamp();
        if (timestamp == null) {
            context.setRequestPolicyViolated();
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_TIMESTAMP);
            return AssertionStatus.BAD_REQUEST;
        }
        if (!timestamp.isSigned()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NOT_SIGNED);
            return AssertionStatus.BAD_REQUEST;
        }

        final WssTimestampDate createdTimestamp = timestamp.getCreated();
        if (createdTimestamp == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT);
            return AssertionStatus.BAD_REQUEST;
        }

        final String createdIsoString = createdTimestamp.asIsoString().trim();
        final long created = createdTimestamp.asTime();
        final long now = System.currentTimeMillis();
        long expires;
        if (timestamp.getExpires() != null) {
            expires = timestamp.getExpires().asTime();
        } else {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT, String.valueOf(DEFAULT_EXPIRY_TIME));
            expires = created + DEFAULT_EXPIRY_TIME;
        }

        if (expires <= (now - EXPIRY_GRACE_TIME_MILLIS)) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_STALE_TIMESTAMP);
            return AssertionStatus.BAD_REQUEST;
        }

        if (created > now)
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CLOCK_SKEW, String.valueOf(created));

        if (created <= (now - MAXIMUM_MESSAGE_AGE_MILLIS)) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CREATED_TOO_OLD);
            return AssertionStatus.BAD_REQUEST;
        }

        final String messageIdStr;
        if (wsaMessageId != null) {
            messageIdStr = wsaMessageId;
        } else {
            try {
                String senderId = getSenderId(wssResults.getSigningTokens(timestamp.asElement()), createdIsoString);
                if (senderId == null) return AssertionStatus.BAD_REQUEST;
                messageIdStr = senderId;
            } catch (MultipleSenderIdException e) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MULTIPLE_SENDER_IDS);
                return AssertionStatus.BAD_REQUEST;
            }
        }

        MessageId messageId = new MessageId(messageIdStr, expires + CACHE_ID_EXTRA_TIME_MILLIS);
        try {
            messageIdManager.assertMessageIdIsUnique(messageId);
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED, messageIdStr);
        } catch (MessageIdManager.DuplicateMessageIdException e) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_REPLAY, messageIdStr);
            return AssertionStatus.BAD_REQUEST;
        }

        return AssertionStatus.NONE;
    }

    private String getSenderId(XmlSecurityToken[] signingTokens, String createdIsoString)
        throws MultipleSenderIdException, UnsupportedEncodingException
    {
        String senderId = null;

        for (XmlSecurityToken signingToken : signingTokens) {
            if (signingToken instanceof X509SigningSecurityToken) {
                if (senderId != null) throw new MultipleSenderIdException();
                X509Certificate signingCert = ((X509SigningSecurityToken)signingToken).getMessageSigningCertificate();
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT);

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
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_SKI, signingCert.getSubjectDN().getName());
                    return null;
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e); // can't happen, misconfigured VM
                }
            } else if (signingToken instanceof SecurityContextToken) {
                if (senderId != null) throw new MultipleSenderIdException();
                // It was signed by a WS-SecureConversation session's derived key
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SC_KEY);
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
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_ENC_KEY);
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
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_UNSUPPORTED_TOKEN_TYPE, signingToken.getClass().getName());
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
