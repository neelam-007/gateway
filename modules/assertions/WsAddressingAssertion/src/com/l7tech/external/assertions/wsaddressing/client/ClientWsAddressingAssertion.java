/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.wsaddressing.client;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.w3c.dom.Element;
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

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException {
        return AssertionStatus.NONE;
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
        final String other = assertion.getEnableOtherNamespace();
        final String ns;
        if (other != null && other.length() > 0) {
            ns = other;
        } else if (assertion.isEnableWsAddressing10()) {
            ns = SoapUtil.WSA_NAMESPACE_10;
        } else if (assertion.isEnableWsAddressing200408()) {
            ns = SoapUtil.WSA_NAMESPACE2;
        } else {
            throw new PolicyAssertionException(assertion, "Unable to select WS-Addressing namespace URI");
        }
        final String id = context.prepareWsaMessageId(true, ns, uuidPrefix);
        Element wsaMessageIdEl = SoapUtil.setWsaMessageId(context.getRequest().getXmlKnob().getDocumentWritable(), ns, id);
        if (assertion.isRequireSignature()) {
            context.getDefaultWssRequirements().getElementsToSign().add(wsaMessageIdEl);
        }
        return AssertionStatus.NONE;
    }
}
