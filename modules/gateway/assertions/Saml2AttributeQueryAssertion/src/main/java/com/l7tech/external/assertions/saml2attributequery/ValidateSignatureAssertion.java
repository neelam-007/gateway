package com.l7tech.external.assertions.saml2attributequery;

import com.l7tech.policy.assertion.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 22-Jan-2009
 * Time: 11:42:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ValidateSignatureAssertion extends Assertion implements UsesVariables {

    //- PUBLIC

    /**
     * Bean constructor
     */
    public ValidateSignatureAssertion() {
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String[] getVariablesUsed() {
        if(variableName == null) {
            return new String[0];
        } else {
            return new String[] {variableName};
        }
    }

    /**
     * Get metadata for this assertion.
     *
     * @return The assertion metadata.
     */
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Validate Digital Signature");
        meta.put(AssertionMetadata.LONG_NAME, "Validate Digital Signature");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Disable automatic properties editor
        //meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.saml2attributequery.console.ValidateSignatureAssertionPropertiesEditor");

        // Disable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:CertificateAttributes" rather than "set:modularAssertions"

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private String variableName = null;

    private static final String META_INITIALIZED = ValidateSignatureAssertion.class.getName() + ".metadataInitialized";
}
