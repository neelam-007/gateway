package com.l7tech.server.message.metrics;

import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.ServiceFinished;
import com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Unit Tests for {@link GatewayMetricsPublisher}
 */
@RunWith(MockitoJUnitRunner.class)
public class GatewayMetricsPublisherTest {
    @Mock
    private AssertionModuleUnregistrationEvent assertionModuleUnregistrationEvent;

    @Mock
    private AssertionFinished assertionFinished;

    @Mock
    private ServiceFinished serviceFinished;

    private GatewayMetricsPublisher publisher;

    @Mock
    private GatewayMetricsListener subscriber;

    @Before
    public void setUp() throws Exception {
        publisher = new GatewayMetricsPublisher();
    }

//TODO: write test that covers module unload

    @Test
    public void testAddRemoveListener() throws Exception {
        assertFalse("Subscriber set should be empty", publisher.hasSubscribers());

        publisher.addListener(subscriber);
        assertTrue("Subscriber set should not be empty", publisher.hasSubscribers());

        publisher.removeListener(subscriber);
        assertFalse("Subscriber set should be empty", publisher.hasSubscribers());
    }

    @Test
    public void testPublishEvent() throws Exception {
        publisher.addListener(subscriber);
        assertTrue("Subscriber set should not be empty", publisher.hasSubscribers());

        Mockito.verify(subscriber, Mockito.never()).assertionFinished(Mockito.any(AssertionFinished.class));
        publisher.publishEvent(assertionFinished);
        Mockito.verify(subscriber, Mockito.times(1)).assertionFinished(assertionFinished);

        Mockito.verify(subscriber, Mockito.never()).serviceFinished(Mockito.any(ServiceFinished.class));
        publisher.publishEvent(serviceFinished);
        Mockito.verify(subscriber, Mockito.times(1)).serviceFinished(serviceFinished);
        Mockito.verify(subscriber, Mockito.times(1)).assertionFinished(assertionFinished); // make sure it remains one
    }
}