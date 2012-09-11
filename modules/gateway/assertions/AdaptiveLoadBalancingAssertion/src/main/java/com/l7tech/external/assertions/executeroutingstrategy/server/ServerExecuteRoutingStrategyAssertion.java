package com.l7tech.external.assertions.executeroutingstrategy.server;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.Feedback;
import com.l7tech.common.io.failover.Service;
import com.l7tech.external.assertions.executeroutingstrategy.ExecuteRoutingStrategyAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the ExecuteRoutingStrategyAssertion.
 *
 * @see com.l7tech.external.assertions.executeroutingstrategy.ExecuteRoutingStrategyAssertion
 */
public class ServerExecuteRoutingStrategyAssertion extends AbstractServerAssertion<ExecuteRoutingStrategyAssertion> {

    private final String[] varsUsed;

    public ServerExecuteRoutingStrategyAssertion( final ExecuteRoutingStrategyAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        varsUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {

        Map<String, Object> varMap = context.getVariableMap(varsUsed, getAudit());

        if(!varMap.containsKey(assertion.getStrategy()) || !(varMap.get(assertion.getStrategy()) instanceof FailoverStrategy)){
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_VAR_NOT_FOUND, assertion.getStrategy());
            return AssertionStatus.FALSIFIED;
        }

        FailoverStrategy<Service> fs = (FailoverStrategy) varMap.get(assertion.getStrategy());
        Service route = fs.selectService();
        if (route == null) {
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_NO_ROUTE,assertion.getStrategy());
            return AssertionStatus.FALSIFIED;
        }
        //set route and feedback route
        context.setVariable(assertion.getRoute(), route.getName());
        context.setVariable(assertion.getFeedbackRoute(), route);
        //set empty feedback if it does not exist
        setFeedback(context);

        return AssertionStatus.NONE;
    }

    /**
     * sets empty feedback list if can't find or retruns existing one otherwise
     * @param context  policy enforcement context
     * @return  list of feedbacks
     */
    private void setFeedback(PolicyEnforcementContext context) {
        Map<String, Object> varMap = context.getVariableMap(new String[]{assertion.getFeedback()}, getAudit());
        if(varMap.isEmpty() || !varMap.get(assertion.getFeedback()).getClass().isAssignableFrom(new ArrayList<Feedback>().getClass())){
            context.setVariable(assertion.getFeedback(), new ArrayList<Feedback>());
        }
    }

}
