/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.wsaddressing.client;

import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.message.Message;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * @author alex
 */
public class ClientWsAddressingAssertion extends ClientAssertion {
    private final WsAddressingAssertion assertion;

    public ClientWsAddressingAssertion(WsAddressingAssertion assertion) {
        this.assertion = assertion;
    }

    public String getName() {
        return "WS-Addressing";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        // No useful SSB behaviour if target != request
        if (assertion.getTarget() != TargetMessageType.REQUEST) return AssertionStatus.NONE;

        final Map<String, String> props = context.getSsg().getProperties();

        // TODO make the MessageID prefix configurable in the assertion instead of just locally?
        final String uuidPrefix = props.get("wsAddressing.uuidPrefix");

        // Figure out which WSA NS to use (Always use "other" if specified)
        final String ns = getWsaNamespaceUri();
        final String id = context.prepareWsaMessageId(true, ns, uuidPrefix);
        Element wsaMessageIdEl = SoapUtil.setWsaMessageId(context.getRequest().getXmlKnob().getDocumentWritable(), ns, id);
        if (assertion.isRequireSignature()) {
            context.getDefaultWssRequirements().getElementsToSign().add(wsaMessageIdEl);
        }
        return AssertionStatus.NONE;
    }

    private String getWsaNamespaceUri() throws PolicyAssertionException {
        final String other = assertion.getEnableOtherNamespace();
        final String ns;
        if (other != null && other.length() > 0) {
            ns = other;
        } else if (assertion.isEnableWsAddressing10()) {
            ns = SoapConstants.WSA_NAMESPACE_10;
        } else if (assertion.isEnableWsAddressing200408()) {
            ns = SoapConstants.WSA_NAMESPACE2;
        } else {
            throw new PolicyAssertionException(assertion, "Unable to select WS-Addressing namespace URI");
        }
        return ns;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException {
        // Find and strip any MessageID headers from the response.
        Message response = context.getResponse();
        if (!response.isSoap())
            return AssertionStatus.NONE; // Success, instead of NOT_APPLICABLE, since there's nothing that needs to be undecorated

        final String ns = getWsaNamespaceUri();

        Document doc = response.getXmlKnob().getDocumentWritable();

        Element messageIdElement = SoapUtil.getWsaMessageIdElement(doc, ns);
        if (messageIdElement != null) messageIdElement.getParentNode().removeChild(messageIdElement);

        Element relatesToElement = SoapUtil.getWsaRelatesToElement(doc, ns);
        if (relatesToElement != null) relatesToElement.getParentNode().removeChild(relatesToElement);

        SoapUtil.removeEmptySoapHeader(doc);

        return AssertionStatus.NONE;
    }
}
