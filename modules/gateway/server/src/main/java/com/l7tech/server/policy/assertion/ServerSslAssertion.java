/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpClientCert;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

public class ServerSslAssertion extends AbstractServerAssertion<SslAssertion> implements ServerAssertion {
    private final Auditor auditor;

    public ServerSslAssertion(SslAssertion data, ApplicationContext springContext) {
        super(data);
        auditor = new Auditor(this, springContext, logger);
        serverHttpClientCert = new ServerHttpClientCert(new HttpClientCert(), springContext);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        final HttpServletRequestKnob hsRequestKnob = (HttpServletRequestKnob)context.getRequest().getKnob(HttpServletRequestKnob.class);
        final HttpServletRequest httpServletRequest = hsRequestKnob == null ? null : hsRequestKnob.getHttpServletRequest();
        final FtpRequestKnob ftpRequestKnob = hsRequestKnob != null ? null : (FtpRequestKnob)context.getRequest().getKnob(FtpRequestKnob.class);
        if (httpServletRequest == null && ftpRequestKnob == null) {
            logger.info("Request not received over FTP or HTTP; don't know how to check for SSL");
            context.setRequestPolicyViolated();
            return AssertionStatus.BAD_REQUEST;
        }
        boolean ssl = httpServletRequest!=null ? httpServletRequest.isSecure() : ftpRequestKnob.isSecure();
        AssertionStatus status;

        SslAssertion.Option option = assertion.getOption();

        if ( option == SslAssertion.REQUIRED) {
            final boolean iscred = assertion.isCredentialSource();
            if (ssl) {
                status = AssertionStatus.NONE;
                auditor.logAndAudit(AssertionMessages.SSL_REQUIRED_PRESENT);
                if (iscred && httpServletRequest!=null) {
                    status = processAsCredentialSourceAssertion(context, auditor);
                } else if (iscred) {
                    status = AssertionStatus.FALSIFIED;
                }
            } else {
                if (iscred) {
                    status = AssertionStatus.AUTH_REQUIRED;
                    auditor.logAndAudit(AssertionMessages.HTTPCREDS_AUTH_REQUIRED);
                } else {
                    status = AssertionStatus.FALSIFIED;
                    auditor.logAndAudit(AssertionMessages.SSL_REQUIRED_ABSENT);
                }
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

        if (status != AssertionStatus.NONE)
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
        auditor.logAndAudit(AssertionMessages.HTTPCREDS_AUTH_REQUIRED);
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final ServerHttpClientCert serverHttpClientCert;
    protected final Logger logger = Logger.getLogger(getClass().getName());
}
