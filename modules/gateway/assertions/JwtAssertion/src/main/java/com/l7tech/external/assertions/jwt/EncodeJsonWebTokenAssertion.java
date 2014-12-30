package com.l7tech.external.assertions.jwt;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


public class EncodeJsonWebTokenAssertion extends Assertion implements UsesVariables, SetsVariables {

    public static final String CLUSTER_PROPERTY_SHOW_ALL = "jwt.showAllAlgorithms";
    private String sourceVariable;
    private String headerAction = JsonWebTokenConstants.HEADERS_USE_DEFAULT;
    private String sourceHeaders;

    private boolean signPayload;
    private String signatureAlgorithm = "None";
    private String signatureSecretKey;

    private String signatureSourceVariable;
    private String privateKeyGoid;
    private String privateKeyAlias;
    private String signatureKeyType;
    private String signatureJwksKeyId;

    private boolean encryptPayload;
    private String keyManagementAlgorithm;
    private String contentEncryptionAlgorithm;
    private String encryptionKey;
    private String encryptionSecret;
    private String encryptionKeyType;
    private String encryptionKeyId;

    private String targetVariable;

    private int signatureSourceType;

    private int encryptionSourceType;

    public int getSignatureSourceType() {
        return signatureSourceType;
    }

    public void setSignatureSourceType(int signatureSourceType) {
        this.signatureSourceType = signatureSourceType;
    }

    public boolean isSignPayload() {
        return signPayload;
    }

    public void setSignPayload(boolean signPayload) {
        this.signPayload = signPayload;
    }

    public boolean isEncryptPayload() {
        return encryptPayload;
    }

    public void setEncryptPayload(boolean encryptPayload) {
        this.encryptPayload = encryptPayload;
    }

    public String getSourceVariable() {
        return sourceVariable;
    }

    public void setSourceVariable(String sourceVariable) {
        this.sourceVariable = sourceVariable;
    }

    public String getHeaderAction() {
        return headerAction;
    }

    public void setHeaderAction(String headerAction) {
        this.headerAction = headerAction;
    }

    public String getSourceHeaders() {
        return sourceHeaders;
    }

    public void setSourceHeaders(String sourceHeaders) {
        this.sourceHeaders = sourceHeaders;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSignatureSecretKey() {
        return signatureSecretKey;
    }

    public void setSignatureSecretKey(String signatureSecretKey) {
        this.signatureSecretKey = signatureSecretKey;
    }

    public String getSignatureSourceVariable() {
        return signatureSourceVariable;
    }

    public void setSignatureSourceVariable(String signatureSourceVariable) {
        this.signatureSourceVariable = signatureSourceVariable;
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

    public String getSignatureKeyType() {
        return signatureKeyType;
    }

    public void setSignatureKeyType(String signatureKeyType) {
        this.signatureKeyType = signatureKeyType;
    }

    public String getSignatureJwksKeyId() {
        return signatureJwksKeyId;
    }

    public void setSignatureJwksKeyId(String signatureJwksKeyId) {
        this.signatureJwksKeyId = signatureJwksKeyId;
    }

    public String getKeyManagementAlgorithm() {
        return keyManagementAlgorithm;
    }

    public void setKeyManagementAlgorithm(String keyManagementAlgorithm) {
        this.keyManagementAlgorithm = keyManagementAlgorithm;
    }

    public String getContentEncryptionAlgorithm() {
        return contentEncryptionAlgorithm;
    }

    public void setContentEncryptionAlgorithm(String contentEncryptionAlgorithm) {
        this.contentEncryptionAlgorithm = contentEncryptionAlgorithm;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getEncryptionSecret() {
        return encryptionSecret;
    }

    public void setEncryptionSecret(String encryptionSecret) {
        this.encryptionSecret = encryptionSecret;
    }

    public String getEncryptionKeyType() {
        return encryptionKeyType;
    }

    public void setEncryptionKeyType(String encryptionKeyType) {
        this.encryptionKeyType = encryptionKeyType;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }

    public String getTargetVariable() {
        return targetVariable;
    }

    public void setTargetVariable(String targetVariable) {
        this.targetVariable = targetVariable;
    }

    public int getEncryptionSourceType() {
        return encryptionSourceType;
    }

    public void setEncryptionSourceType(int encryptionSourceType) {
        this.encryptionSourceType = encryptionSourceType;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = EncodeJsonWebTokenAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        props.put(CLUSTER_PROPERTY_SHOW_ALL, new String[] {
                "Show all algorithms, INCLUDING those not certified for use with the Encode Json Web Token assertion. Value is a Boolean.  Default: false",
                "false"
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Encode Json Web Token");
        meta.put(AssertionMetadata.DESCRIPTION, "Creates a JSON Web Token.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jwt.console.EncodeJsonWebTokenPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Encode Json Web Token Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set nice, informative policy node name for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, EncodeJsonWebTokenAssertion.Validator.class.getName());

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(getTargetVariable() + ".compact", true, false, null, false)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(
                sourceVariable,
                sourceHeaders,
                signatureSecretKey,
                signatureSourceVariable,
                signatureJwksKeyId,
                encryptionKey,
                encryptionKeyId
        );
    }

    @Override
    public EncodeJsonWebTokenAssertion clone() {
        final EncodeJsonWebTokenAssertion clone = (EncodeJsonWebTokenAssertion) super.clone();
        clone.setSourceVariable(sourceVariable);
        clone.setHeaderAction(headerAction);
        clone.setSourceHeaders(sourceHeaders);
        clone.setSignatureAlgorithm(signatureAlgorithm);
        clone.setSignatureSecretKey(signatureSecretKey);
        clone.setSignatureSourceVariable(signatureSourceVariable);
        clone.setPrivateKeyGoid(privateKeyGoid);
        clone.setPrivateKeyAlias(privateKeyAlias);
        clone.setSignatureKeyType(signatureKeyType);
        clone.setSignatureJwksKeyId(signatureJwksKeyId);

        clone.setKeyManagementAlgorithm(keyManagementAlgorithm);
        clone.setContentEncryptionAlgorithm(contentEncryptionAlgorithm);
        clone.setEncryptionKey(encryptionKey);
        clone.setEncryptionKeyType(encryptionKeyType);
        clone.setEncryptionKeyId(encryptionKeyId);

        clone.setTargetVariable(targetVariable);

        clone.setSignPayload(signPayload);
        clone.setEncryptPayload(encryptPayload);
        clone.setSignatureSourceType(signatureSourceType);

        clone.setEncryptionSourceType(encryptionSourceType);
        clone.setEncryptionSecret(encryptionSecret);

        return clone;
    }

    public static class Validator implements AssertionValidator {
        private final EncodeJsonWebTokenAssertion assertion;

        public Validator(@NotNull final EncodeJsonWebTokenAssertion assertion) {
            this.assertion = assertion;
        }

        @Override
        public void validate(final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result) {
            if("RSASSA-PKCS-v1_5 using SHA-256".equals(assertion.getSignatureAlgorithm())){
                result.addError(new PolicyValidatorResult.Error(assertion, "The use of 'RSASSA-PKCS-v1_5 using SHA-256' for encryption is not recommended, please use 'RSASSA-PSS using SHA-256 and MGF1 with SHA-256'.", null));
            }
            if("RSASSA-PKCS-v1_5 using SHA-384".equals(assertion.getSignatureAlgorithm())){
                result.addError(new PolicyValidatorResult.Error(assertion, "The use of 'RSASSA-PKCS-v1_5 using SHA-384' for encryption is not recommended, please use 'RSASSA-PSS using SHA-384 and MGF1 with SHA-384'.", null));
            }
            if("RSASSA-PKCS-v1_5 using SHA-512".equals(assertion.getSignatureAlgorithm())){
                result.addError(new PolicyValidatorResult.Error(assertion, "The use of 'RSASSA-PKCS-v1_5 using SHA-256' for encryption is not recommended, please use 'RSASSA-PSS using SHA-512 and MGF1 with SHA-512'.", null));
            }
            if(assertion.getKeyManagementAlgorithm() != null && assertion.getKeyManagementAlgorithm().startsWith("RSAES-PKCS1-V1_5")){
                result.addError(new PolicyValidatorResult.Error(assertion,
                        "The use of 'RSAES-PKCS1-V1_5' for encryption is not recommended, please use 'RSAES OAEP using default parameters' or 'RSAES OAEP using SHA-256 and MGF1 with SHA-256'.", null));
            }
        }
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<EncodeJsonWebTokenAssertion>(){
        @Override
        public String getAssertionName( final EncodeJsonWebTokenAssertion assertion, final boolean decorate) {
            final StringBuilder sb = new StringBuilder("Encode Json Web Token");
            if(decorate){
                if(!assertion.isSignPayload() && !assertion.isEncryptPayload()){
                    sb.append(": unsecure");
                }
                if(assertion.isSignPayload() && assertion.isEncryptPayload()){
                    sb.append(": sign & encrypt payload");
                } else {
                    if(assertion.isSignPayload()){
                        sb.append(": sign payload");
                    }
                    if(assertion.isEncryptPayload()){
                        sb.append(": encrypt payload");
                    }
                }
            }
            return sb.toString();
        }
    };
}
