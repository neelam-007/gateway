package com.l7tech.external.assertions.createroutingstrategy.server;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.Service;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
import java.util.*;

/**
 * Server side implementation of the CreateRoutingStrategyAssertion.
 *
 * @see com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion
 */
public class ServerCreateRoutingStrategyAssertion extends AbstractServerAssertion<CreateRoutingStrategyAssertion> {

    /**
     * Construct the ServerCreateRoutingStrategyAssertion.
     * Do not pre-build the service list and the fail over strategy.
     *
     * @param assertion  CreateRoutingStrategyAssertion
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     * @throws com.l7tech.gateway.common.LicenseException
     */
    public ServerCreateRoutingStrategyAssertion(final CreateRoutingStrategyAssertion assertion) throws PolicyAssertionException, LicenseException {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        AssertionStatus assertionStatus = AssertionStatus.NONE;
        Service[] routes = null;

        //Determine if there is any service defined as context variable.
        boolean hasDynamicService = assertion.getVariablesUsed().length > 0;
        //If no dynamic service, use the cached services and strategy which created during compile time.
        //we don't need to compile the service list if no service defined as context variable.
        if (hasDynamicService) {
            Map<String, Object> variableMap = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
            routes = expandRoutes(variableMap);
        } else {
            routes = assertion.getRoutes().toArray(new Service[assertion.getRoutes().size()]);
        }

        if(routes.length > 0) {
            context.setVariable(assertion.getRouteList(), routes);

            // Check the context whether it is due to service or other
            // Use default goid if context is not because of service execution.
            Goid serviceGoid = (context.getService() != null ? context.getService().getGoid() : Goid.DEFAULT_GOID);
            FailoverStrategy strategy = RoutingStrategyManager.getInstance().getStrategy(
                    serviceGoid, hasDynamicService, routes, assertion);

            context.setVariable(assertion.getStrategy(), strategy);
        } else {
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_CRS_NO_ROUTES);
            assertionStatus = AssertionStatus.FALSIFIED;
        }

        return assertionStatus;
    }


    /**
     * Expand the service list. Process static variable, context variable and multi-valued variable.
     * and concat all values to the service list.
     *
     * @param variableMap Variables to retrieve
     * @return The service list to process for the failover strategy.
     */
    private Service[] expandRoutes(Map<String, Object> variableMap) {

        List<Service> result = new ArrayList<Service>();
        List<Service> routesVar = assertion.getRoutes();
        for (Service routeVar : routesVar) {
            List<Object> routes = ExpandVariables.processNoFormat(routeVar.getName(), variableMap, getAudit(), false);
            for (Object route : routes) {
                String routeName = ((String) route).trim();
                if (routeName.length() > 0) {
                    result.add(new Service(routeName, routeVar.getProperties()));
                }
            }
        }
        return (Service[]) result.toArray(new Service[result.size()]);
    }
}
