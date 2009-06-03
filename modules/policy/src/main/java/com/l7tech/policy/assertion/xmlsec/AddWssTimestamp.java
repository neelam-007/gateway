/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.PrivateKeyableSupport;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.DESCRIPTION;
import static com.l7tech.policy.assertion.AssertionMetadata.ASSERTION_FACTORY;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.Functions;

/**
 * Creates a wsu:Timestamp element and adds it to the SOAP security header in the response.
 *
 * @author alex
 */
@RequiresSOAP(wss=true)
public class AddWssTimestamp extends MessageTargetableAssertion implements ResponseWssConfig, PrivateKeyable {

    /**
     * The recommended expiry time to use when creating response wss timestamps;
     */
    public static final int DEFAULT_EXPIRY_TIME = 5 * TimeUnit.MINUTES.getMultiplier();

    public AddWssTimestamp() {
        super(TargetMessageType.RESPONSE);
    }

    /**
     * Create a new ResponseWssTimestamp with default properties.
     */
    public static AddWssTimestamp newInstance() {
        AddWssTimestamp timestamp = new AddWssTimestamp();
        timestamp.setExpiryMilliseconds(AddWssTimestamp.DEFAULT_EXPIRY_TIME);
        return timestamp;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #timeUnit}.
     */
    private int expiryMillis;

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens = false;
    
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    private boolean signatureRequired = true;
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();

    public void copyFrom(AddWssTimestamp other) {
        super.copyFrom( other );
        this.expiryMillis = other.expiryMillis;
        this.timeUnit = other.timeUnit;
        this.keyReference = other.keyReference;
        this.recipientContext = other.recipientContext;
        this.signatureRequired = other.signatureRequired;
        this.privatekeyableSupport.copyFrom( other.privatekeyableSupport );
    }

    /**
     * Should the response timestamp be signed (default true).
     *
     * @return true for signing
     */
    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    /**
     * Set the timestamp signature flag (default true).
     *
     * @param signatureRequired True to sign the response signature.
     */
    public void setSignatureRequired(boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #getTimeUnit}.
     */
    public int getExpiryMilliseconds() {
        return expiryMillis;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #getTimeUnit}.
     */
    public void setExpiryMilliseconds(int expiryMillis) {
        this.expiryMillis = expiryMillis;
    }

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
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
    public String getKeyAlias() {
        return privatekeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyAlias) {
        privatekeyableSupport.setKeyAlias(keyAlias);
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultKeystoreId) {
        privatekeyableSupport.setNonDefaultKeystoreId(nonDefaultKeystoreId);
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privatekeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefaultKeyStore) {
        privatekeyableSupport.setUsesDefaultKeyStore(usesDefaultKeyStore);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(PALETTE_NODE_NAME, "Add Timestamp");
        meta.put(DESCRIPTION, "Add a Timestamp to the message with an optional signature.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(ASSERTION_FACTORY, new Functions.Unary<AddWssTimestamp, AddWssTimestamp>(){
            @Override
            public AddWssTimestamp call( final AddWssTimestamp responseWssTimestamp ) {
                return AddWssTimestamp.newInstance();
            }
        });
        meta.put(POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, AddWssTimestamp>() {
            @Override
            public String call( final AddWssTimestamp assertion ) {
                String qualifier = "";
                if ( assertion.isSignatureRequired() ) {
                    qualifier = "signed ";
                }
                return AssertionUtils.decorateName(assertion, "Add " + qualifier + "Timestamp");
            }
        });
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.AddWssTimestampPropertiesAction");

        return meta;
    }
}
