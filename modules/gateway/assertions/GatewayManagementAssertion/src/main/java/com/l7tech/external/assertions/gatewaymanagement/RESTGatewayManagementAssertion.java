package com.l7tech.external.assertions.gatewaymanagement;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
@SuppressWarnings({"serial"})
@ProcessesRequest
@RequiresSOAP
public class RESTGatewayManagementAssertion extends Assertion implements MessageTargetable{

    //-PUBLIC
    public static final String SUFFIX_ACTION = "action";
    public static final String SUFFIX_URI = "uri";
    public static final String SUFFIX_BODY = "body";
    public static final String SUFFIX_BASE_URI = "baseuri";

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }


    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(
                variablePrefix+"."+SUFFIX_ACTION,
                variablePrefix+"."+SUFFIX_URI,
                variablePrefix+"."+SUFFIX_BODY);
    }

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
        meta.put(FEATURE_SET_NAME, "set:modularAssertions");      // todo  new feature set
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.gatewaymanagement.server.GatewayManagementModuleLifecycle");
        meta.put(POLICY_VALIDATOR_CLASSNAME, RESTGatewayManagementAssertion.Validator.class.getName());
        meta.put(IS_ROUTING_ASSERTION, Boolean.TRUE);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = RESTGatewayManagementAssertion.class.getName() + ".metadataInitialized";

    private String variablePrefix = "RestGatewayMan";
    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport(false);

    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherMessageVariable);
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return messageTargetableSupport.isTargetModifiedByGateway();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[0];
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
