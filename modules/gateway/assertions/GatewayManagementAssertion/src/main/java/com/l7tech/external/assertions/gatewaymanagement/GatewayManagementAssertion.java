package com.l7tech.external.assertions.gatewaymanagement;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
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
public class GatewayManagementAssertion extends Assertion implements SetsVariables {

    //-PUBLIC

    public static final String SUFFIX_ACTION = "action";
    public static final String SUFFIX_ENTITY_TYPE = "entityType";
    public static final String SUFFIX_ENTITY_ID = "entityId";
    public static final String SUFFIX_MESSAGE = "message";
    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection( Arrays.asList(
        SUFFIX_ACTION,
        SUFFIX_ENTITY_TYPE,
        SUFFIX_ENTITY_ID,
        SUFFIX_MESSAGE    
    ) );

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] metadata;

        if ( variablePrefix == null ) {
            metadata = new VariableMetadata[0];
        } else {
            metadata = new VariableMetadata[] {
                new VariableMetadata(variablePrefix+"."+SUFFIX_ACTION, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ENTITY_TYPE, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ENTITY_ID, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_MESSAGE, false, false, null, false, DataType.STRING),
            };
        }

        return metadata;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Manage Gateway");
        meta.put(DESCRIPTION, "Manage the Gateway." );
        meta.put(PROPERTIES_ACTION_NAME, "Gateway Management Properties");
        meta.put(PALETTE_FOLDERS, new String[] { "internalAssertions" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.gatewaymanagement.console.GatewayManagementAssertionPropertiesDialog");
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.gatewaymanagement.server.GatewayManagementModuleLifecycle");
        meta.put(POLICY_VALIDATOR_CLASSNAME, GatewayManagementAssertion.Validator.class.getName());
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = GatewayManagementAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix;

    public static class Validator implements AssertionValidator {
        private GatewayManagementAssertion assertion;

        public Validator( final GatewayManagementAssertion assertion ) {
            this.assertion = assertion;
        }

        @Override
        public void validate( final AssertionPath path,
                              final PolicyValidationContext pvc,
                              final PolicyValidatorResult result ) {
            final Wsdl wsdl = pvc.getWsdl();
            if ( (wsdl == null) || (!"http://ns.l7tech.com/2010/04/gateway-management".equals(wsdl.getTargetNamespace())) ){
                result.addWarning(new PolicyValidatorResult.Warning(
                                   assertion,
                                   path,
                                   "Assertion is for use only with a Gateway Management Service",
                                   null));
            }

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
                                   path,
                                   "An authentication assertion should precede this assertion, anonymous users have no access permissions.",
                                   null));
            }
        }
    }
}
