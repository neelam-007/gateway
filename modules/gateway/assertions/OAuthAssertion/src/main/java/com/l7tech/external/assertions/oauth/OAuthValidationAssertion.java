package com.l7tech.external.assertions.oauth;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Assertion for validation of OAuth tokens. The tokens are expected to be split into two parts: the signature part; 
 * and the signed data portion that the signature will be validated against. This can done done using any number
 * of existing assertions such as the Regex , XPath, or XSLT assertions.
 */
public class OAuthValidationAssertion extends Assertion implements UsesVariables, SetsVariables {

    public static final String VARIABLE_TOKEN_SIGNATURE_VALID = "oauth.token.valid";

    private String oAuthTokenText;
    private String oAuthTokenSignature;
    private String verifyCertificateName;
    private String signatureAlgorithm;
    private boolean failOnMismatch;
    private boolean signatureEncoded;

    public boolean isFailOnMismatch() {
        return failOnMismatch;
    }

    public void setFailOnMismatch(boolean failOnMismatch) {
        this.failOnMismatch = failOnMismatch;
    }

    public String getOAuthTokenText() {
        return oAuthTokenText;
    }

    public void setOAuthTokenText(String oAuthTokenText) {
        this.oAuthTokenText = oAuthTokenText;
    }

    public String getOAuthTokenSignature() {
        return oAuthTokenSignature;
    }

    public void setOAuthTokenSignature(String oAuthTokenSignature) {
        this.oAuthTokenSignature = oAuthTokenSignature;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public boolean isSignatureEncoded() {
        return signatureEncoded;
    }

    public void setSignatureEncoded(boolean signatureEncoded) {
        this.signatureEncoded = signatureEncoded;
    }

    public String getVerifyCertificateName() {
        return verifyCertificateName;
    }

    public void setVerifyCertificateName(String verifyCertificateName) {
        this.verifyCertificateName = verifyCertificateName;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        // return new VariableMetadata[0];  //To change body of implemented methods use File | Settings | File Templates.
        return new VariableMetadata[] { new VariableMetadata(VARIABLE_TOKEN_SIGNATURE_VALID, false, false, null, true, DataType.STRING) };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames( getOAuthTokenSignature(), getOAuthTokenText() );
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = OAuthValidationAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Verify OAuth Token");
        meta.put(AssertionMetadata.LONG_NAME, "Verify an OAuth token against an input string or a set of variables.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 999);
//        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.oauth.console.OAuthValidationAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "OAuth Token Verification Properties");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:OAuth" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}