package com.l7tech.external.assertions.lookupdynamiccontextvariables.server;

import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.util.Map;

/**
 * Server side implementation of the LookupDynamicContextVariablesAssertion.
 *
 * @see com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion
 */
public class ServerLookupDynamicContextVariablesAssertion extends AbstractServerAssertion<LookupDynamicContextVariablesAssertion> {

    public ServerLookupDynamicContextVariablesAssertion( final LookupDynamicContextVariablesAssertion assertion ) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) {
        final String sourceVariable = assertion.getSourceVariable();
        if(sourceVariable == null || sourceVariable.trim().isEmpty()){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_MISSING_SOURCE);
            return AssertionStatus.FAILED;
        }
        final String targetVariable = assertion.getTargetOutputVariable();
        if(targetVariable == null || targetVariable.trim().isEmpty()){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_MISSING_TARGET);
            return AssertionStatus.FAILED;
        }
        try{
            //lookup the variable name
            final Map<String, Object> lookup = context.getVariableMap(Syntax.getReferencedNames(sourceVariable), getAudit());
            final String process = ExpandVariables.process(sourceVariable, lookup, getAudit());

            //retrieve the variable
            final Map<String, Object> actual = context.getVariableMap(new String[]{process}, getAudit());
            final Object o = ExpandVariables.processSingleVariableAsObject(Syntax.getVariableExpression(process), actual, getAudit());
            context.setVariable(targetVariable, o);
        }
        catch(VariableNameSyntaxException e){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_INVALID_SYNTAX , e.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

}
