package com.l7tech.server.module.simplegatewaymetricextractor;

import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetrics;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.metrics.PerformanceMetricsListener;
import com.l7tech.server.message.metrics.PerformanceMetricsPublisher;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.variable.DebugTraceVariableContextSelector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 */
public class SimpleGatewayMetricExtractor extends PerformanceMetricsListener {
    private static final Logger logger = Logger.getLogger(SimpleGatewayMetricExtractor.class.getName());

    private static SimpleGatewayMetricExtractor instance = null;
    private final PerformanceMetricsPublisher performanceMetricsEventsPublisher;

    private SimpleGatewayMetricExtractor(final ApplicationContext applicationContext) {
        performanceMetricsEventsPublisher = applicationContext.getBean("performanceMetricsPublisher", PerformanceMetricsPublisher.class);
        performanceMetricsEventsPublisher.addListener(this);
    }

    @Override
    public void assertionFinished(@NotNull final AssertionFinished assertionFinished) {
        final Assertion assertion = assertionFinished.getAssertion();

        final PolicyEnforcementContext pec = assertionFinished.getContext();
        String assertionNumber = getAssertionNumber(pec, assertion);
//            String assertionNumber = buildAssertionOrdinalPath((EventObject) applicationEvent.getSource(), assertion);

        final AssertionMetrics metrics = assertionFinished.getAssertionMetrics();

        final Pair<String, String> policyNameAndGuid = getPolicyNameAndGuid(pec);

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        logger.log(
                Level.INFO,
                new StringBuilder("ASSERTION LATENCY: ")
                        .append("request-id=").append(getRequestId(pec)).append(" ")
                        .append("service name=").append(getServiceName(pec)).append(" ")
                        .append("policy name=").append(policyNameAndGuid.left).append(" ")
                        .append("policy guid=").append(policyNameAndGuid.right).append(" ")
                        .append("number=").append(assertionNumber).append(" ")
                        .append("className=").append(assertion.getClass().getSimpleName()).append(" ")
                        .append("assName=").append(assertion.meta().get(AssertionMetadata.SHORT_NAME)).append(" ")
                        .append("startTime=").append(sdf.format(new Date(metrics.getStartTimeMs()))).append(" ")
                        .append("endTime=").append(sdf.format(new Date(metrics.getEndTimeMs()))).append(" ")
                        .append("latency=").append(metrics.getLatencyMs())
                        .toString()
        );

        // TODO add access to PolicyHeader (maybe as ${request.executingPolicy.<policy_header_field>
        // TODO add / verify access to requestId
    }

    // build assertion ordinal path
    private String buildAssertionOrdinalPath(@NotNull final PolicyEnforcementContext pec, @NotNull final Assertion assertion) {
        String ordinalPath = Integer.toString(assertion.getOrdinal());

        final Collection<Integer> ordinals = pec.getAssertionOrdinalPath();
        final int ordinalsSize = ordinals.size();
        if (ordinalsSize > 0) {
            StringBuilder stringBuilder = new StringBuilder(ordinalsSize * 2);
            for (Iterator<Integer> iterator = ordinals.iterator(); iterator.hasNext(); ) {
                Integer ordinal = iterator.next();
                stringBuilder.append(ordinal);
                if (iterator.hasNext()) {
                    stringBuilder.append('.');
                }
            }
            stringBuilder.append('.');
            stringBuilder.append(Integer.toString(assertion.getOrdinal()));
            ordinalPath = stringBuilder.toString();
        }

        return ordinalPath;
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
    }

//    private String getAssertionNumber(@NotNull final EventObject eventSource) {
//        String result = null;
//        if (eventSource.getSource() instanceof  PolicyEnforcementContext) {
//            final Collection<Integer> assertionNumber = ((PolicyEnforcementContext) eventSource.getSource()).getAssertionNumber();
//            result = TextUtils.join(".", assertionNumber).toString();
//        }
//        return result;
//    }

    /**
     * Get the current instance, if there is one.
     *
     * @return  the current instance, created when onModuleLoaded() was called, or null if there isn't one.
     */
    public static SimpleGatewayMetricExtractor getInstance() {
        return instance;
    }

    private void destroy() throws Exception {
        if (performanceMetricsEventsPublisher != null)
            performanceMetricsEventsPublisher.removeListener(this);
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
                logger.log(Level.WARNING, "SimpleGatewayMetricExtractor module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }
}
