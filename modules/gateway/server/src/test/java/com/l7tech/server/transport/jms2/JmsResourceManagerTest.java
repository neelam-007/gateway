package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.util.Config;
import org.apache.commons.pool.PoolableObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.JMSException;
import javax.naming.NamingException;

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

        JmsEndpointConfig.JmsEndpointKey jmsEndpointKey = mock(JmsEndpointConfig.JmsEndpointKey.class);
        JmsEndpoint jmsEndpoint = mock(JmsEndpoint.class);
        JmsConnection jmsConnection = mock(JmsConnection.class);

        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(jmsEndpointKey);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(jmsEndpoint);
        when(mockJmsEndpointConfig.getConnection()).thenReturn(jmsConnection);
        when(jmsEndpointKey.toString()).thenReturn("JmsEndpointKey");
        when(jmsEndpoint.getVersion()).thenReturn(1234);
        when(jmsConnection.getVersion()).thenReturn(5678);

        cachedConnection = ((JmsResourceManagerStub)fixture).new CachedConnectionStub(mockJmsEndpointConfig, mockJmsBag);
        ((JmsResourceManagerStub)fixture).setCachedConnection(cachedConnection);
    }

    @Test (expected = NamingException.class)
    public void testDoWithJmsResources() throws Exception {

        fixture.doWithJmsResources(mockJmsEndpointConfig, new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                fail("Should not be here");
            }
        });
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
        }

        protected CachedConnection getConnection(JmsEndpointConfig endpoint) throws NamingException, JmsRuntimeException {
            return conn;
        }

        protected class CachedConnectionStub extends CachedConnection {

            protected CachedConnectionStub(JmsEndpointConfig cfg, JmsBag bag) {
                super(cfg, bag);
                this.pool.setFactory(new PoolableObjectFactory<JmsBag>()
                {
                    @Override
                    public JmsBag makeObject() throws Exception {
                        throw new NamingException("Correct exception");
                    }

                    @Override
                    public void destroyObject(JmsBag jmsBag) throws Exception {

                    }

                    @Override
                    public boolean validateObject(JmsBag jmsBag) {
                        return false;
                    }

                    @Override
                    public void activateObject(JmsBag jmsBag) throws Exception {

                    }

                    @Override
                    public void passivateObject(JmsBag jmsBag) throws Exception {

                    }
                });
            }

            protected int getSessionPoolSize() {
                return -1;
            }

            protected long getSessionPoolMaxWait() {
                return 1000L;
            }

            protected int getMaxSessionIdle() {
                return 1;
            }
        }
    }
}