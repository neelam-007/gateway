package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsBag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.naming.Context;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SessionHolderFactoryTest {

    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    JmsBag mockJmsBag;
    @Mock
    JmsConnection mockJmsConnection;
    @Mock
    JmsEndpoint mockJmsEndpoint;
    @Mock
    JmsEndpointConfig.JmsEndpointKey mockJmsEndpointKey;
    @Mock
    Properties mockProperties;

    @Before
    public void setUp() throws Exception {
        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(mockJmsEndpointKey);
        when(mockJmsEndpointKey.toString()).thenReturn("mockJmsEndpointKey");

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreatePooledSessionHolder() {
        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsConnection.properties()).thenReturn(mockProperties);
        when(mockProperties.getProperty(JmsConnection.PROP_SESSION_POOL_SIZE, "8" )).thenReturn("8");
        when(mockProperties.getProperty(JmsConnection.PROP_SESSION_POOL_MAX_WAIT, "5000")).thenReturn("50");
        when(mockProperties.getProperty(JmsConnection.PROP_MAX_SESSION_IDLE, "8")).thenReturn("8");
        SessionHolder sessionHolder = SessionHolderFactory.createSessionHolder(mockJmsEndpointConfig, mockJmsBag, new JmsResourceManagerConfig(
                0L,
                0L, //DEFAULT_CONNECTION_MAX_IDLE
                0L,
                0,
                0,
                0,
                0L,
                0L,
                0,
                JmsConnection.DEFAULT_SESSION_POOL_SIZE,
                JmsConnection.DEFAULT_SESSION_POOL_SIZE,
                JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT));


        assertEquals(PooledSessionHolder.class, sessionHolder.getClass());
    }

    @Test
    public void testCreateNonCachedSessionHolder() {
        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsConnection.properties()).thenReturn(mockProperties);
        when(mockProperties.getProperty(JmsConnection.PROP_SESSION_POOL_SIZE, "8" )).thenReturn("0");
        SessionHolder sessionHolder = SessionHolderFactory.createSessionHolder(mockJmsEndpointConfig, mockJmsBag, new JmsResourceManagerConfig(
                0L,
                0L, //DEFAULT_CONNECTION_MAX_IDLE
                0L,
                0,
                0,
                0,
                0L,
                0L,
                0,
                JmsConnection.DEFAULT_SESSION_POOL_SIZE,
                JmsConnection.DEFAULT_SESSION_POOL_SIZE,
                JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT));


        assertEquals(NonCachedSessionHolder.class, sessionHolder.getClass());
    }
}