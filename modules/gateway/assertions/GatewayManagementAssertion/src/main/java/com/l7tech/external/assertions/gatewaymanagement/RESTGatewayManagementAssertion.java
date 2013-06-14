package com.l7tech.external.assertions.gatewaymanagement;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.wsdl.Wsdl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
@SuppressWarnings({"serial"})
@ProcessesRequest
@RequiresSOAP
public class RESTGatewayManagementAssertion extends Assertion {

    //-PUBLIC
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
        meta.put(FEATURE_SET_NAME, "set:modularAssertions");      // todo  add to which feature set?
//        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.gatewaymanagement.console.GatewayManagementAssertionPropertiesDialog");
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.gatewaymanagement.server.GatewayManagementModuleLifecycle");
        meta.put(POLICY_VALIDATOR_CLASSNAME, RESTGatewayManagementAssertion.Validator.class.getName());
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = RESTGatewayManagementAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix;

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
