package com.l7tech.external.assertions.uuidgenerator;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Assertion which generates UUIDs and sets them to a context variable.
 */
public class UUIDGeneratorAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final int MINIMUM_QUANTITY = 1;
    public static final int MAXIMUM_QUANTITY = 100;

    /**
     * Name of the context variable.
     */
    private String targetVariable;

    /**
     * Quantity of UUIDs to generate.
     */
    private String quantity = String.valueOf(MINIMUM_QUANTITY);

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(final String quantity) {
        this.quantity = quantity;
    }

    public String getTargetVariable() {
        return targetVariable;
    }

    public void setTargetVariable(String targetVariable) {
        this.targetVariable = targetVariable;
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(quantity);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (targetVariable == null) return new VariableMetadata[0];
        return new VariableMetadata[]{new VariableMetadata(targetVariable)};
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = UUIDGeneratorAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Generate UUID");
        meta.put(AssertionMetadata.DESCRIPTION, "Generates and stores UUIDs in a single or multivalued context variable.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection                         pass
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/policy16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/policy16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:UUIDGenerator" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
