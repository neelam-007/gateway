package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsEndpointListener;
import com.l7tech.server.util.ThreadPoolBean;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.springframework.context.ApplicationContext;

import java.util.Properties;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PooledJmsEndpointListenerFactoryTest {

    private ApplicationContext applicationContext;
    private ThreadPoolBean threadPoolBean;
    private PooledJmsEndpointListenerFactory factory;
    private JmsConnectionManager jmsConnectionManager;
    private JmsEndpointManager jmsEndpointManager;

    @Before
    public void setup() {
        applicationContext = ApplicationContexts.getTestApplicationContext();
        threadPoolBean = new ThreadPoolBean(ServerConfig.getInstance(), "Test","","", 25);
        threadPoolBean.start();
        factory = new PooledJmsEndpointListenerFactory(threadPoolBean);
        factory.setApplicationContext(applicationContext);
        jmsConnectionManager = (JmsConnectionManager) applicationContext.getBean("jmsConnectionManager");
        jmsEndpointManager = (JmsEndpointManager) applicationContext.getBean("jmsEndpointManager");
    }

    @After
    public void tearDown() {
        threadPoolBean.shutdown();
    }


    @Test
    public void testDedicatedThreadPool() throws Exception {


        //Setup connection
        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setGoid(new Goid(0, 1));
        jmsConnection.setInitialContextFactoryClassname("");
        Properties properties = new Properties();
        properties.setProperty(JmsConnection.PROP_IS_DEDICATED_POOL, Boolean.TRUE.toString());
        properties.setProperty(JmsConnection.PROP_DEDICATED_POOL_SIZE, Integer.toString(5));
        jmsConnection.properties(properties);
        jmsConnection.setQueueFactoryUrl("ConnectionFactory");
        jmsConnection.setJndiUrl("");
        jmsConnectionManager.save(jmsConnection);

        //Setup Endpoint
        JmsEndpoint connector = new JmsEndpoint();
        connector.setGoid(new Goid(0,1));
        connector.setName("Inbound");
        connector.setQueue(true);
        connector.setDestinationName("inbound");
        connector.setConnectionGoid(jmsConnection.getGoid());
        connector.setReplyType(JmsReplyType.NO_REPLY);

        jmsEndpointManager.save(connector);

        JmsEndpointConfig jmsEndpointConfig = new JmsEndpointConfig(jmsConnection, connector, null, applicationContext);
        JmsEndpointListener listener = factory.createListener(jmsEndpointConfig);
        listener.start();


        //Setup connection
        JmsConnection jmsConnection2 = new JmsConnection();
        jmsConnection2.setGoid(new Goid(0,2));
        jmsConnection2.setInitialContextFactoryClassname("");
        Properties properties2 = new Properties();
        properties2.setProperty(JmsConnection.PROP_IS_DEDICATED_POOL, Boolean.TRUE.toString());
        properties2.setProperty(JmsConnection.PROP_DEDICATED_POOL_SIZE, Integer.toString(10));
        jmsConnection2.properties(properties2);
        jmsConnection2.setQueueFactoryUrl("ConnectionFactory");
        jmsConnection2.setJndiUrl("");
        jmsConnectionManager.save(jmsConnection);

        //Setup Endpoint
        JmsEndpoint connector2 = new JmsEndpoint();
        connector2.setGoid(new Goid(0,1));
        connector2.setName("Inbound");
        connector2.setQueue(true);
        connector2.setDestinationName("inbound");
        connector2.setConnectionGoid(jmsConnection.getGoid());
        connector2.setReplyType(JmsReplyType.NO_REPLY);

        jmsEndpointManager.save(connector);

        JmsEndpointConfig jmsEndpointConfig2 = new JmsEndpointConfig(jmsConnection2, connector2, null, applicationContext);
        JmsEndpointListener listener2 = factory.createListener(jmsEndpointConfig2);

        assertFalse(((PooledJmsEndpointListenerImpl) listener).getThreadPoolBean() == ((PooledJmsEndpointListenerImpl) listener2).getThreadPoolBean());
        assertFalse(((PooledJmsEndpointListenerImpl) listener).getThreadPoolBean() == ((PooledJmsEndpointListenerImpl) listener2).getThreadPoolBean());

        listener2.stop();
        listener.stop();
        assertTrue(((PooledJmsEndpointListenerImpl) listener).getThreadPoolBean().isShutdown());

    }

    @Test
    public void testShareThreadPool() throws Exception {

        //Setup connection
        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setGoid(new Goid(0,1));
        jmsConnection.setInitialContextFactoryClassname("");
        Properties properties = new Properties();
        jmsConnection.properties(properties);
        jmsConnection.setQueueFactoryUrl("ConnectionFactory");
        jmsConnection.setJndiUrl("");
        jmsConnectionManager.save(jmsConnection);

        //Setup Endpoint
        JmsEndpoint connector = new JmsEndpoint();
        connector.setGoid(new Goid(0,1));
        connector.setName("Inbound");
        connector.setQueue(true);
        connector.setDestinationName("inbound");
        connector.setConnectionGoid(jmsConnection.getGoid());
        connector.setReplyType(JmsReplyType.NO_REPLY);

        jmsEndpointManager.save(connector);

        JmsEndpointConfig jmsEndpointConfig = new JmsEndpointConfig(jmsConnection, connector, null, applicationContext);
        JmsEndpointListener listener = factory.createListener(jmsEndpointConfig);
        listener.start();


        //Setup connection
        JmsConnection jmsConnection2 = new JmsConnection();
        jmsConnection2.setGoid(new Goid(0,2));
        jmsConnection2.setInitialContextFactoryClassname("");
        Properties properties2 = new Properties();
        properties2.setProperty(JmsConnection.PROP_IS_DEDICATED_POOL, Boolean.TRUE.toString());
        properties2.setProperty(JmsConnection.PROP_DEDICATED_POOL_SIZE, Integer.toString(10));
        jmsConnection2.properties(properties2);
        jmsConnection2.setQueueFactoryUrl("ConnectionFactory");
        jmsConnection2.setJndiUrl("");
        jmsConnectionManager.save(jmsConnection);

        //Setup Endpoint
        JmsEndpoint connector2 = new JmsEndpoint();
        connector2.setGoid(new Goid(0,1));
        connector2.setName("Inbound");
        connector2.setQueue(true);
        connector2.setDestinationName("inbound");
        connector2.setConnectionGoid(jmsConnection.getGoid());
        connector2.setReplyType(JmsReplyType.NO_REPLY);

        jmsEndpointManager.save(connector);

        JmsEndpointConfig jmsEndpointConfig2 = new JmsEndpointConfig(jmsConnection2, connector2, null, applicationContext);
        JmsEndpointListener listener2 = factory.createListener(jmsEndpointConfig2);

        assertFalse(((PooledJmsEndpointListenerImpl) listener).getThreadPoolBean() == ((PooledJmsEndpointListenerImpl) listener2).getThreadPoolBean());
        assertFalse(((PooledJmsEndpointListenerImpl) listener).getThreadPoolBean() == ((PooledJmsEndpointListenerImpl) listener2).getThreadPoolBean());

        //Make sure to stop the listener but the threadpool is still alive for share pool
        listener.stop();
        listener2.stop();
        assertFalse(((PooledJmsEndpointListenerImpl) listener).getThreadPoolBean().isShutdown());
        assertTrue(((PooledJmsEndpointListenerImpl) listener2).getThreadPoolBean().isShutdown());

    }
}
