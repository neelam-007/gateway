package com.l7tech.external.assertions.simplegatewaymetricextractor.server;

import com.l7tech.external.assertions.simplegatewaymetricextractor.SimpleGatewayMetricExtractorEntity;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.message.metrics.LatencyMetrics;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.ServiceFinished;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.metrics.GatewayMetricsListener;
import com.l7tech.server.message.metrics.GatewayMetricsPublisher;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.variable.DebugTraceVariableContextSelector;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Sample listener for a monitoring assertion.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class SimpleGatewayMetricExtractor extends GatewayMetricsListener {
    private static final Logger logger = Logger.getLogger(SimpleGatewayMetricExtractor.class.getName());

    private static SimpleGatewayMetricExtractor instance = null;
    private final GatewayMetricsPublisher gatewayMetricsEventsPublisher;
    private EntityManager<SimpleGatewayMetricExtractorEntity, GenericEntityHeader> entityManager;
    private String serviceFilterName = null;

    private SimpleGatewayMetricExtractor(final ApplicationContext applicationContext) {
        gatewayMetricsEventsPublisher = applicationContext.getBean("gatewayMetricsPublisher", GatewayMetricsPublisher.class);
        gatewayMetricsEventsPublisher.addListener(this);

        GenericEntityManager gem = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        entityManager = gem.getEntityManager(SimpleGatewayMetricExtractorEntity.class);
    }

    /**
     * Extracts and logs the content in AssertionFinished
     */
    @Override
    public void assertionFinished(@NotNull final AssertionFinished assertionFinished) {
        // apply service filter
        final PolicyEnforcementContext pec = assertionFinished.getContext();
        final String serviceName = getServiceName(pec);
        setServiceFilterName();

        if (isEmpty(serviceFilterName) || serviceFilterName.equals(serviceName) ) {
            final Assertion assertion = assertionFinished.getAssertion();
            String assertionNumber = getAssertionNumber(pec, assertion);
            final LatencyMetrics metrics = assertionFinished.getAssertionMetrics();
            final Pair<String, String> policyNameAndGuid = getPolicyNameAndGuid(pec);

            //final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            logger.log(
                    Level.INFO,
                    "ASSERTION LATENCY: " +
                            "request-id=" + getRequestId(pec) + " " +
                            "service name=" + serviceName + " " +
                            "policy name=" + policyNameAndGuid.left + " " +
                            "policy guid=" + policyNameAndGuid.right + " " +
                            "number=" + assertionNumber + " " +
                            "assertion name=" + assertion.meta().get(AssertionMetadata.SHORT_NAME) + " " +
                            "startTime=" + metrics.getStartTimeMs() + " " +//.append("startTime=").append(sdf.format(new Date(metrics.getStartTimeMs()))).append(" ")
                            "latency=" + metrics.getLatencyMs()
            );
        }

        // TODO add access to PolicyHeader (maybe as ${request.executingPolicy.<policy_header_field>
        // TODO add / verify access to requestId
        // TODO add sample use of ConcurrentLinkedQueue
        // TODO add sample how to set user defined context variable
    }

    private void setServiceFilterName() {
        try {
            Collection<SimpleGatewayMetricExtractorEntity> entities = entityManager.findAll();
            if(entities.iterator().hasNext()) {
                serviceFilterName = entities.iterator().next().getServiceNameFilter();
            } else {
                serviceFilterName = null;
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error loading configuration: "+ getMessage(e), getDebugException(e) );
        }
    }

    private String getAssertionNumber(@NotNull final PolicyEnforcementContext pec, @Nullable final Assertion assertion) {
        return DebugTraceVariableContextSelector.buildAssertionNumberStr(pec, assertion);
    }

    private Pair<String, String> getPolicyNameAndGuid(@NotNull final PolicyEnforcementContext pec) {
        final PolicyMetadata meta = pec.getCurrentPolicyMetadata();
        final PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
        return Pair.pair(head == null ? "N/A" : head.getName(), head == null ? "N/A" : head.getGuid());
    }

    private String getServiceName(@NotNull final PolicyEnforcementContext pec) {
        final PublishedService service = pec.getService();
        return service == null ? "N/A" : service.getName();
    }

    private String getRequestId(@NotNull final PolicyEnforcementContext pec) {
        final RequestId requestId = pec.getRequestId();
        return requestId == null ? "N/A" : requestId.toString();
        // TODO add / verify access to requestId
    }

    /**
     * Extracts and logs the content in ServiceFinished
     */
    @Override
    public void serviceFinished(@NotNull ServiceFinished event) {
        final PolicyEnforcementContext pec = event.getContext();
        final String serviceName = getServiceName(pec);
        setServiceFilterName();
        if (isEmpty(serviceFilterName) || serviceFilterName.equals(serviceName) ) {
            final PublishedService service = pec.getService();
            final LatencyMetrics metrics = event.getServiceMetrics();

            logger.log(
                    Level.INFO,
                    "SERVICE LATENCY: " +
                            "request-id=" + getRequestId(pec) + " " +
                            "service name=" + serviceName + " " +
                            "service-id=" + service.getId() + " " +
                            "version=" + service.getVersion() + " " +
                            "startTime=" + metrics.getStartTimeMs() + " " +//.append("startTime=").append(sdf.format(new Date(metrics.getStartTimeMs()))).append(" ")
                            "latency=" + metrics.getLatencyMs()
            );
        }
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
        if (gatewayMetricsEventsPublisher != null)
            gatewayMetricsEventsPublisher.removeListener(this);
    }

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "SimpleGatewayMetricExtractor module is already initialized");
        } else {
            instance = new SimpleGatewayMetricExtractor(context);
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "SimpleGatewayMetricExtractor module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "SimpleGatewayMetricExtractor module threw exception on shutdown: " + getMessage(e), e);
            } finally {
                instance = null;
            }
        }
        GenericEntityManagerSimpleGatewayMetricExtractorServerSupport.clearInstance();
    }
}
