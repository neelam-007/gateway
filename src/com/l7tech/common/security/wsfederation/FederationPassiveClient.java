package com.l7tech.common.security.wsfederation;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Date;
import javax.net.ssl.SSLException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.wstrust.TokenServiceClient;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.common.xml.saml.SamlAssertion;

/**
 * <p>Client implementation for WS-Federation Passive Request Profile.</p>
 *
 * TODO do we need to allow sending of wctx parameters? (opaque context values)
 * TODO ensure no network access for parsing (entity resolution?)
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
     * @param addTimestamp true to add a timestamp to the HTTP request
     * @return the SecurityToken
     * @throws IOException if an error occurs
     * @see com.l7tech.common.security.wsfederation.InvalidHtmlException
     * @see com.l7tech.common.security.wsfederation.InvalidTokenException
     * @see com.l7tech.common.security.wsfederation.ResponseStatusException
     */
    public static SecurityToken obtainFederationToken(GenericHttpClient httpClient
                                                     ,GenericHttpRequestParams httpParams
                                                     ,String realm
                                                     ,boolean addTimestamp) throws IOException {

        if(httpClient==null) throw new IllegalArgumentException("httpClient must not be null");
        if(httpParams==null) throw new IllegalArgumentException("httpParams must not be null");
        if(httpParams.getTargetUrl()==null) throw new IllegalArgumentException("httpParams targetUrl must not be null");
        if(httpParams.getTargetUrl().getQuery()!=null) throw new IllegalArgumentException("url must not have a query string");

        Document result = null;
        URL ipStsUrl = httpParams.getTargetUrl();

        httpParams.setTargetUrl(buildUrl(ipStsUrl.toExternalForm(), realm, addTimestamp));
        if(httpParams.getPasswordAuthentication()!=null) {
            httpParams.setPreemptiveAuthentication(true);
        }

        try {
            // Get RSTR
            SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
            SimpleHttpClient.SimpleHttpResponse response = simpleClient.get(httpParams);
            int status = response.getStatus();
            if (status != 200) {
                throw new ResponseStatusException("Failure status code from server: " + status);
            }

            ContentTypeHeader contentType = response.getContentType();
            if(contentType==null) throw new UnsupportedEncodingException("No content type header in response");
            String type = contentType.getType();
            String subType = contentType.getSubtype();
            if(!"text".equals(type) || !"html".equals(subType)) throw new InvalidHtmlException("Response is not text/html content.");

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
     * TODO [steve] TEST THIS METHOD AGAINST ADFS SERVER
     *
     * @param httpClient the client to use
     * @param httpParams the params (with targetUrl and SSL context set if needed)
     * @param requestorToken the token from the requestors IP/STS
     * @param addTimestamp true to add a timestamp to the HTTP request
     * @return the SecurityToken
     * @throws IOException if an error occurs
     * @see com.l7tech.common.security.wsfederation.InvalidHtmlException
     * @see com.l7tech.common.security.wsfederation.InvalidTokenException
     * @see com.l7tech.common.security.wsfederation.ResponseStatusException
     * /
    public static SecurityToken exchangeFederationToken(GenericHttpClient httpClient
                                                       ,GenericHttpRequestParams httpParams
                                                       ,SecurityToken requestorToken
                                                       ,boolean addTimestamp) throws IOException {

        if(httpClient==null) throw new IllegalArgumentException("httpClient must not be null");
        if(httpParams==null) throw new IllegalArgumentException("httpParams must not be null");
        if(httpParams.getTargetUrl()==null) throw new IllegalArgumentException("httpParams targetUrl must not be null");
        if(httpParams.getTargetUrl().getQuery()!=null) throw new IllegalArgumentException("url must not have a query string");

        Document result = null;
        URL ipStsUrl = httpParams.getTargetUrl();

        String requestBody = buildRequestBody(requestorToken, addTimestamp);

        try {
            // Get RSTR
            byte[] bodyData = requestBody.getBytes("UTF-8");
            httpParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
            httpParams.setExtraHeaders(new GenericHttpHeader[]{new GenericHttpHeader("Content-Length", Integer.toString(bodyData.length))});
            SimpleHttpClient simpleClient = new SimpleHttpClient(httpClient);
            SimpleHttpClient.SimpleHttpResponse response = simpleClient.post(httpParams, bodyData);

            int status = response.getStatus();
            if (status != 200) {
                throw new ResponseStatusException("Failure status code from server: " + status);
            }

            ContentTypeHeader contentType = response.getContentType();
            if(contentType==null) throw new UnsupportedEncodingException("No content type header in response");
            String type = contentType.getType();
            String subType = contentType.getSubtype();
            if(!"text".equals(type) || !"html".equals(subType)) throw new InvalidHtmlException("Response is not text/html content.");

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

    //- PRIVATE

    /**
     * Query String formats
     */
    private static final String QUERY_FORMAT_NO_STAMP = "?wa=wsignin1.0&wtrealm={0}";
    private static final String QUERY_FORMAT_STAMPED = QUERY_FORMAT_NO_STAMP + "&wct={1}";

    /**
     * RSTR template strings
     */
    private static final String RSTR_TEMPLATE = "<wst:RequestSecurityTokenResponse xmlns:wst=\"http://schemas.xmlsoap.org/ws/2005/02/trust\"><wst:RequestedSecurityToken></wst:RequestedSecurityToken><wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\"><wsa:EndpointReference xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"><wsa:Address></wsa:Address></wsa:EndpointReference></wsp:AppliesTo></wst:RequestSecurityTokenResponse>";

    /**
     * Currently only SamlAssertion is supported.
     */
    private static SamlAssertion parseFederationToken(Document token) throws IOException {
        SamlAssertion result = null;
        try {
            Document rstrDoc = token;
            Document soapWrapped;

            // Could be RSTR by itself or could already be in a SOAP Envelope
            if(!SoapUtil.ENVELOPE_EL_NAME.equals(rstrDoc.getDocumentElement().getLocalName())) {
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
            Object rstrObj = TokenServiceClient.parseUnsignedRequestSecurityTokenResponse(soapWrapped);
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
        Document document = null;

        DOMParser htmlparser = new DOMParser();

        try {
            htmlparser.parse(new InputSource(new StringReader(html)));

            Element docEl = htmlparser.getDocument().getDocumentElement();
            NodeList forms = docEl.getElementsByTagName("FORM"); // parser always uppercases elements

            // DO NOT get the inputs from below the form due to a bug in ADFS (form element is empty)
            String xmlText = null;
            NodeList inputs = docEl.getElementsByTagName("INPUT"); // parser always uppercases elements
            for(int i=0; i<inputs.getLength(); i++) {
                String name = ((Element)inputs.item(i)).getAttribute("name");
                String value = ((Element)inputs.item(i)).getAttribute("value"); // decodes for you

                if("wresult".equals(name)) {
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
    private static URL buildUrl(String urlBase, String realm, boolean addTimestamp) {
        try {
            Object[] formatArgs = new Object[]{URLEncoder.encode(realm, "UTF-8")
                                              ,URLEncoder.encode(getTimestamp(), "UTF-8")};

            String format = addTimestamp ? QUERY_FORMAT_STAMPED : QUERY_FORMAT_NO_STAMP;

            return new URL(urlBase + MessageFormat.format(format, formatArgs));
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
    private static String buildRequestBody(SecurityToken token, boolean addTimestamp) throws IOException {
        StringBuffer bodyBuffer = new StringBuffer(512);

        bodyBuffer.append("wa=wsignin1.0");

        if(addTimestamp) {
            bodyBuffer.append("&wct=");
            bodyBuffer.append(URLEncoder.encode(getTimestamp(),"UTF-8"));
        }

        bodyBuffer.append("&wresult=");

        Element samlTokenElement = token.asElement();
        try {
            // Get audience and add to RSTR template
            String audText = XmlUtil.getTextValue(getOneElementByTagNameNS(samlTokenElement, SamlConstants.NS_SAML, SamlConstants.ELEMENT_AUDIENCE));
            Document rstrDoc = XmlUtil.stringToDocument(RSTR_TEMPLATE);
            Element rstrDocEle = rstrDoc.getDocumentElement();
            Element address = getOneElementByTagNameNS(rstrDocEle, "http://schemas.xmlsoap.org/ws/2004/08/addressing", "Address");
            address.appendChild(XmlUtil.createTextNode(rstrDoc, audText));

            // Add token to RSTR
            Element tokenHolder = getOneElementByTagNameNS(rstrDocEle, SoapUtil.WST_NAMESPACE_ARRAY, "RequestedSecurityToken");
            tokenHolder.appendChild(rstrDoc.importNode(samlTokenElement, true));

            // Encode and add to POST body
            bodyBuffer.append(URLEncoder.encode(XmlUtil.nodeToString(rstrDoc),"UTF-8"));
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

        for(int n=0; n<namespaces.length; n++) {
            String namespace = namespaces[n];
            NodeList matchingElements = element.getElementsByTagNameNS(namespace, elementName);
            if(matchingElements.getLength()>1) {
                throw new CausedIOException("Error, multiple " +elementName+ " elements.");
            }
            else if(matchingElements.getLength()==1) {
                if(result!=null) throw new CausedIOException("Error, multiple " +elementName+ " elements.");
                result = (Element) matchingElements.item(0);
            }
        }

        if(result==null) {
            throw new CausedIOException("Missing " +elementName+ " element.");
        }

        return result;
    }
}
