package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This assertion verifies that the message contained an
 * xml digital signature but does not care about which elements
 * were signed. The cert used for the signature is
 * recorded in request.setPrincipalCredentials.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class ServerRequestWssReplayProtection implements ServerAssertion {
    private static final long EXPIRY_GRACE_TIME_MILLIS = 1000L * 60 * 1; // allow messages expired up to 1 minute ago
    private static final long MAXIMUM_MESSAGE_AGE_MILLIS = 1000L * 60 * 60 * 24 * 30; // hard cap of 30 days old
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60 * 5; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY_TIME = 1000L * 60 * 10; // if no Expires, assume expiry after 10 min

    public ServerRequestWssReplayProtection(RequestWssReplayProtection subject) {
        this.subject = subject;
    }

    public AssertionStatus checkRequest(Request request, Response response)
            throws IOException, PolicyAssertionException
    {
        if (!(request instanceof SoapRequest)) {
            logger.info("This type of assertion is only supported with SOAP type of messages");
            return AssertionStatus.BAD_REQUEST;
        }
        SoapRequest soapreq = (SoapRequest)request;
        ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null)
            throw new IOException("This request was not processed for WSS level security.");

        // Validate timestamp first
        WssTimestamp timestamp = wssResults.getTimestamp();
        if (timestamp == null) {
            response.setPolicyViolated(true);
            logger.info("No timestamp present in request; assertion therefore fails.");
            return AssertionStatus.BAD_REQUEST;
        }
        if (!timestamp.isSigned()) {
            logger.info("Timestamp present in request was not signed; assertion therefore fails.");
            return AssertionStatus.BAD_REQUEST;
        }

        if (timestamp.getCreated() == null) {
            logger.info("Timestamp in request has no Created element.");
            return AssertionStatus.BAD_REQUEST;
        }

        final long created = timestamp.getCreated().asDate().getTime();
        final long now = System.currentTimeMillis();
        long expires;
        if (timestamp.getExpires() != null) {
            expires = timestamp.getExpires().asDate().getTime();
        } else {
            logger.info("Timestamp in request has no Expires element; assuming expiry " + DEFAULT_EXPIRY_TIME + "ms after creation");
            expires = created + DEFAULT_EXPIRY_TIME;
        }

        if (expires <= (now - EXPIRY_GRACE_TIME_MILLIS))
            // TODO we need a better exception for this than IOException
            throw new IOException("Request timestamp contained stale Expires date; rejecting entire request");

        if (created > now)
            logger.info("Clock skew: message creation time is in the future: " + created + "; continuing anyway");

        if (MAXIMUM_MESSAGE_AGE_MILLIS > 0 &&
                created <= (now - MAXIMUM_MESSAGE_AGE_MILLIS)) {
            // TODO we need a better exception for this than IOException
            throw new IOException("Request timestamp contained Created older than the maximum message age hard cap");
        }

        SecurityToken signingToken = timestamp.getSigningSecurityToken();

        String messageIdStr = null;
        if (signingToken instanceof X509SecurityToken || signingToken instanceof SamlSecurityToken) {
            X509Certificate signingCert;
            if (signingToken instanceof X509SecurityToken) {
                // It was signed by a client certificate
                logger.log(Level.FINER, "Timestamp was signed with an X509 BinarySecurityToken");
                signingCert = ((X509SecurityToken)signingToken).asX509Certificate();
            } else {
                // It was signed by a SAML holder-of-key assertion
                logger.log(Level.FINER, "Timestamp was signed with a SAML holder-of-key assertion");
                signingCert = ((SamlSecurityToken)signingToken).getSubjectCertificate();
            }

            // Use cert info as sender id
            StringBuffer sb = new StringBuffer();
            sb.append(signingCert.getSubjectDN().toString());
            sb.append(";");
            sb.append(skiToString(signingCert));
            sb.append(";");
            sb.append(signingCert.getIssuerDN().toString());
            sb.append(";");
            sb.append(created);
            messageIdStr = sb.toString();

        } else if (signingToken instanceof SecurityContextToken) {
            // It was signed by a WS-SecureConversation session's derived key
            logger.log(Level.FINER, "Timestamp was signed with a WS-SecureConversation derived key");
            String sessionID = ((SecurityContextToken)signingToken).getContextIdentifier();

            // Use session ID as sender ID
            StringBuffer sb = new StringBuffer();
            sb.append("SessionID=");
            sb.append(sessionID);
            sb.append(";");
            sb.append(created);
            messageIdStr = sb.toString();
        } else
            throw new IOException("Unable to generate replay-protection ID for timestamp -- " +
                                  "it was signed, but not with an unsupported token type " + signingToken.getClass().getName());

        MessageId messageId = new MessageId(messageIdStr, expires + CACHE_ID_EXTRA_TIME_MILLIS);
        try {
            soapreq.getMessageIdManager().assertMessageIdIsUnique(messageId);
            logger.finest("Message ID " + messageIdStr + " has not been seen before unique; assertion succeeds");
        } catch (MessageIdManager.DuplicateMessageIdException e) {
            // TODO we need a better exception for this than IOException
            throw new IOException("Duplicated message ID detected; ID=" + messageIdStr);
        }

        return AssertionStatus.NONE;
    }

    private String skiToString(X509Certificate signingCert) {
        byte[] ski = signingCert.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
        return ski == null ? "" : HexUtils.hexDump(ski);
    }

    private static final Logger logger = Logger.getLogger(ServerRequestWssReplayProtection.class.getName());
    private RequestWssReplayProtection subject;
}
