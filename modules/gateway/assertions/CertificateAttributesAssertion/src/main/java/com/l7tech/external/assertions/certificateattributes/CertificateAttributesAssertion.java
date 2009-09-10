package com.l7tech.external.assertions.certificateattributes;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.cert.CertificateAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;

/**
 *
 */
public class CertificateAttributesAssertion extends Assertion implements SetsVariables {

    //- PUBLIC

    /**
     * Bean constructor
     */
    public CertificateAttributesAssertion() {
    }

    /**
     * Get the variable prefix.
     *
     * @return The prefix
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }

    /**
     * Set the variable prefix.
     *
     * @param variablePrefix The new prefix
     */
    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    /**
     * Get the variables set by this assertion.
     *
     * <p>These variables are not set if the assertion fails.</p>
     *
     * @return The variables that are set.
     */
    @Override
    public VariableMetadata[] getVariablesSet() {
        List<VariableMetadata> variables = new ArrayList<VariableMetadata>();
        for (CertificateAttribute attribute : EnumSet.allOf(CertificateAttribute.class)) {
            String prefixedName = variablePrefix + "." + attribute.toString();
            variables.add( new VariableMetadata( prefixedName, attribute.isPrefixed(), attribute.isMultiValued(), prefixedName, false ) );
        }
        return variables.toArray( new VariableMetadata[variables.size()] );
    }

    /**
     * Get metadata for this assertion.
     *
     * @return The assertion metadata.
     */
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Extract Attributes from Certificate");
        meta.put(AssertionMetadata.DESCRIPTION, "Extract information from the X.509 Certificate of the last authenticated user and place them into context variables.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Certificate Attributes Properties");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertionValidator");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = CertificateAttributesAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix = "certificate";
}
