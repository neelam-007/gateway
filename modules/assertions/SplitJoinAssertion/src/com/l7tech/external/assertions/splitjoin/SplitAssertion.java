package com.l7tech.external.assertions.splitjoin;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This assertion splits a single-valued String context variable into a multi-valued List&lt;String&gt; by
 * splitting on the specified substring.
 */
public class SplitAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(SplitAssertion.class.getName());
    private static final String[] EMPTY_STRING = new String[0];
    private static final VariableMetadata[] EMPTY_VARIABLE_METADATA = new VariableMetadata[0];

    private String splitPattern = ",";
    private String inputVariable;
    private String outputVariable;

    /** @return the regex pattern to split on. */
    public String getSplitPattern() {
        return splitPattern;
    }

    /** @param splitPattern  a regex pattern to split on. */
    public void setSplitPattern(String splitPattern) {
        this.splitPattern = splitPattern;
    }

    /** @return name of variable to split. */
    public String getInputVariable() {
        return inputVariable;
    }

    /** @param inputVariable name of variable to split. */
    public void setInputVariable(String inputVariable) {
        this.inputVariable = inputVariable;
    }

    /** @return name of variable in which to store the result. May be the same as inputVariable. */
    public String getOutputVariable() {
        return outputVariable;
    }

    /** @param outputVariable name of variable in which to store the result.  May be the same as outputVariable. */
    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    public String[] getVariablesUsed() {
        return inputVariable == null ? EMPTY_STRING : new String[] { inputVariable };
    }

    public VariableMetadata[] getVariablesSet() {
        return outputVariable == null ? EMPTY_VARIABLE_METADATA : new VariableMetadata[] {
                new VariableMetadata(outputVariable, false, true, outputVariable, true, DataType.UNKNOWN)
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SplitAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Split Variable");
        meta.put(AssertionMetadata.LONG_NAME, "Split a single-valued context variable into a multi-valued context variable.");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, SplitAssertion>() {
            public String call(SplitAssertion assertion) {
                return "Split variable " + assertion.getInputVariable() + " into " + assertion.getOutputVariable() + " on " + assertion.getSplitPattern();
            }
        });

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/resources/split16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/resources/split16.gif");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

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

        public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
            if (message != null)
                result.addWarning((new PolicyValidatorResult.Warning(assertion, path, message, null)));
        }
    }
}
