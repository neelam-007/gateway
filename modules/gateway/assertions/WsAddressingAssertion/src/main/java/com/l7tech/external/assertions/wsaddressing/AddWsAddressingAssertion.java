/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.wsaddressing;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.xml.KeyReference;

public class AddWsAddressingAssertion extends MessageTargetableAssertion implements WssDecorationConfig {

    public static final String ACTION_AUTOMATIC = "<<auto>>";

    @Override
    public String[] getVariablesUsed() {
        return super.getVariablesUsed();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public String getReplyEndpoint() {
        return replyEndpoint;
    }

    public void setReplyEndpoint(String replyEndpoint) {
        this.replyEndpoint = replyEndpoint;
    }

    public String getFaultEndpoint() {
        return faultEndpoint;
    }

    public void setFaultEndpoint(String faultEndpoint) {
        this.faultEndpoint = faultEndpoint;
    }

    public String getRelatesToMessageId() {
        return relatesToMessageId;
    }

    public void setRelatesToMessageId(String relatesToMessageId) {
        this.relatesToMessageId = relatesToMessageId;
    }

    public String getWsaNamespaceUri() {
        return wsaNamespaceUri;
    }

    public void setWsaNamespaceUri(String wsaNamespaceUri) {
        this.wsaNamespaceUri = wsaNamespaceUri;
    }

    public boolean isSignMessageProperties() {
        return signMessageProperties;
    }

    public void setSignMessageProperties(boolean signMessageProperties) {
        this.signMessageProperties = signMessageProperties;
    }

    @Override
    public String getKeyReference() {
        return keyReference;
    }

    @Override
    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    @Override
    public boolean isProtectTokens() {
        return protectTokens;
    }

    @Override
    public void setProtectTokens(boolean protectTokens) {
        this.protectTokens = protectTokens;
    }

    @Override
    public String getDigestAlgorithmName() {
        return this.digestAlgorithmName;
    }

    @Override
    public void setDigestAlgorithmName(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
        this.recipientContext = recipientContext;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Add WS-Addressing into an outbound message with optional signing.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Add WS-Addressing Properties");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WsAddressing" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    //- PRIVATE
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AddWsAddressingAssertion>(){
        @Override
        public String getAssertionName( final AddWsAddressingAssertion assertion, final boolean decorate) {
            StringBuilder sb = new StringBuilder("Add ");

            if ( assertion.isSignMessageProperties() ) {
                sb.append("Signed ");
            }

            sb.append("WS-Addressing");

            return (decorate)? AssertionUtils.decorateName(assertion, sb): baseName;
        }
    };

    private String action;
    private String messageId;
    private String destination;
    private String sourceEndpoint;
    private String replyEndpoint;
    private String faultEndpoint;
    private String relatesToMessageId;
    private String wsaNamespaceUri;
    private boolean signMessageProperties;
    private boolean protectTokens;
    private String digestAlgorithmName;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private String keyReference = KeyReference.BST.getName();

    private static final String META_INITIALIZED = AddWsAddressingAssertion.class.getName() + ".metadataInitialized";

    private final static String baseName ="Add WS-Addressing";
}
