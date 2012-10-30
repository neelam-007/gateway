package com.l7tech.external.assertions.splitjoin;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * This assertion splits a single-valued String context variable into a multi-valued List&lt;String&gt; by
 * splitting on the specified substring.
 */
public class SplitAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(SplitAssertion.class.getName());
    private static final String[] EMPTY_STRING = new String[0];
    private static final VariableMetadata[] EMPTY_VARIABLE_METADATA = new VariableMetadata[0];

    /**
     * String pattern to use to split inputVariable on.
     */
    private String splitPattern = ",";
    /**
     * Do not rename input and output variable to be source and target variable as this will break existing policies.
     * If they are renamed, the old setters will still be needed to be backwards compatible with previous versions.
     */
    private String inputVariable;
    private String outputVariable;
    private boolean isSplitPatternRegEx = true;//true by default for backwards compatibility
    private boolean isIgnoreEmptyValues;

    public String getSplitPattern() {
        return splitPattern;
    }

    public void setSplitPattern(String splitPattern) {
        this.splitPattern = splitPattern;
    }

    public String getInputVariable() {
        return inputVariable;
    }

    public void setInputVariable(String inputVariable) {
        this.inputVariable = inputVariable;
    }

    public String getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    public boolean isSplitPatternRegEx() {
        return isSplitPatternRegEx;
    }

    public void setSplitPatternRegEx(boolean splitPatternRegEx) {
        isSplitPatternRegEx = splitPatternRegEx;
    }

    public boolean isIgnoreEmptyValues() {
        return isIgnoreEmptyValues;
    }

    public void setIgnoreEmptyValues(boolean ignoreEmptyValues) {
        isIgnoreEmptyValues = ignoreEmptyValues;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return inputVariable == null ? EMPTY_STRING : new String[] { inputVariable };
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return outputVariable == null ? EMPTY_VARIABLE_METADATA : new VariableMetadata[] {
                new VariableMetadata(outputVariable, false, true, outputVariable, true, DataType.UNKNOWN)
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SplitAssertion.class.getName() + ".metadataInitialized";

    private final static String baseName = "Split Variable";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SplitAssertion>(){
        @Override
        public String getAssertionName( final SplitAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return "Split variable " + assertion.getInputVariable() +
                    " into " + assertion.getOutputVariable() +
                    " on \"" + assertion.getSplitPattern() + "\" " +
                    ((assertion.isIgnoreEmptyValues())?" [Ignore empty values]":"");
        }
    };

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Split a single-valued context variable into a multi-valued context variable.");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/split16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Split Variable Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.splitjoin.console.SplitVariablePropertiesDialog");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/split16.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     * Class left in for backwards compatibility as this assertion is deployed in the field. The productized version
     * will not dismiss the dialog if the split pattern is invalid, however there may be policies with an invalid
     * split pattern so this validator warning should be kept.
     */
    public static class Validator implements AssertionValidator {
        private SplitAssertion assertion;
        private String message;

        public Validator(SplitAssertion assertion) {
            this.assertion = assertion;
            try {
                Pattern.compile(assertion.getSplitPattern());
                message = null;
            } catch (PatternSyntaxException e) {
                message = "Bad regex pattern: " + ExceptionUtils.getMessage(e);
            }
        }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            if (message != null)
                result.addWarning((new PolicyValidatorResult.Warning(assertion, message, null)));
        }
    }

}
