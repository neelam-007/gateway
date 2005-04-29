package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Retrieves a SAML assertion from an identity provider website according to the Browser/POST profile.
 * @author alex
 */
public class ServerSamlBrowserArtifact implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerSamlBrowserArtifact.class.getName());

    private final Auditor auditor;
    private final SamlBrowserArtifact assertion;
    private final URL loginUrl;
    private final GenericHttpClient httpClient = new UrlConnectionHttpClient();
    private final SSLContext sslContext;

    public ServerSamlBrowserArtifact(SamlBrowserArtifact assertion, ApplicationContext springContext) {
        this.auditor = new Auditor(this, springContext, logger);
        this.assertion = assertion;
        try {
            loginUrl = new URL(assertion.getSsoEndpointUrl());
        } catch (MalformedURLException e) {
            throw (IllegalArgumentException)new IllegalArgumentException("Invalid SAML browser profile URL: " +
                    assertion.getSsoEndpointUrl()).initCause(e);
        }

        try {
            sslContext = SSLContext.getInstance("SSL");
            final SslClientTrustManager trustManager = (SslClientTrustManager)springContext.getBean("httpRoutingAssertionTrustManager");
            final int timeout = Integer.getInteger(ServerHttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                   ServerHttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            sslContext.init(null, new TrustManager[]{trustManager}, null);
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.SSL_CONTEXT_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        GenericHttpRequestParamsImpl params = new GenericHttpRequestParamsImpl(loginUrl);
        params.setSslSocketFactory(sslContext.getSocketFactory());
        params.setFollowRedirects(false);

        LoginCredentials creds = context.getCredentials();
        if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
            params.setPasswordAuthentication(new PasswordAuthentication(creds.getLogin(), creds.getCredentials()));
        }

        GenericHttpRequest loginGetRequest = null;
        GenericHttpResponse loginGetResponse = null;

        try {
            loginGetRequest = httpClient.createRequest(GenericHttpClient.GET, params);
            loginGetResponse = loginGetRequest.getResponse();
            int status = loginGetResponse.getStatus();
            if (status == 302) {
                HttpHeaders headers = loginGetResponse.getHeaders();
                String location = headers.getOnlyOneValue("Location");
                URL redirectUrl = new URL(loginUrl, location);
                context.setVariable("samlBrowserArtifact.redirectUrl", redirectUrl);
                String query = redirectUrl.getQuery();
                if (query == null) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_NO_QUERY);
                    return AssertionStatus.FAILED;
                }

                Map queryParams;
                try {
                    queryParams = HttpUtils.parseQueryString(query);
                } catch (Exception e) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_BAD_QUERY, null, e);
                    return AssertionStatus.FAILED;
                }

                final String[] artifacts = (String[]) queryParams.get(assertion.getArtifactQueryParameter());
                if (artifacts == null || artifacts.length == 0) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_NO_ARTIFACT);
                    return AssertionStatus.FAILED;
                }

                context.setVariable("samlBrowserArtifact.artifact", URLEncoder.encode(artifacts[0], "UTF-8"));
                return AssertionStatus.NONE;
            } else {
                auditor.logAndAudit(AssertionMessages.SAMLBROWSERARTIFACT_RESPONSE_NON_302);
                return AssertionStatus.FAILED;
            }
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.SAMLBROWSERPOST_LOGINFORM_IOEXCEPTION, null, e);
            return AssertionStatus.FAILED;
        } finally {
            if (loginGetRequest != null) try { loginGetRequest.close(); } catch (Throwable t) { }
            if (loginGetResponse != null) try { loginGetResponse.close(); } catch (Throwable t) { }
        }
    }
}
