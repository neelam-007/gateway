package com.l7tech.server.ems.gateway;

import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.Feedback;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Implementation of {@link GatewayClusterClient}.
 */
class GatewayClusterClientImpl implements GatewayClusterClient {
    private static final String PROP_CACHE_TIMEOUT = "com.l7tech.server.ems.gateway.clusterclient.cacheTimeout";
    private static final String PROP_FAILOVER_STRATEGY_NAME = "com.l7tech.server.ems.gateway.clusterclient.failoverStrategyName";

    private static final Long DEFAULT_CACHE_TIMEOUT = 15 * 1000L; // 15 sec
    private static final String DEFAULT_FAILOVER_STRATEGY_NAME = FailoverStrategyFactory.STICKY.getName();

    private final SsgCluster ssgCluster;
    private final FailoverStrategy<GatewayContext> failover;
    private final int numContexts;
    private final long timeout;

    // Cached data
    private final Object cacheLock = new Object();
    private final Cached<Collection<GatewayApi.EntityInfo>> entityInfos = new Cached<Collection<GatewayApi.EntityInfo>>();
    private final Cached<GatewayApi.ClusterInfo> clusterInfo = new Cached<GatewayApi.ClusterInfo>();
    private final Cached<Collection<GatewayApi.GatewayInfo>> gatewayInfo = new Cached<Collection<GatewayApi.GatewayInfo>>();

    GatewayClusterClientImpl(SsgCluster ssgCluster, List<GatewayContext> nodeContexts) {
        if (ssgCluster == null) throw new NullPointerException("ssgCluster");
        if (nodeContexts == null) throw new NullPointerException("nodeContexts");
        this.ssgCluster = ssgCluster;
        this.numContexts = nodeContexts.size();

        String failname = ConfigFactory.getProperty( PROP_FAILOVER_STRATEGY_NAME, DEFAULT_FAILOVER_STRATEGY_NAME );
        final FailoverStrategy<GatewayContext> failstrat = numContexts>0 ?
                FailoverStrategyFactory.createFailoverStrategy(failname, nodeContexts.toArray(new GatewayContext[numContexts])) :
                new FailedFailoverStrategy();
        this.failover = AbstractFailoverStrategy.makeSynchronized(failstrat);

        this.timeout = ConfigFactory.getLongProperty( PROP_CACHE_TIMEOUT, DEFAULT_CACHE_TIMEOUT );
    }

    @Override
    public SsgCluster getCluster() {
        return ssgCluster;
    }

    @Override
    public void clearCachedData() {
        synchronized (cacheLock) {
            entityInfos.clear();
            clusterInfo.clear();
            gatewayInfo.clear();
        }
    }

    @Override
    public Collection<GatewayApi.EntityInfo> getEntityInfo(final Collection<EntityType> entityTypes) throws GatewayException {
        if (entityTypes == null) throw new NullPointerException();

        Collection<GatewayApi.EntityInfo> allInfos = cacheGetWithRethrow(entityInfos, new GatewayContextUser<Collection<GatewayApi.EntityInfo>>() {
            @Override
            public Collection<GatewayApi.EntityInfo> callUsingContext(GatewayContext context) throws InvocationTargetException {
                try {
                    return context.getApi().getEntityInfo(null);
                } catch (GatewayApi.GatewayException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });

        if (allInfos == null)
            return Collections.emptyList();

        final Set<EntityType> types = entityTypes instanceof Set ? (Set<EntityType>)entityTypes : EnumSet.copyOf(entityTypes);

        return Functions.grep(allInfos, new Functions.Unary<Boolean, GatewayApi.EntityInfo>() {
            @Override
            public Boolean call(GatewayApi.EntityInfo entityInfo) {
                return entityInfo != null && types.contains(entityInfo.getEntityType());
            }
        });
    }

    @Override
    public GatewayApi.ClusterInfo getClusterInfo() throws GatewayException {
        return cacheGetWithRethrow(clusterInfo, new GatewayContextUser<GatewayApi.ClusterInfo>() {
            @Override
            public GatewayApi.ClusterInfo callUsingContext(GatewayContext context) throws SOAPFaultException {
                return context.getApi().getClusterInfo();
            }
        });
    }

    @Override
    public Collection<GatewayApi.GatewayInfo> getGatewayInfo() throws GatewayException {
        return cacheGetWithRethrow(gatewayInfo, new GatewayContextUser<Collection<GatewayApi.GatewayInfo>>() {
            @Override
            public Collection<GatewayApi.GatewayInfo> callUsingContext(GatewayContext context) throws SOAPFaultException {
                return context.getApi().getGatewayInfo();
            }
        });
    }

    @Override
    public GatewayApi getUncachedGatewayApi() {
        return addFailover(GatewayApi.class, new Functions.Unary<GatewayApi, GatewayContext>() {
            @Override
            public GatewayApi call(GatewayContext context) {
                return context.getApi();
            }
        });
    }

    @Override
    public ReportApi getUncachedReportApi() {
        return addFailover(ReportApi.class, new Functions.Unary<ReportApi, GatewayContext>() {
            @Override
            public ReportApi call(GatewayContext context) {
                return context.getReportApi();
            }
        });
    }

    @Override
    public MigrationApi getUncachedMigrationApi() {
        return addFailover(MigrationApi.class, new Functions.Unary<MigrationApi, GatewayContext>() {
            @Override
            public MigrationApi call(GatewayContext context) {
                return context.getMigrationApi();
            }
        });
    }

    /**
     * Add transparent failover to the specified API from GatewayContext.
     *
     * @param interfaceClass the API class to wrap.  Required.
     * @param apiFinder a generator that will select the correct API given a GatewayContext.  Required.
     * @return a proxy for the API that will use the configured failover strategy to select a different cluster
     *         node to talk to if the one it is talking to goes down.
     */
    @SuppressWarnings({ "unchecked" })
    private <IT> IT addFailover(Class<IT> interfaceClass, final Functions.Unary<IT, GatewayContext> apiFinder) {
        return (IT) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { interfaceClass }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                try {
                    return callWithFailover(new GatewayContextUser<Object>() {
                        @Override
                        public Object callUsingContext(GatewayContext context) throws SOAPFaultException, InvocationTargetException {
                            IT delegate = apiFinder.call(context);
                            try {
                                return method.invoke(delegate, args);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } catch ( InvocationTargetException ite ) {
                    throw ite.getTargetException();                
                }
            }
        });
    }

    /** Encapsulates a method of an API provided by GatewayContext, and some arguments to pass the method, that should be called with failover. */
    public interface GatewayContextUser<R> {
        /**
         * Do something with the specified GatewayContext that either produces a result of type R, or fails
         * with one of the supported checked exceptions, or fails by throwing SOAPFaultException.
         * <p/>
         * Implementors could, for example, call getApi().getGatewayInfo() using the specified context.
         *
         * @param context a GatewayContext representing a particular remote call target.  Required.
         * @return the result of doing something with context.  May be null.
         * @throws SOAPFaultException if there is a problem at the SOAP layer.
         * @throws java.lang.reflect.InvocationTargetException if the API method throws a checked exception
         */
        R callUsingContext(GatewayContext context) throws SOAPFaultException, InvocationTargetException;
    }

    /**
     * Invoke remote calls using GatewayContext instances obtained from the specified
     * failover strategy, making up to numAttempts attempts to do so.
     * <P/>
     * This method does no caching; it will always make at least one attempt to invoke the contextUser
     * unless failover immediately returns null.
     *
     * @param failover  a FailoverStrategy that will decide the order in which to try the GatewayContext instances.  Required.
     * @param numAttempts the number of times to attempt to invoke the contextUser before giving up.  Required.
     * @param contextUser a snippet of code that, when given a GatewayContext, will invoke one or more remote methods using it and return a result of type R (or throw an exception).  Required.
     * @return the result returned from contextUser.  May be null if the contextUser can return null.
     * @throws GatewayException if any invocation failed with an exception other than a network related exception;
     *                          if numAttempts invocation attempts failed with a network related exception; or,
     *                          if the failover strategy signalled to give up by returning null from {@link FailoverStrategy#selectService()}.
     * @throws java.lang.reflect.InvocationTargetException if the API method throws a checked exception
     */
    public static <R> R callWithFailover(FailoverStrategy<GatewayContext> failover, int numAttempts, GatewayContextUser<R> contextUser) throws GatewayException, InvocationTargetException {
        WebServiceException lastNetworkException = null;

        for (int i = 0; i < numAttempts; ++i) {
            GatewayContext context = failover.selectService();
            if (context == null)
                break;

            boolean success = false;

            try {
                R result = contextUser.callUsingContext(context);
                success = true;
                return result;
            } catch (WebServiceException e) {
                if (GatewayContext.isNetworkException(e)) {
                    lastNetworkException = e;
                } else {
                    throw new GatewayException(e.getMessage(), e);
                }
            } finally {
                if (success)
                    failover.reportSuccess(context);
                else
                    failover.reportFailure(context);
            }
        }

        throw new FailoverException("Unable to find any working cluster node to talk to: " + ExceptionUtils.getMessage(lastNetworkException), lastNetworkException);
    }

    private <R> R callWithFailover(GatewayContextUser<R> contextUser) throws Throwable {
        return callWithFailover(failover, numContexts, contextUser);
    }


    /**
     * Use the cached data, if we have some and it isn't stale, or else use callWithFailover to obtain it; and
     * rethrow any checked exception thrown by the API method inside a GatewayException.
     *
     * @param cached    a cache that may hold some data of type R.  Required (although the cache may be empty).
     * @param contextUser  remote call for refilling cache.  See {@link #callWithFailover} for more information.
     * @return cached data, or the result of invoking {@link #callWithFailover}.  May be null if the contextUser can return null.
     * @throws GatewayException @see #callWithFailover
     */
    private <R> R cacheGetWithRethrow(Cached<R> cached, GatewayContextUser<R> contextUser) throws GatewayException {
        try {
            return cacheGet(cached, contextUser);
        } catch (InvocationTargetException e) {
            throw new GatewayException(e.getTargetException());
        }
    }

    /**
     * Use the cached data, if we have some and it isn't stale, or else use callWithFailover to obtain it.
     *
     * @param cached    a cache that may hold some data of type R.  Required (although the cache may be empty).
     * @param contextUser  remote call for refilling cache.  See {@link #callWithFailover} for more information.
     * @return cached data, or the result of invoking {@link #callWithFailover}.  May be null if the contextUser can return null.
     * @throws GatewayException @see #callWithFailover
     * @throws java.lang.reflect.InvocationTargetException if the API method throws a checked exception
     */
    private <R> R cacheGet(Cached<R> cached, GatewayContextUser<R> contextUser) throws GatewayException, InvocationTargetException {
        if (cached == null)
            throw new NullPointerException();

        synchronized (cacheLock) {
            final long now = System.currentTimeMillis();
            Pair<Long, R> got = cached.getData();
            if (got != null) {
                final long when = got.left;
                final long age = now - when;
                if (age <= timeout)
                    return got.right;

                cached.clear();
            }

            // Refill while holding lock so only one thread at a time issues remote requests to this cluster
            R updated = callWithFailover(failover, numContexts, contextUser);
            cached.setData(now, updated);
            return updated;
        }
    }

    /**
     * Holds some cached data along with the time it was cached.
     * Cached data is stored via a SoftReference so it can be released if memory space is tight.
     * Does no synchronization of its own.
     */
    private static class Cached<T> {
        private long when;
        private SoftReference<T> data;

        public Pair<Long, T> getData() {
            return data == null ? null : new Pair<Long, T>(when, data.get());
        }

        public void setData(long now, T data) {
            this.when = now;
            this.data = new SoftReference<T>(data);
        }

        public void clear() {
            this.when = 0;
            this.data = null;
        }
    }

    private static final class FailedFailoverStrategy implements FailoverStrategy<GatewayContext> {
        @Override
        public GatewayContext selectService() {
            return null;
        }

        @Override
        public void reportFailure(GatewayContext service) {
        }

        @Override
        public void reportSuccess(GatewayContext service) {
        }

        @Override
        public String getName() {
            return "failed";
        }

        @Override
        public String getDescription() {
            return "Failover Strategy that always fails";
        }

        @Override
        public void reportContent(Object content, Feedback feedback) {
        }
    }
}
