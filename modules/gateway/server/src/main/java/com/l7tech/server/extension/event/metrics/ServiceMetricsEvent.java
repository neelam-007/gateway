package com.l7tech.server.extension.event.metrics;

import com.ca.apim.gateway.extension.event.Event;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.variable.BuiltinVariables.SSGNODE_ID;
import static com.l7tech.policy.variable.BuiltinVariables.SSGNODE_IP;
import static com.l7tech.policy.variable.BuiltinVariables.SSGNODE_NAME;
import static java.lang.Math.subtractExact;
import static java.lang.Math.toIntExact;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * The event fired when off-box service metrics are enabled.
 *
 * This class is used in modules to export the metrics information for off-boxed destinations.
 * Avoid changes to the methods and fields names.
 */
public class ServiceMetricsEvent implements Event {

    private static final Logger LOGGER = getLogger(ServiceMetricsEvent.class.getName());

    private final long time;
    private final String nodeId;
    private final String nodeName;
    private final String nodeIp;
    private final String serviceId;
    private final String serviceName;
    private final String serviceUri;
    private final int totalFrontendLatency;
    private final int totalBackendLatency;
    private final boolean isPolicySuccessful;
    private final boolean isPolicyViolation;
    private final boolean isRoutingFailure;

    /**
     * Constructor that gather all necessary information from the {@link PolicyEnforcementContext}. Do not change current behaviour, unless approved.
     *
     * @param pec the context
     */
    ServiceMetricsEvent(@NotNull PolicyEnforcementContext pec) {
        this.time = pec.getStartTime();

        this.nodeId = getVariableFromContext(pec, SSGNODE_ID);
        this.nodeName = getVariableFromContext(pec, SSGNODE_NAME);
        this.nodeIp = getVariableFromContext(pec, SSGNODE_IP);

        if (pec.getService() != null) {
            this.serviceId = pec.getService().getGoid().toString();
            this.serviceName = pec.getService().getName();
            this.serviceUri = pec.getService().getRoutingUri();
        } else {
            this.serviceId = PublishedService.DEFAULT_GOID.toString();
            this.serviceName = EMPTY;
            this.serviceUri = EMPTY;
        }

        this.totalFrontendLatency = toIntExact(subtractExact(pec.getEndTime(), pec.getStartTime()));
        this.totalBackendLatency = toIntExact(pec.getRoutingTotalTime());

        this.isPolicySuccessful = pec.isAuthorizedRequest() && pec.isCompletedRequest();
        this.isPolicyViolation = !pec.isAuthorizedRequest() && !pec.isCompletedRequest();
        this.isRoutingFailure = pec.isAuthorizedRequest() && !pec.isCompletedRequest();
    }

    public long getTime() {
        return time;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceUri() {
        return serviceUri;
    }

    public int getTotalFrontendLatency() {
        return totalFrontendLatency;
    }

    public int getTotalBackendLatency() {
        return totalBackendLatency;
    }

    public boolean isPolicySuccessful() {
        return isPolicySuccessful;
    }

    public boolean isPolicyViolation() {
        return isPolicyViolation;
    }

    public boolean isRoutingFailure() {
        return isRoutingFailure;
    }

    /**
     * Get the value of a context variable if it's set, otherwise return empty string.
     *
     * @param name the name of the variable to get (case-insensitive), ie "requestXpath.result".  Required.
     * @return  the Object representing the value of the specified variable.  May currently be null in some situations
     *          (for example, when a built-in variable returns null, or selects an empty collection, when using a wrapped PEC).
     */
    private static String getVariableFromContext(PolicyEnforcementContext context, String name) {
        try {
            return context.getVariable(name).toString();
        } catch (NoSuchVariableException e) {
            LOGGER.log(Level.FINE, ExceptionUtils.getDebugException(e), () -> "Error getting variable from PolicyEnforcementContext: " + e.getMessage());
            return EMPTY;
        }
    }
}
