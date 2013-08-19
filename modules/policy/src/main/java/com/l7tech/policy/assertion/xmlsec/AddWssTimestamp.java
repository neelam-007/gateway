/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.TimeUnit;

import java.util.Collections;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Creates a wsu:Timestamp element and adds it to the SOAP security header in the response.
 *
 * @author alex
 */
@RequiresSOAP(wss=true)
public class AddWssTimestamp extends MessageTargetableAssertion implements WssDecorationConfig, PrivateKeyable {

    //- PUBLIC

    /**
     * The recommended expiry time to use when creating response wss timestamps;
     */
    public static final int DEFAULT_EXPIRY_TIME = 5 * TimeUnit.MINUTES.getMultiplier();

    public static enum Resolution { NANOSECONDS, MILLISECONDS, SECONDS }

    public AddWssTimestamp() {
        super(TargetMessageType.RESPONSE, true);
    }

    /**
     * Create a new ResponseWssTimestamp with default properties.
     */
    public static AddWssTimestamp newInstance() {
        AddWssTimestamp timestamp = new AddWssTimestamp();
        timestamp.setExpiryMilliseconds(AddWssTimestamp.DEFAULT_EXPIRY_TIME);
        return timestamp;
    }

    public void copyFrom( final AddWssTimestamp other ) {
        super.copyFrom( other );
        this.expiryMillis = other.expiryMillis;
        this.timeUnit = other.timeUnit;
        this.keyReference = other.keyReference;
        this.recipientContext = other.recipientContext;
        this.signatureRequired = other.signatureRequired;
        this.privatekeyableSupport.copyFrom( other.privatekeyableSupport );
        this.protectTokens = other.protectTokens;
        this.resolution = other.resolution;
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
    public long getExpiryMilliseconds() {
        return expiryMillis;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #getTimeUnit}.
     */
    public void setExpiryMilliseconds(long expiryMillis) {
        this.expiryMillis = expiryMillis;
    }

    @Deprecated
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

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution( final Resolution resolution ) {
        this.resolution = resolution;
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
    public boolean isUsingProtectTokens() {
        // Not exposed in GUI, so don't use at runtime and don't show validator warnings about it        
        return false;
    }

    @Override
    public String getDigestAlgorithmName() {
        return digestAlgorithmName;
    }

    @Override
    public void setDigestAlgorithmName(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
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
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultKeystoreId) {
        privatekeyableSupport.setNonDefaultKeystoreId(nonDefaultKeystoreId);
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultKeystoreId) {
        privatekeyableSupport.setNonDefaultKeystoreId(GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultKeystoreId));
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
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(PALETTE_NODE_NAME, assertionName);
        meta.put(DESCRIPTION, "Add a Timestamp to the message with an optional signature.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(PALETTE_NODE_SORT_PRIORITY, 66000);
        meta.put(ASSERTION_FACTORY, new Functions.Unary<AddWssTimestamp, AddWssTimestamp>(){
            @Override
            public AddWssTimestamp call( final AddWssTimestamp responseWssTimestamp ) {
                return AddWssTimestamp.newInstance();
            }
        });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.AddWssTimestampPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Timestamp Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/About16.gif");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.AddWssTimestampAssertionValidator");

        Java5EnumTypeMapping mapping = new Java5EnumTypeMapping(Resolution.class, "resolutionValue");
        meta.put( WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Collections.<TypeMapping>singleton( mapping ) ) );

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = AddWssTimestamp.class.getName() + ".metadataInitialized";

    private static final String assertionName = "Add Timestamp";

    private static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AddWssTimestamp>(){
        @Override
        public String getAssertionName( final AddWssTimestamp assertion, final boolean decorate) {
            final String qualifier = (assertion.isSignatureRequired()) ? "Signed " : null;
            final String name = qualifier == null? assertionName : "Add " + qualifier + "Timestamp";
            return decorate? AssertionUtils.decorateName(assertion, name) : assertionName;
        }
    };

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #timeUnit}.
     */
    private long expiryMillis;

    /**
     * TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})
     */
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens = false;

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    private boolean signatureRequired = true;
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();
    private Resolution resolution;
    private String digestAlgorithmName;
}
