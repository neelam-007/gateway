package com.l7tech.external.assertions.idattr;

import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.List;

public class IdentityAttributesAssertion extends Assertion implements UsesVariables, SetsVariables {
    private String variablePrefix;
    private long identityProviderOid;
    private IdentityMapping[] lookupAttributes;

    public static final String DEFAULT_VAR_PREFIX = "authenticatedUser";

    public String[] getVariablesUsed() {
        return new String[0];
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    public void setIdentityProviderOid(long identityProviderOid) {
        this.identityProviderOid = identityProviderOid;
    }

    public IdentityMapping[] getLookupAttributes() {
        return lookupAttributes;
    }

    public void setLookupAttributes(IdentityMapping[] lookupAttributes) {
        this.lookupAttributes = lookupAttributes;
    }

    public VariableMetadata[] getVariablesSet() {
        if (lookupAttributes == null || lookupAttributes.length == 0) return new VariableMetadata[0];
        String vp = variablePrefix;
        if (vp == null) vp = DEFAULT_VAR_PREFIX;
        List<VariableMetadata> metas = new ArrayList<VariableMetadata>();
        for (IdentityMapping im : lookupAttributes) {
            final AttributeConfig ac = im.getAttributeConfig();
            metas.add(new VariableMetadata(vp + "." + ac.getVariableName(), false, im.isMultivalued(), null, false, ac.getType()));
        }
        return metas.toArray(new VariableMetadata[0]);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = IdentityAttributesAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Identity Attributes");
        meta.put(AssertionMetadata.LONG_NAME, "");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.idattr.console.IdentityAttributesAssertionDialog");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/userAttrs16.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/userAttrs16.png");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:IdentityAttributes" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
