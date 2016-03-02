package com.l7tech.external.assertions.createroutingstrategy.server;

import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.Service;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Routing Strategy Manager.
 */
public class RoutingStrategyManager {

    private static RoutingStrategyManager INSTANCE;

    /**
     * Failover Strategy cache
     * Key = Service GOID.
     * Value = Map, where key = Strategy prefix and value = StrategyPair.
     */
    // TODO: Add support for multiple FailoverStrategy with the SAME strategy prefix in same service.
    private final ConcurrentMap<Goid, ConcurrentMap<String, StrategyPair>> cachedStrategies;
    private final FailoverStrategyFactory failoverStrategyFactory;

    public static void createInstance(@NotNull ApplicationContext context) {
        if (INSTANCE != null) {
            throw new IllegalStateException( "RoutingStrategyManager has already been initialized" );
        }
        INSTANCE = new RoutingStrategyManager(context);
    }

    @NotNull
    public static RoutingStrategyManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException( "RoutingStrategyManager has not been initialized" );
        }
        return INSTANCE;
    }

    /**
     * Retrieve the Failover Strategy. Determine to return the cached strategy or construct a new FailoverStrategy.
     *
     * @param serviceGoid the service GOID.
     * @param hasDynamicService indicates if routes have dynamic services.
     * @param routes the service list for the Failover strategy.
     * @param assertion the assertion.
     * @return Failover Strategy.
     */
    @NotNull
    public FailoverStrategy<Service> getStrategy(@NotNull Goid serviceGoid, boolean hasDynamicService, @NotNull Service[] routes, @NotNull CreateRoutingStrategyAssertion assertion) {
        ConcurrentMap<String, StrategyPair> innerMap = cachedStrategies.get(serviceGoid);
        if (innerMap == null) {
            innerMap = new ConcurrentHashMap<>();
            ConcurrentMap<String, StrategyPair> previous = cachedStrategies.putIfAbsent(serviceGoid, innerMap);
            if (previous != null) {
                innerMap = previous; // Someone else beat us to it, use theirs
            }
        }

        String prefix = assertion.getStrategy();
        String strategyName = assertion.getStrategyName();
        Map<String, String> strategyProperties = assertion.getStrategyProperties();

        StrategyPair strategyPair = innerMap.get(prefix);
        if (strategyPair == null) {
            FailoverStrategy<Service> strategy = createStrategy(strategyName, routes, strategyProperties);
            strategyPair = new StrategyPair(routes, strategy);
            StrategyPair previous = innerMap.putIfAbsent(prefix, strategyPair);
            if (previous != null) {
                strategyPair = previous; // Someone else beat us to it, use theirs
            }
        }

        if (hasDynamicService && !Arrays.equals(strategyPair.getRoutes(), routes)) {
            // If the cached service list != the service list, construct a new Strategy.
            FailoverStrategy<Service> strategy = createStrategy(strategyName, routes, strategyProperties);
            strategyPair = new StrategyPair(routes, strategy);
            innerMap.put(prefix, strategyPair); // Put, instead of putIfAbsent.
        }

        return strategyPair.getStrategy();
    }

    @NotNull
    private FailoverStrategy<Service> createStrategy(@NotNull String strategyName, @NotNull Service[] routes, @NotNull Map<String, String> strategyProperties) {
        return AbstractFailoverStrategy.makeSynchronized(
                failoverStrategyFactory.createFailoverStrategy(strategyName, routes, strategyProperties));
    }

    private RoutingStrategyManager(@NotNull ApplicationContext context) {
        failoverStrategyFactory = (FailoverStrategyFactory) context.getBean("failoverStrategyFactory");
        cachedStrategies = new ConcurrentHashMap<>();

        ApplicationEventProxy appEventProxy = (ApplicationEventProxy) context.getBean("applicationEventProxy");
        appEventProxy.addApplicationListener(
                new ApplicationListener() {
                    @Override
                    public void onApplicationEvent(ApplicationEvent event) {
                        if (event instanceof EntityInvalidationEvent) {
                            EntityInvalidationEvent eiEvent = (EntityInvalidationEvent) event;
                            if (PublishedService.class.equals(eiEvent.getEntityClass())) {
                                Goid[] ids = eiEvent.getEntityIds();
                                char[] ops = eiEvent.getEntityOperations();
                                for (int ix = 0; ix < ids.length; ix++) {
                                    if (EntityInvalidationEvent.UPDATE == ops[ix] ||
                                        EntityInvalidationEvent.DELETE == ops[ix]) {
                                        // Remove from the cache.
                                        cachedStrategies.remove(ids[ix]);
                                    }
                                }
                            }
                        }
                    }
                }
        );
    }

    /**
     * Pair containing service list and fail over strategy.
     */
    private static class StrategyPair extends Pair<Service[], FailoverStrategy<Service>> {

        private StrategyPair(@NotNull Service[] routes, @NotNull FailoverStrategy<Service> strategy) {
            super(routes, strategy);
        }

        @NotNull
        private Service[] getRoutes() {
            return left;
        }

        @NotNull
        private FailoverStrategy<Service> getStrategy() {
            return right;
        }
    }
}
