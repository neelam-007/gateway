package com.l7tech.server.transport.jms2;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.server.transport.jms2.asynch.JmsThreadPool;
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;

/**
 * User: vchan
 */
public class JmsThreadPoolTest extends TestCase {

    private ApplicationContext ctx;

    protected void setUp() throws Exception {

        if (ctx == null) {
            ctx = ApplicationContexts.getTestApplicationContext();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    public void testThreadPoolInit() {

        try {

            JmsThreadPool pool = JmsThreadPool.getInstance();
            assertNotNull(pool);
            
        } catch (Exception ex) {
            fail("Unexpected exception occurred: " + ex);
        }
    }


}
