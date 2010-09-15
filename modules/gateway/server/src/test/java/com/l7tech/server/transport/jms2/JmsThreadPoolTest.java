package com.l7tech.server.transport.jms2;

import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.transport.jms2.asynch.JmsThreadPool;

import org.junit.Test;
import org.junit.Assert;

/**
 * User: vchan
 */
public class JmsThreadPoolTest {

    @Test
    public void testThreadPoolInit() {

        ServerConfigStub configStub = new ServerConfigStub();
        configStub.putProperty("jmsListenerThreadLimit", "25");

        JmsThreadPool pool = null;
        try {
            pool = new JmsThreadPool(configStub);
            Assert.assertNotNull(pool);

        } catch (Exception ex) {
            Assert.fail("Unexpected exception occurred: " + ex);
        } finally {
            if(pool != null){
                pool.shutdown();
            }
        }
    }

    /**
     * Tests that a value of 0 for jms.listenerThreadLimit does not cause the JmsThreadPool any issues, as the value
     * is ignored.
     */
    @Test
    public void testServerConfig(){
        ServerConfigStub configStub = new ServerConfigStub();
        configStub.putProperty("jmsListenerThreadLimit", "0");

        JmsThreadPool pool = null;
        try {
            pool = new JmsThreadPool(configStub);
            Assert.assertNotNull(pool);

        } catch (Exception ex) {
            Assert.fail("Unexpected exception occurred: " + ex);
        } finally {
            if(pool != null){
                pool.shutdown();
            }
        }
    }
}
