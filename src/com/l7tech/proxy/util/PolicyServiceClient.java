/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.URLConnection;
import java.net.PasswordAuthentication;
import java.net.HttpURLConnection;

/**
 * Builds request messages for the PolicyService and helps parse the responses.
 */
public class PolicyServiceClient {
    public static final Logger log = Logger.getLogger(PolicyServiceClient.class.getName());


    public static Document createSignedGetPolicyRequest(String serviceId,                                                  
                                                  X509Certificate clientCert,
                                                  PrivateKey clientKey)
            throws GeneralSecurityException
    {
        return createSignedGetPolicyRequest(serviceId, null, clientCert, clientKey);
    }

    public static Document createSignedGetPolicyRequest(String serviceId,                                                  
                                                  SamlHolderOfKeyAssertion samlAss,
                                                  PrivateKey clientKey)
            throws GeneralSecurityException
    {
        return createSignedGetPolicyRequest(serviceId, samlAss, null, clientKey);
    }
    
    private static Document createSignedGetPolicyRequest(String serviceId,
                                                         SamlHolderOfKeyAssertion samlAss,
                                                         X509Certificate clientCert,
                                                         PrivateKey clientKey)
            throws GeneralSecurityException
    {
        Document msg = createGetPolicyRequest(serviceId);
        WssDecorator decorator = new WssDecoratorImpl();
        WssDecorator.DecorationRequirements req = new WssDecorator.DecorationRequirements();
        if (samlAss != null)
            req.setSenderSamlToken(samlAss.asElement());
        req.setSenderCertificate(clientCert);
        req.setSenderPrivateKey(clientKey);
        req.setSignTimestamp(true);
        try {
            Element header = SoapUtil.getHeaderElement(msg);
            if (header == null) throw new IllegalStateException("missing header"); // can't happen
            Element body = SoapUtil.getBodyElement(msg);
            if (body == null) throw new IllegalStateException("missing body"); // can't happen
            Element sid = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                SoapUtil.L7_SERVICEID_ELEMENT);
            if (sid == null) throw new IllegalStateException("missing sid"); // can't happen
            Element mid = SoapUtil.getL7aMessageIdElement(msg); // correlation ID
            if (mid == null) throw new IllegalStateException("missing mid"); // can't happen
            req.getElementsToSign().add(sid);
            req.getElementsToSign().add(body);
            req.getElementsToSign().add(mid);
            decorator.decorateMessage(msg, req);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (WssDecorator.DecoratorException e) {
            throw new RuntimeException(e); // shouldn't happen
        }
        return msg;
    }

    public static Document createGetPolicyRequest(String serviceId) {
        Document msg;
        try {
            msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                           "<soap:Header>" +
                                           "<L7a:" + SoapUtil.L7_SERVICEID_ELEMENT + " " +
                                           "xmlns:L7a=\"" + SoapUtil.L7_MESSAGEID_NAMESPACE + "\"/>" +
                                           "</soap:Header>" +
                                           "<soap:Body>" +
                                           "<wsx:GetPolicy xmlns:wsx=\"" + SoapUtil.WSX_NAMESPACE + "\"/>" +
                                           "</soap:Body></soap:Envelope>");
            Element header = SoapUtil.getHeaderElement(msg);
            Element sid = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                SoapUtil.L7_SERVICEID_ELEMENT);
            sid.appendChild(XmlUtil.createTextNode(msg, serviceId));
            SoapUtil.setL7aMessageId(msg, SoapUtil.generateUniqeUri()); // correlation ID
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        }
        return msg;
    }

    /**
     * Examine a response from the policy service and extract the policy.  This variant ensures that the
     * response was properly signed by the specified server certificate.
     *
     * @param originalRequest the original request as sent to the policy service.
     *                        Used to verify that the correlation ID matches up in the reply (preventing replay attacks).
     * @param response the reponse from the policy service, which must by a SOAP message signed by serverCertificate.
     * @param serverCertificate the serverCertificate.  Must match the certificate that signed the response.
     * @param clientCert optional. if specified along with clientKey, encrypted responses can be processed.
     * @param clientKey optional. if specified along with clientCert, encrypted responses can be processed.
     * @return the Policy retrieved from the policy service
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     */
    public static Policy parseGetPolicyResponse(Document originalRequest, Document response, X509Certificate serverCertificate,
                                                X509Certificate clientCert, PrivateKey clientKey)
            throws InvalidDocumentFormatException, GeneralSecurityException, ProcessorException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        WssProcessor wssProcessor = new WssProcessorImpl();
        WssProcessor.ProcessorResult result;
        try {
            result = wssProcessor.undecorateMessage(response, clientCert, clientKey, null);
        } catch (WssProcessor.BadContextException e) {
            throw new ProcessorException(e); // can't happen
        }

        WssProcessor.SecurityToken[] tokens = result.getSecurityTokens();
        X509Certificate signingCert = null;
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken token = tokens[i];
            if (token instanceof WssProcessor.X509SecurityToken) {
                WssProcessor.X509SecurityToken x509Token = (WssProcessor.X509SecurityToken)token;
                if (x509Token.isPossessionProved()) {
                    if (signingCert != null)
                        throw new InvalidDocumentFormatException("Policy server response contained multiple proved X509 security tokens.");
                    signingCert = x509Token.asX509Certificate();
                    if (!Arrays.equals(signingCert.getEncoded(),
                                      serverCertificate.getEncoded()))
                        throw new ServerCertificateUntrustedException("Policy server response was signed, but not by the server certificate we expected.");
                }
            }
        }

        return parseGetPolicyResponse(originalRequest, result.getUndecoratedMessage(), result.getElementsThatWereSigned());
    }

    /**
     * Examine a response from the policy service and extract the policy.  This variant does no checking
     * of signatures in the response.
     *
     * @param originalRequest the original request as sent to the policy service.
     *                        Used to verify that the correlation ID matches up in the reply (preventing replay attacks).
     * @param response the reponse from the policy service, which may or may not have been signed.
     * @return the Policy retrieved from the policy service
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     * @deprecated This method might not be needed.  If it isn't, we'll remove it completely.
     */
    private static Policy parseGetPolicyResponse(Document originalRequest, Document response)
            throws InvalidDocumentFormatException, BadCredentialsException
    {
        return parseGetPolicyResponse(originalRequest, response, (WssProcessor.ParsedElement[])null);
    }

    private static Policy parseGetPolicyResponse(Document originalRequest, Document response, WssProcessor.ParsedElement[] elementsThatWereSigned)
            throws InvalidDocumentFormatException, BadCredentialsException
    {
        {
            // check for fault message from server
            Element payload = SoapUtil.getPayloadElement(response);
            if (payload == null) throw new MissingRequiredElementException("Policy server response is missing SOAP Body or payload element");
            if (response.getDocumentElement().getNamespaceURI().equals(payload.getNamespaceURI()) && "Fault".equals(payload.getLocalName()))
                translateSoapFault(payload);
        }

        // check correlation ID in the response
        String requestMessageId = SoapUtil.getL7aMessageId(originalRequest);
        if (requestMessageId == null || requestMessageId.length() < 1) {
            log.warning("Original policy request contained no MessageId -- skipping RelatesTo check"); // can't happen
        } else {
            Element relatesTo = SoapUtil.getL7aRelatesToElement(response);
            if (elementsThatWereSigned != null)
                if (!ProcessorResultUtil.nodeIsPresent(relatesTo, elementsThatWereSigned))
                    throw new InvalidDocumentFormatException("Policy server response did not sign the L7a:RelatesTo");
            String idstr = XmlUtil.getTextValue(relatesTo);
            if (!requestMessageId.equals(idstr))
                throw new InvalidDocumentFormatException("Policy server response did not include an L7a:RelatesTo matching our requests L7a:MessageID.");
        }

        // PGet metadata from header
        Element header = SoapUtil.getHeaderElement(response);
        if (header == null) throw new MissingRequiredElementException("Policy server response is missing SOAP Header element");
        Element policyVersion = XmlUtil.findOnlyOneChildElementByName(header,
                                                                      SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                      SoapUtil.L7_POLICYVERSION_ELEMENT);
        if (policyVersion == null) throw new MissingRequiredElementException("Policy server response is missing soap:Header/L7a:PolicyVersion element");
        if (elementsThatWereSigned != null)
            if (!ProcessorResultUtil.nodeIsPresent(policyVersion, elementsThatWereSigned))
                throw new InvalidDocumentFormatException("Policy server response did not sign the PolicyVersion");
        String version = XmlUtil.getTextValue(policyVersion);

        Element body = SoapUtil.getBodyElement(response);
        if (elementsThatWereSigned != null)
            if (!ProcessorResultUtil.nodeIsPresent(body, elementsThatWereSigned))
                throw new InvalidDocumentFormatException("Policy server response did not sign the body");

        if (body == null) throw new MessageNotSoapException("Policy server response is missing body");
        Element payload = XmlUtil.findOnlyOneChildElementByName(body, SoapUtil.WSX_NAMESPACE, "GetPolicyResponse");
        if (payload == null) throw new MissingRequiredElementException("Policy server response is missing wsx:GetPolicyResponse");
        Element policy = XmlUtil.findOnlyOneChildElementByName(payload,
                                                               new String[] {
                                                                    WspConstants.L7_POLICY_NS,
                                                                    WspConstants.WSP_POLICY_NS
                                                               },
                                                               "Policy");
        if (policy == null) throw new MissingRequiredElementException("Policy server response is missing Policy element");
        Assertion assertion = null;
        try {
            assertion = WspReader.parse(policy);
        } catch (InvalidPolicyStreamException e) {
            throw new InvalidDocumentFormatException("Policy server response contained a Policy that could not be parsed", e);
        }
        return new Policy(assertion, version);
    }

    private static void translateSoapFault(Element fault) throws InvalidDocumentFormatException, BadCredentialsException {
        final String s = "unauthorized policy download";
        final String faultXml;
        try {
            faultXml = XmlUtil.nodeToString(fault);
            if (faultXml.indexOf(s) >= 0)
                throw new BadCredentialsException(s);
            log.severe("Unexpected SOAP fault from policy service: " + faultXml);
            throw new InvalidDocumentFormatException("Unexpected SOAP fault from policy service");
        } catch (IOException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    /**
     * Connect to the specified URL and download policy using the specified already-decorated GetPolicy request.
     * The URL is assumed to belong to the specified SSG.
     *
     * @param url
     * @param ssg
     * @param requestDoc
     * @param httpBasicAuthorization optional.  If specified, the Authorization: HTTP header will be set to whatever is in this string.
     * @param serverCertificate  required.  used to verify identity of signer of downloaded policy.
     * @param clientCert         optional. if specified along with clientKey, an encrypted response can be processed.
     * @param clientKey          optional. if specified along with clientCert, an encrypted response can be processed.
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws BadCredentialsException
     */
    private static Policy obtainResponse(URL url,
                                         Ssg ssg,
                                         Document requestDoc,
                                         String httpBasicAuthorization,
                                         X509Certificate serverCertificate,
                                         X509Certificate clientCert,
                                         PrivateKey clientKey)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException
    {
        log.log(Level.INFO, "Downloading policy from " + url.toString());

        CurrentRequest.setPeerSsg(ssg);
        URLConnection conn = url.openConnection();
        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Connection to policy server was not an HttpURLConnection; instead it was " + conn.getClass());
        HttpURLConnection httpConn = (HttpURLConnection)conn;
        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection sslConn = (HttpsURLConnection)conn;
            sslConn.setSSLSocketFactory(ClientProxySecureProtocolSocketFactory.getInstance());
        }
        if (httpBasicAuthorization != null)
            conn.setRequestProperty("Authorization", httpBasicAuthorization); 
        conn.setDoOutput(true);
        conn.setAllowUserInteraction(false);
        conn.setRequestProperty(XmlUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
        XmlUtil.nodeToOutputStream(requestDoc, conn.getOutputStream());
        final int code = httpConn.getResponseCode();
        log.log(Level.FINE, "Policy server responded with: " + code + " " + httpConn.getResponseMessage());
        int len = conn.getContentLength();
        log.log(Level.FINEST, "Policy server response content length=" + len);
        CurrentRequest.setPeerSsg(null);
        String contentType = conn.getContentType();
        if (contentType == null || contentType.indexOf(XmlUtil.TEXT_XML) < 0)
            throw new IOException("Policy server returned unsupported content type " + conn.getContentType());
        Document response = null;
        InputStream inputStream;
        try {
            inputStream = conn.getInputStream();
        } catch (IOException e) {
            inputStream = httpConn.getErrorStream();
            if (inputStream == null)
                throw e;
        }
        try {
            response = XmlUtil.parse(inputStream);
        } catch (SAXException e) {
            throw new CausedIOException("Unable to XML parse GetPolicyResponse", e);
        }
        Policy result = null;
        try {
            result = parseGetPolicyResponse(requestDoc,
                                            response,
                                            serverCertificate,
                                            clientCert,
                                            clientKey);
        } catch (ProcessorException e) {
            throw new CausedIOException("Unable to obtain policy from policy server", e);
        }
        return result;
    }

    /**
     * Connect to the specified SSG over HTTP and download policy using a GetPolicy request, authenticating
     * with a WSS Signature using the specified client certificate, and verifying that the reponse signature
     * was valid and made by the specified serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentRequest.getPeerSsg() up-to-date.
     * @param serviceId          required. the identifier of the service whose policy we wish to download.  Opaque to the client.
     * @param serverCertificate  required. used to verify identity of signer of downloaded policy.
     * @param useSsl             If true, will use HTTPS instead of HTTP.  If we have a client cert for this Ssg it
     *                           will be presented to the Gateway if we are challenged for it during the handshake.
     * @param clientCert         required. used to sign the request (and to decyrpt any encrypted portion in a response)
     * @param clientKey          required. used to sign the request (and to decyrpt any encrypted portion in a response)
     * @return a new Policy.  Never null.
     * @throws IOException if there is a network problem
     * @throws GeneralSecurityException if there is a problem with a certificate or a crypto operation.
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     */
    public static Policy downloadPolicyWithWssSignature(Ssg ssg,
                                                        String serviceId,
                                                        X509Certificate serverCertificate,
                                                        boolean useSsl,
                                                        X509Certificate clientCert,
                                                        PrivateKey clientKey)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException
    {
        URL url = new URL(useSsl ? "https" : "http",
                          ssg.getSsgAddress(),
                          useSsl ? ssg.getSslPort() : ssg.getSsgPort(),
                          SecureSpanConstants.POLICY_SERVICE_FILE);
        Document requestDoc = createSignedGetPolicyRequest(serviceId, clientCert, clientKey);
        return obtainResponse(url, ssg, requestDoc, null, serverCertificate, clientCert, clientKey);
    }

    /**
     * Connect to the specified SSG over HTTPS and download policy using a GetPolicy request, authenticating
     * with HTTP Basic-over-SSL, and verifying that the response signature was valid and made by the specified
     * serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentRequest.getPeerSsg() up-to-date.
     * @param serviceId          required. the identifier of the service whose policy we wish to download.  Opaque to the client.
     * @param serverCertificate  required. used to verify identity of signer of downloaded policy.
     * @param basicCredentials   required. the credentials to use for HTTP Basic-over-SSL authentication.
     * @return a new Policy.  Never null.
     * @throws IOException if there is a network problem
     * @throws GeneralSecurityException if there is a problem with a certificate or a crypto operation.
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     */
    public static Policy downloadPolicyWithHttpBasicOverSsl(Ssg ssg,
                                                            String serviceId,
                                                            X509Certificate serverCertificate,
                                                            PasswordAuthentication basicCredentials)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException
    {
        URL url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), SecureSpanConstants.POLICY_SERVICE_FILE);
        Document requestDoc = createGetPolicyRequest(serviceId);
        String auth = "Basic " + HexUtils.encodeBase64(
                (basicCredentials.getUserName() + ":" + new String(basicCredentials.getPassword())).getBytes());        
        return obtainResponse(url, ssg, requestDoc, auth, serverCertificate, null, null);
    }
    
    /**
     * Connect to the specified SSG over HTTP and download an anonymous policy using a GetPolicy request,
     * authenticating using a signature using the specified SAML Holder-of-key asseriton + subject private key,
     * and verifying that the response signature was valid and made by the specified serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentRequest.getPeerSsg() up-to-date.
     * @param serviceId          required. the identifier of the service whose policy we wish to download.  Opaque to the client.
     * @param serverCertificate  required. used to verify identity of signer of downloaded policy.
     * @param useSsl             If true, will use HTTPS instead of HTTP.  If we have a client cert for this Ssg it
     *                           will be presented to the Gateway if we are challenged for it during the handshake.
     * @param samlAss            required. a Saml holder-of-key assertion containing your client cert as the subject.
     *                           The whole assertion must already be signed by an issuer trusted by this policy service.
     * @param subjectPrivateKey  required. The private key corresponding to the subject certificate in samlAss.
     * @return a new Policy.  Never null.
     * @throws IOException if there is a network problem
     * @throws GeneralSecurityException if there is a problem with a certificate or a crypto operation.
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your (lack of) credentials
     */
    public static Policy downloadPolicyWithSamlAssertion(Ssg ssg,
                                                         String serviceId,
                                                         X509Certificate serverCertificate,
                                                         boolean useSsl,
                                                         SamlHolderOfKeyAssertion samlAss,
                                                         PrivateKey subjectPrivateKey)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException
    {
        URL url = new URL(useSsl ? "https" : "http",
                          ssg.getSsgAddress(),
                          useSsl ? ssg.getSslPort() : ssg.getSsgPort(),
                          SecureSpanConstants.POLICY_SERVICE_FILE);
        Document requestDoc = createSignedGetPolicyRequest(serviceId, samlAss, subjectPrivateKey);
        return obtainResponse(url, ssg, requestDoc, null, serverCertificate, samlAss.getSubjectCertificate(), subjectPrivateKey);
    }

    /**
     * Connect to the specified SSG over HTTP and download an anonymous policy using a GetPolicy request with
     * no client side authentication (other than client cert if we have one available and useSsl is true),
     * but still verifying that the response signature was valid and made by the specified serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentRequest.getPeerSsg() up-to-date.
     * @param serviceId          required. the identifier of the service whose policy we wish to download.  Opaque to the client.
     * @param serverCertificate  required. used to verify identity of signer of downloaded policy.
     * @param useSsl             If true, will use HTTPS instead of HTTP.  If we have a client cert for this Ssg it
     *                           will be presented to the Gateway if we are challenged for it during the handshake.
     * @return a new Policy.  Never null.
     * @throws IOException if there is a network problem
     * @throws GeneralSecurityException if there is a problem with a certificate or a crypto operation.
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your (lack of) credentials
     */
    public static Policy downloadPolicyWithNoAuthentication(Ssg ssg,
                                                            String serviceId,
                                                            X509Certificate serverCertificate,
                                                            boolean useSsl)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException
    {
        URL url = new URL(useSsl ? "https" : "http",
                          ssg.getSsgAddress(),
                          useSsl ? ssg.getSslPort() : ssg.getSsgPort(),
                          SecureSpanConstants.POLICY_SERVICE_FILE);
        Document requestDoc = createGetPolicyRequest(serviceId);
        return obtainResponse(url, ssg, requestDoc, null, serverCertificate, null, null);        
    }
}
