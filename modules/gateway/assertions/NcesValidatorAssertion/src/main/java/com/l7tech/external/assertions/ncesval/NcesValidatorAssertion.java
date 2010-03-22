package com.l7tech.external.assertions.ncesval;

import com.l7tech.policy.CertificateInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.security.types.CertificateValidationType;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates that messages are NCES-compliant.  This implies the following:
 * <ul>
 * <li>A wsu:Timestamp is present
 * <li>A wsa:MessageID is present
 * <li>An optional SAML Assertion is present
 * <li>A ds:Signature is present, valid and covers all of the above.
 * </ul>
 */
@RequiresSOAP(wss=true)
public class NcesValidatorAssertion extends MessageTargetableAssertion implements UsesVariables {
    private boolean samlRequired;
    private CertificateValidationType certificateValidationType;
    private CertificateInfo[] trustedCertificateInfo;
    private CertificateInfo[] trustedIssuerCertificateInfo;

    public NcesValidatorAssertion() {
        super(false);
    }

    public boolean isSamlRequired() {
        return samlRequired;
    }

    public void setSamlRequired(boolean samlRequired) {
        this.samlRequired = samlRequired;
    }

    public CertificateValidationType getCertificateValidationType() {
        return certificateValidationType;
    }

    public void setCertificateValidationType( CertificateValidationType certificateValidationType ) {
        this.certificateValidationType = certificateValidationType;
    }

    public CertificateInfo[] getTrustedCertificateInfo() {
        return trustedCertificateInfo;
    }

    public void setTrustedCertificateInfo( CertificateInfo[] trustedCertificateInfo ) {
        this.trustedCertificateInfo = trustedCertificateInfo;
    }

    public CertificateInfo[] getTrustedIssuerCertificateInfo() {
        return trustedIssuerCertificateInfo;
    }

    public void setTrustedIssuerCertificateInfo( CertificateInfo[] trustedIssuerCertificateInfo ) {
        this.trustedIssuerCertificateInfo = trustedIssuerCertificateInfo;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = NcesValidatorAssertion.class.getName() + ".metadataInitialized";

    final static String baseName = "Validate Against NCES Requirements";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<NcesValidatorAssertion>(){
        @Override
        public String getAssertionName( final NcesValidatorAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Validate Message for NCES Compliance.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        // Set up smart Getter for nice, informative policy node name, for GUI
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:NcesValidator" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.ncesval.NcesValidatorAssertionValidator");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "NCES Validator Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ncesval.console.NcesValidatorAssertionPropertiesDialog");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     * @deprecated this is only for backward compatibility with the old property name
     */
    @Deprecated
    public void setOtherMessageVariableName(String otherMessageVariableName) {
        setOtherTargetMessageVariable(otherMessageVariableName);
    }
}
