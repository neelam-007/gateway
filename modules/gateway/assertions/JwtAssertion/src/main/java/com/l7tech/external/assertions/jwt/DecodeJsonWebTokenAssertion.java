package com.l7tech.external.assertions.jwt;


import com.google.common.collect.Lists;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DecodeJsonWebTokenAssertion extends Assertion implements UsesVariables, SetsVariables {

    private String sourcePayload;
    private String validationType;
    private boolean privateKeyFromList;

    private String signatureSecret;

    private String privateKeyGoid;
    private String privateKeyAlias;

    private String privateKeySource;
    private String keyType;
    private String keyId;
    private String targetVariablePrefix;

    public String getSourcePayload() {
        return sourcePayload;
    }

    public void setSourcePayload(String sourcePayload) {
        this.sourcePayload = sourcePayload;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public String getSignatureSecret() {
        return signatureSecret;
    }

    public void setSignatureSecret(String signatureSecret) {
        this.signatureSecret = signatureSecret;
    }

    public boolean isPrivateKeyFromList() {
        return privateKeyFromList;
    }

    public void setPrivateKeyFromList(boolean privateKeyFromList) {
        this.privateKeyFromList = privateKeyFromList;
    }

    public String getPrivateKeyGoid() {
        return privateKeyGoid;
    }

    public void setPrivateKeyGoid(String privateKeyGoid) {
        this.privateKeyGoid = privateKeyGoid;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    public String getPrivateKeySource() {
        return privateKeySource;
    }

    public void setPrivateKeySource(String privateKeySource) {
        this.privateKeySource = privateKeySource;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getTargetVariablePrefix() {
        return targetVariablePrefix;
    }

    public void setTargetVariablePrefix(String targetVariablePrefix) {
        this.targetVariablePrefix = targetVariablePrefix;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = DecodeJsonWebTokenAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Decode Json Web Token");
        meta.put(AssertionMetadata.DESCRIPTION, "Decode a JSON Web Token.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jwt.console.DecodeJsonWebTokenPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Decode Json Web Token Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set nice, informative policy node name for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


    @Override
    public VariableMetadata[] getVariablesSet() {
        final List<VariableMetadata> set = Lists.newArrayList();
        set.add(new VariableMetadata(getTargetVariablePrefix() + ".type", true, false, null, false));
        set.add(new VariableMetadata(getTargetVariablePrefix() + ".header", true, false, null, false));
        set.add(new VariableMetadata(getTargetVariablePrefix() + ".header.names", true, true, null, false));
        if (JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(validationType)) {
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".payload", true, false, null, false));
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".signature", true, false, null, false));
        } else {
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".encrypted_key", true, false, null, false));
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".initialization_vector", true, false, null, false));
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".cipher_text", true, false, null, false));
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".authentication_tag", true, false, null, false));
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".payload", true, false, null, false));
            if(!JsonWebTokenConstants.VALIDATION_NONE.equals(validationType)){
                set.add(new VariableMetadata(getTargetVariablePrefix() + ".plaintext", true, false, null, false));
            }
        }
        if(!JsonWebTokenConstants.VALIDATION_NONE.equals(validationType)){
            set.add(new VariableMetadata(getTargetVariablePrefix() + ".valid", true, false, null, false));
        }
        return set.toArray(new VariableMetadata[set.size()]);
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(
                sourcePayload,
                signatureSecret,
                privateKeySource,
                keyId
        );
    }

    @Override
    public DecodeJsonWebTokenAssertion clone(){
        final DecodeJsonWebTokenAssertion c = (DecodeJsonWebTokenAssertion) super.clone();
        c.setSourcePayload(sourcePayload);
        c.setValidationType(validationType);
        c.setSignatureSecret(signatureSecret);
        c.setPrivateKeyFromList(privateKeyFromList);
        c.setPrivateKeyGoid(privateKeyGoid);
        c.setPrivateKeyAlias(privateKeyAlias);
        c.setPrivateKeySource(privateKeySource);
        c.setKeyType(keyType);
        c.setKeyId(keyId);
        c.setTargetVariablePrefix(targetVariablePrefix);
        return c;
    }
}