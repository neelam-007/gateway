package com.l7tech.external.assertions.lookupdynamiccontextvariables;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 *<p>A modular assertion to dynamically lookup a context variable and storing its result into another context variable</p>
 *
 * @author KDiep
 */
public class LookupDynamicContextVariablesAssertion extends Assertion implements SetsVariables {

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
    private String targetOutputVariable;
    private DataType targetDataType;

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
        if(getTargetOutputVariable() == null || getTargetOutputVariable().trim().isEmpty()) return new VariableMetadata[0];
        return new VariableMetadata[]{
                new VariableMetadata(getTargetOutputVariable(), false, false, null, true, getTargetDataType())
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.LONG_NAME, "Looks up the value of a context variable and stores the value in another context variable. The name of the lookup variable can be specified using static text combined with other context variables.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Lookup Context Variable Properties");
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
                String target = assertion.getTargetOutputVariable();
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
