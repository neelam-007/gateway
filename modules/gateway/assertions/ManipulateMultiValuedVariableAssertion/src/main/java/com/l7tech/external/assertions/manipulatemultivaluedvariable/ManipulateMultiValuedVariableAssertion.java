package com.l7tech.external.assertions.manipulatemultivaluedvariable;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME;

/**
 * 
 */
public class ManipulateMultiValuedVariableAssertion extends Assertion implements UsesVariables, SetsVariables {

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }

    @Override
    public String[] getVariablesUsed() {
        // variableName will be set if it does not exist => do not declare it as being needed.
        return new String[]{variableValue};
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(variableName != null) {
            return new VariableMetadata[]{
                    new VariableMetadata(variableName, false, true, null, true, DataType.UNKNOWN)};
        }
        return new VariableMetadata[]{};
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Manipulate a multi valued variable. This allows the variable to be created for values to be dynamically added to the multi valued variable.");

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, baseName + " Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.manipulatemultivaluedvariable.console.ManipulateMultiValuedVariableAssertionDialog");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //-PROTECTED
    protected static final Logger logger = Logger.getLogger(ManipulateMultiValuedVariableAssertion.class.getName());

    //- PRIVATE

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ManipulateMultiValuedVariableAssertion.class.getName() + ".metadataInitialized";
    private final static String baseName = "Manipulate Multi Valued Variable";

    private String variableName;
    private String variableValue;
}
