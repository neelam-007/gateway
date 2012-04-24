package com.l7tech.external.assertions.evaluatejsonpathexpression;

import com.l7tech.external.assertions.evaluatejsonpathexpression.server.EvaluateJsonPathExpressionAdminImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * A modular assertion to query JSON object using JSONPath expression.
 * </p>
 * <p>For more information on JSONPath please refer to <link>http://code.google.com/p/json-path/</link> and <link>http://goessner.net/articles/JsonPath/</link></p>
 */
public class EvaluateJsonPathExpressionAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {
    /**
     * The default variable prefix.
     */
    public static final String VARIABLE_PREFIX = "jsonPath";

    private static final String DEFAULT_EVALUATOR = "JsonPath";

    private static final String META_INITIALIZED = EvaluateJsonPathExpressionAssertion.class.getName() + ".metadataInitialized";
    private static final String SUFFIX_FOUND = "found";
    private static final String SUFFIX_COUNT = "count";
    private static final String SUFFIX_RESULT = "result";
    private static final String SUFFIX_RESULTS = "results";
    
    private static final String[] AVAILABLE_SUFFIXES = new String[]{
            SUFFIX_FOUND, SUFFIX_COUNT, SUFFIX_RESULT, SUFFIX_RESULTS
    };
    
    private static final List<String> SUPPORTED_EVALUATOR;
    static {
        List<String> l = new ArrayList<String>();
        l.add("JsonPath");
        SUPPORTED_EVALUATOR = Collections.unmodifiableList(l);
    }
    
    private String evaluator = DEFAULT_EVALUATOR;
    private String expression;
    private String variablePrefix = VARIABLE_PREFIX;
    
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Evaluate JSON Path Expression");
        meta.put(AssertionMetadata.LONG_NAME, "Evaluate JSON Path Expression");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/regex16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final ExtensionInterfaceBinding<EvaluateJsonPathExpressionAdmin> binding = new ExtensionInterfaceBinding<EvaluateJsonPathExpressionAdmin>(
                        EvaluateJsonPathExpressionAdmin.class,
                        null,
                        new EvaluateJsonPathExpressionAdminImpl());
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     *
     * @return the JSONPath expression.
     */
    public String getExpression() {
        return expression;
    }

    /**
     *
     * @param expression the JSONPath expression to use.
     */
    public void setExpression(final String expression) {
        this.expression = expression;
    }

    /**
     *
     * @return the target output variable prefix.
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }

    /**
     *
     * @param variablePrefix the target variable prefix to use.
     */
    public void setVariablePrefix(final String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    /**
     * 
     * @return the evaluator.
     */
    public String getEvaluator() {
        return evaluator;
    }

    /**
     * 
     * @param evaluator the evaluator to use.
     */
    public void setEvaluator(final String evaluator) {
        this.evaluator = evaluator;
    }

    /**
     *
     * @return an array of all the variable suffixes.
     */
    public static String[] getVariableSuffixes() {
        return AVAILABLE_SUFFIXES;
    }

    /**
     *
     * @return a list of supported evaluators.
     */
    public static List<String> getSupportedEvaluator(){
        return SUPPORTED_EVALUATOR;
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(getVariablePrefix() + "." + SUFFIX_FOUND),
                new VariableMetadata(getVariablePrefix() + "." + SUFFIX_COUNT),
                new VariableMetadata(getVariablePrefix() + "." + SUFFIX_RESULT),
                new VariableMetadata(getVariablePrefix() + "." + SUFFIX_RESULTS, false, true, null, false)
        );
    }

    @Override
    protected MessageTargetableAssertion.VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(getExpression());
    }

    @Override
    public EvaluateJsonPathExpressionAssertion clone(){
        EvaluateJsonPathExpressionAssertion copy = (EvaluateJsonPathExpressionAssertion) super.clone();
        copy.expression = expression;
        copy.variablePrefix = variablePrefix;
        return copy;
    }
}