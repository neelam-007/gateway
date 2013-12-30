package com.l7tech.server.transport.jms2;

import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.transport.jms2.asynch.PooledJmsEndpointListenerFactory;
import com.l7tech.gateway.common.Component;

import com.l7tech.server.util.ThreadPoolBean;
import org.junit.Ignore;
import org.junit.Assert;

/**
 * @author: vchan
 */
@Ignore("This test requires a JMS queue")
public class JmsBootProcessTest extends JmsTestCase {

    JmsBootProcess bootProcess;

    protected void setUp() throws Exception {
        super.setUp();

        // anything else to setup?
    }

    protected void tearDown() throws Exception {

        super.tearDown();

        if (bootProcess != null && bootProcess.isStarted()) {
            bootProcess.doStop();
            bootProcess = null;
        }
    }


    public void xxxtestLifecycle() {

        try {
            bootProcess = appCtx.getBean("jmsBootprocess", JmsBootProcess.class);
            Assert.assertNotNull("Could not obtain jmsBootProcess bean from applicationContext", bootProcess);
            startBootProcess();

            // wait for process to do it's thang
            waitForAWhile(15000L);

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception: " + ex);

        } catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected un-caught exception: " + ex);
        }
    }


    /**
     * Test single listener with one message on queue
     */
    public void testSingleListener() {

        final String testName = "BootTest.01";
        try {
            // create single jms test endpoint
            TestConnectionManager connMgr = new TestConnectionManager(testName, 1);
            TestEndpointManager endptMgr = new TestEndpointManager(testName);
            endptMgr.connMgr = connMgr;
            ThreadPoolBean jmsThreadPool = new ThreadPoolBean( config, "JMS Thread Pool", "jmsListenerThreadLimit",
                    "jms.listenerThreadLimit", 25);
            bootProcess = new JmsBootProcess(jmsThreadPool, licenseManager, connMgr, endptMgr, mapper, new PooledJmsEndpointListenerFactory(), null);
            bootProcess.setApplicationContext(appCtx);

            // add data to queue
            populateQueues(createEndpoints(connMgr, endptMgr), 15);

            // start Jms subsystem
            startBootProcess();

            // wait for process to do it's thang
            waitForAWhile(15000L); //

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception: " + ex);

        } catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected un-caught exception: " + ex);
        }
    }


    private void startBootProcess() throws LifecycleException {

        bootProcess.start();
        bootProcess.onApplicationEvent(new ReadyForMessages(this, Component.GW_SERVER, "192.168.1.123"));
    }

    private void waitForAWhile(long time) {
        synchronized(this) {

            long whenToStop = System.currentTimeMillis() + time;

            while (true) {
                try {
                    Thread.sleep(10000L);

                    if (System.currentTimeMillis() >= whenToStop) {

                        bootProcess.doStop();
                        break;

                    } else {
                        //todo get a reference to the thread pool and print it's stats
                    }
                    
                } catch(InterruptedException iex) {}
            }
        }
    }

}
