package com.l7tech.external.assertions.lookupdynamiccontextvariables.server;

import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.util.Collections;
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
        String sourceVariable = assertion.getSourceVariable();
        if(sourceVariable == null || sourceVariable.isEmpty()){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_MISSING_SOURCE);
            return AssertionStatus.FAILED;
        }
        final String[] referencedNames = Syntax.getReferencedNames(sourceVariable);

        for(String s : referencedNames){
            final Map<String, Object> vars = Collections.unmodifiableMap(context.getVariableMap(new String[]{s}, getAudit()));
            if(vars.get(s) == null){
                logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_NOT_FOUND, s);
                return AssertionStatus.FAILED;
            }
            final String resolved = ExpandVariables.process(Syntax.getVariableExpression(s), vars, getAudit(), true);
            sourceVariable = sourceVariable.replaceAll(Syntax.REGEX_PREFIX + s + Syntax.REGEX_SUFFIX, resolved);
        }

        final Map<String, Object> vars = Collections.unmodifiableMap(context.getVariableMap(new String[]{sourceVariable}, getAudit()));
        if(vars.get(sourceVariable) == null){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_NOT_FOUND, sourceVariable);
            return AssertionStatus.FAILED;
        }
        final String result = ExpandVariables.process(Syntax.getVariableExpression(sourceVariable), vars, getAudit(), true);
        context.setVariable(assertion.getTargetOutputVariable(), result);
        return AssertionStatus.NONE;
    }

}
