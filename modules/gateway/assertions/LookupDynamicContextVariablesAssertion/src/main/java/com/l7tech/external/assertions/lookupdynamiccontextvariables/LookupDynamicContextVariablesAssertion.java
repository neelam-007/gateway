package com.l7tech.external.assertions.lookupdynamiccontextvariables;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 *<p>A modular assertion to dynamically lookup a context variable and storing its result into another context variable</p>
 *
 * @author KDiep
 */
public class LookupDynamicContextVariablesAssertion extends Assertion implements SetsVariables {

    private static final String DEFAULT_OUTPUT_VARIABLE = "lookup.result";

    private static final String META_INITIALIZED = LookupDynamicContextVariablesAssertion.class.getName() + ".metadataInitialized";

    private String sourceVariable;
    private String targetOutputVariable = DEFAULT_OUTPUT_VARIABLE;

    /**
     *
     * @return the source variable.
     */
    public String getSourceVariable() {
        return sourceVariable;
    }

    /**
     *
     * @param sourceVariable the source variable to lookup.
     */
    public void setSourceVariable(final String sourceVariable) {
        this.sourceVariable = sourceVariable;
    }

    /**
     *
     * @return the target output variable name.
     */
    public String getTargetOutputVariable() {
        return targetOutputVariable;
    }

    /**
     *
     * @param targetOutputVariable the target output variable name to store the result to.
     */
    public void setTargetOutputVariable(final String targetOutputVariable) {
        this.targetOutputVariable = targetOutputVariable;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(getTargetOutputVariable(), false, false, null, true, DataType.STRING)
        };
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Look Up Dynamic Context Variables");
        meta.put(AssertionMetadata.LONG_NAME, "Looks up a value dynamically from a context variable (which may be composed of multiple variables) and places the results in another context variable.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Dynamic Context Variables Lookup Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
