package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpClientCert;

import java.io.IOException;

public class ServerSslAssertion extends AbstractServerAssertion<SslAssertion> {


    public ServerSslAssertion(SslAssertion data) {
        super(data);
        serverHttpClientCert = new ServerHttpClientCert(new HttpClientCert(assertion.isCheckCertValidity()));
    }

    @Override
    protected void injectDependencies() {
        inject( serverHttpClientCert );
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        final HttpRequestKnob hsRequestKnob = context.getRequest().getKnob(HttpRequestKnob.class);
        final FtpRequestKnob ftpRequestKnob = hsRequestKnob != null ? null : context.getRequest().getKnob(FtpRequestKnob.class);
        if (hsRequestKnob == null && ftpRequestKnob == null) {
            logger.info("Request not received over FTP or HTTP; don't know how to check for SSL");
            context.setRequestPolicyViolated();
            return AssertionStatus.BAD_REQUEST;
        }
        boolean ssl = hsRequestKnob != null ? hsRequestKnob.isSecure() : ftpRequestKnob.isSecure();
        AssertionStatus status;

        SslAssertion.Option option = assertion.getOption();

        if ( option == SslAssertion.REQUIRED) {
            final boolean iscred = assertion.isCredentialSource();
            if (ssl) {
                status = AssertionStatus.NONE;
                logAndAudit(AssertionMessages.SSL_REQUIRED_PRESENT);
                if (iscred && hsRequestKnob != null) {
                    status = processAsCredentialSourceAssertion(context);
                } else if (iscred) {
                    status = AssertionStatus.FALSIFIED;
                }
            } else {
                if (iscred) {
                    status = AssertionStatus.AUTH_REQUIRED;
                    logAndAudit(AssertionMessages.HTTPCREDS_AUTH_REQUIRED);
                } else {
                    status = AssertionStatus.FALSIFIED;
                    logAndAudit(AssertionMessages.SSL_REQUIRED_ABSENT);
                }
            }
        } else if ( option == SslAssertion.FORBIDDEN) {
            if (ssl) {
                status = AssertionStatus.FALSIFIED;
                logAndAudit(AssertionMessages.SSL_FORBIDDEN_PRESENT);
            } else {
                status = AssertionStatus.NONE;
                logAndAudit(AssertionMessages.SSL_FORBIDDEN_ABSENT);
            }
        } else {
            status = AssertionStatus.NONE;
            if(ssl) {
                logAndAudit(AssertionMessages.SSL_OPTIONAL_PRESENT);
            } else {
                logAndAudit(AssertionMessages.SSL_OPTIONAL_ABSENT);
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
    private AssertionStatus processAsCredentialSourceAssertion(PolicyEnforcementContext context)
      throws PolicyAssertionException, IOException {
        Message request = context.getRequest();
        HttpRequestKnob httpReq = request.getKnob(HttpRequestKnob.class);
        if (httpReq != null) {
            return serverHttpClientCert.checkRequest(context);
        }
        // add SSL support for different protocols
        logger.info("Request not received over HTTP; cannot check for client certificate");
        context.setAuthenticationMissing();
        logAndAudit(AssertionMessages.HTTPCREDS_AUTH_REQUIRED);
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final ServerHttpClientCert serverHttpClientCert;
}
