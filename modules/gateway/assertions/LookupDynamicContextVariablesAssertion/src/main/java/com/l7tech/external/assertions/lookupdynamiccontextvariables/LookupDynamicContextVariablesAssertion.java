package com.l7tech.external.assertions.lookupdynamiccontextvariables;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

/**
 *<p>A modular assertion to dynamically lookup a context variable and storing its result into another context variable</p>
 *
 * @author KDiep
 */
public class LookupDynamicContextVariablesAssertion extends Assertion implements SetsVariables, UsesVariables {

    /**
     * The output variable suffix.
     */
    public static final String OUTPUT_SUFIX = ".output";

    /**
     * The found variable suffix.
     */
    public static final String FOUND_SUFIX = ".found";

    /**
     * The multivalued variable suffix.
     */
    public static final String MULTIVALUED_SUFIX = ".multivalued";

    /**
     * The default variable prefix.
     */
    public static final String DEFAULT_VARIABLE_PREFIX = "found";

    /**
     * All the available suffixes.
     */
    public static final String[] VARIABLE_SUFFIXES = new String[]{OUTPUT_SUFIX, FOUND_SUFIX, MULTIVALUED_SUFIX};

    /**
     * The maximum user definable field length.  Any text exceeding this length will be truncated.
     */
    private static final int MAX_USER_DEFINABLE_FIELD_LENGTH = 60;

    /**
     * The supported data types.
     */
    public static final DataType[] SUPPORTED_TYPES = new DataType[]{
            DataType.STRING,
            DataType.DATE_TIME,
            DataType.CERTIFICATE,
            DataType.ELEMENT,
            DataType.MESSAGE
    };

    private static final String META_INITIALIZED = LookupDynamicContextVariablesAssertion.class.getName() + ".metadataInitialized";

    private static final String BASE_NAME = "Look Up Context Variable";

    private String sourceVariable;
    private String targetOutputVariablePrefix = DEFAULT_VARIABLE_PREFIX;
    private DataType targetDataType;
    private boolean failOnNotFound = true;

    /**
     *
     * @return the target data type.
     */
    public DataType getTargetDataType() {
        return targetDataType;
    }

    /**
     *
     * @param targetDataType the target data type.
     */
    public void setTargetDataType(final DataType targetDataType) {
        this.targetDataType = targetDataType;
    }

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
     * @return the target output variable prefix.
     */
    public String getTargetOutputVariablePrefix() {
        return targetOutputVariablePrefix;
    }

    /**
     *
     * @param targetOutputVariablePrefix the target output variable prefix.
     */
    public void setTargetOutputVariablePrefix(final String targetOutputVariablePrefix) {
        this.targetOutputVariablePrefix = targetOutputVariablePrefix;
    }

    /**
     *
     * @return true if the assertion should fail if the lookup context variable does not exist, false otherwise.
     */
    public boolean isFailOnNotFound() {
        return failOnNotFound;
    }

    /**
     *
     * @param failOnNotFound indicate if the assertion should fail when the lookup context variable does not exist.
     */
    public void setFailOnNotFound(final boolean failOnNotFound) {
        this.failOnNotFound = failOnNotFound;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(getTargetOutputVariablePrefix() + OUTPUT_SUFIX, false, false, null, true, getTargetDataType()),
                new VariableMetadata(getTargetOutputVariablePrefix() + MULTIVALUED_SUFIX, false, false, null, true, DataType.BOOLEAN),
                new VariableMetadata(getTargetOutputVariablePrefix() + FOUND_SUFIX, false, false, null, true, DataType.BOOLEAN)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(getSourceVariable());
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.LONG_NAME, "Looks up the value of a context variable and stores the value in another context variable. The name of the lookup variable can be specified using static text combined with other context variables.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, BASE_NAME + " Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<LookupDynamicContextVariablesAssertion>() {
            @Override
            public String getAssertionName(final LookupDynamicContextVariablesAssertion assertion, final boolean decorate) {
                if(!decorate) return BASE_NAME;
                String source = assertion.getSourceVariable();
                if(source != null && source.length() > MAX_USER_DEFINABLE_FIELD_LENGTH){
                    source = source.substring(0, MAX_USER_DEFINABLE_FIELD_LENGTH) + "...";
                }
                String target = assertion.getTargetOutputVariablePrefix() + OUTPUT_SUFIX;
                if(target != null && target.length() > MAX_USER_DEFINABLE_FIELD_LENGTH){
                    target = target.substring(0, MAX_USER_DEFINABLE_FIELD_LENGTH) + "...";
                }
                final String decoration = BASE_NAME + ": find " + source + "; output value to ${" + target + "}";
                return AssertionUtils.decorateName(assertion, decoration);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
