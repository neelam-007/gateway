/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpClientCert;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerSslAssertion implements ServerAssertion {
    public ServerSslAssertion( SslAssertion data ) {
        _data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        boolean ssl = context.getHttpServletRequest().isSecure();
        AssertionStatus status;

        SslAssertion.Option option = _data.getOption();

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        if ( option == SslAssertion.REQUIRED) {
            if (ssl) {
                status = AssertionStatus.NONE;
                auditor.logAndAudit(AssertionMessages.SSL_REQUIRED_PRESENT);
            } else {
                status = AssertionStatus.FALSIFIED;
                auditor.logAndAudit(AssertionMessages.SSL_REQUIRED_ABSENT);
            }
            if (_data.isCredentialSource()) {
                return processAsCredentialSourceAssertion(context, auditor);
            }
        } else if ( option == SslAssertion.FORBIDDEN) {
            if (ssl) {
                status = AssertionStatus.FALSIFIED;
                auditor.logAndAudit(AssertionMessages.SSL_FORBIDDEN_PRESENT);
            } else {
                status = AssertionStatus.NONE;
                auditor.logAndAudit(AssertionMessages.SSL_FORBIDDEN_ABSENT);
            }
        } else {
            status = AssertionStatus.NONE;
            if(ssl) {
                auditor.logAndAudit(AssertionMessages.SSL_OPTIONAL_PRESENT);
            } else {
                auditor.logAndAudit(AssertionMessages.SSL_OPTIONAL_ABSENT);
            }
        }

        if (status == AssertionStatus.FALSIFIED)
            context.setRequestPolicyViolated();

        return status;
    }

    /**
     * Process the SSL assertion as credential source. Look for the client side certificate over
     * various protocols. Currently works only over http.
     */
    private AssertionStatus processAsCredentialSourceAssertion(PolicyEnforcementContext context, Auditor auditor)
      throws PolicyAssertionException, IOException {
        Message request = context.getRequest();
        HttpRequestKnob httpReq = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReq != null) {
            return serverHttpClientCert.checkRequest(context);
        }
        // add SSL support for different protocols
        logger.info("Request not received over HTTP; cannot check for client certificate");
        context.setAuthenticationMissing();
        auditor.logAndAudit(AssertionMessages.AUTH_REQUIRED);
        return AssertionStatus.AUTH_REQUIRED;
    }

    protected SslAssertion _data;
    private final ServerHttpClientCert serverHttpClientCert = new ServerHttpClientCert();
    protected final Logger logger = Logger.getLogger(getClass().getName());
}
