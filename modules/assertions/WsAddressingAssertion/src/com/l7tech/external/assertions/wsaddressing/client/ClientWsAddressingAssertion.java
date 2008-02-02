/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.wsaddressing.client;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
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
        Map<String, String> props = context.getSsg().getProperties();

        // TODO make the MessageID prefix configurable in the assertion instead of just locally?
        String uuidPrefix = props.get("wsAddressing.uuidPrefix");

        final String id = context.prepareWsaMessageId(true, uuidPrefix);
        Element wsaMessageIdEl = SoapUtil.setWsaMessageId(context.getRequest().getXmlKnob().getDocumentWritable(), id);
        if (assertion.isRequireSignature()) {
            context.getDefaultWssRequirements().getElementsToSign().add(wsaMessageIdEl);
        }
        return AssertionStatus.NONE;
    }
}
