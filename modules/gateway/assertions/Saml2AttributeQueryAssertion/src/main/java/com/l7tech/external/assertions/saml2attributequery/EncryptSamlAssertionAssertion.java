package com.l7tech.external.assertions.saml2attributequery;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23-Jan-2009
 * Time: 6:48:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class EncryptSamlAssertionAssertion extends Assertion implements UsesVariables {
    public static final String VARIABLE_NAME = "saml2.encrypt.cert.subjectDN";

    //- PUBLIC

    /**
     * Bean constructor
     */
    public EncryptSamlAssertionAssertion() {
    }

    public String[] getVariablesUsed() {
        return new String[] {VARIABLE_NAME};
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
        meta.put(AssertionMetadata.SHORT_NAME, "Encrypt SAML Assertion");
        meta.put(AssertionMetadata.LONG_NAME, "Encrypt SAML Assertion");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Disable automatic properties editor
        meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);

        //meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.saml2attributequery.console.Saml2AttributeQueryAssertionPropertiesEditor");

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

    private static final String META_INITIALIZED = EncryptSamlAssertionAssertion.class.getName() + ".metadataInitialized";
}
