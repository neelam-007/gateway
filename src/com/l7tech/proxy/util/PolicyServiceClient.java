/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Logger;

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
        Document msg = createGetPolicyRequest(serviceId);
        WssDecorator decorator = new WssDecoratorImpl();
        WssDecorator.DecorationRequirements req = new WssDecorator.DecorationRequirements();
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
            sid.appendChild(msg.createTextNode(serviceId));
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
     * @return the Policy retrieved from the policy service
     * @throws InvalidDocumentFormatException if the policy service response was not formatted correctly
     * @throws BadCredentialsException if the policy service denies access to this policy to your credentials
     */
    public static Policy parseGetPolicyResponse(Document originalRequest, Document response, X509Certificate serverCertificate)
            throws InvalidDocumentFormatException, GeneralSecurityException, ProcessorException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        WssProcessor wssProcessor = new WssProcessorImpl();
        WssProcessor.ProcessorResult result;
        try {
            result = wssProcessor.undecorateMessage(response, null, null, null);
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
}
