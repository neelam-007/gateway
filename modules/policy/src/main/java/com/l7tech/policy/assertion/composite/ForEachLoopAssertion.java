package com.l7tech.policy.assertion.composite;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Similar to an All assertion, but runs its child assertions zero or more times.
 */
public class ForEachLoopAssertion extends CompositeAssertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(ForEachLoopAssertion.class.getName());

    private String loopVariableName;
    private String variablePrefix;
    private int iterationLimit;
    // If you add fields here, you must update ForEachLoopAssertionTypeMapping or they won't make it into the policy XML

    public ForEachLoopAssertion() {
    }

    public ForEachLoopAssertion( List<? extends Assertion> children ) {
        super( children );
    }

    public String getLoopVariableName() {
        return loopVariableName;
    }

    public void setLoopVariableName(String loopVariableName) {
        this.loopVariableName = loopVariableName;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public int getIterationLimit() {
        return iterationLimit;
    }

    public void setIterationLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return loopVariableName == null ? new String[0] : new String[] { loopVariableName };
    }

    public VariableMetadata[] getVariablesSet() {
        return variablePrefix == null ? new VariableMetadata[0] : new VariableMetadata[] {
                new VariableMetadata(variablePrefix + ".value", false, false, null, true),
                new VariableMetadata(variablePrefix + ".iterations", false, false, null, true),
                new VariableMetadata(variablePrefix + ".failures", false, false, null, true),
                new VariableMetadata(variablePrefix + ".exceededlimit", false, false, null, true),
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ForEachLoopAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Apply to Each Member");
        meta.put(AssertionMetadata.LONG_NAME, "Apply to Each Member Of Multivalued Variable");
        meta.put(AssertionMetadata.DESCRIPTION, "Runs all child assertions against each value of a multivalued variable.");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.assertion.composite.ForEachLoopAssertionValidator");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.console.panels.ForEachLoopAssertionPolicyNode");
        meta.put(AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.assertion.composite.ForEachLoopAssertionTypeMapping");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(AssertionMetadata.POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ForEachLoopAssertionPropertiesDialog");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
