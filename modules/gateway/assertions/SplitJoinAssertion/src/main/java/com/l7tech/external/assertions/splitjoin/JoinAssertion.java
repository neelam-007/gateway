package com.l7tech.external.assertions.splitjoin;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.InvalidContextVariableException;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

/**
 * This assertion joins a multi-valued context variable containing List&lt;String&gt;
 * into a single-valued String value, interleaving each pair of values with a specified
 * join string.
 */
public class JoinAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(JoinAssertion.class.getName());
    private static final String[] EMPTY_STRING = new String[0];
    private static final VariableMetadata[] EMPTY_VARIABLE_METADATA = new VariableMetadata[0];

    private String joinSubstring = ",";
    /**
     * Do not rename input and output variable to be source and target variable as this will break existing policies.
     * If they are renamed, the old setters will still be needed to be backwards compatible with previous versions.
     */
    private String inputVariable;
    private String outputVariable;

    /** @return the substring to insert between each pair of items. Never null. */
    public String getJoinSubstring() {
        return joinSubstring;
    }

    /** @param joinSubstring  the substring to insert between each pair of items. */
    public void setJoinSubstring(String joinSubstring) {
        this.joinSubstring = joinSubstring;
    }

    /** @return name of variable to join.*/
    public String getInputVariable() {
        return inputVariable;
    }

    /** @param inputVariable name of variable to join. */
    public void setInputVariable(String inputVariable) {
        this.inputVariable = inputVariable;
    }

    /** @return name of variable in which to store the result.  May be the same as inputVariable.*/
    public String getOutputVariable() {
        return outputVariable;
    }

    /** @param outputVariable name of variable in which to store the result.  May be the same as outputVariable. */
    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return inputVariable == null ? EMPTY_STRING : new String[] { inputVariable };
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return outputVariable == null ? EMPTY_VARIABLE_METADATA : new VariableMetadata[] {
                new VariableMetadata(outputVariable, false, true, outputVariable, true, DataType.STRING)
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = JoinAssertion.class.getName() + ".metadataInitialized";

    private final static String baseName = "Join Variable";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<JoinAssertion>(){
        @Override
        public String getAssertionName( final JoinAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return "Join variable " + assertion.getInputVariable() + " into " + assertion.getOutputVariable() + " using \"" + assertion.getJoinSubstring() + '"';
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Join a multi-valued context variable into a single-valued context variable.");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/join16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Join Variable Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.splitjoin.console.JoinVariablePropertiesDialog");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/join16.gif");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
