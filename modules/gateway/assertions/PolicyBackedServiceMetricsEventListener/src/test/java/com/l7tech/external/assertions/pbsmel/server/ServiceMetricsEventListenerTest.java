package com.l7tech.external.assertions.pbsmel.server;

import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.server.extension.event.metrics.ServiceMetricsEvent;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.*;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the ServiceMetricsEventListener.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceMetricsEventListenerTest {

    private static final String JSON_TEMPLATE;
    private static final Long TIMESTAMP = System.currentTimeMillis();

    static {
        // Generates the template using a date in the local timezone
        String date = DateUtils.getDefaultTimeZoneFormattedString(new Date(TIMESTAMP));
        JSON_TEMPLATE = "{\"time\":"+TIMESTAMP+",\"formattedTime\":\"" + date + "\",\"nodeId\":\"0000000000000000ffffffffffffffff\",\"nodeName\":\"localhost\",\"nodeIp\":\"127.0.0.1\",\"serviceId\":\"0000000000000000ffffffffffffffff\",\"serviceName\":\"Service\",\"serviceUri\":\"/service\",\"totalFrontendLatency\":250,\"totalBackendLatency\":100,\"isPolicySuccessful\":true,\"isPolicyViolation\":false,\"isRoutingFailure\":false}";
    }

    @Mock
    private PolicyBackedServiceRegistry pbsreg;

    @Mock
    private ServiceMetricsProcessor serviceMetricsProcessor;

    private final Goid pbs1Id = new Goid(0L, 1L);
    private ServiceMetricsEventListener serviceMetricsEventListener;

    @Before
    public void setUp() {
        PolicyBackedService pbs1 = new PolicyBackedService();
        pbs1.setGoid(pbs1Id);
        pbs1.setName("ServiceMetricsProcessor Policy-Backed Service Test");
        pbs1.setServiceInterfaceName(ServiceMetricsProcessor.class.getName());

        serviceMetricsEventListener = new ServiceMetricsEventListener(pbsreg, pbs1);
    }

    @Test
    public void testSupportedEventType() {
        Class<ServiceMetricsEvent> clazz = serviceMetricsEventListener.supportedEventType();
        assertEquals(ServiceMetricsEvent.class, clazz);
    }

    @Test
    public void testOnEventSuccess() {
        when(pbsreg.getImplementationProxy(eq(ServiceMetricsProcessor.class), eq(pbs1Id)))
                .thenReturn(serviceMetricsProcessor);

        ServiceMetricsEvent event = Mockito.mock(ServiceMetricsEvent.class);
        serviceMetricsEventListener.onEvent(event);
        verify(serviceMetricsProcessor, times(1)).process(any(Message.class));

    }

    @Test
    public void testOnEventFail() {
        when(pbsreg.getImplementationProxy(eq(ServiceMetricsProcessor.class), eq(pbs1Id)))
                .thenReturn(serviceMetricsProcessor);
        doThrow(new AssertionStatusException("Backing-policy failed.")).when(serviceMetricsProcessor).process(any(Message.class));

        ServiceMetricsEvent event = Mockito.mock(ServiceMetricsEvent.class);
        serviceMetricsEventListener.onEvent(event);
        verify(serviceMetricsProcessor, times(1)).process(any(Message.class));
        // process method is called, but exception is caught by ServiceMetricsEventListener.onEvent() and logged.
    }

    @Test
    public void testJsonConversion() {
        ServiceMetricsEvent event = mock(ServiceMetricsEvent.class);
        when(event.getTime()).thenReturn(TIMESTAMP);
        when(event.getNodeId()).thenReturn(Goid.DEFAULT_GOID.toString());
        when(event.getNodeIp()).thenReturn("127.0.0.1");
        when(event.getNodeName()).thenReturn("localhost");
        when(event.getServiceId()).thenReturn(Goid.DEFAULT_GOID.toString());
        when(event.getServiceName()).thenReturn("Service");
        when(event.getServiceUri()).thenReturn("/service");
        when(event.getTotalBackendLatency()).thenReturn(100);
        when(event.getTotalFrontendLatency()).thenReturn(250);
        when(event.isPolicySuccessful()).thenReturn(true);
        when(event.isPolicyViolation()).thenReturn(false);
        when(event.isRoutingFailure()).thenReturn(false);

        String json = this.serviceMetricsEventListener.generateJson(event);
        assertNotNull(json);
        assertEquals(JSON_TEMPLATE, json);
    }

}
