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

    public static final String BREAK = ".break";
    
    private String loopVariableName;
    private String variablePrefix;
    private int iterationLimit;
    // If you add fields here, you must update ForEachLoopAssertionTypeMapping or they won't make it into the policy XML

    public ForEachLoopAssertion() {
    }

    public ForEachLoopAssertion( List<? extends Assertion> children ) {
        super( children );
    }

    public ForEachLoopAssertion(String loopVariableName, String variablePrefix, List<? extends Assertion> children ) {
        super( children );
        setLoopVariableName(loopVariableName);
        setVariablePrefix(variablePrefix);
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
        // Variables used should not include prefix.break because it is set by a child of this assertion,
        // which nonstandard apparently-backwards execution flow will not currently be understood by existing policy validators
        return loopVariableName == null ? new String[0] : new String[] { loopVariableName };
    }

    public VariableMetadata[] getVariablesSet() {
        return variablePrefix == null ? new VariableMetadata[0] : new VariableMetadata[] {
                new VariableMetadata(variablePrefix + ".current", false, false, null, true),
                new VariableMetadata(variablePrefix + ".iterations", false, false, null, true),
                new VariableMetadata(variablePrefix + ".exceededlimit", false, false, null, true),
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ForEachLoopAssertion.class.getName() + ".metadataInitialized";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ForEachLoopAssertion>(){
        @Override
        public String getAssertionName( final ForEachLoopAssertion assertion, final boolean decorate) {
            String shortName = assertion.meta().get(AssertionMetadata.SHORT_NAME);
            if(!decorate) return shortName;

            StringBuilder sb = new StringBuilder(256);
            sb.append(shortName);

            if (assertion.getLoopVariableName() != null && assertion.getLoopVariableName().length() > 0) {
                sb.append(" of ${");
                sb.append(assertion.getLoopVariableName());
                sb.append("}");
            }

            if (assertion.getVariablePrefix() != null && assertion.getVariablePrefix().length() > 0 && !assertion.getVariablePrefix().equals(assertion.getLoopVariableName())) {
                sb.append(" as ${");
                sb.append(assertion.getVariablePrefix());
                sb.append(".");
                sb.append("current}");
            }

            return AssertionUtils.decorateName( assertion, sb);
        }
    };

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Run Assertions for Each Item");
        meta.put(AssertionMetadata.LONG_NAME, "Run Assertions for Each Item Of Multivalued Variable");
        meta.put(AssertionMetadata.DESCRIPTION, "A composite assertion that runs all child assertions against each value within a multivalued context variable. The loop can be terminated early by setting this variable: &lt;prefix&gt;.break = true." );

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.assertion.composite.ForEachLoopAssertionValidator");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.console.panels.ForEachLoopAssertionPolicyNode");
        meta.put(AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.assertion.composite.ForEachLoopAssertionTypeMapping");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(AssertionMetadata.POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ForEachLoopAssertionPropertiesDialog");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
