package com.l7tech.external.assertions.mqnative.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Test the MqNativeListenerThread component without dependency on a live MQ server.
 */
@RunWith(MockitoJUnitRunner.class)
public class MqNativeListenerThreadTest extends AbstractJUnit4SpringContextTests {

    /**
     * Exercise polling thread logic in MqNativeListenerThread.run()
     */
    @Test
    public void listenerThreadRun() {
        // TODO
    }
}
