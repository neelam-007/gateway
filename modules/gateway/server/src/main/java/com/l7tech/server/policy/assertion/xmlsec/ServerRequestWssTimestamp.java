/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerRequestWssTimestamp extends AbstractMessageTargetableServerAssertion<RequestWssTimestamp> {
    private static final Logger logger = Logger.getLogger(ServerRequestWssTimestamp.class.getName());
    private static final long DEFAULT_GRACE = 60000;
    private static final int PROP_CACHE_AGE = 151013;

    private final Auditor auditor;
    private final SecurityTokenResolver securityTokenResolver;
    private final ServerConfig serverConfig;

    public ServerRequestWssTimestamp(RequestWssTimestamp assertion, BeanFactory spring) {
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
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTAPPLICABLE, what);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, ExceptionUtils.getMessage(e));
            return AssertionStatus.BAD_REQUEST;
        }

        final ProcessorResult processorResult = WSSecurityProcessorUtils.getWssResults(msg, what, securityTokenResolver, auditor);
        if (processorResult == null) return AssertionStatus.NOT_APPLICABLE; // WssProcessorUtil.getWssResults has already audited the message

        final WssTimestamp wssTimestamp = processorResult.getTimestamp();
        if (wssTimestamp == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTIMESTAMP, what);
            return AssertionStatus.BAD_REQUEST;
        }

        if ( assertion.isSignatureRequired() ) {
            final ParsedElement element = ProcessorResultUtil.getParsedElementForNode( wssTimestamp.asElement(), processorResult.getElementsThatWereSigned() );
            if ( new IdentityTarget().equals( new IdentityTarget(assertion.getIdentityTarget() )) ) {
                if ( element==null || !WSSecurityProcessorUtils.isValidSingleSigner(processorResult, new ParsedElement[]{element}) ) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOT_SIGNED, what);
                    return AssertionStatus.BAD_REQUEST;
                }
            } else {
                // Ensure signed with the required identity
                if ( element==null ||
                     !WSSecurityProcessorUtils.isValidSigningIdentity(
                             authContext,
                             assertion.getIdentityTarget(),
                             processorResult,
                             new ParsedElement[]{element} ) ) {
                    auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOT_SIGNED, what);
                    return AssertionStatus.BAD_REQUEST;
                }
            }
        }

        final long now = System.currentTimeMillis();
        WssTimestampDate createdEl = wssTimestamp.getCreated();
        if (createdEl == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NO_CREATED, what); // Created is required according to BSP 1.0
            return AssertionStatus.BAD_REQUEST;
        }

        long createdFutureFuzz = serverConfig.getLongPropertyCached(ServerConfig.PARAM_TIMESTAMP_CREATED_FUTURE_GRACE, DEFAULT_GRACE, PROP_CACHE_AGE);
        final long created = createdEl.asTime();
        if (created - createdFutureFuzz > now) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_CREATED_FUTURE, what);
            return AssertionStatus.BAD_REQUEST;
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
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_EXPIRES_TOOLATE, what);
            constrain = true;
            expires = created + assertion.getMaxExpiryMilliseconds();
        }

        long expiresPastFuzz = serverConfig.getLongPropertyCached(ServerConfig.PARAM_TIMESTAMP_EXPIRES_PAST_GRACE, DEFAULT_GRACE, PROP_CACHE_AGE);
        if (expires + expiresPastFuzz < now) {
            if (constrain && (!(originalExpires + expiresPastFuzz < now))) {
                // then this only expired because we constrained the expiry time so audit that
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_EXPIRED_TRUNC, what);
                return AssertionStatus.BAD_REQUEST;
            } else {
                // the timestamp as it came in was expired so just audit that
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_EXPIRED, what);
                return AssertionStatus.BAD_REQUEST;
            }
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }
}
