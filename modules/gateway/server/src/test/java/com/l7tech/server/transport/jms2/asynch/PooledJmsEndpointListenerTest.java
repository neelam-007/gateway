package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointListenerFactory;
import com.l7tech.server.transport.jms2.JmsTestCase;
import com.l7tech.server.util.ThreadPoolBean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.*;
import javax.naming.NamingException;
import java.util.List;

/**
 * User: vchan
 */
@Ignore("This test requires a JMS queue")
public class PooledJmsEndpointListenerTest extends JmsTestCase {

    // factory
    JmsEndpointListenerFactory factory;

    ThreadPoolBean jmsThreadPool;

//    List<JmsEndpointListener> listeners;

    @Override
    @Before
    public void setUp() throws Exception {

        super.setUp();

        if (factory == null) {
            // use a test factory

            ServerConfigStub configStub = new ServerConfigStub();
            configStub.putProperty("jmsListenerThreadLimit", "25");

            jmsThreadPool = new ThreadPoolBean(configStub, "JMS Thread Pool", "jmsListenerThreadLimit", "jms.listenerThreadLimit", 25);
            // asynch processing
            factory = new JmsEndpointListenerFactory() {
                @Override
                public JmsEndpointListener createListener(JmsEndpointConfig endpointConfig) {
                    return new TestEndpointListener(endpointConfig);
                }
            };

            // synchronous process
//            factory = new LegacyJmsEndpointListenerFactory() {
//                public JmsEndpointListener createListener(JmsEndpointConfig endpointConfig) {
//                    return new TestEndpointListener(endpointConfig);
//                }
//            };
        }
    }

    @After
    public void tearDown() throws Exception {

        super.tearDown();

        jmsThreadPool.shutdown();
    }


    /**
     * Test 01 - Simply populate the test queues and check that the listeners have processed
     * all messages.
     */
    @Test
    public void testCase01() {

        final int numListeners = 2;
//        final int numData = 10;
        List<JmsEndpointListener> listeners = null;

        try {

            JmsEndpointConfig[] cfgs = createEndpoints("testCase01", numListeners);

            // create the listeners
            listeners = createListeners(cfgs, factory);

            // start listeners
            for (JmsEndpointListener l: listeners) {
                l.start();
            }

            // create message producer threads
            spawnMessageProducers(cfgs, 0);

            // wait some time
            synchronized (this) {
                Thread.sleep(60000L); // runtime
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception encountered: " + ex);

        } finally {

            if (listeners != null) {
                for (JmsEndpointListener l: listeners) {
                    l.stop();
                }

                for (JmsEndpointListener l: listeners) {
                    l.ensureStopped();
                }
            }
        }

        // compare populated vs result data
        compareTestData(testData, resultData);
    }


    /* ========================================================================================== */

    /*
     * Overridden task method to use a test task
     */
    class TestEndpointListener extends PooledJmsEndpointListenerImpl {
//    class TestEndpointListener extends LegacyJmsEndpointListenerImpl {

        TestEndpointListener(JmsEndpointConfig endpointConfig) {
            super(endpointConfig);
        }

//        protected void handleMessage(Message dequeueMessage) throws JmsRuntimeException {
//
////            try {
////                QueueSender q = getFailureSender();
////            } catch (Exception ex) {
////                ex.printStackTrace();
////            }
//
//            try {
//                dequeueMessage.acknowledge();
//
//                if (dequeueMessage instanceof BytesMessage) {
//
//                    BytesMessage msg = (BytesMessage) dequeueMessage;
//                    byte[] msgBody = new byte[(int)msg.getBodyLength()]; // we control test msg size
//
//                    String msgPayload;
//                    if (msg.readBytes(msgBody) > 0)
//                        msgPayload = new String(msgBody).trim();
//                    else
//                        msgPayload = "";
//
//                    if (_endpointCfg.isTransactional()) {
//                        try {
//                            getJmsBag().getSession().commit();
//                        } catch (Exception ex) {
//
//                        }
//                    }
//
//                    synchronized(resultLock) {
//                        resultData.put(msg.getJMSMessageID(), msgPayload);
//                    }
//
//                } else {
//                    throw new JmsRuntimeException("Only BytesMessagesExpected");
//                }
//            } catch (JMSException jex) {
//                throw new JmsRuntimeException("Couldn't read Jms BytesMessage", jex);
//            }
//        }

    }


    /*
     * Test JmsTask that just extracts the Jms message id + body
     */
    class TestJmsTask extends JmsTask {

        private boolean transactional;
        private javax.jms.Session jmsSession;

        TestJmsTask(JmsEndpointConfig endpoint, JmsBag jmsBag, Message jmsMessage, Queue failureQ ) {
            super(endpoint, jmsBag);

            this.transactional = endpoint.isTransactional();
            this.jmsSession = jmsBag.getSession();
        }

        // For test, just check that the message payload is correct
        public void onMessage(Message jmsMessage) {

            try {
                if (jmsMessage instanceof BytesMessage) {

                    BytesMessage msg = (BytesMessage) jmsMessage;
                    byte[] msgBody = new byte[(int)msg.getBodyLength()]; // we control test msg size

                    String msgPayload;
                    if (msg.readBytes(msgBody) > 0)
                        msgPayload = new String(msgBody).trim();
                    else
                        msgPayload = "";

                    if (transactional) {
                        jmsSession.commit();
                    }

                    synchronized(resultLock) {
                        resultData.put(msg.getJMSMessageID(), msgPayload);
                    }

                } else {
                }
            } catch (JMSException jex) {
            }
        }
    }
}
