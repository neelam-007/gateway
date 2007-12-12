/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssTimestamp;
import com.l7tech.common.security.xml.processor.WssTimestampDate;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerRequestWssTimestamp extends AbstractServerAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerRequestWssTimestamp.class.getName());
    private final RequestWssTimestamp assertion;
    private final Auditor auditor;
    private static final int DEFAULT_CREATED_FUTURE_FUZZ = (60 * 1000);
    private static final int DEFAULT_EXPIRES_PAST_FUZZ = (60 * 1000);

    private final int createdFutureFuzz = Integer.getInteger(this.getClass().getName() + ".createdFutureGrace", DEFAULT_CREATED_FUTURE_FUZZ).intValue();
    private final int expiresPastFuzz = Integer.getInteger(this.getClass().getName() + ".expiresPastGrace", DEFAULT_EXPIRES_PAST_FUZZ).intValue();

    public ServerRequestWssTimestamp(RequestWssTimestamp assertion, ApplicationContext spring) {
        super(assertion);
        this.assertion = assertion;
        this.auditor = new Auditor(this, spring, logger);
        logger.info("Created future grace period: " + createdFutureFuzz);
        logger.info("Expires past grace period: " + expiresPastFuzz);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        XmlKnob reqXml = (XmlKnob)context.getRequest().getKnob(XmlKnob.class);
        if (reqXml == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTAPPLICABLE);
            return AssertionStatus.NOT_APPLICABLE;
        }

        SecurityKnob reqSec = (SecurityKnob)context.getRequest().getKnob(SecurityKnob.class);
        if (reqSec == null) return notimestamp();

        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTAPPLICABLE);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { ExceptionUtils.getMessage(e) });
            return AssertionStatus.BAD_REQUEST;
        }

        ProcessorResult processorResult = reqSec.getProcessorResult();
        if (processorResult == null) return notimestamp();

        WssTimestamp wssTimestamp = processorResult.getTimestamp();
        if (wssTimestamp == null) return notimestamp();

        if (assertion.isSignatureRequired() && !wssTimestamp.isSigned()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOT_SIGNED);
            return AssertionStatus.BAD_REQUEST;
        }

        long now = System.currentTimeMillis();
        WssTimestampDate createdEl = wssTimestamp.getCreated();
        if (createdEl == null) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NO_CREATED); // Created is required according to BSP 1.0
            return AssertionStatus.BAD_REQUEST;
        }

        long created = createdEl.asTime();
        if (created - createdFutureFuzz > now) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_CREATED_FUTURE);
            return AssertionStatus.BAD_REQUEST;
        }

        WssTimestampDate expiresEl = wssTimestamp.getExpires();
        long expires;
        if (expiresEl != null) {
            expires = expiresEl.asTime();
        } else {
            expires = created + assertion.getMaxExpiryMilliseconds();  // Expires is optional according to BSP 1.0
        }

        long window = expires - created;
        boolean constrain = false;
        long originalExpires = expires;
        if (window > assertion.getMaxExpiryMilliseconds()) {
            auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_EXPIRES_TOOLATE);
            constrain = true;
            expires = created + assertion.getMaxExpiryMilliseconds();
        }

        if (expires + expiresPastFuzz < now) {
            if (constrain && (!(originalExpires + expiresPastFuzz < now))) {
                // then this only expired because we constrained the expiry time so audit that
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_EXPIRED_TRUNC);
                return AssertionStatus.BAD_REQUEST;
            } else {
                // the timestamp as it came in was expired so just audit that
                auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_EXPIRED);
                return AssertionStatus.BAD_REQUEST;
            }
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus notimestamp() {
        auditor.logAndAudit(AssertionMessages.REQUEST_WSS_TIMESTAMP_NOTIMESTAMP);
        return AssertionStatus.BAD_REQUEST;
    }
}
