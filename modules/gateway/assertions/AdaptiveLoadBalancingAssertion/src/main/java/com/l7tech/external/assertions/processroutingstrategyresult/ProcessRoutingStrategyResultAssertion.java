package com.l7tech.external.assertions.processroutingstrategyresult;

import com.l7tech.external.assertions.adaptiveloadbalancing.AbstractAdaptiveLoadBalancing;
import com.l7tech.external.assertions.executeroutingstrategy.ExecuteRoutingStrategyAssertion;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;

import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;

/**
 *
 */
public class ProcessRoutingStrategyResultAssertion extends AbstractAdaptiveLoadBalancing implements UsesVariables {

    protected static final Logger logger = Logger.getLogger(ProcessRoutingStrategyResultAssertion.class.getName());
    private static final String baseName = "Process Routing Strategy Result";
    public static final String DEFAULT_FEEDBACK = "feedback";
    public static final String CURRENT_FEEDBACK = ".current";
    public static final String FEEDBACK_ROUTE = ".route";
    public static final String FEEDBACK_LATENCY = ".latency";
    public static final String FEEDBACK_STATUS = ".status";
    public static final String FEEDBACK_REASON_CODE = ".reasonCode";
    private String feedback = DEFAULT_FEEDBACK;

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public String getFeedbackRoute() {
        return this.feedback + CURRENT_FEEDBACK + FEEDBACK_ROUTE;
    }

    public String getFeedbackLatency() {
        return this.feedback + CURRENT_FEEDBACK + FEEDBACK_LATENCY;
    }

    public String getFeedbackStatus() {
        return this.feedback + CURRENT_FEEDBACK + FEEDBACK_STATUS;
    }

    public String getReasonCode() {
        return this.feedback + CURRENT_FEEDBACK + FEEDBACK_REASON_CODE;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return new String[]{getStrategy(), getFeedback(), getFeedbackRoute(), getFeedbackLatency(), getFeedbackStatus(), getReasonCode()};
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = ProcessRoutingStrategyResultAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Process Routing Strategy Result Assertion");
        meta.put(AssertionMetadata.DESCRIPTION, "Updates the Routing Strategy with success/failure status and generates feedback data.");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.processroutingstrategyresult.console.ProcessRoutingStrategyResultAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Process Routing Strategy Result Properties");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(POLICY_VALIDATOR_CLASSNAME, Validator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final int MAX_DISPLAY_LENGTH = 60;


    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ProcessRoutingStrategyResultAssertion>(){
        @Override
        public String getAssertionName( final ProcessRoutingStrategyResultAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer name = new StringBuffer(baseName + " for ");
            name.append("${");
            name.append(assertion.getStrategy());
            name.append("}");
            name.append(" as ");
            name.append("${");
            name.append(assertion.getFeedback());
            name.append("}");
            if(name.length() > MAX_DISPLAY_LENGTH) {
                name = name.replace(MAX_DISPLAY_LENGTH - 1, name.length() - 1, "...");
            }
            return name.toString();
        }
    };

    public static class Validator extends AbstractAdaptiveLoadBalancing.AbstractValidator {

        public Validator(ProcessRoutingStrategyResultAssertion assertion) {
            super(assertion);
        }

        @Override
        public boolean checkDependency(Assertion a) {
            return a.getClass().isAssignableFrom(ExecuteRoutingStrategyAssertion.class);
        }

        @Override
        public String getValidationErrorMsg() {
            return "Must be preceded by a Execute Routing Strategy assertion";
        }

        @Override
        protected void checkVariables(PolicyValidatorResult result, AbstractAdaptiveLoadBalancing a) {

            if(!assertion.getClass().isAssignableFrom(ProcessRoutingStrategyResultAssertion.class) ||
               !a.getClass().isAssignableFrom(ExecuteRoutingStrategyAssertion.class)){
                result.addError(new PolicyValidatorResult.Error(assertion, "Assertion is not the right type", null));
                return;
            }

            final String feedback = ((ProcessRoutingStrategyResultAssertion) assertion).getFeedback();

            ExecuteRoutingStrategyAssertion preceedingAssertion = (ExecuteRoutingStrategyAssertion)a;
            if(!feedback.equalsIgnoreCase(preceedingAssertion.getFeedback())){
              result.addWarning(new PolicyValidatorResult.Warning(assertion, feedback + " variable not found", null));
           }
        }

    }
}
