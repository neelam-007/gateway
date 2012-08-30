package com.l7tech.external.assertions.validatenonsoapsaml;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.HasOptionalSamlSignature;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;

import java.util.logging.Logger;

/**
 * Validate a SAML assertion not received via WS-Security in a SOAP message
 */
public class ValidateNonSoapSamlAssertion extends RequireSaml implements HasOptionalSamlSignature {
    public ValidateNonSoapSamlAssertion() {
    }

    public ValidateNonSoapSamlAssertion(final ValidateNonSoapSamlAssertion copyFrom) {
        copyFrom(copyFrom);
    }

    @Override
    public boolean isRequireDigitalSignature() {
        return requireDigitalSignature;
    }

    @Override
    public void setRequireDigitalSignature(boolean requireDigitalSignature) {
        this.requireDigitalSignature = requireDigitalSignature;
    }

    @Override
    public void copyFrom(RequireSaml requestWssSaml) {
        super.copyFrom(requestWssSaml);
        ValidateNonSoapSamlAssertion nonSoap = (ValidateNonSoapSamlAssertion) requestWssSaml;
        setRequireDigitalSignature(nonSoap.isRequireDigitalSignature());
    }

    final static String baseName = "(Non-SOAP) Validate SAML Assertion";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ValidateNonSoapSamlAssertion>(){
        @Override
        public String getAssertionName( final ValidateNonSoapSamlAssertion assertion, final boolean decorate) {
            return (decorate)? assertion.describe(): baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Validate a SAML assertion not received using WS-Security");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.validatenonsoapsaml.console.AddValidateNonSoapSamlAssertionAdvice");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.external.assertions.validatenonsoapsaml.console.EditValidateNonSoapSamlAssertionAction");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    // - PROTECTED

    protected static final Logger logger = Logger.getLogger(ValidateNonSoapSamlAssertion.class.getName());

    @Override
    protected String getAssertionDisplayName() {
        return baseName;
    }

    // - PRIVATE
    private boolean requireDigitalSignature = true;

    private static final String META_INITIALIZED = ValidateNonSoapSamlAssertion.class.getName() + ".metadataInitialized";
}
