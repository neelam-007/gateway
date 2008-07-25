package com.l7tech.server.transport.jms2;

import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.transport.jms2.asynch.JmsThreadPool;

import org.springframework.context.ApplicationContext;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.Assert;

/**
 * User: vchan
 */
public class JmsThreadPoolTest {

    private ApplicationContext ctx;

    @Before
    public void setUp() throws Exception {

        if (ctx == null) {
            ctx = ApplicationContexts.getTestApplicationContext();
        }
    }

    @Ignore("This test creates a thread pool without stopping it")
    @Test
    public void testThreadPoolInit() {

        try {

            JmsThreadPool pool = JmsThreadPool.getInstance();
            Assert.assertNotNull(pool);
            
        } catch (Exception ex) {
            Assert.fail("Unexpected exception occurred: " + ex);
        }
    }


}
