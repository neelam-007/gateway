package com.l7tech.external.assertions.pbsmel.server;

import com.ca.apim.gateway.extension.event.EventListener;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.GsonBuilder;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.server.extension.event.metrics.ServiceMetricsEvent;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener extension to process service metrics using policy-backed services.
 * For each policy-backed service, an event listener will be registered.
 */
public class ServiceMetricsEventListener implements EventListener<ServiceMetricsEvent> {

    private static final Logger LOGGER = Logger.getLogger(ServiceMetricsEventListener.class.getName());

    private final PolicyBackedServiceRegistry pbsreg;
    private final PolicyBackedService pbs;

    ServiceMetricsEventListener(final PolicyBackedServiceRegistry pbsreg, final PolicyBackedService pbs) {
        this.pbsreg = pbsreg;
        this.pbs = pbs;
    }

    @Override
    public void onEvent(ServiceMetricsEvent event) {
        LOGGER.log(Level.FINE, "Processing service metrics processor policy-backed service ''{0}''.", new Object[]{pbs.getName()});

        final String json = this.generateJson(event);
        try (Message message = new Message();
             StashManager sm = new ByteArrayStashManager();
             InputStream is = new ByteArrayInputStream(json.getBytes())) {
            final ServiceMetricsProcessor proxy =
                    pbsreg.getImplementationProxy(ServiceMetricsProcessor.class, pbs.getGoid());
            message.initialize(sm, ContentTypeHeader.APPLICATION_JSON, is);
            proxy.process(message);

            LOGGER.log(Level.FINE, "Finished processing service metrics processor policy-backed service ''{0}''.", new Object[]{pbs.getName()});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, ExceptionUtils.getDebugException(e),
                    () -> "Error occurred processing service metrics processor policy-backed service '" + pbs.getName() + "'. " + ExceptionUtils.getMessage(e));
        }
    }

    @Override
    public Class<ServiceMetricsEvent> supportedEventType() {
        return ServiceMetricsEvent.class;
    }

    /**
     * Converts the event to the internal json class and then generates the json string.
     *
     * @param event the {@link ServiceMetricsEvent} object
     * @return the json String built using {@link ServiceMetricsEventJson}
     */
    @VisibleForTesting
    public String generateJson(ServiceMetricsEvent event) {
        return new GsonBuilder().create().toJson(new ServiceMetricsEventJson(event));
    }

    /**
     * Internal inner class with the json fields. Created to avoid that changes in the event class causes changes in the json format.
     */
    private static class ServiceMetricsEventJson {

        @SuppressWarnings("UnusedDeclaration")
        private final long time;
        @SuppressWarnings("UnusedDeclaration")
        private final String nodeId;
        @SuppressWarnings("UnusedDeclaration")
        private final String nodeName;
        @SuppressWarnings("UnusedDeclaration")
        private final String nodeIp;
        @SuppressWarnings("UnusedDeclaration")
        private final String serviceId;
        @SuppressWarnings("UnusedDeclaration")
        private final String serviceName;
        @SuppressWarnings("UnusedDeclaration")
        private final String serviceUri;
        @SuppressWarnings("UnusedDeclaration")
        private final int totalFrontendLatency;
        @SuppressWarnings("UnusedDeclaration")
        private final int totalBackendLatency;
        @SuppressWarnings("UnusedDeclaration")
        private final boolean isPolicySuccessful;
        @SuppressWarnings("UnusedDeclaration")
        private final boolean isPolicyViolation;
        @SuppressWarnings("UnusedDeclaration")
        private final boolean isRoutingFailure;

        ServiceMetricsEventJson(@NotNull ServiceMetricsEvent event) {
            this.time = event.getTime();
            this.nodeId = event.getNodeId();
            this.nodeName = event.getNodeName();
            this.nodeIp = event.getNodeIp();
            this.serviceId = event.getServiceId();
            this.serviceName = event.getServiceName();
            this.serviceUri = event.getServiceUri();
            this.totalFrontendLatency = event.getTotalFrontendLatency();
            this.totalBackendLatency = event.getTotalBackendLatency();
            this.isPolicySuccessful = event.isPolicySuccessful();
            this.isPolicyViolation = event.isPolicyViolation();
            this.isRoutingFailure = event.isRoutingFailure();
        }
    }
}
