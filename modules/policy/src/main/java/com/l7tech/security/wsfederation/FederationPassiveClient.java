package com.l7tech.security.wsfederation;

import com.l7tech.util.HtmlConstants;
import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.wstrust.TokenServiceClient;
import com.l7tech.security.wstrust.WsTrustConfig;
import com.l7tech.security.wstrust.WsTrustConfigFactory;
import com.l7tech.util.*;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.common.io.XmlUtil;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Client implementation for WS-Federation Passive Request Profile.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class FederationPassiveClient {

    //- PUBLIC

    /**
     * <p>Get a SecurityToken from a WS-Federation server using the passive
     * request profile.</p>
     *
     * <p>This method would be called to get a token from the local federation
     * server (e.g. SSO)</p>
     *
     * @param httpClient the client to use
     * @param httpParams the params (with targetUrl, credentials and SSL context set if needed)
     * @param realm the federation realm
     * @param replyUrl the reply url (application url) may be null
     * @param context the context may be null
     * @param addTimestamp true to add a timestamp to the HTTP request
     * @return the SecurityToken
     * @throws IOException if an error occurs
     * @see com.l7tech.security.wsfederation.InvalidHtmlException
     * @see com.l7tech.security.wsfederation.InvalidTokenException
     * @see com.l7tech.security.wsfederation.ResponseStatusException
     */
    public static XmlSecurityToken obtainFederationToken(GenericHttpClient httpClient
                                                     ,GenericHttpRequestParams httpParams
                                                     ,String realm
                                                     ,String replyUrl
                                                     ,String context
                                                     ,boolean addTimestamp) throws IOException {

        if(httpClient==null) throw new IllegalArgumentException("httpClient must not be null");
        if(httpParams==null) throw new IllegalArgumentException("httpParams must not be null");
        if(httpParams.getTargetUrl()==null) throw new IllegalArgumentException("httpParams targetUrl must not be null");
        if(httpParams.getTargetUrl().getQuery()!=null) throw new IllegalArgumentException("url must not have a query string");

        Document result;
        URL ipStsUrl = httpParams.getTargetUrl();

        URL targetUrl = buildUrl(ipStsUrl.toExternalForm(), realm, replyUrl, context, addTimestamp);
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "WS-Federation token request URL: '"+targetUrl.toExternalForm()+"'.");
        }
        GenericHttpRequestParams httpParamsCopy = new GenericHttpRequestParams(httpParams);

        httpParamsCopy.setTargetUrl(targetUrl);
        if(httpParamsCopy.getPasswordAuthentication()!=null) {
            httpParamsCopy.setPreemptiveAuthentication(true);
        }

        try {
            // Get RSTR
            SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
            SimpleHttpClient.SimpleHttpResponse response = simpleClient.get(httpParamsCopy);
            int status = response.getStatus();
            if (status != HttpConstants.STATUS_OK) {
                throw new ResponseStatusException("Failure status code from server: " + status, status);
            }

            ContentTypeHeader contentType = response.getContentType();
            if(contentType==null) throw new UnsupportedEncodingException("No content type header in response");
            if(!contentType.isHtml()) throw new InvalidHtmlException("Response is not html content " + contentType);

            String encoding = contentType.getEncoding();
            byte[] responseBytes = response.getBytes();
            String html = new String(responseBytes, encoding);
            result = getDocument(html);
        }
        catch(GenericHttpException ghe) {
           Throwable sslException = ExceptionUtils.getCauseIfCausedBy(ghe, SSLException.class);
            if (sslException instanceof SSLException)
                throw (SSLException)sslException; // rethrow as SSLException so server cert can be discovered if necessary
            throw ghe; // let it through as-is
        }

        return parseFederationToken(result);
    }

    /**
     * <p>Exchange a SecurityToken with a WS-Federation server using the
     * passive request profile.</p>
     *
     * <p>This method would be called to exchange a token from a requestor
     * IP/STS with one from the resource IP/STS (using a FORM/POST).</p>
     *
     * @param httpClient the client to use
     * @param httpParams the params (with targetUrl and SSL context set if needed)
     * @param requestorToken the token from the requestors IP/STS
     * @param context the context for the token (end point url)
     * @param addTimestamp true to add a timestamp to the HTTP request
     * @return the SecurityToken
     * @throws IOException if an error occurs
     * @see com.l7tech.security.wsfederation.InvalidHtmlException
     * @see com.l7tech.security.wsfederation.InvalidTokenException
     * @see com.l7tech.security.wsfederation.ResponseStatusException
     */
    public static XmlSecurityToken exchangeFederationToken(GenericHttpClient httpClient
                                                       ,GenericHttpRequestParams httpParams
                                                       , XmlSecurityToken requestorToken
                                                       ,String context
                                                       ,boolean addTimestamp) throws IOException {

        if(httpClient==null) throw new IllegalArgumentException("httpClient must not be null");
        if(httpParams==null) throw new IllegalArgumentException("httpParams must not be null");
        if(httpParams.getTargetUrl()==null) throw new IllegalArgumentException("httpParams targetUrl must not be null");

        Document result;
        String requestBody = buildRequestBody(requestorToken, context, addTimestamp);

        try {
            // Get RSTR
            byte[] bodyData = requestBody.getBytes(HttpConstants.ENCODING_UTF8);
            httpParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
            httpParams.setExtraHeaders(new GenericHttpHeader[]{new GenericHttpHeader(HttpConstants.HEADER_CONTENT_LENGTH, Integer.toString(bodyData.length))});
            SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
            SimpleHttpClient.SimpleHttpResponse response = simpleClient.post(httpParams, bodyData);

            int status = response.getStatus();
            if (status != HttpConstants.STATUS_OK) {
                throw new ResponseStatusException("Failure status code from server: " + status, status);
            }

            ContentTypeHeader contentType = response.getContentType();
            if(contentType==null) throw new UnsupportedEncodingException("No content type header in response");
            if(!contentType.isHtml()) throw new InvalidHtmlException("Response is not html content " + contentType);

            String encoding = contentType.getEncoding();
            byte[] responseBytes = response.getBytes();
            String html = new String(responseBytes, encoding);
            result = getDocument(html);
        }
        catch(GenericHttpException ghe) {
           Throwable sslException = ExceptionUtils.getCauseIfCausedBy(ghe, SSLException.class);
            if (sslException instanceof SSLException)
                throw (SSLException)sslException; // rethrow as SSLException so server cert can be discovered if necessary
            throw ghe; // let it through as-is
        }

        return parseFederationToken(result);
    }

    /**
     * <p>POST a SecurityToken with a WS-Federation resource server.</p>
     *
     * <p>This method would be called to authenticate the caller with the
     * resource server before routing.</p>
     *
     * @param httpClient the client to use
     * @param httpParams the params (with targetUrl and SSL context set if needed)
     * @param resourceToken the token to authenticate with
     * @param context the context for the token (end point url)
     * @param addTimestamp true to add a timestamp to the HTTP request
     * @return the SecurityToken
     * @throws IOException if an error occurs
     * @see com.l7tech.security.wsfederation.ResponseStatusException
     * @see com.l7tech.security.wsfederation.NotAuthorizedException
     */
    public static Set<HttpCookie> postFederationToken( final GenericHttpClient httpClient,
                                                       final GenericHttpRequestParams httpParams,
                                                       final XmlSecurityToken resourceToken,
                                                       final String context,
                                                       final boolean addTimestamp ) throws IOException {

        if(httpClient==null) throw new IllegalArgumentException("httpClient must not be null");
        if(httpParams==null) throw new IllegalArgumentException("httpParams must not be null");
        if(httpParams.getTargetUrl()==null) throw new IllegalArgumentException("httpParams targetUrl must not be null");

        Set<HttpCookie> cookies = new LinkedHashSet<HttpCookie>();
        URL ipStsUrl = httpParams.getTargetUrl();

        String requestBody = buildRequestBody(resourceToken, context, addTimestamp);

        try {
            // POST RSTR
            byte[] bodyData = requestBody.getBytes(HttpConstants.ENCODING_UTF8);
            httpParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
            httpParams.setExtraHeaders(new GenericHttpHeader[]{new GenericHttpHeader(HttpConstants.HEADER_CONTENT_LENGTH, Integer.toString(bodyData.length))});
            SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
            SimpleHttpClient.SimpleHttpResponse response = simpleClient.post(httpParams, bodyData);

            int status = response.getStatus();
            if (status != HttpConstants.STATUS_FOUND && status != HttpConstants.STATUS_SEE_OTHER) {
                throw new ResponseStatusException("Failure status code from server: " + status, status);
            }

            HttpHeaders headers = response.getHeaders();

            // check redirect is not to a federation server (i.e. auth required)
            String responseUrlStr = headers.getOnlyOneValue(HttpConstants.HEADER_LOCATION);
            if(responseUrlStr==null) throw new GenericHttpException("No such header: " + HttpConstants.HEADER_LOCATION);
            URL responseUrl = new URL(ipStsUrl, responseUrlStr);
            if(isFederationServerUrl(responseUrl)) {
                throw new NotAuthorizedException("Redirected to federation server.");
            }

            List cookieHeaders = headers.getValues(HttpConstants.HEADER_SET_COOKIE);
            try {
                for (Object cookieHeader : cookieHeaders) {
                    String value = (String) cookieHeader;
                    cookies.add(new HttpCookie(ipStsUrl, value));
                }
            }
            catch(HttpCookie.IllegalFormatException hcife) {
                throw new CausedIOException("Illegal cookie header", hcife);
            }

            // assume that if we don't get cookies we're authorized in some other manner?
        }
        catch(GenericHttpException ghe) {
           Throwable sslException = ExceptionUtils.getCauseIfCausedBy(ghe, SSLException.class);
            if (sslException instanceof SSLException)
                throw (SSLException)sslException; // rethrow as SSLException so server cert can be discovered if necessary
            throw ghe; // let it through as-is
        }

        return Collections.unmodifiableSet(cookies);
    }

    /**
     * Check if a URL is recognizable as a "federation server" URL.
     *
     * <p>Currently this just checks for a sign-in action query parameter.</p>
     *
     * @param url the url to check
     * @return true if the given URL is a "federation server" URL
     */
    public static boolean isFederationServerUrl(URL url) {
        boolean isFedUrl = false;

        String responseUrlQuery = url.getQuery();
        if(responseUrlQuery!=null) {
            try {
                ParameterizedString ps = new ParameterizedString(responseUrlQuery);
                if(ps.parameterHasSingleValue(WSFederationConstants.PARAM_ACTION)
                && WSFederationConstants.VALUE_ACTION_SIGNIN.equals(ps.getParameterValue(WSFederationConstants.PARAM_ACTION))){
                    isFedUrl = true;
                }
            }
            catch(IllegalArgumentException iae) {
                if(logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Could not parse query string: " + iae.getMessage());
            }
        }

        return isFedUrl;
    }

    //- PRIVATE

    /**
     *
     */
    private static final Logger logger = Logger.getLogger(FederationPassiveClient.class.getName());

    /**
     * RSTR template strings
     */
    private static final String RSTR_TEMPLATE = "<wst:RequestSecurityTokenResponse xmlns:wst=\"http://schemas.xmlsoap.org/ws/2005/02/trust\"><wst:RequestedSecurityToken></wst:RequestedSecurityToken><wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\"><wsa:EndpointReference xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"><wsa:Address></wsa:Address></wsa:EndpointReference></wsp:AppliesTo></wst:RequestSecurityTokenResponse>";

    /**
     * NEKO Config
     */
    private static final String NEKO_PROP_ELEMS = "http://cyberneko.org/html/properties/names/elems";
    private static final Short NEKO_VALUE_LOWERCASE = (short)2;

    /**
     * Currently only SamlAssertion is supported.
     */
    private static SamlAssertion parseFederationToken(final Document rstrDoc) throws IOException {
        SamlAssertion result;
        try {
            Document soapWrapped;

            // Could be RSTR by itself or could already be in a SOAP Envelope
            if(!SoapConstants.ENVELOPE_EL_NAME.equals(rstrDoc.getDocumentElement().getLocalName())) {
                // SOAP it
                SOAPMessage message = SoapUtil.makeMessage(); // this takes a long time the first time
                SOAPEnvelope env = message.getSOAPPart().getEnvelope();
                soapWrapped = XmlUtil.createEmptyDocument(env.getElementName().getLocalName(), env.getElementName().getPrefix(), env.getElementName().getURI());
                SOAPBody body = env.getBody();
                Element bodyEle = soapWrapped.createElementNS(body.getElementName().getURI(), body.getElementName().getQualifiedName());
                soapWrapped.getDocumentElement().appendChild(bodyEle);
                bodyEle.appendChild(soapWrapped.importNode(rstrDoc.getDocumentElement(), true));
            }
            else {
               soapWrapped = rstrDoc;
            }

            // Parse
            WsTrustConfig wstConfig = WsTrustConfigFactory.getDefaultWsTrustConfig();
            TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig);
            Object rstrObj = tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(soapWrapped);
            if (!(rstrObj instanceof SamlAssertion)) {
                throw new InvalidTokenException("Unsupported token type");
            }
            result = (SamlAssertion) rstrObj;
        }
        catch(SOAPException se) {
            throw new InvalidTokenException("Error with SOAP wrapper", se);
        }
        catch(InvalidDocumentFormatException idfe) {
            throw new InvalidTokenException("Error with token format", idfe);
        }
        return result;
    }

    /**
     * Get the XML document embedded in the hidden HTML form element "wresult".
     */
    private static Document getDocument(String html) throws IOException {
        Document document;


        try {
            DOMParser htmlparser = new DOMParser();
            htmlparser.setProperty(NEKO_PROP_ELEMS, NEKO_VALUE_LOWERCASE); //get neko to lowercase element names
            htmlparser.setEntityResolver(XmlUtil.getSafeEntityResolver());
            htmlparser.parse(new InputSource(new StringReader(html)));

            Element docEl = htmlparser.getDocument().getDocumentElement();

            // DO NOT get the inputs from below the form due to a bug in ADFS (form element is empty)
            String xmlText = null;
            NodeList inputs = docEl.getElementsByTagName(HtmlConstants.ELE_INPUT);
            for(int i=0; i<inputs.getLength(); i++) {
                String name = ((Element)inputs.item(i)).getAttribute(HtmlConstants.ATTR_NAME);
                String value = ((Element)inputs.item(i)).getAttribute(HtmlConstants.ATTR_VALUE); // decodes for you

                if(WSFederationConstants.PARAM_RESULT.equals(name)) {
                    if(xmlText!=null) { // then we have multiple wresult elements, throw an exception
                        throw new InvalidHtmlException("Multiple wresult elements in HTML/FORM");
                    }
                    xmlText = value;
                }
            }

            if(xmlText!=null) {
                document = XmlUtil.stringToDocument(xmlText);
            }
            else { // missing wresult element, throw an exception
                throw new InvalidHtmlException("Missing wresult element in HTML/FORM");
            }
        }
        catch(SAXException se) {
            throw new InvalidHtmlException("Cannot parse HTML from server", se);
        }

        return document;
    }

    /**
     *
     */
    private static String getTimestamp() {
        return ISO8601Date.format(new Date());
    }

    /**
     *
     */
    private static void appendParameter(StringBuffer buffer, char seperator, String name, String value) {
        if(seperator>0) {
            buffer.append(seperator);
        }
        buffer.append(name);
        buffer.append('=');
        buffer.append(value);
    }

    /**
     * https://adserv.l7tech.com/adfs/ls/auth/integrated/?wa=wsignin1.0&wreply=http%3a%2f%2ffedserv.l7tech.com%2fACMEWarehouseWS%2fService1.asmx&whr=urn%3afederation%3aself&wct=2005-10-22T01%3a06%3a14Z
     */
    private static URL buildUrl(String urlBase, String realm, String reply, String context, boolean addTimestamp) {
        try {
            // build query string based on supplied values
            StringBuffer qsFormat = new StringBuffer(512);
            appendParameter(qsFormat, '?', WSFederationConstants.PARAM_ACTION, WSFederationConstants.VALUE_ACTION_SIGNIN);

            if(realm!=null && realm.trim().length()>0) {
                appendParameter(qsFormat, '&', WSFederationConstants.PARAM_REALM, URLEncoder.encode(realm, HttpConstants.ENCODING_UTF8));
            }

            if(reply!=null && reply.trim().length()>0) {
                appendParameter(qsFormat, '&', WSFederationConstants.PARAM_REPLY, URLEncoder.encode(reply, HttpConstants.ENCODING_UTF8));
            }

            // add the context (if given)
            if(context!=null && context.trim().length()>0) {
                appendParameter(qsFormat, '&', WSFederationConstants.PARAM_CONTEXT, URLEncoder.encode(context, HttpConstants.ENCODING_UTF8));
            }

            if(addTimestamp) {
                appendParameter(qsFormat, '&', WSFederationConstants.PARAM_TIME, URLEncoder.encode(getTimestamp(), HttpConstants.ENCODING_UTF8));
            }

            return new URL(urlBase + qsFormat.toString());
        }
        catch(UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee); // does not happen
        }
        catch(MalformedURLException murle) {
            throw new IllegalStateException(murle);
        }
    }

    /**
     * Wraps up the token as in the original RSTR and encodes with other form parameters
     */
    private static String buildRequestBody(XmlSecurityToken token, String context, boolean addTimestamp) throws IOException {
        StringBuffer bodyBuffer = new StringBuffer(512);

        // add action
        appendParameter(bodyBuffer, (char)0, WSFederationConstants.PARAM_ACTION, WSFederationConstants.VALUE_ACTION_SIGNIN);

        // add timestamp if requested
        if(addTimestamp) {
            appendParameter(bodyBuffer, '&', WSFederationConstants.PARAM_TIME, URLEncoder.encode(getTimestamp(), HttpConstants.ENCODING_UTF8));
        }

        // add the context (if given)
        if(context!=null && context.trim().length()>0) {
            appendParameter(bodyBuffer, '&', WSFederationConstants.PARAM_CONTEXT, URLEncoder.encode(context, HttpConstants.ENCODING_UTF8));
        }

        Element samlTokenElement = token.asElement();
        try {
            // Get audience and add to RSTR template
            String audText = DomUtils.getTextValue(getOneElementByTagNameNS(samlTokenElement, SamlConstants.NS_SAML, SamlConstants.ELEMENT_AUDIENCE));
            Document rstrDoc = XmlUtil.stringToDocument(RSTR_TEMPLATE);
            Element rstrDocEle = rstrDoc.getDocumentElement();
            Element address = getOneElementByTagNameNS(rstrDocEle, "http://schemas.xmlsoap.org/ws/2004/08/addressing", "Address");
            address.appendChild(DomUtils.createTextNode(rstrDoc, audText));

            // Add token to RSTR
            Element tokenHolder = getOneElementByTagNameNS(rstrDocEle, SoapConstants.WST_NAMESPACE_ARRAY, "RequestedSecurityToken");
            Element imported = (Element) rstrDoc.importNode(samlTokenElement, true);
            tokenHolder.appendChild(imported);

            // Encode and add to POST body
            appendParameter(bodyBuffer, '&', WSFederationConstants.PARAM_RESULT, URLEncoder.encode(XmlUtil.nodeToString(rstrDoc), HttpConstants.ENCODING_UTF8));
        }
        catch(SAXException se) {
            throw new CausedIOException("Error with RSTR template", se);
        }

        return bodyBuffer.toString();
    }

    /**
     *
     */
    private static Element getOneElementByTagNameNS(Element element, String namespace, String elementName) throws CausedIOException {
        return getOneElementByTagNameNS(element, new String[]{namespace}, elementName);
    }

    /**
     *
     */
    private static Element getOneElementByTagNameNS(Element element, String[] namespaces, String elementName) throws CausedIOException {
        Element result = null;

        for (String namespace : namespaces) {
            NodeList matchingElements = element.getElementsByTagNameNS(namespace, elementName);
            if (matchingElements.getLength() > 1) {
                throw new CausedIOException("Error, multiple " + elementName + " elements.");
            } else if (matchingElements.getLength() == 1) {
                if (result != null) throw new CausedIOException("Error, multiple " + elementName + " elements.");
                result = (Element) matchingElements.item(0);
            }
        }

        if(result==null) {
            throw new CausedIOException("Missing " +elementName+ " element.");
        }

        return result;
    }
}
