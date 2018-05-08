package com.l7tech.server.extension.event.metrics;

import com.ca.apim.gateway.extension.event.EventListener;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageProcessingEvent;
import com.l7tech.server.extension.registry.event.EventListenerRegistryImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.l7tech.server.message.PolicyEnforcementContextFactory.createPolicyEnforcementContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceMetricsEventListenerProxyTest {

    private ServiceMetricsEventListenerProxy eventListenerProxy;
    private ServiceMetricsEventListenerTest1 listenerTest1;
    private ServiceMetricsEventListenerTest2 listenerTest2;

    @Mock
    private MessageProcessed mockMessageProcessed;

    @Before
    public void before() throws Exception{
        EventListenerRegistryImpl registry = new EventListenerRegistryImpl();
        this.eventListenerProxy = new ServiceMetricsEventListenerProxy() {
            @Override
            void submit(EventListener listener, ServiceMetricsEvent metricsEvent) {
                // avoid threading into tests
                listener.onEvent(metricsEvent);
            }
        };
        this.eventListenerProxy.setEventListenerRegistry(registry);

        listenerTest1 = new ServiceMetricsEventListenerTest1();
        listenerTest2 = new ServiceMetricsEventListenerTest2();
        registry.register("event1", listenerTest1);
        registry.register("event2", listenerTest2);
    }

    @Test
    public void ignoreOtherEvent() {
        eventListenerProxy.handleEvent(new OtherMessageProcessingEvent(new Object()));

        assertTrue(listenerTest1.events.isEmpty());
        assertTrue(listenerTest2.events.isEmpty());
    }

    @Test
    public void callListenersCorrectly() {
        PolicyEnforcementContext context = createPolicyEnforcementContext(new Message(), new Message());
        context.setPolicyExecutionAttempted(true);
        PublishedService publishedService = new PublishedService();
        publishedService.setRoutingUri("route");
        publishedService.setName("name");
        publishedService.setGoid(PublishedService.DEFAULT_GOID);
        context.setService(publishedService);
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        context.setEndTime();

        when(mockMessageProcessed.getContext()).thenReturn(context);

        ServiceMetricsEvent event = new ServiceMetricsEvent(context);

        eventListenerProxy.handleEvent(mockMessageProcessed);

        assertEquals(1, listenerTest1.events.size());
        assertEquals(1, listenerTest2.events.size());
        assertTrue(listenerTest1.events.iterator().next() instanceof ServiceMetricsEvent);
        assertTrue(listenerTest2.events.iterator().next() instanceof ServiceMetricsEvent);
        assertEquals(event.getServiceName(), listenerTest1.events.iterator().next().getServiceName());
        assertEquals(event.getServiceName(), listenerTest2.events.iterator().next().getServiceName());
    }

    private static class OtherMessageProcessingEvent extends MessageProcessingEvent {

        public OtherMessageProcessingEvent(Object source) {
            super(source, createPolicyEnforcementContext(new Message(), new Message()));
        }
    }

    private static class ServiceMetricsEventListenerTest1 implements EventListener<ServiceMetricsEvent> {

        private List<ServiceMetricsEvent> events = new ArrayList<>();

        @Override
        public void onEvent(ServiceMetricsEvent event) {
            this.events.add(event);
        }

        @Override
        public Class<ServiceMetricsEvent> supportedEventType() {
            return ServiceMetricsEvent.class;
        }
    }

    private static class ServiceMetricsEventListenerTest2 implements EventListener<ServiceMetricsEvent> {

        private List<ServiceMetricsEvent> events = new ArrayList<>();

        @Override
        public void onEvent(ServiceMetricsEvent event) {
            this.events.add(event);
        }

        @Override
        public Class<ServiceMetricsEvent> supportedEventType() {
            return ServiceMetricsEvent.class;
        }
    }
}
