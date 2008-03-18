/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessorUtil;
import com.l7tech.common.security.xml.processor.WssTimestamp;
import com.l7tech.common.security.xml.processor.WssTimestampDate;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerRequestWssTimestamp extends AbstractServerAssertion<RequestWssTimestamp> {
    private static final Logger logger = Logger.getLogger(ServerRequestWssTimestamp.class.getName());
    private final RequestWssTimestamp assertion;
    private final Auditor auditor;
    private static final int DEFAULT_CREATED_FUTURE_FUZZ = (60 * 1000);
    private static final int DEFAULT_EXPIRES_PAST_FUZZ = (60 * 1000);

    private final int createdFutureFuzz = Integer.getInteger(this.getClass().getName() + ".createdFutureGrace", DEFAULT_CREATED_FUTURE_FUZZ).intValue();
    private final int expiresPastFuzz = Integer.getInteger(this.getClass().getName() + ".expiresPastGrace", DEFAULT_EXPIRES_PAST_FUZZ).intValue();
    private final SecurityTokenResolver securityTokenResolver;

    public ServerRequestWssTimestamp(RequestWssTimestamp assertion, ApplicationContext spring) {
        super(assertion);
        this.assertion = assertion;
        this.auditor = new Auditor(this, spring, logger);
        this.securityTokenResolver = (SecurityTokenResolver)spring.getBean("securityTokenResolver");
        logger.info("Created future grace period: " + createdFutureFuzz);
        logger.info("Expires past grace period: " + expiresPastFuzz);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String what = assertion.getTargetName();
        final Message msg;
        try {
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.FAILED;
        }

        final XmlKnob xmlKnob = (XmlKnob)msg.getKnob(XmlKnob.class);
        if (xmlKnob == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTAPPLICABLE, what);
            return AssertionStatus.NOT_APPLICABLE;
        }

        try {
            if (!msg.isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTAPPLICABLE, what);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, ExceptionUtils.getMessage(e));
            return AssertionStatus.BAD_REQUEST;
        }

        final ProcessorResult processorResult = WssProcessorUtil.getWssResults(msg, what, securityTokenResolver, auditor);
        if (processorResult == null) return AssertionStatus.NOT_APPLICABLE; // WssProcessorUtil.getWssResults has already audited the message

        final WssTimestamp wssTimestamp = processorResult.getTimestamp();
        if (wssTimestamp == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTIMESTAMP, what);
            return AssertionStatus.BAD_REQUEST;
        }

        if (assertion.isSignatureRequired() && !wssTimestamp.isSigned()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOT_SIGNED, what);
            return AssertionStatus.BAD_REQUEST;
        }

        final long now = System.currentTimeMillis();
        WssTimestampDate createdEl = wssTimestamp.getCreated();
        if (createdEl == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NO_CREATED, what); // Created is required according to BSP 1.0
            return AssertionStatus.BAD_REQUEST;
        }

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

}
