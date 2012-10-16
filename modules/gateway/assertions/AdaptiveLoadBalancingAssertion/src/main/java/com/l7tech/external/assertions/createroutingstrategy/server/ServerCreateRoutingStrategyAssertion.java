package com.l7tech.external.assertions.createroutingstrategy.server;

import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.Service;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the CreateRoutingStrategyAssertion.
 *
 * @see com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion
 */
public class ServerCreateRoutingStrategyAssertion extends AbstractServerAssertion<CreateRoutingStrategyAssertion> {

    /**
     * Failover Strategy cache
     */
    private FailoverStrategy<Service> cachedStrategy;
    private Service[] cachedRoutes;
    private FailoverStrategyFactory failoverStrategyFactory;

    /**
     * True when using context variable in the service list. False if no context variable is defined in the service list.
     */
    private boolean hasDynamicService = false;

    /**
     * Construct the ServerCreateRoutingStrategyAssertion, determine if any context variable is defined for the service list,
     * pre-build the service list and the fail over strategy.
     *
     * @param assertion  CreateRoutingStrategyAssertion
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     * @throws com.l7tech.gateway.common.LicenseException
     */
    public ServerCreateRoutingStrategyAssertion(final CreateRoutingStrategyAssertion assertion, ApplicationContext context ) throws PolicyAssertionException, LicenseException {

        super(assertion);
        this.failoverStrategyFactory = (FailoverStrategyFactory) context.getBean("failoverStrategyFactory");
        //Determine if there is any service defined as context variable.
        Service[] services = assertion.getRoutes().toArray(new Service[assertion.getRoutes().size()]);
        String[] serviceNames = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            Service service = services[i];
            serviceNames[i] = service.getName();
        }
        String[] result = Syntax.getReferencedNames(serviceNames);
        if (result.length > 0) {
            hasDynamicService = true;
        } else {
            hasDynamicService = false;
            cachedRoutes = services;
            cachedStrategy = createStrategy(services) ;
        }

    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        AssertionStatus assertionStatus = AssertionStatus.NONE;
        Service[] routes = null;

        //If no dynamic service, use the cached services and strategy which created during compile time.
        //we don't need to compile the service list if no service defined as context variable.
        if (hasDynamicService) {
            Map<String, Object> variableMap = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
            routes = expandRoutes(variableMap);
        } else {
            routes = cachedRoutes;
        }

        if(routes.length > 0) {
            context.setVariable(assertion.getRouteList(), routes);
            context.setVariable(assertion.getStrategy(), getStrategy(routes));
        }
        else {
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

    /**
     * Retrieve the Failover Strategy. Determine to return the cached strategy or construct a new FailoverStrategy.
     *
     * @param routes the service list for the Failover strategy.
     * @return Failover Strategy.
     */
    private synchronized FailoverStrategy<Service> getStrategy(Service[] routes) {

        if (hasDynamicService) {
            //if the cached Servers list != the service list, construct a new Strategy
            if (Arrays.equals(cachedRoutes, routes)) {
                return cachedStrategy;
            } else {
                FailoverStrategy<Service> strategy = createStrategy(routes);
                cachedRoutes = routes;
                cachedStrategy = strategy;
                return strategy;
            }
        } else {
            return cachedStrategy;
        }
    }

    private FailoverStrategy<Service> createStrategy(Service[] routes) {
        return AbstractFailoverStrategy.makeSynchronized(
                failoverStrategyFactory.createFailoverStrategy(assertion.getStrategyName(),
                        routes, assertion.getStrategyProperties()));
    }
}
