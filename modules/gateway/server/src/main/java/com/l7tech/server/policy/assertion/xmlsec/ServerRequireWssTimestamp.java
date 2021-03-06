package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.security.xml.processor.WssTimestamp;
import com.l7tech.security.xml.processor.WssTimestampDate;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;

/**
 * @author alex
 */
public class ServerRequireWssTimestamp extends AbstractMessageTargetableServerAssertion<RequireWssTimestamp> {
    private static final boolean requireCredentialSigningToken = ConfigFactory.getBooleanProperty( "com.l7tech.server.policy.requireSigningTokenCredential", true );
    private static final long DEFAULT_GRACE = 60000L;
    private static final int PROP_CACHE_AGE = 151013;

    private final SecurityTokenResolver securityTokenResolver;
    private final Config config;

    public ServerRequireWssTimestamp(RequireWssTimestamp assertion, BeanFactory spring) {
        super(assertion);
        this.securityTokenResolver = spring.getBean("securityTokenResolver",SecurityTokenResolver.class);
        this.config = spring.getBean("serverConfig", Config.class);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if ( !SecurityHeaderAddressableSupport.isLocalRecipient(assertion) ) {
            logAndAudit( AssertionMessages.REQUESTWSS_NOT_FOR_US );
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String what,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        try {
            if (!msg.isSoap()) {
                logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOTAPPLICABLE, what );
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, ExceptionUtils.getMessage( e ) );
            return getBadMessageStatus();
        }

        final ProcessorResult processorResult;
        if ( isRequest() && !config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
            processorResult = msg.getSecurityKnob().getProcessorResult();
        } else {
            processorResult = WSSecurityProcessorUtils.getWssResults(msg, what, securityTokenResolver, getAudit());
        }
        if (processorResult == null) {
            return AssertionStatus.NOT_APPLICABLE; // WssProcessorUtil.getWssResults has already audited the message
        }

        final WssTimestamp wssTimestamp = processorResult.getTimestamp();
        if (wssTimestamp == null) {
            logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOTIMESTAMP, what );
            return getBadMessageStatus();
        }

        if ( assertion.isSignatureRequired() ) {
            final Collection<ParsedElement> elements = ProcessorResultUtil.getParsedElementsForNode( wssTimestamp.asElement(), processorResult.getElementsThatWereSigned() );

            final Message relatedRequestMessage = msg.getRelated( MessageRole.REQUEST );
            if ( new IdentityTarget().equals( new IdentityTarget(assertion.getIdentityTarget() )) ) {
                if ( elements.isEmpty() ||
                    (isResponse() && !isValidSignatureConfirmation(msg, processorResult, elements)) ||
                    ! WSSecurityProcessorUtils.isValidSingleSigner(
                        authContext,
                        processorResult,
                        elements.toArray(new ParsedElement[elements.size()]),
                        requireCredentialSigningToken,
                        relatedRequestMessage,
                        relatedRequestMessage==null ? null : context.getAuthenticationContext( relatedRequestMessage ),
                        getAudit())) {
                    logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOT_SIGNED, what );
                    return getBadMessageStatus();
                }
            } else {
                // Ensure signed with the required identity
                if ( elements.isEmpty() ||
                     (isResponse() && !isValidSignatureConfirmation(msg, processorResult, elements)) ||
                     ! WSSecurityProcessorUtils.isValidSigningIdentity(
                             authContext,
                             assertion.getIdentityTarget(),
                             processorResult,
                             elements.toArray(new ParsedElement[elements.size()]))) {
                    logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOT_SIGNED, what );
                    return getBadMessageStatus();
                }
            }
        } else  if (isResponse() && !isValidSignatureConfirmation(msg, processorResult, null)) {
            return AssertionStatus.FALSIFIED;
        }

        final long now = System.currentTimeMillis();
        WssTimestampDate createdEl = wssTimestamp.getCreated();
        if (createdEl == null) {
            logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_NO_CREATED, what ); // Created is required according to BSP 1.0
            return getBadMessageStatus();
        }

        long createdFutureFuzz = config.getLongProperty( ServerConfigParams.PARAM_TIMESTAMP_CREATED_FUTURE_GRACE, DEFAULT_GRACE );
        final long created = createdEl.asTime();
        if (created - createdFutureFuzz > now) {
            logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_CREATED_FUTURE, what );
            return getBadMessageStatus();
        }

        final WssTimestampDate expiresEl = wssTimestamp.getExpires();
        long expires;
        if (expiresEl != null) {
            expires = expiresEl.asTime();
        } else {
            expires = created + assertion.getMaxExpiryMilliseconds();  // Expires is optional according to BSP 1.0
        }

        final long window = expires - created;
        boolean constrain = false;
        long originalExpires = expires;
        if (window > assertion.getMaxExpiryMilliseconds()) {
            logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_EXPIRES_TOOLATE, what );
            constrain = true;
            expires = created + assertion.getMaxExpiryMilliseconds();
        }

        long expiresPastFuzz = config.getLongProperty( ServerConfigParams.PARAM_TIMESTAMP_EXPIRES_PAST_GRACE, DEFAULT_GRACE );
        if (expires + expiresPastFuzz < now) {
            if (constrain && (!(originalExpires + expiresPastFuzz < now))) {
                // then this only expired because we constrained the expiry time so audit that
                logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_EXPIRED_TRUNC, what );
                return getBadMessageStatus();
            } else {
                // the timestamp as it came in was expired so just audit that
                logAndAudit( AssertionMessages.REQUIRE_WSS_TIMESTAMP_EXPIRED, what );
                return getBadMessageStatus();
            }
        }

        return AssertionStatus.NONE;
    }



    /**
     * Validate confirmation and add signature confirmation elements to collection if not null.
     */
    private boolean isValidSignatureConfirmation( final Message message,
                                                  final ProcessorResult wssResults,
                                                  final Collection<ParsedElement> signatureConfirmationElementHolder ) {
        Pair<Boolean,Collection<ParsedElement>> signatureConfirmationValidation =
                WSSecurityProcessorUtils.processSignatureConfirmations(message.getSecurityKnob(), wssResults, getAudit());

        if ( signatureConfirmationElementHolder != null && signatureConfirmationValidation.getValue() != null ) {
            signatureConfirmationElementHolder.addAll(signatureConfirmationValidation.getValue());
        }
        
        return signatureConfirmationValidation.getKey();
    }
}
