/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.util;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.*;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.util.CausedIOException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.BuildInfo;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateRevokedException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds request messages for the PolicyService and helps parse the responses.
 */
public class PolicyServiceClient {
    public static final Logger log = Logger.getLogger(PolicyServiceClient.class.getName());

    public static Document createDecoratedGetPolicyRequest(String serviceId,
                                                         SamlAssertion samlAss,
                                                         X509Certificate clientCert,
                                                         PrivateKey clientKey,
                                                         Date timestampCreatedDate)
            throws GeneralSecurityException
    {
        Document msg = createGetPolicyRequest(serviceId);
        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements req = new DecorationRequirements();
        boolean canSign = clientKey != null && (samlAss != null || clientCert != null);
        if (samlAss != null) {
            if (samlAss.isBearerToken())
                canSign = false;
            // if we make a signature, include the saml token in it
            req.setSenderSamlToken(samlAss, canSign && !samlAss.isHolderOfKey());
        }
        if (canSign) {
            req.setSenderMessageSigningCertificate(clientCert);
            req.setSenderMessageSigningPrivateKey(clientKey);
            req.setSignTimestamp();
        }
        try {
            if (canSign) {
                Element header = SoapUtil.getHeaderElement(msg);
                Element body = SoapUtil.getBodyElement(msg);
                Element sid = DomUtils.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                    SoapUtil.L7_SERVICEID_ELEMENT);
                Element mid = SoapUtil.getL7aMessageIdElement(msg); // correlation ID
                req.getElementsToSign().add(sid);
                req.getElementsToSign().add(body);
                req.getElementsToSign().add(mid);
            }
            req.setTimestampCreatedDate(timestampCreatedDate);
            decorator.decorateMessage(new Message(msg), req);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (DecoratorException e) {
            throw new RuntimeException(e); // shouldn't happen
        } catch (SAXException se) {
            throw new RuntimeException(se); // shouldn't happen
        } catch (IOException ioe) {
            throw new RuntimeException(ioe); // shouldn't happen
        }
        return msg;
    }

    public static Document createDecoratedGetPolicyRequest(String serviceId, KerberosServiceTicket kerberosTicket)
            throws GeneralSecurityException
    {
        Document msg = createGetPolicyRequest(serviceId);
        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements req = new DecorationRequirements();
        try {
            req.setIncludeKerberosTicket(true);
            req.setKerberosTicket(kerberosTicket);
            decorator.decorateMessage(new Message(msg), req);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // can't happen
        } catch (DecoratorException e) {
            throw new RuntimeException(e); // shouldn't happen
        } catch (SAXException se) {
            throw new RuntimeException(se); // shouldn't happen
        } catch (IOException ioe) {
            throw new RuntimeException(ioe); // shouldn't happen
        }
        return msg;
    }

    public static Document createGetPolicyRequest(String serviceId) {
        Document msg;
        try {
            msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                                           "<soap:Header xmlns:L7a=\"" + SoapUtil.L7_MESSAGEID_NAMESPACE + "\">" +
                                           "<L7a:" + SoapUtil.L7_CLIENTVERSION_ELEMENT + "/>" +
                                           "<L7a:" + SoapUtil.L7_SERVICEID_ELEMENT + "/>" +
                                           "</soap:Header>" +
                                           "<soap:Body>" +
                                           "<wsx:GetPolicy xmlns:wsx=\"" + SoapUtil.WSX_NAMESPACE + "\"/>" +
                                           "</soap:Body></soap:Envelope>");
            Element header = SoapUtil.getHeaderElement(msg);
            Element cver = DomUtils.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                SoapUtil.L7_CLIENTVERSION_ELEMENT);
            cver.appendChild(DomUtils.createTextNode(msg, BuildInfo.getFormalProductVersion()));
            Element sid = DomUtils.findOnlyOneChildElementByName(header, SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                SoapUtil.L7_SERVICEID_ELEMENT);
            sid.appendChild(DomUtils.createTextNode(msg, serviceId));
            SoapUtil.setL7aMessageId(msg, SoapUtil.generateUniqueUri(null, false)); // correlation ID
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
     * @param clientKey optional. if specified along with getCachedClientCert, encrypted responses can be processed.
     * @param signedResponseRequired if true, an InvalidDocumentFormatException will be thrown if the response
     *                               does not include a signed body, timestamp, and L7a:relatesTo.
     * @param outTimestampCreated if an array of length >= 1 is passed, its first element will be set to the signed
     *                           Created date from the response, if any.
     * @param outTimestampCreatedWasSigned if an array of length >= 1 is passed, its first element will be set to
     *                                     a boolean representing true iff. the the timestamp returned in outTimestampCreated
     *                                     was signed.  Not meaningful unless outTimestampCreate was passed.
     * @return the Policy retrieved from the policy service
     * @throws ServerCertificateUntrustedException if the server certificate is not trusted
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     */
    public static Policy parseGetPolicyResponse(Document originalRequest,
                                                Document response,
                                                X509Certificate serverCertificate,
                                                X509Certificate clientCert,
                                                PrivateKey clientKey,
                                                boolean signedResponseRequired,
                                                Date[] outTimestampCreated,
                                                boolean[] outTimestampCreatedWasSigned)
            throws InvalidDocumentFormatException, GeneralSecurityException, ProcessorException, BadCredentialsException
    {
        {
            // check for fault message from server
            Element payload = SoapUtil.getPayloadElement(response);
            if (payload == null) throw new MissingRequiredElementException("Policy server response is missing SOAP Body or payload element");
            if (response.getDocumentElement().getNamespaceURI().equals(payload.getNamespaceURI()) && "Fault".equals(payload.getLocalName()))
                translateSoapFault(payload);
        }


        WssProcessor wssProcessor = new WssProcessorImpl();
        ProcessorResult result;
        try {
            result = wssProcessor.undecorateMessage(new Message(response), null, null, new SimpleSecurityTokenResolver(clientCert, clientKey));
        } catch (BadSecurityContextException e) {
            throw new ProcessorException(e); // can't happen
        } catch (IOException e) {
            throw new ProcessorException(e); // can't happen
        } catch (SAXException e) {
            throw new ProcessorException(e); // can't happen
        }

        XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
        X509Certificate signingCert = null;
        for (XmlSecurityToken token : tokens) {
            if (token instanceof X509BinarySecurityTokenImpl) {
                X509SigningSecurityToken x509Token = (X509SigningSecurityToken) token;
                if (x509Token.isPossessionProved()) {
                    if (signingCert != null)
                        throw new InvalidDocumentFormatException("Policy server response contained multiple proved X509 security tokens.");
                    signingCert = x509Token.getMessageSigningCertificate();
                    if (!CertUtils.certsAreEqual(signingCert, serverCertificate)) {
                        throw new ServerCertificateUntrustedException("Policy server response was signed, but not by the server certificate we expected.");
                    }
                }
            }
        }

        SignedElement[] signedElements = result.getElementsThatWereSigned();
        WssTimestamp timestamp = result.getTimestamp();
        final boolean timestampSigned = (timestamp != null && timestamp.asElement() != null &&
            ProcessorResultUtil.nodeIsPresent(timestamp.asElement(), signedElements));
        if (signedResponseRequired && !timestampSigned)
                throw new InvalidDocumentFormatException("Policy server response did not include a signed timestamp, but our request required a signed response.");
        if (outTimestampCreated != null && outTimestampCreated.length > 0 && timestamp != null && timestamp.getCreated() != null) {
            outTimestampCreated[0] = new Date(timestamp.getCreated().asTime());
            if (outTimestampCreatedWasSigned != null && outTimestampCreatedWasSigned.length > 0)
                outTimestampCreatedWasSigned[0] = timestampSigned;
        }        
        if (signedElements == null || signedElements.length < 1 || signingCert == null) {
            if (signedResponseRequired)
                throw new InvalidDocumentFormatException("Policy server response was not signed, but our request required a signed response.");
            return parseGetPolicyResponse(originalRequest, response, null);
        }
        return parseGetPolicyResponse(originalRequest, response, result.getElementsThatWereSigned());
    }

    private static Policy parseGetPolicyResponse(Document originalRequest, Document response, ParsedElement[] elementsThatWereSigned)
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
            String idstr = DomUtils.getTextValue(relatesTo);
            if (!requestMessageId.equals(idstr))
                throw new InvalidDocumentFormatException("Policy server response did not include an L7a:RelatesTo matching our requests L7a:MessageID.");
        }

        // PGet metadata from header
        Element header = SoapUtil.getHeaderElement(response);
        if (header == null) throw new MissingRequiredElementException("Policy server response is missing SOAP Header element");
        Element policyVersion = DomUtils.findOnlyOneChildElementByName(header,
                                                                      SoapUtil.L7_MESSAGEID_NAMESPACE,
                                                                      SoapUtil.L7_POLICYVERSION_ELEMENT);
        if (policyVersion == null) throw new MissingRequiredElementException("Policy server response is missing soap:Header/L7a:PolicyVersion element");
        if (elementsThatWereSigned != null)
            if (!ProcessorResultUtil.nodeIsPresent(policyVersion, elementsThatWereSigned))
                throw new InvalidDocumentFormatException("Policy server response did not sign the PolicyVersion");
        String version = DomUtils.getTextValue(policyVersion);

        Element body = SoapUtil.getBodyElement(response);
        if (elementsThatWereSigned != null)
            if (!ProcessorResultUtil.nodeIsPresent(body, elementsThatWereSigned))
                throw new InvalidDocumentFormatException("Policy server response did not sign the body");

        if (body == null) throw new MessageNotSoapException("Policy server response is missing body");
        Element payload = DomUtils.findOnlyOneChildElementByName(body, SoapUtil.WSX_NAMESPACE, "GetPolicyResponse");
        if (payload == null) throw new MissingRequiredElementException("Policy server response is missing wsx:GetPolicyResponse");
        Element policy = DomUtils.findOnlyOneChildElementByName(payload, WspConstants.POLICY_NAMESPACES, "Policy");
        if (policy == null) throw new MissingRequiredElementException("Policy server response is missing Policy element");
        Assertion assertion;
        try {
            assertion = WspReader.getDefault().parsePermissively(policy, WspReader.OMIT_DISABLED);
        } catch (InvalidPolicyStreamException e) {
            throw new InvalidDocumentFormatException("Policy server response contained a Policy that could not be parsed", e);
        }
        return new Policy(assertion, version);
    }

    private static void translateSoapFault(Element fault) throws InvalidDocumentFormatException, BadCredentialsException {
        // TODO use a proper fault URI for this error
        final String s = "unauthorized policy download";
        final String faultXml;
        try {
            faultXml = XmlUtil.nodeToString(fault);
            if (faultXml.indexOf(s) >= 0) {
                log.info("Auth failed from policy service: " + faultXml);
                throw new BadCredentialsException(s);
            }
            log.severe("Unexpected SOAP fault from policy service: " + faultXml);
            throw new InvalidDocumentFormatException("Unexpected SOAP fault from policy service");
        } catch (IOException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    /**
     * Connect to the specified URL and download policy using the specified already-decorated GetPolicy request.
     * The URL is assumed to belong to the specified SSG.
     * <p>
     * As a side-effect of downloading policy, we'll check for gross clock-skew between this SSG's clock
     * and our own and enable compensation if some is detected.
     *
     * @param httpClient          required.  The GenericHttpClient to use for the request.
     * @param url
     * @param requestDoc
     * @param httpBasicCredentials optional.  If specified, the Authorization: HTTP header will be set to whatever is in this string.
     * @param serverCertificate  required.  used to verify identity of signer of downloaded policy.
     * @param clientCert         optional. if specified along with clientKey, an encrypted response can be processed.
     * @param clientKey          optional. if specified along with getCachedClientCert, an encrypted response can be processed.
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws BadCredentialsException
     */
    private static Policy obtainResponse(GenericHttpClient httpClient,
                                         URL url,
                                         Ssg ssg,
                                         Document requestDoc,
                                         PasswordAuthentication httpBasicCredentials,
                                         X509Certificate serverCertificate,
                                         X509Certificate clientCert,
                                         PrivateKey clientKey)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException, ClientCertificateException
    {
        log.log(Level.INFO, "Downloading policy from " + url.toString());

        final long millisBefore = System.currentTimeMillis();
        final GenericHttpRequestParams params = new GenericHttpRequestParams(url);
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        if (httpBasicCredentials != null)
            params.setPasswordAuthentication(httpBasicCredentials);

        boolean usingSsl = "https".equalsIgnoreCase(url.getProtocol());
        CurrentSslPeer.set(ssg);

        SimpleHttpClient client = new SimpleHttpClient(httpClient);
        try {
            SimpleHttpClient.SimpleXmlResponse conn = client.postXml(params, requestDoc);

            final int code = conn.getStatus();
            log.log(Level.FINE, "Policy server responded with: " + code);
            final Long len = conn.getContentLength();
            log.log(Level.FINEST, "Policy server response content length=" + len);

            Document response = conn.getDocument();

            final long millisAfter = System.currentTimeMillis();
            final long roundTripMillis = millisAfter - millisBefore;
            log.log(Level.FINER, "Policy download took " + roundTripMillis + "ms");
            Policy result;
            Date ssgTime = null; // Trusted timestamp from the SSG

            String certStatus = conn.getHeaders().getOnlyOneValue(SecureSpanConstants.HttpHeaders.CERT_STATUS);
            if (SecureSpanConstants.CERT_INVALID.equalsIgnoreCase(certStatus)) {
                log.log(Level.INFO, "Policy download failed due to invalid client certificate.");
                throw new ClientCertificateRevokedException("Client certificate invalid.");
            }

            Date[] timestampCreatedDate = new Date[] { null };
            boolean[] timestampWasSigned = new boolean[] { false };
            result = parseGetPolicyResponse(requestDoc,
                                            response,
                                            serverCertificate,
                                            clientCert,
                                            clientKey,
                                            !usingSsl,
                                            timestampCreatedDate,
                                            timestampWasSigned);
            if (timestampCreatedDate[0] != null) {
                if (usingSsl || timestampWasSigned[0])
                    ssgTime = timestampCreatedDate[0];
            }

            if (ssgTime != null && Math.abs(ssgTime.getTime() - millisAfter) > 10000 + roundTripMillis) {
                final long ssgDiff = ssgTime.getTime() - ((millisAfter + millisBefore) / 2);
                final long posDiff = Math.abs(ssgDiff);
                final String aheadBehind = ssgDiff > 0 ? "ahead of" : "behind";
                log.log(Level.INFO, "Noting that Gateway " + ssg + " clock is at about " + posDiff + "ms " + aheadBehind + " local clock.");
                ssg.getRuntime().setTimeOffset(ssgDiff);
            }

            CurrentSslPeer.clear();
            return result;
        } catch (SAXException e) {
            throw new InvalidDocumentFormatException(e);
        } catch (ProcessorException e) {
            throw new CausedIOException("Unable to obtain policy from policy server", e);
        }
    }

    /**
     * Connect to the specified SSG over HTTP and download policy using a GetPolicy request, authenticating
     * with a WSS Signature using the specified client certificate, and verifying that the reponse signature
     * was valid and made by the specified serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentSslPeer.getPeerSsg() up-to-date.
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
    public static Policy downloadPolicyWithWssSignature(GenericHttpClient httpClient,
                                                        Ssg ssg,
                                                        String serviceId,
                                                        X509Certificate serverCertificate,
                                                        boolean useSsl,
                                                        X509Certificate clientCert,
                                                        PrivateKey clientKey)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException, ClientCertificateException
    {
        URL url = new URL(useSsl ? "https" : "http",
                          ssg.getSsgAddress(),
                          useSsl ? ssg.getSslPort() : ssg.getSsgPort(),
                          ssg.getRuntime().getPolicyServiceFile());
        Date timestampCreatedDate = ssg.getRuntime().getDateTranslatorToSsg().translate(new Date());
        Document requestDoc = createDecoratedGetPolicyRequest(serviceId, null, clientCert, clientKey, timestampCreatedDate);
        return obtainResponse(httpClient, url, ssg, requestDoc, null, serverCertificate, clientCert, clientKey);
    }

    public static Policy downloadPolicyWithKerberos(GenericHttpClient httpClient,
                                                    Ssg ssg,
                                                    String serviceId,
                                                    X509Certificate serverCertificate,
                                                    KerberosServiceTicket kerberosTicket)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException, ClientCertificateException
    {
        URL url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), ssg.getRuntime().getPolicyServiceFile());
        Document requestDoc = createDecoratedGetPolicyRequest(serviceId, kerberosTicket);
        return obtainResponse(httpClient, url, ssg, requestDoc, null, serverCertificate, null, null);
    }

    /**
     * Connect to the specified SSG over HTTPS and download policy using a GetPolicy request, authenticating
     * with HTTP Basic-over-SSL, and verifying that the response signature was valid and made by the specified
     * serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentSslPeer.getPeerSsg() up-to-date.
     * @param serviceId          required. the identifier of the service whose policy we wish to download.  Opaque to the client.
     * @param serverCertificate  required. used to verify identity of signer of downloaded policy.
     * @param basicCredentials   required. the credentials to use for HTTP Basic-over-SSL authentication.
     * @return a new Policy.  Never null.
     * @throws IOException if there is a network problem
     * @throws GeneralSecurityException if there is a problem with a certificate or a crypto operation.
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     */
    public static Policy downloadPolicyWithHttpBasicOverSsl(GenericHttpClient httpClient,
                                                            Ssg ssg,
                                                            String serviceId,
                                                            X509Certificate serverCertificate,
                                                            PasswordAuthentication basicCredentials)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException, ClientCertificateException
    {
        URL url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), ssg.getRuntime().getPolicyServiceFile());
        Document requestDoc = createGetPolicyRequest(serviceId);
        return obtainResponse(httpClient, url, ssg, requestDoc, basicCredentials, serverCertificate, null, null);
    }
    
    /**
     * Connect to the specified SSG over HTTP and download an anonymous policy using a GetPolicy request,
     * authenticating using a signature using the specified SAML Holder-of-key asseriton + subject private key,
     * and verifying that the response signature was valid and made by the specified serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentSslPeer.getPeerSsg() up-to-date.
     * @param serviceId          required. the identifier of the service whose policy we wish to download.  Opaque to the client.
     * @param serverCertificate  required. used to verify identity of signer of downloaded policy.
     * @param useSsl             If true, will use HTTPS instead of HTTP.  If we have a client cert for this Ssg it
     *                           will be presented to the Gateway if we are challenged for it during the handshake.
     * @param samlAss            required. a Saml holder-of-key assertion containing your client cert as the subject.
     *                           The whole assertion must already be signed by an issuer trusted by this policy service.
     * @param subjectPrivateKey  The private key corresponding to the subject certificate in samlAss, or null if samlAss
     *                           cannot be used for signing the request.  If null, SSL is recommended.
     * @return a new Policy.  Never null.
     * @throws IOException if there is a network problem
     * @throws GeneralSecurityException if there is a problem with a certificate or a crypto operation.
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your (lack of) credentials
     */
    public static Policy downloadPolicyWithSamlAssertion(GenericHttpClient httpClient,
                                                         Ssg ssg,
                                                         String serviceId,
                                                         X509Certificate serverCertificate,
                                                         boolean useSsl,
                                                         SamlAssertion samlAss,
                                                         PrivateKey subjectPrivateKey)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException, ClientCertificateException
    {
        URL url = new URL(useSsl ? "https" : "http",
                          ssg.getSsgAddress(),
                          useSsl ? ssg.getSslPort() : ssg.getSsgPort(),
                          ssg.getRuntime().getPolicyServiceFile());
        Date timestampCreatedDate = ssg.getRuntime().getDateTranslatorToSsg().translate(new Date());
        Document requestDoc = createDecoratedGetPolicyRequest(serviceId, samlAss, null, subjectPrivateKey, timestampCreatedDate);
        return obtainResponse(httpClient, url, ssg, requestDoc, null, serverCertificate, samlAss.getMessageSigningCertificate(), subjectPrivateKey);
    }

    /**
     * Connect to the specified SSG over HTTP and download an anonymous policy using a GetPolicy request with
     * no client side authentication (other than client cert if we have one available and useSsl is true),
     * but still verifying that the response signature was valid and made by the specified serverCertificate.
     *
     * @param ssg                required. the Ssg from which we are downloading.  Used to keep CurrentSslPeer.getPeerSsg() up-to-date.
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
    public static Policy downloadPolicyWithNoAuthentication(GenericHttpClient httpClient,
                                                            Ssg ssg,
                                                            String serviceId,
                                                            X509Certificate serverCertificate,
                                                            boolean useSsl)
            throws IOException, GeneralSecurityException, BadCredentialsException, InvalidDocumentFormatException, ClientCertificateException
    {
        URL url = new URL(useSsl ? "https" : "http",
                          ssg.getSsgAddress(),
                          useSsl ? ssg.getSslPort() : ssg.getSsgPort(),
                          ssg.getRuntime().getPolicyServiceFile());
        Document requestDoc = createGetPolicyRequest(serviceId);
        return obtainResponse(httpClient, url, ssg, requestDoc, null, serverCertificate, null, null);
    }
}
