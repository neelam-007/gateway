package com.l7tech.server.transport.jms2;

import com.l7tech.common.Component;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.transport.jms2.synch.LegacyJmsEndpointListenerFactory;

/**
 * @author: vchan
 */
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
            bootProcess = (JmsBootProcess) appCtx.getBean("jmsBootprocess", JmsBootProcess.class);
            assertNotNull("Could not obtain jmsBootProcess bean from applicationContext", bootProcess);
            startBootProcess();

            // wait for process to do it's thang
            waitForAWhile(15000L);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception: " + ex);

        } catch (Throwable ex) {
            ex.printStackTrace();
            fail("Unexpected un-caught exception: " + ex);
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
            bootProcess = new JmsBootProcess(serverConfig, licenseManager, connMgr, endptMgr, mapper, null);
            bootProcess.setApplicationContext(appCtx);

            // choose the jms listener factory
            bootProcess.setJmsEndpointListenerFactory(new LegacyJmsEndpointListenerFactory());

            // add data to queue
            populateQueues(createEndpoints(connMgr, endptMgr), 15);

            // start Jms subsystem
            startBootProcess();

            // wait for process to do it's thang
            waitForAWhile(15000L); //

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception: " + ex);

        } catch (Throwable ex) {
            ex.printStackTrace();
            fail("Unexpected un-caught exception: " + ex);
        }
    }


    private void startBootProcess() throws LifecycleException {

        bootProcess.start();
//        bootProcess.onApplicationEvent(new LicenseEvent(this, Level.INFO, "Updated", "JUnit Test License"));
        bootProcess.onApplicationEvent(new ReadyForMessages(this, Component.GW_SERVER, "192.168.1.123"));
    }

    private void waitForAWhile(long time) {
        synchronized(this) {

            try {
                Thread.sleep(time);
                bootProcess.doStop();

            } catch(InterruptedException iex) {}
        }
    }

}
