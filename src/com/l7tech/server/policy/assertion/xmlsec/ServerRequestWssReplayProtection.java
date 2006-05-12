package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.cluster.DistributedMessageIdManager;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssTimestamp;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.logging.Logger;

/**
 * This assertion asserts that this message had a signed timestamp, and that no message with this timestamp signed
 * by one of the same signing tokens has been seen recently.
 */
public class ServerRequestWssReplayProtection extends AbstractServerAssertion implements ServerAssertion {
    private static final long EXPIRY_GRACE_TIME_MILLIS = 1000L * 60 * 1; // allow messages expired up to 1 minute ago
    private static final long MAXIMUM_MESSAGE_AGE_MILLIS = 1000L * 60 * 60 * 24 * 30; // hard cap of 30 days old
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60 * 5; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY_TIME = 1000L * 60 * 10; // if no Expires, assume expiry after 10 min

    private final ApplicationContext applicationContext;
    private final Auditor auditor;

    public ServerRequestWssReplayProtection(RequestWssReplayProtection subject, ApplicationContext ctx) {
        super(subject);
        this.applicationContext = ctx;
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        ProcessorResult wssResults;

        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NON_SOAP);
                return AssertionStatus.BAD_REQUEST;
            }
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
            if (wssResults == null) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_WSS_LEVEL_SECURITY);
                context.setRequestPolicyViolated();
                context.setAuthenticationMissing();
                return AssertionStatus.FALSIFIED;
            }

        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        // Validate timestamp first
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

        if (timestamp.getCreated() == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT);
            return AssertionStatus.BAD_REQUEST;
        }

        final long created = timestamp.getCreated().asDate().getTime();
        final long now = System.currentTimeMillis();
        long expires;
        if (timestamp.getExpires() != null) {
            expires = timestamp.getExpires().asDate().getTime();
        } else {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT, new String[] {String.valueOf(DEFAULT_EXPIRY_TIME)});
            expires = created + DEFAULT_EXPIRY_TIME;
        }

        if (expires <= (now - EXPIRY_GRACE_TIME_MILLIS))
            // TODO we need a better exception for this than IOException
            throw new IOException("Request timestamp contained stale Expires date; rejecting entire request");

        if (created > now)
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CLOCK_SKEW, new String[] {String.valueOf(created)});

        if (created <= (now - MAXIMUM_MESSAGE_AGE_MILLIS)) {
            // TODO we need a better exception for this than IOException
            throw new IOException("Request timestamp contained Created older than the maximum message age hard cap");
        }

        XmlSecurityToken[] signingTokens = wssResults.getSigningTokens(timestamp.asElement());

        for (int i = 0; i < signingTokens.length; i++) {
            XmlSecurityToken signingToken = signingTokens[i];

            String messageIdStr = null;
            if (signingToken instanceof X509SigningSecurityToken) {
                X509Certificate signingCert = ((X509SigningSecurityToken)signingToken).getMessageSigningCertificate();
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT);

                // Use cert info as sender id
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(Long.toString(created).getBytes("UTF-8"));
                    md.update(signingCert.getSubjectDN().toString().getBytes("UTF-8"));
                    md.update(signingCert.getIssuerDN().toString().getBytes("UTF-8"));
                    md.update(skiToString(signingCert).getBytes("UTF-8"));
                    byte[] digest = md.digest();
                    messageIdStr = HexUtils.hexDump(digest);
                } catch (CertificateEncodingException e) {
                    throw new IOException("Unable to generate replay-protection ID; a SKI cannot be derived from signing cert '" + signingCert.getSubjectDN().getName() + "'");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e); // can't happen, misconfigured VM
                }
            } else if (signingToken instanceof SecurityContextToken) {
                // It was signed by a WS-SecureConversation session's derived key
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_SC_KEY);
                String sessionID = ((SecurityContextToken)signingToken).getContextIdentifier();

                // Use session ID as sender ID
                StringBuffer sb = new StringBuffer();
                sb.append(created);
                sb.append(";");
                sb.append("SessionID=");
                sb.append(sessionID);
                messageIdStr = sb.toString();
            } else if (signingToken instanceof EncryptedKey) {
                // It was signed by an EncryptedKey
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_ENC_KEY);
                String encryptedKeySha1 = ((EncryptedKey)signingToken).getEncryptedKeySHA1();

                StringBuffer sb = new StringBuffer();
                sb.append(created);
                sb.append(";");
                sb.append("EncryptedKeySHA1=");
                sb.append(encryptedKeySha1);
                messageIdStr = sb.toString();                                
            } else
                throw new IOException("Unable to generate replay-protection ID for timestamp -- " +
                                      "it was signed, but with the unsupported token type " + signingToken.getClass().getName());

            MessageId messageId = new MessageId(messageIdStr, expires + CACHE_ID_EXTRA_TIME_MILLIS);
            try {
                DistributedMessageIdManager dmm = (DistributedMessageIdManager)applicationContext.getBean("distributedMessageIdManager");
                dmm.assertMessageIdIsUnique(messageId);
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED, new String[]{ messageIdStr});
            } catch (MessageIdManager.DuplicateMessageIdException e) {
                // TODO we need a better exception for this than IOException
                throw new IOException("Duplicated message ID detected; ID=" + messageIdStr);
            }
        }

        return AssertionStatus.NONE;
    }

    private String skiToString(X509Certificate signingCert) throws CertificateEncodingException {
        byte[] ski = CertUtils.getSKIBytesFromCert(signingCert);
        return ski == null ? "" : HexUtils.hexDump(ski);
    }

    private static final Logger logger = Logger.getLogger(ServerRequestWssReplayProtection.class.getName());
}
