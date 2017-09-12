package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.JMSException;
import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class JmsResourceManagerTest {

    JmsResourceManager fixture;
    JmsResourceManagerStub.CachedConnectionStub cachedConnection;

    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    JmsEndpointConfig.JmsEndpointKey jmsEndpointKey;
    @Mock
    JmsEndpoint mockJmsEndpoint;
    @Mock
    JmsConnection jmsConnection;
    @Mock
    JmsBag mockJmsBag;

    @Before
    public void setUp() throws Exception {
        fixture = new JmsResourceManagerStub("test", new Config() {

            @Override
            public String getProperty(String propertyName) {
                return null;
            }

            @Override
            public String getProperty(String propertyName, String defaultValue) {
                return null;
            }

            @Override
            public int getIntProperty(String propertyName, int defaultValue) {
                return 0;
            }

            @Override
            public long getLongProperty(String propertyName, long defaultValue) {
                return 0;
            }

            @Override
            public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
                return false;
            }

            @Override
            public long getTimeUnitProperty(String propertyName, long defaultValue) {
                return 0;
            }
        });


        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(jmsEndpointKey);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        when(mockJmsEndpointConfig.getConnection()).thenReturn(jmsConnection);
        when(jmsEndpointKey.toString()).thenReturn("JmsEndpointKey");
        when(mockJmsEndpoint.getVersion()).thenReturn(1234);
        when(jmsConnection.getVersion()).thenReturn(5678);
        Properties properties = new Properties();
        properties.setProperty(JmsConnection.PROP_SESSION_POOL_SIZE, String.valueOf(-1));
        when(jmsConnection.properties()).thenReturn(properties);
    }

    @Test (expected = NamingException.class)
    public void testDoWithJmsResources() throws Exception {
        cachedConnection = ((JmsResourceManagerStub)fixture).new CachedConnectionStub(mockJmsEndpointConfig, mockJmsBag, new NamingException("Correct exception"));
        ((JmsResourceManagerStub)fixture).setCachedConnection(cachedConnection);

        fixture.doWithJmsResources(mockJmsEndpointConfig, new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                fail("Should not be here");
            }
        });
    }

    @Test
    public void testDoWithJmsResource_sessionPoolSizeZero() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(JmsConnection.PROP_SESSION_POOL_SIZE, String.valueOf(0));
        when(jmsConnection.properties()).thenReturn(properties);
        List<JmsBag> jmsBagList = new ArrayList<>();
        cachedConnection = ((JmsResourceManagerStub)fixture).new CachedConnectionStub(mockJmsEndpointConfig, mockJmsBag);
        ((JmsResourceManagerStub)fixture).setCachedConnection(cachedConnection);
        fixture.doWithJmsResources(mockJmsEndpointConfig, new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                jmsBagList.add(bag);
            }
        });
        fixture.doWithJmsResources(mockJmsEndpointConfig, new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                jmsBagList.add(bag);
            }
        });

        assertNotEquals(jmsBagList.get(0), jmsBagList.get(1));
    }

    @Test
    public void testDoWithJmsResource_sessionPoolSizeNotZero() throws Exception {

        List<JmsBag> jmsBagList = new ArrayList<>();
        cachedConnection = ((JmsResourceManagerStub)fixture).new CachedConnectionStub(mockJmsEndpointConfig, mockJmsBag);
        ((JmsResourceManagerStub)fixture).setCachedConnection(cachedConnection);
        fixture.doWithJmsResources(mockJmsEndpointConfig, new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                jmsBagList.add(bag);
            }
        });
        fixture.doWithJmsResources(mockJmsEndpointConfig, new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                jmsBagList.add(bag);
            }
        });
        assertEquals(jmsBagList.get(0), jmsBagList.get(1));
    }

    public class JmsResourceManagerStub extends JmsResourceManager {

        private JmsResourceManager.CachedConnection conn;

        /**
         * Create a new JMS Resource manager.
         * <p/>
         * <p>The name for the manager should be unique.</p>
         *
         * @param name   The name to use
         * @param config The configuration source.
         */
        public JmsResourceManagerStub(String name, Config config) {
            super(name, config);
        }

        public void setCachedConnection(final CachedConnection cc) {
            this.conn = cc;
            connectionHolder.put(jmsEndpointKey, cc);
        }

        protected class CachedConnectionStub extends CachedConnection {

            private final Exception exception;

            protected CachedConnectionStub(JmsEndpointConfig cfg, JmsBag owner) {
                this(cfg, owner, null);
            }

            protected CachedConnectionStub(JmsEndpointConfig cfg, JmsBag owner, Exception exception) {
                super(cfg, owner);
                this.exception = exception;
            }

            protected long getSessionPoolMaxWait() {
                return 1000L;
            }

            protected int getMaxSessionIdle() {
                return 1;
            }

            protected JmsBag makeJmsBag() throws Exception {
                if (exception != null)
                    throw exception;

                return mock(JmsBag.class);
            }
        }
    }
}