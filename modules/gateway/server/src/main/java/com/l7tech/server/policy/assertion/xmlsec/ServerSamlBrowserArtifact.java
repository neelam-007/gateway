package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.http.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.AuthenticationProperties;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Charsets;
import com.l7tech.util.HtmlConstants;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.cyberneko.html.parsers.DOMParser;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

/**
 * Retrieves a SAML assertion from an identity provider website according to the Browser/Artifact profile.
 *
 * @author alex
 */
public class ServerSamlBrowserArtifact extends AbstractServerAssertion<SamlBrowserArtifact> {

    //- PUBLIC

    /**
     *
     */
    public ServerSamlBrowserArtifact(SamlBrowserArtifact assertion, ApplicationContext springContext) {
        super(assertion);
        try {
            loginUrl = new URL(assertion.getSsoEndpointUrl());
        } catch (MalformedURLException e) {
            throw (IllegalArgumentException)new IllegalArgumentException("Invalid SAML browser profile URL: " +
                    assertion.getSsoEndpointUrl()).initCause(e);
        }
        httpClient = springContext.getBean( "anonHttpClientFactory", GenericHttpClientFactory.class ).createHttpClient();
    }

    /**
     *
     */
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return doCheckRequest(context, true);
    }

    //- PRIVATE

    /**
     * Prefix used for any SAML Single Sign On cookies.
     */
    private static final String COOKIE_PREFIX = CookieUtils.PREFIX_GATEWAY_MANAGED + "ssso-";

    /**
     * NEKO Config
     */
    private static final String NEKO_PROP_ELEMS = "http://cyberneko.org/html/properties/names/elems";
    private static final Short NEKO_VALUE_LOWERCASE = 2;


    private final URL loginUrl;

    private final GenericHttpClient httpClient;

    /**
     *
     */
    private AssertionStatus doCheckRequest(PolicyEnforcementContext context, boolean passCookies) throws PolicyAssertionException {
        AuthenticationProperties ap = assertion.getAuthenticationProperties();
        GenericHttpState httpState = new GenericHttpState();
        HttpContext state = new BasicHttpContext();
        httpState.setStateObject(state);
        GenericHttpRequestParams loginParams = new GenericHttpRequestParams(loginUrl, httpState);
        loginParams.setFollowRedirects(false);

        GenericHttpRequest loginRequest = null;
        GenericHttpResponse loginResponse = null;
        try {
            LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
            if (creds == null) {
                throw new AssertionException(AssertionStatus.AUTH_REQUIRED, AssertionMessages.SAMLBROWSER_CREDENTIALS_NOCREDS);
            }

            if (creds.getFormat() != CredentialFormat.CLEARTEXT) {
                throw new AssertionException(AssertionStatus.AUTH_REQUIRED, AssertionMessages.SAMLBROWSER_CREDENTIALS_CREDS_NOT_PASSWORD);
            }

            // created later if required.
            boolean usingCache = false;
            final Collection<Cookie> inCookies = new HashSet<Cookie>();

            if (AuthenticationProperties.METHOD_BASIC.equals(ap.getMethod())) {
                loginParams.setPasswordAuthentication(new PasswordAuthentication(creds.getLogin(), creds.getCredentials()));
                loginRequest = httpClient.createRequest(HttpMethod.GET, loginParams);
            } else if (AuthenticationProperties.METHOD_FORM.equals(ap.getMethod())) {

                if(ap.isEnableCookies() && passCookies) {
                    // then grab any cookies off of the request (from bridge)
                    usingCache = addCookies(context, (HttpContext) httpState.getStateObject(), loginUrl.getHost(), inCookies);
                }

                if(usingCache) {
                    // then just request the page
                    if(logger.isLoggable(Level.FINER)) logger.finer("Using cached HttpState when processing '" + context.getRequestId() + "'.");
                    loginRequest = httpClient.createRequest(HttpMethod.GET, loginParams);
                }
                else {
                    String usernameFieldname = ap.getUsernameFieldname();
                    String passwordFieldname = ap.getPasswordFieldname();
                    Map<String, String> formPostParams = new HashMap<String, String>();

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
                    loginRequest = httpClient.createRequest(HttpMethod.POST, loginParams);
                    loginRequest.setInputStream(new ByteArrayInputStream(getFormPostParameters(formPostParams).getBytes(Charsets.UTF8)));

                    if(ap.isRedirectAfterSubmit()) {
                        GenericHttpRequest redirectRequest = loginRequest;
                        loginRequest = null;
                        try {
                            // then we need to get the URL of the page we are being redirected to
                            URL targetUrl = processRedirect(loginParams, redirectRequest);
                            loginParams.setTargetUrl(targetUrl);
                            loginParams.setContentType(null);

                            loginRequest = httpClient.createRequest(HttpMethod.GET, loginParams);
                        } finally {
                            ResourceUtils.closeQuietly(redirectRequest);
                        }
                    }
                }
            }

            loginResponse = loginRequest.getResponse();
            HttpHeaders loginResponseHeaders = loginResponse.getHeaders();
            int loginResponseStatus = loginResponse.getStatus();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Artifact request to ''{0}'', completed with HTTP status code {1}",
                        new Object[]{loginUrl, loginResponseStatus });
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
                    byte[] responseBytes = IOUtils.slurpStream(loginResponse.getInputStream());
                    logger.info(loginResponseHeaders.toString());
                    logger.info(new String(responseBytes, HttpConstants.ENCODING_UTF8));
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_REDIRECT_NO_ARTIFACT);
                }

                context.setVariable(SamlBrowserArtifact.VAR_ARTIFACT, URLEncoder.encode(artifacts[0], HttpConstants.ENCODING_UTF8));

                if(ap.isEnableCookies()) {
                    // pass cookies back to the bridge/client
                    passCookies(context, (HttpContext) httpState.getStateObject(), inCookies);
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
                                new Object[]{loginResponseStatus, new String(IOUtils.slurpStream(loginResponse.getInputStream()))});
                    }
                    throw new AssertionException(AssertionStatus.FAILED, AssertionMessages.SAMLBROWSERARTIFACT_RESPONSE_NON_302);
                }
            }
        } catch (IOException e) {
            logAndAudit(AssertionMessages.SAMLBROWSER_LOGINFORM_IOEXCEPTION, null, e);
            return AssertionStatus.FAILED;
        } catch(AssertionException ae) {
            logAndAudit(ae.getAssertionMessage(), null, ae.getCause());
            return ae.getAssertionStatus();
        } finally {
            ResourceUtils.closeQuietly(loginRequest, loginResponse);
        }
    }

    /**
     * Take prefixed cookies from incoming request, remove prefix and add to http state
     */
    private boolean addCookies(PolicyEnforcementContext context, HttpContext state, String cookieDomain, Collection<Cookie> added) {
        boolean addedCookies = false;
        final Set<HttpCookie> cookies = context.getCookies();
        CookieStore cookieStore = (CookieStore) state.getAttribute(ClientContext.COOKIE_STORE);
        if (cookieStore == null) {
            cookieStore = new BasicCookieStore();
            state.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        }

        for ( final HttpCookie cookie : cookies ) {
            if (cookie.getCookieName().startsWith(COOKIE_PREFIX)) {
                // Get and fixup HTTP Client cookie
                BasicClientCookie httpClientCookie = (BasicClientCookie) CookieUtils.toHttpClientCookie(cookie, COOKIE_PREFIX);
                String cookiePath = cookie.getPath();
                if (cookiePath == null) cookiePath = "/";
                httpClientCookie.setPath(cookiePath);
                httpClientCookie.setDomain(cookieDomain);

                // Loggit
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Adding a cookie to Login Form request '" + httpClientCookie.getName() + "'.");
                }

                // Add to state
                cookieStore.addCookie(httpClientCookie);
                added.add(httpClientCookie);
                addedCookies = true;
            }
        }

        return addedCookies;
    }

    /**
     * Get cookies from the http state, prefix them and add them to the response.
     */
    private void passCookies(PolicyEnforcementContext context, HttpContext state, Collection originalCookies) {

        CookieStore cookieStore = (CookieStore) state.getAttribute(ClientContext.COOKIE_STORE);

        Set<Cookie> newCookies = new LinkedHashSet<Cookie>(cookieStore.getCookies());
        newCookies.removeAll(originalCookies);

        for (Cookie cookie : newCookies) {
            // loggit
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "New cookie from SSO endpoint, name='" + cookie.getName() + "'.");
            }
            // modify for client
            BasicClientCookie basicClientCookie = new BasicClientCookie(COOKIE_PREFIX+cookie.getName(), cookie.getValue());
            basicClientCookie.setComment(cookie.getComment());
            basicClientCookie.setDomain(null);
            basicClientCookie.setExpiryDate(cookie.getExpiryDate());
            basicClientCookie.setPath(null);
            basicClientCookie.setSecure(cookie.isSecure());
            basicClientCookie.setVersion(cookie.getVersion());

            context.addCookie(CookieUtils.fromHttpClientCookie(basicClientCookie, true));
        }
    }

    private void requestAndProcessForm(GenericHttpState httpState, GenericHttpRequestParams loginParams, AuthenticationProperties formProperties) throws AssertionException {
        AuthenticationProperties ap = assertion.getAuthenticationProperties();

        GenericHttpRequest loginFormRequest = null;
        GenericHttpResponse loginFormResponse = null;
        try {
            GenericHttpRequestParams getLoginFormParams = new GenericHttpRequestParams(loginUrl, httpState);
            getLoginFormParams.setFollowRedirects(false);
            loginFormRequest = httpClient.createRequest(HttpMethod.GET, getLoginFormParams);
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
            ResourceUtils.closeQuietly(loginFormRequest, loginFormResponse);
        }
    }

    private URL processForm(InputStream formHtmlIn, boolean autodetectRequired, boolean copyFields, AuthenticationProperties formProperties) throws IOException, AssertionException {
        DOMParser htmlparser = new DOMParser();
        try {
            htmlparser.setProperty(NEKO_PROP_ELEMS, NEKO_VALUE_LOWERCASE); //get neko to lowercase element names
            htmlparser.setEntityResolver( XmlUtil.getSafeEntityResolver());
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
        String formMethod = form.getAttribute( HtmlConstants.ATTR_METHOD);

        boolean foundUsernameField = false;
        boolean foundPasswordField = false;
        Map<String,String> formPostParams = new HashMap<String,String>();

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
    private URL processRedirect(GenericHttpRequestParams loginParams, GenericHttpRequest formPostRequest) throws IOException, AssertionException {
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
            ResourceUtils.closeQuietly(formRedirectResponse);
        }
    }

    private String getFormPostParameters(Map<String, String> formPostParams) throws UnsupportedEncodingException {
        StringBuffer fieldsBuf = new StringBuffer();
        for (Map.Entry<String, String> fieldEntry : formPostParams.entrySet()) {
            String name = fieldEntry.getKey();
            String value = fieldEntry.getValue();
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
