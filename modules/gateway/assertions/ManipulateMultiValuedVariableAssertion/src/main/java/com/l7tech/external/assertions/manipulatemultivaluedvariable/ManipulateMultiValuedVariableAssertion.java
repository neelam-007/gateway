package com.l7tech.external.assertions.manipulatemultivaluedvariable;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
public class ManipulateMultiValuedVariableAssertion extends Assertion implements UsesVariables, SetsVariables {

    public String getTargetVariableName() {
        return targetVariableName;
    }

    /**
     * //TODO Delete for Goatfish. Only in place to support any dev builds which created assertions while this assertion was a prototype.
     *
     * @param variableName
     */
    @Deprecated
    public void setVariableName(String variableName) {
        setTargetVariableName(variableName);
    }

    public void setTargetVariableName(String targetVariableName) {
        this.targetVariableName = targetVariableName;
    }

    public String getSourceVariableName() {
        return sourceVariableName;
    }

    public void setSourceVariableName(String sourceVariableName) {
        this.sourceVariableName = sourceVariableName;
    }

    /**
     * //TODO Delete for Goatfish. Only in place to support any dev builds which created assertions while this assertion was a prototype.
     * @param variableValue
     */
    public void setVariableValue(String variableValue){
        this.sourceVariableName = variableValue;
    }

    @Override
    public String[] getVariablesUsed() {
        // targetVariableName will be set if it does not exist => do not declare it as being needed.
        if (sourceVariableName != null) {
            return Syntax.getReferencedNames(Syntax.getVariableExpression(sourceVariableName));
        }
        return new String[]{};
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(targetVariableName != null) {
            return new VariableMetadata[]{
                    new VariableMetadata(targetVariableName, false, true, null, true, DataType.UNKNOWN)};
        }
        return new VariableMetadata[]{};
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Allows for Multivalued variables to be both created and manipulated. Appending a variable (single or multivalued) is currently supported.");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, baseName + " Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.manipulatemultivaluedvariable.console.ManipulateMultiValuedVariableAssertionDialog");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = ManipulateMultiValuedVariableAssertion.class.getName() + ".metadataInitialized";
    private final static String baseName = "Manipulate Multivalued Variable";

    private String targetVariableName;
    private String sourceVariableName;

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ManipulateMultiValuedVariableAssertion>(){
        @Override
        public String getAssertionName( final ManipulateMultiValuedVariableAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder name = new StringBuilder(baseName + " ");
            name.append(assertion.getTargetVariableName());
            name.append(" append variable ");
            name.append(assertion.getSourceVariableName());
            return name.toString();
        }
    };

}
