package com.l7tech.server.transport.jms2;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.transport.jms.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import com.l7tech.server.transport.jms.JmsUtil;
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.NamingException;
import java.util.*;

/**
 * User: vchan
 */
public class JmsTestCase extends TestCase {

    // Spring initialized objects
    protected ApplicationContext appCtx;
    protected JmsPropertyMapper mapper;
    protected ServerConfig serverConfig;
    protected LicenseManager licenseManager;

    // Test data maps
    protected HashMap<String, String> testData;
    protected HashMap<String, String> resultData;

    // Message producer threads
    protected List<TestMessageProducer> testProducers;

    protected final Object dataLock = new Object();
    protected final Object resultLock = new Object();

    // set to true to run tests transactionally
    protected final boolean runTransactional = true;

    // Random number generator used for tasks
    protected final Random rand = new Random(System.currentTimeMillis());


    protected void setUp() throws Exception {

        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
        }

        if (mapper == null) {
            mapper = (JmsPropertyMapper) appCtx.getBean("jmsPropertyMapper", JmsPropertyMapper.class);
        }

        if (serverConfig == null) {
            serverConfig = (ServerConfig) appCtx.getBean("serverConfig", ServerConfig.class);
        }

        if (licenseManager == null) {
            licenseManager = (LicenseManager) appCtx.getBean("licenseManager", LicenseManager.class);
        }

        // reset objects
        testData = new HashMap<String, String>();
        resultData = new HashMap<String, String>();
        testProducers = new ArrayList<TestMessageProducer>();
    }


    protected void tearDown() throws Exception {

        // stop any/all msg producer threads
        if (testProducers != null) {

            for (TestMessageProducer prod : testProducers) {
                prod.stop = true;
            }
            testProducers.clear();
        }
        
    }

    protected List<JmsEndpointListener> createListeners(JmsEndpointConfig[] cfgs, JmsEndpointListenerFactory factory)
            throws Exception
    {
        List<JmsEndpointListener> listeners = new ArrayList<JmsEndpointListener>();

        for (JmsEndpointConfig c : cfgs) {
            listeners.add(factory.createListener(c));
        }

        return listeners;
    }


    protected List<JmsEndpointListener> createListenersWithData(JmsEndpointConfig[] cfgs, JmsEndpointListenerFactory factory, int numData)
            throws Exception
    {
        List<JmsEndpointListener> listeners = new ArrayList<JmsEndpointListener>();

        for (JmsEndpointConfig c : cfgs) {
            listeners.add(factory.createListener(c));
        }

        // now create test data in the queues
        populateQueues(cfgs, numData);

        return listeners;
    }

    protected JmsEndpointConfig[] createEndpoints(String testName) throws Exception {

        return createEndpoints(new TestConnectionManager(testName), new TestEndpointManager(testName));
    }

    protected JmsEndpointConfig[] createEndpoints(String testName, int numListener) throws Exception {

        return createEndpoints(new TestConnectionManager(testName, numListener), new TestEndpointManager(testName));
    }

    protected JmsEndpointConfig[] createEndpoints(TestConnectionManager connManager, TestEndpointManager endpointManager)
            throws Exception
    {
        Collection<JmsConnection> connections = connManager.findAll();

        // there is a one-to-one mapping between connection and endpoints

        List<JmsEndpointConfig> cfgList = new ArrayList<JmsEndpointConfig>();
        for (JmsConnection conn : connections) {
            cfgList.add(new JmsEndpointConfig(conn, endpointManager.findEndpointForConnection(conn), mapper, appCtx));
        }

        JmsEndpointConfig[] result = new JmsEndpointConfig[cfgList.size()];
        cfgList.toArray(result);
        return result;
    }

    protected void populateQueues(JmsEndpointConfig[] cfgs, int numData) {

        boolean fail = false;
        for (JmsEndpointConfig c : cfgs) {

            JmsBag bag = null;
            QueueSession session = null;
            Queue queue = null;
            QueueSender qSender = null;
            try {
                bag = JmsUtil.connect(c, c.isTransactional(), Session.CLIENT_ACKNOWLEDGE);
                session = (QueueSession) bag.getSession();
                queue = (Queue) bag.getJndiContext().lookup( c.getEndpoint().getDestinationName() );
                qSender = session.createSender(queue);

                String msgPayload = null;
                for (int i=0; i<numData; i++) {

                    msgPayload = "<test><id>"+c.getDisplayName()+i+"</id><payload>MyPayload</payload></test>";

                    BytesMessage msg = session.createBytesMessage();
                    msg.writeBytes(msgPayload.getBytes());
                    qSender.send(msg);

                    if (c.isTransactional()) {
                        session.commit();
                    }

                    synchronized(dataLock) {
                        testData.put(msg.getJMSMessageID(), msgPayload);
                    }
                }

            } catch (JMSException ex) {
                ex.printStackTrace();
            } catch (JmsConfigException ex) {
                ex.printStackTrace();
            } catch (NamingException ex) {
                ex.printStackTrace();
            } finally {

                if (qSender != null)
                    try { qSender.close(); } catch (Exception ex) {} // ignore

                if (bag != null)
                    bag.close();
            }
        }
    }


    protected void spawnMessageProducers(JmsEndpointConfig[] cfgs, int behaviour) {

        TestMessageProducer prod = null;
        for (JmsEndpointConfig c : cfgs) {
            prod = new TestMessageProducer(c, 0);
            testProducers.add(prod);
            prod.start();
        }
    }

    protected void compareTestData(HashMap<String, String> target, HashMap<String, String> results) {

        assertEquals(target.size(), results.size());

        Iterator<String> it = target.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            assertTrue(results.containsKey(key));
            assertEquals(target.get(key), results.get(key));
        }
    }

    /* ========================================================================================== */

    class TestConnectionManager extends JmsConnectionManagerStub {

        long OidRoot = 1000L;
        String testCase;
        Integer numConnections;

        Collection<JmsConnection> connections;

        TestConnectionManager(String testCase) {

            this.testCase = testCase;
        }

        TestConnectionManager(String testCase, Integer numConnections) {
            this(testCase);

            this.numConnections = numConnections;
        }

        Collection<JmsConnection> findAll(int count) throws FindException {

            if (connections == null) {
                Collection<JmsConnection> result = new ArrayList<JmsConnection>();
                for (int i=0; i<count; i++) {
                    JmsConnection conn = provider.createConnection(AMQ_QUEUE_DEFAULT+testCase+"."+i, AMQ_JNDI_URL);
                    conn.setOid(OidRoot++);
                    conn.setVersion(1);
                    result.add(conn);
                }
                connections = result;
            }
            return connections;
        }

        public Collection<JmsConnection> findAll() throws FindException {

            if (numConnections != null)
                return findAll(numConnections);

            return super.findAll();
        }
    }


    /* ========================================================================================== */

    class TestEndpointManager extends JmsEndpointManagerStub {

        long OidRoot = 1000L;
        String testCase;

        TestConnectionManager connMgr;

        TestEndpointManager(String testCase) {

            this.testCase = testCase;
        }

        public JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException {

            JmsEndpoint[] result = new JmsEndpoint[1];

            for (JmsConnection c : connMgr.findAll()) {

                if (c.getOid() == connectionOid) {
                    result[0] = findEndpointForConnection(c);
                    return result;
                }
            }

            return new JmsEndpoint[0];
        }

        protected JmsEndpoint findEndpointForConnection(JmsConnection conn) {

            JmsEndpoint endpt = new JmsEndpoint();
            endpt.setOid(OidRoot++);
            endpt.setVersion(1);
            endpt.setConnectionOid(conn.getOid());
            endpt.setName(conn.getName());
            endpt.setDestinationName(conn.getName());
            endpt.setReplyType(JmsReplyType.AUTOMATIC);
            endpt.setDisabled(false);
            endpt.setMaxConcurrentRequests(1);
            endpt.setMessageSource(true);
            endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
            endpt.setUseMessageIdForCorrelation(false);
            // set Transactional
            if (runTransactional)
                endpt.setAcknowledgementType(JmsAcknowledgementType.ON_COMPLETION);

            return endpt;
        }

    }

    /* ========================================================================================== */

    class TestMessageProducer extends Thread {

        JmsEndpointConfig[] endpoint;
        int behaviour;
        boolean stop;

        Random rand = new Random(System.currentTimeMillis());

        TestMessageProducer(JmsEndpointConfig cfg, int behaviour) {

            this.endpoint = new JmsEndpointConfig[] {cfg};
            this.behaviour = behaviour;
        }

        public void run() {

            while(true) {

                populateQueues(endpoint, nextRecordSize());
                
                rest();

                if (stop)
                    break;
            }
        }

        void rest() {
            synchronized(this) {

                try {
                    Thread.sleep(nextWaitInterval());
                } catch (InterruptedException ex) {}
            }
        }

        int nextRecordSize() {
            return Math.abs((rand.nextInt() % 15) + 1); // between 1 - 15 records
        }

        long nextWaitInterval() {
            return Math.abs((rand.nextLong() % 10L) + 1) * 1000L; // between 1 - 10 seconds
        }
    }

    /* ========================================================================================== */

    protected abstract class JmsTestTask implements Runnable {

        protected int failCount;
        protected List<String> failMessages = new ArrayList<String>();

        public int getFailCount() {
            return failCount;
        }

        public List<String> getFailMessages() {
            return failMessages;
        }

        protected long nextWaitInterval() {
            return Math.abs((rand.nextLong() % 10L) + 1) * 100L; // between 1/10 - 1 second
        }
    }
}
