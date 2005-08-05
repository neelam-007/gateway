package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.cyberneko.html.parsers.DOMParser;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
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

    private MultiThreadedHttpConnectionManager cman;
    private final GenericHttpClient httpClient;
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

        cman = new MultiThreadedHttpConnectionManager();
        cman.setMaxConnectionsPerHost(200);
        cman.setMaxTotalConnections(2000);

        httpClient = new CommonsHttpClient(cman);

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
        GenericHttpRequestParams loginParams = new GenericHttpRequestParams(loginUrl);
        loginParams.setSslSocketFactory(sslContext.getSocketFactory());
        loginParams.setFollowRedirects(false);

        LoginCredentials creds = context.getCredentials();
        if (creds == null) {
            auditor.logAndAudit(AssertionMessages.SAMLBROWSERARTIFACT_NOCREDS);
            return AssertionStatus.AUTH_REQUIRED;
        }
        
        if (creds.getFormat() != CredentialFormat.CLEARTEXT) {
            auditor.logAndAudit(AssertionMessages.SAMLBROWSERARTIFACT_CREDS_NOT_PASSWORD);
            return AssertionStatus.AUTH_REQUIRED;
        }

        GenericHttpRequest loginRequest = null;
        GenericHttpResponse loginResponse = null;
        try {
            if (assertion.getMethod().equals(GenericHttpClient.METHOD_GET)) {
                loginParams.setPasswordAuthentication(new PasswordAuthentication(creds.getLogin(), creds.getCredentials()));
                loginRequest = httpClient.createRequest(GenericHttpClient.GET, loginParams);
            } else if (assertion.getMethod().equals(GenericHttpClient.METHOD_POST)) {
                GenericHttpRequestParams getLoginFormParams = new GenericHttpRequestParams(loginUrl);
                getLoginFormParams.setSslSocketFactory(sslContext.getSocketFactory());
                getLoginFormParams.setFollowRedirects(false);
                GenericHttpRequest getLoginFormRequest = httpClient.createRequest(GenericHttpClient.GET, getLoginFormParams);
                GenericHttpResponse getLoginFormResponse = getLoginFormRequest.getResponse();
                int status = getLoginFormResponse.getStatus();
                if (status != 200) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERPOST_LOGINFORM_IOEXCEPTION);
                    return AssertionStatus.FAILED;
                }

                DOMParser htmlparser = new DOMParser();
                try {
                    htmlparser.parse(new InputSource(getLoginFormResponse.getInputStream()));
                } catch (SAXException e) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERPOST_LOGINFORM_IOEXCEPTION);
                    return AssertionStatus.FAILED;
                }

                Element docEl = htmlparser.getDocument().getDocumentElement();
                NodeList forms = docEl.getElementsByTagName("form");
                if (forms == null || forms.getLength() == 0) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERPOST_LOGINFORM_NO_FORM);
                    return AssertionStatus.FAILED;
                } if (forms.getLength() > 1) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERPOST_LOGINFORM_MULTIPLE_FORMS);
                    return AssertionStatus.FAILED;
                }

                Element form = (Element)forms.item(0);
                String formAction = form.getAttribute("action");
                String formMethod = form.getAttribute("method");

                loginParams.setTargetUrl(new URL(loginUrl, formAction));

                StringBuffer fieldsBuf = new StringBuffer();
                addPostField(fieldsBuf, assertion.getUsernameFieldname(), creds.getLogin());
                addPostField(fieldsBuf, assertion.getPasswordFieldname(), new String(creds.getCredentials()));
                for (Iterator i = assertion.getExtraFields().keySet().iterator(); i.hasNext();) {
                    String name = (String)i.next();
                    String value = (String)assertion.getExtraFields().get(name);
                    addPostField(fieldsBuf, name, value);
                }

                if (!formMethod.equalsIgnoreCase(GenericHttpClient.METHOD_POST)) {
                    auditor.logAndAudit(AssertionMessages.SAMLBROWSERPOST_LOGINFORM_BAD_METHOD);
                    return AssertionStatus.FAILED;
                }

/*
                // Get cookies!
                List setCookies = getLoginFormResponse.getHeaders().getValues("Set-Cookie");
                ArrayList cookieHeaders = new ArrayList();
                for (Iterator i = setCookies.iterator(); i.hasNext();) {
                    String value = (String)i.next();
                    HttpCookie cookie = new HttpCookie(value);
                    HttpHeader header = new GenericHttpHeader("Cookie", cookie.getCookieName() + "=" + cookie.getCookieValue());
                    cookieHeaders.add(header);
                }

                loginParams.setExtraHeaders((HttpHeader[])cookieHeaders.toArray(new HttpHeader[0]));
*/

                loginParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
                loginRequest = httpClient.createRequest(GenericHttpClient.POST, loginParams);
                loginRequest.setInputStream(new ByteArrayInputStream(fieldsBuf.toString().getBytes("UTF-8")));
            }

            loginResponse = loginRequest.getResponse();
            HttpHeaders loginResponseHeaders = loginResponse.getHeaders();
            int loginResponseStatus = loginResponse.getStatus();
            if (loginResponseStatus == 302) {
                String location = loginResponseHeaders.getOnlyOneValue("Location");
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

                    byte[] responseBytes = HexUtils.slurpStream(loginResponse.getInputStream(), 65536);
                    logger.info(loginResponseHeaders.toString());
                    logger.info(new String(responseBytes, "UTF-8"));

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
            if (loginRequest != null) try { loginRequest.close(); } catch (Throwable t) { }
            if (loginResponse != null) try { loginResponse.close(); } catch (Throwable t) { }
        }
    }

    private void addPostField(StringBuffer postBuf, String name, String value) throws UnsupportedEncodingException {
        if (postBuf.length() > 0) postBuf.append("&");
        postBuf.append(URLEncoder.encode(name, "UTF-8"));
        postBuf.append('=');
        postBuf.append(URLEncoder.encode(value, "UTF-8"));
    }
}
