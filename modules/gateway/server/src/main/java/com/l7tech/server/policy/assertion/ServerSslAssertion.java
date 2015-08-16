package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.TlsKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

public class ServerSslAssertion extends AbstractServerAssertion<SslAssertion> {


    public ServerSslAssertion(SslAssertion data) {
        super(data);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        final TlsKnob tlsKnob = context.getRequest().getKnob(TlsKnob.class);
        final boolean ssl = tlsKnob != null && tlsKnob.isSecure();
        final AssertionStatus status;

        final SslAssertion.Option option = assertion.getOption();

        if (option == SslAssertion.REQUIRED) {
            final boolean iscred = assertion.isCredentialSource();
            if (ssl && iscred) {
                status = processAsCredentialSourceAssertion(tlsKnob, context);
            } else if (ssl) {
                status = AssertionStatus.NONE;
                logAndAudit(AssertionMessages.SSL_REQUIRED_PRESENT);
            } else if (iscred) {
                status = AssertionStatus.AUTH_REQUIRED;
                logAndAudit(AssertionMessages.HTTPCREDS_AUTH_REQUIRED);
            } else {
                status = AssertionStatus.FALSIFIED;
                logAndAudit(AssertionMessages.SSL_REQUIRED_ABSENT);
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
     * Process the SSL assertion as credential source. Look for the client side certificate on the tlsknob.
     */
    private AssertionStatus processAsCredentialSourceAssertion(@NotNull final TlsKnob tlsKnob, PolicyEnforcementContext context)
      throws PolicyAssertionException, IOException {
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        LoginCredentials pc = authContext.getLastCredentials();
        // bugzilla #1884
        if (pc != null && !pc.getCredentialSourceAssertion().equals(assertion.getClass())) {
            pc = null;
        }
        if ( pc == null ) {
            final X509Certificate[] certs = tlsKnob.getClientCertificate();
            if (certs != null && certs.length > 0 && certs[0] != null) {
                final TlsClientCertToken token = new TlsClientCertToken(certs[0]);
                final LoginCredentials credentials = LoginCredentials.makeLoginCredentials(token, SslAssertion.class);
                if (assertion.isCheckCertValidity()) {
                    try {
                        //checks if the certificate is expired or not yet valid
                        certs[0].checkValidity();
                    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                        logAndAudit( AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{ ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e) );
                        context.setRequestPolicyViolated();
                        return AssertionStatus.AUTH_FAILED;
                    }
                }
                logAndAudit(AssertionMessages.HTTPCLIENTCERT_FOUND, certs[0].getIssuerDN() == null ? "" : certs[0].getIssuerDN().getName());
                authContext.addCredentials(credentials);
                return AssertionStatus.NONE;
            } else {
                logAndAudit(AssertionMessages.HTTPCLIENTCERT_NO_CERT);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
        } else {
            authContext.addCredentials( pc );
            return AssertionStatus.NONE;
        }
    }
}
