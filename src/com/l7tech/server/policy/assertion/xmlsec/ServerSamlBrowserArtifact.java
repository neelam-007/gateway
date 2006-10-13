package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.html.HtmlConstants;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.AuthenticationProperties;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.cyberneko.html.parsers.DOMParser;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retrieves a SAML assertion from an identity provider website according to the Browser/Artifact profile.
 *
 * @author alex, $Author$
 * @version $Revision$
 */
public class ServerSamlBrowserArtifact extends AbstractServerAssertion implements ServerAssertion {

    //- PUBLIC

    /**
     *
     */
    public ServerSamlBrowserArtifact(SamlBrowserArtifact assertion, ApplicationContext springContext) {
        super(assertion);
        this.auditor = new Auditor(this, springContext, logger);
        this.assertion = assertion;
        try {
            loginUrl = new URL(assertion.getSsoEndpointUrl());
        } catch (MalformedURLException e) {
            throw (IllegalArgumentException)new IllegalArgumentException("Invalid SAML browser profile URL: " +
                    assertion.getSsoEndpointUrl()).initCause(e);
        }
        httpClient = new CommonsHttpClient(CommonsHttpClient.newConnectionManager());

        try {
           sslContext = SSLContext.getInstance("SSL");
           final SslClientTrustManager trustManager = (SslClientTrustManager)springContext.getBean("httpRoutingAssertionTrustManager");
           hostnameVerifier = (HostnameVerifier)springContext.getBean("httpRoutingAssertionHostnameVerifier", HostnameVerifier.class);
           final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                  HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
           sslContext.getClientSessionContext().setSessionTimeout(timeout);
           sslContext.init(null, new TrustManager[]{trustManager}, null);
       } catch (Exception e) {
           auditor.logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, e);
           throw new RuntimeException(e);
       }
    }

    /**
     *
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return doCheckRequest(context, true);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerSamlBrowserArtifact.class.getName());

    /**
     * Prefix used for any SAML Single Sign On cookies.
     */
    private static final String COOKIE_PREFIX = CookieUtils.PREFIX_GATEWAY_MANAGED + "ssso-";

    /**
     * NEKO Config
     */
    private static final String NEKO_PROP_ELEMS = "http://cyberneko.org/html/properties/names/elems";
    private static final Short NEKO_VALUE_LOWERCASE = new Short((short)2);

    private final Auditor auditor;
    private final SamlBrowserArtifact assertion;
    private final URL loginUrl;

    private final GenericHttpClient httpClient;
    private final SSLContext sslContext;
    private final HostnameVerifier hostnameVerifier;

    /**
     *
     */
    private AssertionStatus doCheckRequest(PolicyEnforcementContext context, boolean passCookies) throws PolicyAssertionException {
        AuthenticationProperties ap = assertion.getAuthenticationProperties();
        GenericHttpState httpState = new GenericHttpState();
        HttpState state = new HttpState();
        state.setCookiePolicy(CookiePolicy.COMPATIBILITY);
        httpState.setStateObject(state);
        GenericHttpRequestParams loginParams = new GenericHttpRequestParams(loginUrl, httpState);
        loginParams.setSslSocketFactory(sslContext.getSocketFactory());
        loginParams.setHostnameVerifier(hostnameVerifier);
        loginParams.setFollowRedirects(false);

        GenericHttpRequest loginRequest = null;
        GenericHttpResponse loginResponse = null;
        try {
            LoginCredentials creds = context.getCredentials();
            if (creds == null) {
                throw new AssertionException(AssertionStatus.AUTH_REQUIRED, AssertionMessages.SAMLBROWSER_CREDENTIALS_NOCREDS);
            }

            if (creds.getFormat() != CredentialFormat.CLEARTEXT) {
                throw new AssertionException(AssertionStatus.AUTH_REQUIRED, AssertionMessages.SAMLBROWSER_CREDENTIALS_CREDS_NOT_PASSWORD);
            }

            // created later if required.
            boolean usingCache = false;
            final Collection inCookies = new HashSet();

            if (AuthenticationProperties.METHOD_BASIC.equals(ap.getMethod())) {
                loginParams.setPasswordAuthentication(new PasswordAuthentication(creds.getLogin(), creds.getCredentials()));
                loginRequest = httpClient.createRequest(GenericHttpClient.GET, loginParams);
            } else if (AuthenticationProperties.METHOD_FORM.equals(ap.getMethod())) {

                if(ap.isEnableCookies() && passCookies) {
                    // then grab any cookies off of the request (from bridge)
                    usingCache = addCookies(context, (HttpState) httpState.getStateObject(), loginUrl.getHost(), inCookies);
                }

                if(usingCache) {
                    // then just request the page
                    if(logger.isLoggable(Level.FINER)) logger.finer("Using cached HttpState when processing '" + context.getRequestId() + "'.");
                    loginRequest = httpClient.createRequest(GenericHttpClient.GET, loginParams);
                }
                else {
                    String usernameFieldname = ap.getUsernameFieldname();
                    String passwordFieldname = ap.getPasswordFieldname();
                    Map formPostParams = new HashMap();

                    if(ap.isRequestForm()) {
                        // request and (if necessary) process form
                        AuthenticationProperties formProperties = new AuthenticationProperties();
                        requestAndProcessForm(httpState, loginParams, formProperties);

                        if(usernameFieldname==null || usernameFieldname.length()==0) {
                            usernameFieldname = formProperties.getUsernameFieldname();
                            passwordFieldname = formProperties.getPasswordFieldname();
                        }
                        formPostParams.putAll(formProperties.getAdditionalFields());
                    }

                    //merge assertion data with that gathered from any retrieved form & login data.
                    formPostParams.putAll(ap.getAdditionalFields());
                    formPostParams.put(usernameFieldname, creds.getLogin());
                    formPostParams.put(passwordFieldname, new String(creds.getCredentials()));

                    if(logger.isLoggable(Level.FINER)) logger.finer("Full post params for request '"+context.getRequestId()+"' are: " + formPostParams);

                    loginParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
                    loginRequest = httpClient.createRequest(GenericHttpClient.POST, loginParams);
                    loginRequest.setInputStream(new ByteArrayInputStream(getFormPostParameters(formPostParams).getBytes("UTF-8")));

                    if(ap.isRedirectAfterSubmit()) {
                        GenericHttpRequest redirectRequest = loginRequest;
                        loginRequest = null;
                        try {
                            // then we need to get the URL of the page we are being redirected to
                            URL targetUrl = processRedirect(httpState, loginParams, redirectRequest);
                            loginParams.setTargetUrl(targetUrl);
                            loginParams.setContentType(null);

                            loginRequest = httpClient.createRequest(GenericHttpClient.GET, loginParams);
                        }
                        finally {
                            if (redirectRequest != null) try { redirectRequest.close(); } catch (Throwable t) { }
                        }
                    }
                }
            }

            loginResponse = loginRequest.getResponse();
            HttpHeaders loginResponseHeaders = loginResponse.getHeaders();
            int loginResponseStatus = loginResponse.getStatus();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Artifact request to ''{0}'', completed with HTTP status code {1}",
                        new Object[]{loginUrl, new Integer(loginResponseStatus)});
            }
            if (loginResponseStatus == HttpConstants.STATUS_FOUND
              ||loginResponseStatus == HttpConstants.STATUS_SEE_OTHER) {
                String location = loginResponseHeaders.getOnlyOneValue(HttpConstants.HEADER_LOCATION);
                if(location==null) throw new GenericHttpException("No such header: " + HttpConstants.HEADER_LOCATION);
                URL redirectUrl = new URL(loginUrl, location);
                context.setVariable(SamlBrowserArtifact.VAR_REDIRECT_URL, redirectUrl);
                String query = redirectUrl.getQuery();
                if (query == null) {
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_NO_QUERY);
                }

                Map queryParams;
                try {
                    queryParams = ParameterizedString.parseQueryString(query);
                } catch (Exception e) {
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_BAD_QUERY, e);
                }

                final String[] artifacts = (String[]) queryParams.get(assertion.getArtifactQueryParameter());
                if (artifacts == null || artifacts.length == 0) {
                    byte[] responseBytes = HexUtils.slurpStreamLocalBuffer(loginResponse.getInputStream());
                    logger.info(loginResponseHeaders.toString());
                    logger.info(new String(responseBytes, HttpConstants.ENCODING_UTF8));
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_NO_ARTIFACT);
                }

                context.setVariable(SamlBrowserArtifact.VAR_ARTIFACT, URLEncoder.encode(artifacts[0], HttpConstants.ENCODING_UTF8));

                if(ap.isEnableCookies()) {
                    // pass cookies back to the bridge/client
                    passCookies(context, (HttpState) httpState.getStateObject(), inCookies);
                }

                return AssertionStatus.NONE;
            } else {
                if(usingCache) {
                    // then our session may no longer be valid, so we have to try again using a new cookie.
                    if(logger.isLoggable(Level.FINER)) logger.finer("Session expired for request '" + context.getRequestId() + "'.");
                    return doCheckRequest(context, false);
                }
                else {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "Unexpected HTTP status code ({0}), response body is: \n{1}",
                                new Object[]{new Integer(loginResponseStatus), new String(HexUtils.slurpStreamLocalBuffer(loginResponse.getInputStream()))});
                    }
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_RESPONSE_NON_302);
                }
            }
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.SAMLBROWSER_LOGINFORM_IOEXCEPTION, null, e);
            return AssertionStatus.FAILED;
        } catch(AssertionException ae) {
            auditor.logAndAudit(ae.getAssertionMessage(), null, ae.getCause());
            return ae.getAssertionStatus();
        } finally {
            if (loginRequest != null) try { loginRequest.close(); } catch (Throwable t) { }
            if (loginResponse != null) try { loginResponse.close(); } catch (Throwable t) { }
        }
    }

    /**
     * Take prefixed cookies from incoming request, remove prefix and add to http state
     */
    private boolean addCookies(PolicyEnforcementContext context, HttpState state, String cookieDomain, Collection added) {
        boolean addedCookies = false;
        Set cookies = context.getCookies();
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            HttpCookie cookie = (HttpCookie) iterator.next();
            if(cookie.getCookieName().startsWith(COOKIE_PREFIX)) {
                // Get and fixup HTTP Client cookie
                Cookie httpClientCookie = CookieUtils.toHttpClientCookie(cookie);
                String cookieName = cookie.getCookieName().substring(COOKIE_PREFIX.length());
                String cookiePath = cookie.getPath();
                if(cookiePath==null) cookiePath = "/";
                httpClientCookie.setName(cookieName);
                httpClientCookie.setPath(cookiePath);
                httpClientCookie.setDomain(cookieDomain);

                // Loggit
                if(logger.isLoggable(Level.FINE)){
                    logger.fine("Adding a cookie to Login Form request '" + httpClientCookie.getName() + "'.");
                }

                // Add to state
                state.addCookie(httpClientCookie);
                added.add(httpClientCookie);
                addedCookies = true;
            }
        }

        return addedCookies;
    }

    /**
     * Get cookies from the http state, prefix them and add them to the response.
     */
    private void passCookies(PolicyEnforcementContext context, HttpState state, Collection originalCookies) {

        Cookie[] cookies = state.getCookies();

        Set newCookies = new LinkedHashSet(Arrays.asList(cookies));
        newCookies.removeAll(originalCookies);

        for (Iterator iterator = newCookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();

            // loggit
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "New cookie from SSO endpoint, name='"+cookie.getName()+"'.");
            }

            // modify for client
            cookie.setName(COOKIE_PREFIX + cookie.getName());
            cookie.setDomain(null);
            cookie.setDomainAttributeSpecified(false);
            cookie.setPath(null);
            cookie.setPathAttributeSpecified(false);

            context.addCookie(CookieUtils.fromHttpClientCookie(cookie, true));
        }
    }

    private void requestAndProcessForm(GenericHttpState httpState, GenericHttpRequestParams loginParams, AuthenticationProperties formProperties) throws AssertionException {
        AuthenticationProperties ap = assertion.getAuthenticationProperties();

        GenericHttpRequest loginFormRequest = null;
        GenericHttpResponse loginFormResponse = null;
        try {
            GenericHttpRequestParams getLoginFormParams = new GenericHttpRequestParams(loginUrl, httpState);
            getLoginFormParams.setSslSocketFactory(sslContext.getSocketFactory());
            getLoginFormParams.setHostnameVerifier(hostnameVerifier);
            getLoginFormParams.setFollowRedirects(false);
            loginFormRequest = httpClient.createRequest(GenericHttpClient.GET, getLoginFormParams);
            loginFormResponse = loginFormRequest.getResponse();
            int status = loginFormResponse.getStatus();
            if (status != HttpConstants.STATUS_OK) {
                throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_NON_200);
            }

            boolean autodetectRequired = (ap.getUsernameFieldname()==null || ap.getUsernameFieldname().length()==0);
            boolean targetRequired = ap.getFormTarget()==null || ap.getFormTarget().length()==0;
            if(autodetectRequired || ap.isCopyFormFields() || targetRequired) {
                URL targetUrl = processForm(loginFormResponse.getInputStream(), autodetectRequired, ap.isCopyFormFields(), formProperties);
                if(targetRequired) {
                    loginParams.setTargetUrl(targetUrl);
                }
            }
            if(!targetRequired) {
                loginParams.setTargetUrl(new URL(ap.getFormTarget()));
            }
        } catch (IOException e) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_IOEXCEPTION, e);
        } finally {
            if (loginFormRequest != null) try { loginFormRequest.close(); } catch (Throwable t) { }
            if (loginFormResponse != null) try { loginFormResponse.close(); } catch (Throwable t) { }
        }
    }

    private URL processForm(InputStream formHtmlIn, boolean autodetectRequired, boolean copyFields, AuthenticationProperties formProperties) throws IOException, AssertionException {
        DOMParser htmlparser = new DOMParser();
        try {
            htmlparser.setProperty(NEKO_PROP_ELEMS, NEKO_VALUE_LOWERCASE); //get neko to lowercase element names
            htmlparser.setEntityResolver(XmlUtil.getSafeEntityResolver());
            htmlparser.parse(new InputSource(formHtmlIn));
        } catch (SAXException e) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_IOEXCEPTION);
        }

        Element docEl = htmlparser.getDocument().getDocumentElement();
        NodeList forms = docEl.getElementsByTagName(HtmlConstants.ELE_FORM);
        if (forms == null || forms.getLength() == 0) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_NO_FORM);
        }
        else if (forms.getLength() > 1) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_MULTIPLE_FORMS);
        }

        Element form = (Element)forms.item(0);
        String formAction = form.getAttribute(HtmlConstants.ATTR_ACTION);
        String formMethod = form.getAttribute(HtmlConstants.ATTR_METHOD);

        boolean foundUsernameField = false;
        boolean foundPasswordField = false;
        Map formPostParams = new HashMap();

        // Parse form inputs (text, password, hidden and submit types only)
        if(autodetectRequired||copyFields) {
            NodeList inputs = form.getElementsByTagName(HtmlConstants.ELE_INPUT);
            for(int i=0; i<inputs.getLength(); i++) {
                String name = ((Element)inputs.item(i)).getAttribute(HtmlConstants.ATTR_NAME);
                String type = ((Element)inputs.item(i)).getAttribute(HtmlConstants.ATTR_TYPE);
                String value = ((Element)inputs.item(i)).getAttribute(HtmlConstants.ATTR_VALUE);
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest("Found form element of type '"+type+"', name='"+name+"', value='"+value+"'");
                }

                if(type==null || type.length()==0) {
                    type = HtmlConstants.VALUE_TEXT; // text is the default type
                }

                if(HtmlConstants.VALUE_TEXT.equalsIgnoreCase(type) && autodetectRequired) {
                    if(foundUsernameField) {
                        throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_MULTIPLE_FIELDS);
                    }
                    else {
                        foundUsernameField = true;
                        formProperties.setUsernameFieldname(name);
                    }
                }
                else if(HtmlConstants.VALUE_PASSWORD.equalsIgnoreCase(type) && autodetectRequired) {
                    if(foundPasswordField) {
                        throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_MULTIPLE_FIELDS);
                    }
                    else {
                        foundPasswordField = true;
                        formProperties.setPasswordFieldname(name);
                    }
                }
                else if(copyFields
                        && (HtmlConstants.VALUE_HIDDEN.equalsIgnoreCase(type)
                          ||HtmlConstants.VALUE_SUBMIT.equalsIgnoreCase(type))){
                    if(name!=null && name.length()>0) {
                        formPostParams.put(name,value==null?"":value);
                    }
                }
            }
        }

        if(autodetectRequired && (!foundUsernameField || !foundPasswordField)) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_CANT_FIND_FIELDS);
        }

        if(formMethod==null || !formMethod.trim().equalsIgnoreCase(HtmlConstants.VALUE_POST)) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_INVALID);
        }

        formProperties.setAdditionalFields(formPostParams);
        URL formTarget = new URL(loginUrl, formAction);

        // this MUST be a resource on the same server
        if(!loginUrl.getHost().equals(formTarget.getHost())) {
            throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_INVALID);
        }

        if(logger.isLoggable(Level.FINEST)) {
            logger.finest("Form target url is: " + formTarget.toExternalForm());
        }

        return formTarget;
    }

    /**
     * This modifies the given loginParams on success so the target url is the redirection.
     */
    private URL processRedirect(GenericHttpState httpState, GenericHttpRequestParams loginParams, GenericHttpRequest formPostRequest) throws IOException, AssertionException {
        GenericHttpResponse formRedirectResponse = null;
        try {
            formRedirectResponse = formPostRequest.getResponse();
            HttpHeaders formRedirectResponseHeaders = formRedirectResponse.getHeaders();
            int formRedirectResponseStatus = formRedirectResponse.getStatus();
            if(formRedirectResponseStatus == HttpConstants.STATUS_FOUND
            || formRedirectResponseStatus == HttpConstants.STATUS_SEE_OTHER){ // Should be 303 but we'll take a 302 ...
                String location = formRedirectResponseHeaders.getOnlyOneValue(HttpConstants.HEADER_LOCATION);
                if(location==null) throw new GenericHttpException("No such header: " + HttpConstants.HEADER_LOCATION);
                URL targetUrl = loginParams.getTargetUrl();
                URL redirectUrl = new URL(targetUrl, location);

                // this MUST be a resource on the same server
                if(!targetUrl.getHost().equals(redirectUrl.getHost())) {
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSER_LOGINFORM_REDIRECT_INVALID);
                }

                return redirectUrl;
            }
            else {
                throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_RESPONSE_NON_302);
            }
        } finally {
            if (formRedirectResponse != null) try { formRedirectResponse.close(); } catch (Throwable t) { }
        }
    }

    private String getFormPostParameters(Map formPostParams) throws UnsupportedEncodingException {
        StringBuffer fieldsBuf = new StringBuffer();
        for (Iterator i = formPostParams.entrySet().iterator(); i.hasNext();) {
            Map.Entry fieldEntry = (Map.Entry) i.next();
            String name = (String) fieldEntry.getKey();
            String value = (String) fieldEntry.getValue();
            addPostField(fieldsBuf, name, value);
        }

        return fieldsBuf.toString();
    }

    /**
     * Append data to a buffer of post formatted name value pairs.
     */
    private void addPostField(StringBuffer postBuf, String name, String value) throws UnsupportedEncodingException {
        if(name!=null) {
            if (postBuf.length() > 0) postBuf.append("&");
            postBuf.append(URLEncoder.encode(name, HttpConstants.ENCODING_UTF8));
            postBuf.append('=');
            postBuf.append(URLEncoder.encode(value==null?"":value, HttpConstants.ENCODING_UTF8));
        }
    }

    /**
     * Exception thrown internally to HALT processing.
     */
    private static class AssertionException extends Exception
    {
        private final AssertionStatus status;
        private final AssertionMessages.M message;

        AssertionException(AssertionStatus status, AssertionMessages.M message) {
            super(message.getMessage());
            this.status = status;
            this.message = message;
        }

        AssertionException(AssertionStatus status, AssertionMessages.M message, Throwable cause) {
            super(message.getMessage(), cause);
            this.status = status;
            this.message = message;
        }

        public AssertionStatus getAssertionStatus() {
            return status;
        }

        public AssertionMessages.M getAssertionMessage() {
            return message;
        }
    }
}
