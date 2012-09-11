package com.l7tech.external.assertions.executeroutingstrategy;

import com.l7tech.external.assertions.adaptiveloadbalancing.AbstractAdaptiveLoadBalancing;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;

/**
 * 
 */
public class ExecuteRoutingStrategyAssertion extends AbstractAdaptiveLoadBalancing implements UsesVariables, SetsVariables {

    private static final String baseName = "Execute Routing Strategy";

    public static final String DEFAULT_ROUTE = "route";
    public static final String DEFAULT_FEEDBACK = "feedback";
    public static final String CURRENT_FEEDBACK = ".current";

    public static final String FEEDBACK_ROUTE = ".route";

    private String route = DEFAULT_ROUTE;
    private String feedback = DEFAULT_FEEDBACK;


    /**
     * The route variable name to store the route
     * @return
     */
    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * The feedback variable name to store the feedback result list
     * @return
     */
    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public String getFeedbackRoute() {
        return getFeedback() + ExecuteRoutingStrategyAssertion.CURRENT_FEEDBACK + ExecuteRoutingStrategyAssertion.FEEDBACK_ROUTE;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(route, false, false, null, false, DataType.STRING),
                new VariableMetadata(feedback, false, false, null, false, DataType.UNKNOWN),
                new VariableMetadata(feedback + CURRENT_FEEDBACK  , true, false, null, false, DataType.STRING),
                new VariableMetadata(feedback + CURRENT_FEEDBACK + FEEDBACK_ROUTE  , false, false, null, false, DataType.STRING),
        };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return new String[]{getStrategy()};
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = ExecuteRoutingStrategyAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Execute Routing Strategy Assertion");
        meta.put(AssertionMetadata.DESCRIPTION, "Retrieve the Routing Strategy from defined Routing Strategy Prefix, acquire available route destination, and set the route destination to Output Variable Name.");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.executeroutingstrategy.console.ExecuteRoutingStrategyAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Execute Routing Strategy Properties");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final int MAX_DISPLAY_LENGTH = 80;

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ExecuteRoutingStrategyAssertion>(){
        @Override
        public String getAssertionName( final ExecuteRoutingStrategyAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer name = new StringBuffer(baseName + " as ");
            name.append("${");
            name.append(assertion.getStrategy());
            name.append("}");
            if(name.length() > MAX_DISPLAY_LENGTH) {
                name = name.replace(MAX_DISPLAY_LENGTH - 1, name.length() - 1, "...");
            }
            return name.toString();
        }
    };

    public static class Validator extends AbstractAdaptiveLoadBalancing.AbstractValidator {

        public Validator(AbstractAdaptiveLoadBalancing assertion) {
            super(assertion);
        }

        @Override
        public boolean checkDependency(Assertion a) {
            return a.getClass().isAssignableFrom(CreateRoutingStrategyAssertion.class);
        }

        @Override
        public String getValidationErrorMsg() {
            return "Must be preceded by a Create Routing Strategy assertion";
        }

    }
}
