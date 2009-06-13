/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import com.l7tech.security.token.http.HttpClientCertToken;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class functionality heas been replaces with the ServerSslAssertion that
 * requires the client certificate. This class exists as the SslAssertion uses it
 * to extract and validate the ssl client certificate.
 *
 * The server-side processing for HTTPS with client certificates.  Note that this
 * class is not a subclass of <code>ServerHttpCredentialSource</code> because it
 * works at a lower level without stuff like <code>WWW-Authenticate</code> and
 * <code>Authorization</code> headers.
 */
public class ServerHttpClientCert extends ServerCredentialSourceAssertion<HttpClientCert> {
    public static final String PARAM_HTTP_X509CERT = "javax.servlet.request.X509Certificate";
    private static final Logger logger = Logger.getLogger(ServerHttpClientCert.class.getName());
    private final Auditor auditor;

    public ServerHttpClientCert(HttpClientCert assertion, ApplicationContext springContext) {
        super(assertion, springContext);
        this.auditor = new Auditor(this, springContext, logger);
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map<String, String> authParams)
            throws IOException, CredentialFinderException
    {
        HttpRequestKnob httpReq = request.getKnob(HttpRequestKnob.class);
        if (httpReq == null) {
            auditor.logAndAudit(AssertionMessages.HTTPCLIENTCERT_NOT_HTTP);
            return null;
        }

        X509Certificate[] certChain = httpReq.getClientCertificate();

        if ( certChain == null || certChain.length < 1 ) {
            auditor.logAndAudit(AssertionMessages.HTTPCLIENTCERT_NO_CERT);
            return null;
        }

        X509Certificate clientCert = certChain[0];
        if (clientCert == null) throw new CredentialFinderException("Null cert in chain");

        try {
            clientCert.checkValidity();
        } catch (CertificateExpiredException cee) {
            throw new CredentialFinderException( "Client Certificate has expired", cee, AssertionStatus.AUTH_FAILED );
        } catch (CertificateNotYetValidException cnyve ) {
            throw new CredentialFinderException( "Client Certificate is not yet valid", cnyve, AssertionStatus.AUTH_FAILED );
        }

        HttpClientCertToken token = new HttpClientCertToken(clientCert);

        auditor.logAndAudit(AssertionMessages.HTTPCLIENTCERT_FOUND, token.getCertCn() == null ? token.getCertDn() : token.getCertCn());

        // TODO where's the chain?
        return LoginCredentials.makeLoginCredentials( token, SslAssertion.class );
    }

    @Override
    protected AssertionStatus checkCredentials(LoginCredentials pc, Map<String, String> authParams) throws CredentialFinderException {
        return AssertionStatus.NONE;
    }

    @Override
    protected void challenge(PolicyEnforcementContext context, Map<String, String> authParams) {
        // HOW DO I CHALLENGED X.509
    }
}
