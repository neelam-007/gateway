package com.l7tech.external.assertions.gatewaymanagement;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
@SuppressWarnings({"serial"})
@ProcessesRequest
public class RESTGatewayManagementAssertion extends MessageTargetableAssertion {

    private static final String META_INITIALIZED = RESTGatewayManagementAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix = "restGatewayMan";

    public static final String SUFFIX_ACTION = "action";
    public static final String SUFFIX_URI = "uri";
    public static final String SUFFIX_STATUS = "status";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "REST Manage Gateway");
        meta.put(DESCRIPTION, "Manage the Gateway with a RESTful interface." );
        meta.put(PROPERTIES_ACTION_NAME, "REST Gateway Management Properties");
        meta.put(PALETTE_FOLDERS, new String[] { "internalAssertions" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.gatewaymanagement.server.GatewayManagementModuleLifecycle");
        meta.put(POLICY_VALIDATOR_CLASSNAME, RESTGatewayManagementAssertion.Validator.class.getName());
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);
        meta.putNull(PROPERTIES_ACTION_FACTORY);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        if( getTarget().equals(TargetMessageType.OTHER)){
            return super.doGetVariablesUsed().withVariables(
                variablePrefix+"."+SUFFIX_ACTION,
                variablePrefix+"."+SUFFIX_URI);
        }
        return super.doGetVariablesUsed();
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        final VariableMetadata[] varsSet = new VariableMetadata[] {
                new VariableMetadata(variablePrefix+"."+SUFFIX_STATUS, false, false, null, true)
        };
        return super.doGetVariablesSet().withVariables(varsSet);
    }

    public static class Validator implements AssertionValidator {
        private RESTGatewayManagementAssertion assertion;

        public Validator( final RESTGatewayManagementAssertion assertion ) {
            this.assertion = assertion;
        }

        @Override
        public void validate( final AssertionPath path,
                              final PolicyValidationContext pvc,
                              final PolicyValidatorResult result ) {

            boolean seenIdentity = false;
            for ( final Assertion assertion : path.getPath() ) {
                if ( assertion == this.assertion ) {
                    break;
                }

                if ( !assertion.isEnabled() || !Assertion.isRequest(assertion) ) {
                    continue;
                }

                if ( assertion instanceof IdentityAssertion ) {
                    seenIdentity = true;
                    break;
                }
            }

            if ( !seenIdentity ) {
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                        "An authentication assertion should precede this assertion, anonymous users have no access permissions.",
                                   null));
            }
        }
    }
}
