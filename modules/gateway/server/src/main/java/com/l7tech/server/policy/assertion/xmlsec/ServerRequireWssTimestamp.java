/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssTimestamp;
import com.l7tech.security.xml.processor.WssTimestampDate;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Collection;

/**
 * @author alex
 */
public class ServerRequireWssTimestamp extends AbstractMessageTargetableServerAssertion<RequireWssTimestamp> {
    private static final Logger logger = Logger.getLogger(ServerRequireWssTimestamp.class.getName());
    private static final boolean requireCredentialSigningToken = SyspropUtil.getBoolean( "com.l7tech.server.policy.requireSigningTokenCredential", true );
    private static final long DEFAULT_GRACE = 60000;
    private static final int PROP_CACHE_AGE = 151013;

    private final Auditor auditor;
    private final SecurityTokenResolver securityTokenResolver;
    private final ServerConfig serverConfig;

    public ServerRequireWssTimestamp(RequireWssTimestamp assertion, BeanFactory spring) {
        super(assertion,assertion);
        this.auditor = spring instanceof ApplicationContext
                ? new Auditor(this, (ApplicationContext) spring, logger)
                : new LogOnlyAuditor(logger);
        this.securityTokenResolver = (SecurityTokenResolver)spring.getBean("securityTokenResolver");
        this.serverConfig = (ServerConfig)spring.getBean("serverConfig", ServerConfig.class);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if ( !SecurityHeaderAddressableSupport.isLocalRecipient(assertion) ) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
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
                auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOTAPPLICABLE, what);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, ExceptionUtils.getMessage(e));
            return getBadMessageStatus();
        }

        final ProcessorResult processorResult;
        if ( isRequest() ) {
            processorResult = msg.getSecurityKnob().getProcessorResult();
        } else {
            processorResult = WSSecurityProcessorUtils.getWssResults(msg, what, securityTokenResolver, auditor);
        }
        if (processorResult == null) {
            return AssertionStatus.NOT_APPLICABLE; // WssProcessorUtil.getWssResults has already audited the message
        }

        final WssTimestamp wssTimestamp = processorResult.getTimestamp();
        if (wssTimestamp == null) {
            auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOTIMESTAMP, what);
            return getBadMessageStatus();
        }

        if ( assertion.isSignatureRequired() ) {
            final Collection<ParsedElement> elements = ProcessorResultUtil.getParsedElementsForNode( wssTimestamp.asElement(), processorResult.getElementsThatWereSigned() );
            ParsedElement[] elementsToCheck = WSSecurityProcessorUtils.processSignatureConfirmations(msg.getSecurityKnob(), processorResult, elements.toArray(new ParsedElement[elements.size()]));
            if ( new IdentityTarget().equals( new IdentityTarget(assertion.getIdentityTarget() )) ) {
                if ( elements.isEmpty() ||
                    ! WSSecurityProcessorUtils.isValidSignatureConfirmations(processorResult.getSignatureConfirmation(), auditor) ||
                    ! WSSecurityProcessorUtils.isValidSingleSigner(
                        authContext,
                        processorResult,
                        elementsToCheck,
                        requireCredentialSigningToken ) ) {
                    auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOT_SIGNED, what);
                    return getBadMessageStatus();
                }
            } else {
                // Ensure signed with the required identity
                if ( elements.isEmpty() ||
                     ! WSSecurityProcessorUtils.isValidSignatureConfirmations(processorResult.getSignatureConfirmation(), auditor) ||
                     ! WSSecurityProcessorUtils.isValidSigningIdentity(
                             authContext,
                             assertion.getIdentityTarget(),
                             processorResult,
                             elementsToCheck )) {
                    auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_NOT_SIGNED, what);
                    return getBadMessageStatus();
                }
            }
        }

        final long now = System.currentTimeMillis();
        WssTimestampDate createdEl = wssTimestamp.getCreated();
        if (createdEl == null) {
            auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_NO_CREATED, what); // Created is required according to BSP 1.0
            return getBadMessageStatus();
        }

        long createdFutureFuzz = serverConfig.getLongPropertyCached(ServerConfig.PARAM_TIMESTAMP_CREATED_FUTURE_GRACE, DEFAULT_GRACE, PROP_CACHE_AGE);
        final long created = createdEl.asTime();
        if (created - createdFutureFuzz > now) {
            auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_CREATED_FUTURE, what);
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
            auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_EXPIRES_TOOLATE, what);
            constrain = true;
            expires = created + assertion.getMaxExpiryMilliseconds();
        }

        long expiresPastFuzz = serverConfig.getLongPropertyCached(ServerConfig.PARAM_TIMESTAMP_EXPIRES_PAST_GRACE, DEFAULT_GRACE, PROP_CACHE_AGE);
        if (expires + expiresPastFuzz < now) {
            if (constrain && (!(originalExpires + expiresPastFuzz < now))) {
                // then this only expired because we constrained the expiry time so audit that
                auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_EXPIRED_TRUNC, what);
                return getBadMessageStatus();
            } else {
                // the timestamp as it came in was expired so just audit that
                auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_TIMESTAMP_EXPIRED, what);
                return getBadMessageStatus();
            }
        }

        if (! WSSecurityProcessorUtils.isValidSignatureConfirmations(processorResult.getSignatureConfirmation(), auditor)) {
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }
}
