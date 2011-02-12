package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.security.xml.processor.WssTimestamp;
import com.l7tech.security.xml.processor.WssTimestampDate;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This assertion asserts that this message had a signed timestamp, and that no message with this timestamp signed
 * by one of the same signing tokens has been seen recently.
 */
public class ServerWssReplayProtection extends AbstractMessageTargetableServerAssertion<WssReplayProtection> {
    private static final long EXPIRY_GRACE_TIME_MILLIS = 1000L * 60 * 1; // allow messages expired up to 1 minute ago
    private static final long MAXIMUM_MESSAGE_AGE_MILLIS = 1000L * 60 * 60 * 24 * 30; // hard cap of 30 days old
    private static final long CACHE_ID_EXTRA_TIME_MILLIS = 1000L * 60 * 5; // cache IDs for at least 5 min extra
    private static final long DEFAULT_EXPIRY_TIME = 1000L * 60 * 10; // if no Expires, assume expiry after 10 min

    private static final String ID_PREFIX_WSA_HASHED = "uuid:wsa:digest:";
    private static final int MAX_WSA_MESSAGEID_HASHTHRESHOLD = SyspropUtil.getInteger("com.l7tech.server.messageIDHashThrehold" , 150); // 255 is the max we allow in the DB
    private static final int MAX_WSA_MESSAGEID_MAXLENGTH = SyspropUtil.getInteger("com.l7tech.server.messageIDMaxLength" , 8192); // 8k limit
    private static final String ID_FORMAT_CUSTOM_HASHED = "uuid:custom:scope:{0}:{1}";

    private static final Charset UTF8 = Charsets.UTF8;

    private final Auditor auditor;
    private final MessageIdManager messageIdManager;
    private final Config config;
    private final SecurityTokenResolver securityTokenResolver;
    private final TimeSource timeSource;

    public ServerWssReplayProtection( final WssReplayProtection subject, final BeanFactory spring ) {
        super(subject, subject);
        this.auditor = spring instanceof ApplicationContext
                ? new Auditor(this, (ApplicationContext) spring, logger)
                : new LogOnlyAuditor(logger);
        this.messageIdManager = spring.getBean("distributedMessageIdManager",MessageIdManager.class);
        this.config = spring.getBean("serverConfig", Config.class);
        this.securityTokenResolver = spring.getBean("securityTokenResolver",SecurityTokenResolver.class);
        this.timeSource = getTimeSource();
    }

    private static final class MultipleSenderIdException extends Exception { }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String targetName,
                                              final AuthenticationContext authContext ) throws IOException {
        final Pair<String,Long> messageIdAndExpiry;
        try {
            if ( assertion.isCustomProtection() ) {
                messageIdAndExpiry = getCustomIdentifierAndExpiry( context, targetName );
            } else {
                messageIdAndExpiry = getDefaultIdentifierAndExpiry( context, msg, targetName, authContext );
            }
        } catch ( AssertionStatusException ase ) {
            return ase.getAssertionStatus();            
        }

        MessageId messageId = new MessageId(messageIdAndExpiry.left, messageIdAndExpiry.right);
        try {
            messageIdManager.assertMessageIdIsUnique(messageId);
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_PROTECTION_SUCCEEDED, messageIdAndExpiry.left, targetName);
        } catch (MessageIdManager.DuplicateMessageIdException e) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_REPLAY, messageIdAndExpiry.left, targetName);
            return getBadMessageStatus();
        } catch (MessageIdManager.MessageIdCheckException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error checking for replay : " + ExceptionUtils.getMessage( e )}, e);
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    protected TimeSource getTimeSource() {
        return new TimeSource();
    }

    private Pair<String,Long> getDefaultIdentifierAndExpiry( final PolicyEnforcementContext context,
                                                             final Message msg,
                                                             final String targetName,
                                                             final AuthenticationContext authContext ) throws IOException {
        final long now = timeSource.currentTimeMillis();
        final String messageIdStr;
        final long expires;

        final ProcessorResult wssResults;
        try {
            if (!msg.isSoap()) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_NOT_SOAP, targetName);
                throw new AssertionStatusException(AssertionStatus.NOT_APPLICABLE);
            }

            if ( isRequest() && !config.getBooleanProperty(ServerConfig.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
                wssResults = msg.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(msg, targetName, securityTokenResolver, auditor);
            }

            if (wssResults == null) {
                if ( isRequest() ) {
                    // WssProcessorUtil.getWssResults already audited any error messages for non-request
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_WSS_LEVEL_SECURITY, targetName);
                    // If we're dealing with something other than the request, there's no point sending a challenge or
                    // policy URL to the original requestor.
                    context.setRequestPolicyViolated();
                    context.setAuthenticationMissing();
                }
                throw new AssertionStatusException(AssertionStatus.NOT_APPLICABLE);
            }
        } catch (SAXException e) {
            // In practice, this can only happen if a mutating assertion (e.g. XSLT or Regex) has changed this message
            // from SOAP to non-SOAP--if it was originally SOAP, Trogdor will have already run, and the message will
            // have been parsed before we get here.
            throw new IOException(ExceptionUtils.getMessage(e), e);
        }

        // See if there's a wsa:MessageID
        String wsaMessageId = null;
        final Message relatedRequestMessage = msg.getRelated( MessageRole.REQUEST );
        final SignedElement[] signedElements =  WSSecurityProcessorUtils.filterSignedElementsByIdentity(
                    authContext,
                    wssResults,
                    assertion.getIdentityTarget(),
                    false, // Not checking signing token in case this assertion occurs before the credential assertion
                    relatedRequestMessage,
                    relatedRequestMessage==null ? null : context.getAuthenticationContext( relatedRequestMessage ) );
        for (SignedElement signedElement : signedElements) {
            Element el = signedElement.asElement();
            if (DomUtils.elementInNamespace(el, SoapConstants.WSA_NAMESPACE_ARRAY) && SoapConstants.MESSAGEID_EL_NAME.equals(el.getLocalName())) {
                if (wsaMessageId != null) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MULTIPLE_MESSAGE_IDS, targetName);
                    throw new AssertionStatusException(getBadMessageStatus());
                } else {
                    wsaMessageId = DomUtils.getTextValue(el);
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_GOT_SIGNED_MESSAGE_ID, targetName, wsaMessageId);
                    // continue in order to detect multiple MessageIDs
                }
            }
        }

        if (wsaMessageId == null) auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_SIGNED_MESSAGE_ID, targetName);
        // OK to proceed with timestamp alone

        // Validate timestamp
        WssTimestamp timestamp = wssResults.getTimestamp();
        if (timestamp == null) {
            if ( isRequest() )
                context.setRequestPolicyViolated();
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_TIMESTAMP, targetName);
            throw new AssertionStatusException(getBadMessageStatus());
        }
        if (ProcessorResultUtil.getParsedElementsForNode(timestamp.asElement(), signedElements).isEmpty()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NOT_SIGNED, targetName);
            throw new AssertionStatusException(getBadMessageStatus());
        }

        final WssTimestampDate createdTimestamp = timestamp.getCreated();
        if (createdTimestamp == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_CREATED_ELEMENT, targetName);
            throw new AssertionStatusException(getBadMessageStatus());
        }

        final String createdIsoString = createdTimestamp.asIsoString().trim();
        final long created = createdTimestamp.asTime();

        if (timestamp.getExpires() != null) {
            expires = timestamp.getExpires().asTime();
        } else {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_NO_EXPIRES_ELEMENT, targetName, String.valueOf(DEFAULT_EXPIRY_TIME));
            expires = created + DEFAULT_EXPIRY_TIME;
        }

        if (expires <= (now - EXPIRY_GRACE_TIME_MILLIS)) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_STALE_TIMESTAMP, targetName);
            throw new AssertionStatusException(getBadMessageStatus());
        }

        if (created > now)
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CLOCK_SKEW, targetName, String.valueOf(created));

        if (created <= (now - MAXIMUM_MESSAGE_AGE_MILLIS)) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CREATED_TOO_OLD, targetName);
            throw new AssertionStatusException(getBadMessageStatus());
        }

        if ( wsaMessageId != null ) {
            if ( wsaMessageId.length() > MAX_WSA_MESSAGEID_MAXLENGTH ) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MESSAGE_ID_TOO_LARGE, Integer.toString(wsaMessageId.length()));
                throw new AssertionStatusException(getBadMessageStatus());
            }

            // hash if id is too large to store natively
            if ( wsaMessageId.length() > MAX_WSA_MESSAGEID_HASHTHRESHOLD ) {
                messageIdStr = ID_PREFIX_WSA_HASHED + hash( wsaMessageId );
            } else {
                messageIdStr = encode(wsaMessageId);
            }
        } else {
            try {
                String senderId = getSenderId(timestamp.asElement(), signedElements, createdIsoString, targetName);
                if (senderId == null) throw new AssertionStatusException(getBadMessageStatus());
                messageIdStr = senderId;
            } catch (MultipleSenderIdException e) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_MULTIPLE_SENDER_IDS, targetName);
                throw new AssertionStatusException(getBadMessageStatus());
            }
        }
        return new Pair<String,Long>( messageIdStr, expires  + CACHE_ID_EXTRA_TIME_MILLIS);
    }

    private Pair<String,Long> getCustomIdentifierAndExpiry( final PolicyEnforcementContext context,
                                                            final String targetName ) {
        final long now = timeSource.currentTimeMillis();
        final String messageIdStr;
        final long expires;
        try {
            final String varName = assertion.getCustomIdentifierVariable();
            final List<String> variableNames = new ArrayList<String>();
            if ( varName != null ) variableNames.add(varName);
            if ( assertion.getCustomScope() != null ){
                variableNames.addAll(Arrays.asList(Syntax.getReferencedNames(assertion.getCustomScope())));
            }
            final Map<String,Object> vars = context.getVariableMap( variableNames.toArray(new String[variableNames.size()]), auditor );
            final String scope = assertion.getCustomScope()==null ? "" : ExpandVariables.process( assertion.getCustomScope(), vars, auditor, true);
            final String id = ExpandVariables.process(Syntax.SYNTAX_PREFIX+varName+Syntax.SYNTAX_SUFFIX, vars, auditor, true);
            if ( id.isEmpty() ) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_CUSTOM_VAR_EMPTY, targetName);
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);

            }
            auditor.logAndAudit( AssertionMessages.REQUEST_WSS_REPLAY_USING_SCOPE_AND_ID, targetName, scope, id);
            messageIdStr = MessageFormat.format( ID_FORMAT_CUSTOM_HASHED, hash(scope), hash(id));
            expires = now + assertion.getCustomExpiryTime();
        } catch (IllegalArgumentException e) {
            auditor.logAndAudit(
                    AssertionMessages.REQUEST_WSS_REPLAY_CUSTOM_VAR_ERROR,
                    new String[]{targetName, ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }
        return new Pair<String,Long>( messageIdStr, expires );
    }

    private String getSenderId( final Element timestampElement,
                                final SignedElement[] signedElementsForIdentity,
                                final String createdIsoString,
                                final String what )
        throws MultipleSenderIdException, UnsupportedEncodingException
    {
        String senderId = null;

        for ( SignedElement element : signedElementsForIdentity ) {
            if ( !timestampElement.isSameNode(element.asElement()) )
                continue;

            SigningSecurityToken signingToken = element.getSigningSecurityToken();
            if (signingToken instanceof X509SigningSecurityToken) {
                if (senderId != null) throw new MultipleSenderIdException();
                X509Certificate signingCert = ((X509SigningSecurityToken)signingToken).getMessageSigningCertificate();
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_CERT, what);

                // Use cert info as sender id
                try {
                    byte[] digest = HexUtils.getSha1Digest( new byte[][]{
                        createdIsoString.getBytes(UTF8),
                        signingCert.getSubjectDN().toString().getBytes(UTF8),
                        signingCert.getIssuerDN().toString().getBytes(UTF8),
                        skiToString(signingCert).getBytes(UTF8)
                    } );
                    senderId = HexUtils.hexDump(digest);
                } catch (CertificateEncodingException e) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_NO_SKI, what, signingCert.getSubjectDN().getName());
                    return null;
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
                sb.append(encode(sessionID));
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
                sb.append(escapeBASE64(encryptedKeySha1));
                senderId = sb.toString();
            } else if (signingToken instanceof KerberosSigningSecurityToken) {
                if (senderId != null) throw new MultipleSenderIdException();
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_TIMESTAMP_SIGNED_WITH_KERBEROS, what);

                String kerberosTokenSha1 = KerberosUtils.getBase64Sha1(((KerberosSigningSecurityToken)signingToken).getTicket());

                StringBuffer sb = new StringBuffer();
                sb.append(createdIsoString);
                sb.append(";");
                sb.append("Kerberosv5APREQSHA1=");
                sb.append(escapeBASE64(kerberosTokenSha1));
                senderId = sb.toString();
            } else {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_REPLAY_UNSUPPORTED_TOKEN_TYPE, what, signingToken.getClass().getName());
                return null;
            }
        }

        return senderId;
    }

    private String hash( final String text ) {
        return escapeBASE64(HexUtils.encodeBase64( HexUtils.getSha512Digest( HexUtils.encodeUtf8(text) ), true ));
    }

    /**
     * Encode BASE64 text for safe use as a message identifier. 
     */
    private String escapeBASE64( final String base64Text ) {
        return base64Text.replace( '/', '-' ).replace( '+', '_' );
    }

    private String encode( final String text ) {
        return escapeBASE64(HexUtils.encodeBase64(HexUtils.encodeUtf8(text)));
    }

    private String skiToString(X509Certificate signingCert) throws CertificateEncodingException {
        byte[] ski = CertUtils.getSKIBytesFromCert(signingCert);
        return ski == null ? "" : HexUtils.hexDump(ski);
    }

    private static final Logger logger = Logger.getLogger(ServerWssReplayProtection.class.getName());
}
