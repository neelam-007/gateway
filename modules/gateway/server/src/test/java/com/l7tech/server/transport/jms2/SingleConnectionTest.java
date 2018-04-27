package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.naming.NamingException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SingleConnectionTest {
    @Mock
    PooledSessionHolder mockPooledConnection;
    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    JmsEndpoint mockJmsEndpoint;
    @Mock
    JmsConnection mockJmsConnection;
    @Mock
    JmsEndpointConfig.JmsEndpointKey mockJmsEndpointKey;

    JmsResourceManagerConfig cacheConfig;

    SingleConnection fixture;

    @Before
    public void setUp() throws Exception {
        cacheConfig = new JmsResourceManagerConfig(
                0L,
                300000L, //DEFAULT_CONNECTION_MAX_IDLE
                0L,
                0,
                0,
                0,
                0L,
                0L,
                0,
                JmsConnection.DEFAULT_SESSION_POOL_SIZE,
                JmsConnection.DEFAULT_SESSION_POOL_SIZE,
                50L /*JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT*/);//default max wait is too long for the unit tests

        when(mockPooledConnection.getConnectionVersion()).thenReturn(1);
        when(mockPooledConnection.getName()).thenReturn("mockJmsConnection");
        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(mockJmsEndpointKey);
        when(mockJmsEndpointKey.toString()).thenReturn("mockJmsEndpointKey");
        Properties props = new Properties();
        when(mockJmsConnection.properties()).thenReturn(props);
        fixture = spy(new SingleConnection(mockJmsEndpointConfig, cacheConfig) {
            @Override
            protected SessionHolder newConnection(final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
                return mockPooledConnection;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void borrowConnection() throws Exception {
        SessionHolder connection = fixture.borrowConnection();
        assertEquals(mockPooledConnection, connection);
        verify(fixture, times(1)).touch();
        verify(mockPooledConnection, times(1)).ref();
    }

    @Test
    public void borrowConnection2times() throws Exception {
        SessionHolder borrow1 = fixture.borrowConnection();
        SessionHolder borrow2 = fixture.borrowConnection();
        assertEquals(mockPooledConnection, borrow1);
        assertEquals(mockPooledConnection, borrow2);
        verify(fixture, times(2)).touch();
        verify(mockPooledConnection, times(2)).ref();
    }

    @Test
    public void returnConnection() throws Exception {
        fixture.returnConnection(mockPooledConnection);
        verify(mockPooledConnection, times(1)).unRef();
    }

    @Test
    public void close() throws Exception {
        fixture.close();
        verify(mockPooledConnection, times(1)).close();
    }

    @Test
    public void invalidate() throws Exception {
        fixture.invalidate(any(SingleSessionHolder.class));
        verify(mockPooledConnection, times(1)).close();
    }

    @Test
    public void testIdleTimeoutExpired() throws Exception {
        AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis() - 400000);
        when(mockPooledConnection.getLastAccessTime()).thenReturn(lastAccessTime);
        assertTrue(fixture.isIdleTimeoutExpired());
    }

    @Test
    public void testIdleTimeoutNotExpired() throws Exception {
        AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis() - 25000);
        when(mockPooledConnection.getLastAccessTime()).thenReturn(lastAccessTime);
        assertFalse(fixture.isIdleTimeoutExpired());
    }

}