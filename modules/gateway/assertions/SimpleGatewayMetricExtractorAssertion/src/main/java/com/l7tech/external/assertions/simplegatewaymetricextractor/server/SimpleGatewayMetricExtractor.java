package com.l7tech.external.assertions.simplegatewaymetricextractor.server;

import com.l7tech.external.assertions.simplegatewaymetricextractor.SimpleGatewayMetricExtractorEntity;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.ServiceFinished;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.metrics.GatewayMetricsListener;
import com.l7tech.server.message.metrics.GatewayMetricsPublisher;
import com.l7tech.server.message.metrics.LatencyMetrics;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.variable.DebugTraceVariableContextSelector;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample listener for a monitoring assertion.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class SimpleGatewayMetricExtractor extends GatewayMetricsListener {
    private static final Logger logger = Logger.getLogger(SimpleGatewayMetricExtractor.class.getName());
    private static final int FLUSH_PERIOD = 30000;

    private static SimpleGatewayMetricExtractor instance = null;
    private final GatewayMetricsPublisher gatewayMetricsEventsPublisher;
    private final EntityManager<SimpleGatewayMetricExtractorEntity, GenericEntityHeader> entityManager;
    private final AtomicReference<String> serviceNameFilterRef = new AtomicReference<>();
    private final Map<RequestId, Queue<LatencyObject>> latencyHash = new ConcurrentHashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Immutable
     */
    private static abstract class LatencyObject {

        protected LatencyObject(
                @Nullable final PublishedService service,
                @NotNull final LatencyMetrics metrics
        ) {
            this.serviceName = (service != null) ? service.getName() : null;
            this.serviceId = (service != null) ? service.getId() : null;
            this.metrics = metrics;
        }

        @Nullable
        public String getServiceName() {
            return serviceName;
        }

        @Nullable
        public String getServiceId() {
            return serviceId;
        }

        @NotNull
        public LatencyMetrics getMetrics() {
            return metrics;
        }

        private final String serviceName;
        private final String serviceId;
        @NotNull
        private final LatencyMetrics metrics;
    }

    /**
     * Immutable
     */
    public static class AssertionFinishedLatency extends LatencyObject {
        private AssertionFinishedLatency(
                @Nullable final PublishedService service,
                @NotNull final LatencyMetrics metrics,
                final PolicyMetadata policyMetadata,
                final String assertionName,
                final String assertionNumber
        ) {
            super(service, metrics);

            final PolicyHeader policyHeader = policyMetadata != null ? policyMetadata.getPolicyHeader() : null;
            this.policyName = policyHeader != null ? policyHeader.getDisplayName() : null;
            this.policyId = policyHeader != null ? policyHeader.getGuid() : null;
            this.assertionName = assertionName;
            this.assertionNumber = assertionNumber;
        }

        public String getPolicyName() {
            return policyName;
        }

        public String getPolicyId() {
            return policyId;
        }

        public String getAssertionName() {
            return assertionName;
        }

        public String getAssertionNumber() {
            return assertionNumber;
        }

        private final String policyName;
        private final String policyId;
        private final String assertionName;
        private final String assertionNumber;
    }

    /**
     * Immutable
     */
    public static class ServiceFinishedLatency extends LatencyObject {
        private ServiceFinishedLatency(
                @NotNull final PublishedService service,
                @NotNull final LatencyMetrics metrics
        ) {
            super(service, metrics);
            this.serviceVersion = service.getVersion();
        }

        public int getServiceVersion() {
            return serviceVersion;
        }

        private final int serviceVersion;
    }


    private SimpleGatewayMetricExtractor(final ApplicationContext applicationContext) {
        gatewayMetricsEventsPublisher = applicationContext.getBean("gatewayMetricsPublisher", GatewayMetricsPublisher.class);
        gatewayMetricsEventsPublisher.addListener(this);

        GenericEntityManager gem = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        entityManager = gem.getEntityManager(SimpleGatewayMetricExtractorEntity.class);

        final ApplicationEventProxy applicationEventProxy = applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                if (event instanceof Started) {
                    serviceNameFilterRef.set(getServiceNameFilterFromManager());
                    Background.scheduleRepeated(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    flushEnqueuedLatencies();
                                }
                            },
                            FLUSH_PERIOD,
                            FLUSH_PERIOD
                    );
                } else //noinspection StatementWithEmptyBody
                    if (event instanceof LicenseChangeEvent) {
                        // TODO handle license event here
                    } else if (event instanceof EntityInvalidationEvent) {
                        final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
                        if (GenericEntity.class.equals(entityInvalidationEvent.getEntityClass()) && entityInvalidationEvent.getSource() instanceof GenericEntity) {
                            final char[] ops = entityInvalidationEvent.getEntityOperations();
                            final GenericEntity entity = (GenericEntity) entityInvalidationEvent.getSource();
                            if (SimpleGatewayMetricExtractorEntity.class.getName().equals(entity.getEntityClassName())) {
                                // since there is only one generic entity, just get it from the manager
                                serviceNameFilterRef.set(getServiceNameFilterFromManager());
                            }
                        }
                    }

            }
        });
    }

    /**
     * Extracts and queue the content in AssertionFinished
     */
    @Override
    public void assertionFinished(@NotNull final AssertionFinished assertionFinished) {
        // apply service filter
        final PolicyEnforcementContext pec = assertionFinished.getContext();
        final PublishedService service = pec.getService();
        final String serviceNameFilter = serviceNameFilterRef.get();

        // Service object for "Global Policy Message Received" is null; therefore, we cannot filter them by service name
        // For the time being we will include it anyways (decide later)
        if (StringUtils.isEmpty(serviceNameFilter) || service == null || serviceNameFilter.equals(service.getName())) {
            final Assertion assertion = assertionFinished.getAssertion();
            final LatencyMetrics metrics = assertionFinished.getAssertionMetrics();
            final RequestId requestId = pec.getRequestId();

            enqueueLatency(
                    requestId,
                    new AssertionFinishedLatency(
                            service,
                            metrics,
                            pec.getCurrentPolicyMetadata(),
                            assertion.meta().get(AssertionMetadata.SHORT_NAME),
                            getAssertionNumber(pec, assertion)
                    )
            );
        }

        // TODO add access to PolicyHeader (maybe as ${request.executingPolicy.<policy_header_field>
        // TODO add / verify access to requestId
        // TODO add sample use of ConcurrentLinkedQueue
        // TODO add sample how to set user defined context variable
    }

    /**
     * Extracts and queue the content in ServiceFinished
     */
    @Override
    public void serviceFinished(@NotNull final ServiceFinished event) {
        final PolicyEnforcementContext pec = event.getContext();
        final PublishedService service = pec.getService();
        final String serviceNameFilter = serviceNameFilterRef.get();

        if (StringUtils.isEmpty(serviceNameFilter) || serviceNameFilter.equals(service.getName()) ) {
            final LatencyMetrics metrics = event.getServiceMetrics();
            final RequestId requestId = pec.getRequestId();

            enqueueLatency(
                    requestId,
                    new ServiceFinishedLatency(service, metrics)
            );
        }
    }

    private void enqueueLatency(@NotNull final RequestId requestId, @NotNull final LatencyObject object) {
        try {
            rwLock.readLock().lock();
            final Queue<LatencyObject> enqueuedObjects = latencyHash.computeIfAbsent(
                    requestId,
                    new Function<RequestId, Queue<LatencyObject>>() {
                        @Override
                        public Queue<LatencyObject> apply(final RequestId requestId) {
                            return new ConcurrentLinkedQueue<>();
                        }
                    }
            );
            if (enqueuedObjects != null) {
                enqueuedObjects.add(object);
            } else {
                // should never happen
                logger.log(Level.SEVERE, "Failed to create new ConcurrentLinkedQueue for latencyHash: requestId = " + requestId);
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void flushEnqueuedLatencies() {
        // even though using ConcurrentHashMap iterators is safe here, as this method is called only from the timer thread (and nowhere else)
        // and other threads (like the assertionFinished() or serviceFinished() methods) only enqueue entries and DO NOT remove them
        // there is still a slight chance of a racing condition when assertionFinished() or serviceFinished() arrives just after
        // we process all enqueued latencies (i.e. after drainLatencies(); and before iterator.remove(); as shown below),
        // in this case the newly added latency object from assertionFinished() or serviceFinished() will never be processed
        // as its key is about to be deleted here with iterator.remove();
        //
        // Hence the need for ReentrantReadWriteLock!

        final Collection<Pair<RequestId, LatencyObject>> drainedLatencies = new LinkedList<>();
        try {
            rwLock.writeLock().lock();
            for (final Iterator<Map.Entry<RequestId, Queue<LatencyObject>>> iterator = latencyHash.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<RequestId, Queue<LatencyObject>> entry = iterator.next();
                drainLatencies(entry.getKey(), entry.getValue(), drainedLatencies);
                iterator.remove();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        logLatencies(drainedLatencies);
        drainedLatencies.clear(); // just in case
    }

    private void drainLatencies(
            @NotNull final RequestId requestId,
            @NotNull final Queue<LatencyObject> enqueuedObjects,
            @NotNull final Collection<Pair<RequestId, LatencyObject>> drainedLatencies
    ) {
        LatencyObject object = enqueuedObjects.poll();
        while (object != null) {
            drainedLatencies.add(Pair.pair(requestId, object));
            object = enqueuedObjects.poll();
        }
    }

    private void logLatencies(@NotNull final Collection<Pair<RequestId, LatencyObject>> latencies) {
        latencies.forEach(
                new Consumer<Pair<RequestId, LatencyObject>>() {
                    @Override
                    public void accept(final Pair<RequestId, LatencyObject> requestIdLatencyObjectPair) {
                        final RequestId requestId = requestIdLatencyObjectPair.left;
                        final LatencyObject object = requestIdLatencyObjectPair.right;
                        if (object instanceof AssertionFinishedLatency) {
                            final AssertionFinishedLatency assertionFinishedLatency = (AssertionFinishedLatency) object;
                            //final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            logger.log(
                                    Level.INFO,
                                    "ASSERTION LATENCY: " +
                                            "request-id=" + requestId.toString() + " " +
                                            "service name=" + StringUtils.defaultIfEmpty(assertionFinishedLatency.getServiceName(), "N/A") + " " +
                                            "policy name=" + StringUtils.defaultIfEmpty(assertionFinishedLatency.getPolicyName(), "N/A") + " " +
                                            "policy guid=" + StringUtils.defaultIfEmpty(assertionFinishedLatency.getPolicyId(), "N/A") + " " +
                                            "number=" + StringUtils.defaultIfEmpty(assertionFinishedLatency.getAssertionNumber(), "N/A") + " " +
                                            "assertion name=" + StringUtils.defaultIfEmpty(assertionFinishedLatency.getAssertionName(), "N/A") + " " +
                                            "startTime=" + assertionFinishedLatency.getMetrics().getStartTimeMs() + " " +//.append("startTime=").append(sdf.format(new Date(metrics.getStartTimeMs()))).append(" ")
                                            "latency=" + assertionFinishedLatency.getMetrics().getLatencyMs()
                            );
                        } else if (object instanceof ServiceFinishedLatency) {
                            final ServiceFinishedLatency serviceFinishedLatency = (ServiceFinishedLatency) object;
                            //final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            logger.log(
                                    Level.INFO,
                                    "SERVICE LATENCY: " +
                                            "request-id=" + requestId.toString() + " " +
                                            "service name=" + StringUtils.defaultIfEmpty(serviceFinishedLatency.getServiceName(), "N/A") + " " +
                                            "service-id=" + StringUtils.defaultIfEmpty(serviceFinishedLatency.getServiceId(), "N/A") + " " +
                                            "version=" + serviceFinishedLatency.getServiceVersion() + " " +
                                            "startTime=" + serviceFinishedLatency.getMetrics().getStartTimeMs() + " " +//.append("startTime=").append(sdf.format(new Date(metrics.getStartTimeMs()))).append(" ")
                                            "latency=" + serviceFinishedLatency.getMetrics().getLatencyMs()
                            );
                        } else {
                            logger.log(Level.WARNING, "Unrecognized LatencyObject impl: " + object.getClass().getName());
                        }

                    }
                }
        );
    }

    @Nullable
    private String getServiceNameFilterFromManager() {
        try {
            final SimpleGatewayMetricExtractorEntity entity = entityManager.findByUniqueName(SimpleGatewayMetricExtractorEntity.ENTITY_UNIQUE_NAME);
            if (entity != null) {
                return entity.getServiceNameFilter();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error loading configuration: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return null;
    }

    @NotNull
    private String getAssertionNumber(@NotNull final PolicyEnforcementContext pec, @Nullable final Assertion assertion) {
        return DebugTraceVariableContextSelector.buildAssertionNumberStr(pec, assertion);
    }

    /**
     * Get the current instance, if there is one.
     *
     * @return  the current instance, created when onModuleLoaded() was called, or null if there isn't one.
     */
    public static SimpleGatewayMetricExtractor getInstance() {
        return instance;
    }

    private void destroy() throws Exception {
        try {
            if (gatewayMetricsEventsPublisher != null)
                gatewayMetricsEventsPublisher.removeListener(this);
        } finally {
            flushEnqueuedLatencies();
        }
    }

    /**
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "SimpleGatewayMetricExtractor module is already initialized");
        } else {
            instance = new SimpleGatewayMetricExtractor(context);
        }
    }

    /**
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "SimpleGatewayMetricExtractor module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "SimpleGatewayMetricExtractor module threw exception on shutdown: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } finally {
                instance = null;
            }
        }
        GenericEntityManagerSimpleGatewayMetricExtractorServerSupport.clearInstance();
    }
}
