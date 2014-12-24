package com.l7tech.external.assertions.pbkvs.server;

import com.l7tech.external.assertions.pbkvs.PolicyBackedKeyValueStoreAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.polback.KeyValueStore;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server side implementation of the PolicyBackedKeyValueStoreAssertion.
 *
 * @see com.l7tech.external.assertions.pbkvs.PolicyBackedKeyValueStoreAssertion
 */
public class ServerPolicyBackedKeyValueStoreAssertion extends AbstractServerAssertion<PolicyBackedKeyValueStoreAssertion> {
    private final String[] variablesUsed;

    @Inject
    private PolicyBackedServiceRegistry pbsreg;

    public ServerPolicyBackedKeyValueStoreAssertion( final PolicyBackedKeyValueStoreAssertion assertion ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        KeyValueStore kvs = pbsreg.getImplementationProxy( KeyValueStore.class, assertion.getPolicyBackedServiceGoid() );

        Map<String, Object> variableMap = context.getVariableMap( variablesUsed, getAudit() );
        String operation = assertion.getOperation();
        String key = assertion.getKey();

        switch ( operation ) {
            case PolicyBackedKeyValueStoreAssertion.OPERATION_GET:

                Object result = kvs.get( key );

                if ( result != null ) {
                    context.setVariable( assertion.getTargetVariableName(), result.toString() );
                } else {
                    return AssertionStatus.FAILED;
                }

                return AssertionStatus.NONE;

            case PolicyBackedKeyValueStoreAssertion.OPERATION_PUT:

                String value = ExpandVariables.process( assertion.getValue(), variableMap, getAudit() );

                kvs.put( key, value );

                return AssertionStatus.NONE;

            default:
                logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unrecognized operation: " + operation );
                break;
        }

        return AssertionStatus.FAILED;
    }
}
