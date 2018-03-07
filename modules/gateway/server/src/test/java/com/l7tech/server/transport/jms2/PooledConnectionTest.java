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
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PooledConnectionTest {
    @Mock
    SingleSessionHolder mockSingleSession;
    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    JmsEndpoint mockJmsEndpoint;
    @Mock
    JmsConnection mockJmsConnection;
    @Mock
    JmsEndpointConfig.JmsEndpointKey mockJmsEndpointKey;

    JmsResourceManagerConfig cacheConfig;

    PooledConnection fixture;

    @Before
    public void setUp() throws Exception {
        cacheConfig = new JmsResourceManagerConfig(
                0L,
                300000L,
                120000L,
                0,
                2,
                JmsConnection.DEFAULT_CONNECTION_POOL_SIZE,
                50L,//Default MaxWait is too long for the test
                JmsConnection.DEFAULT_CONNECTION_POOL_EVICT_INTERVAL,
                JmsConnection.DEFAULT_CONNECTION_POOL_SIZE,
                0,
                0,
                0L);

        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(mockJmsEndpointKey);
        when(mockJmsEndpointKey.toString()).thenReturn("mockJmsEndpointKey");
        Properties props = new Properties();
        when(mockJmsConnection.properties()).thenReturn(props);
        fixture = spy(new PooledConnection(mockJmsEndpointConfig, cacheConfig) {
            @Override
            protected SingleSessionHolder newConnection(final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
                return mock(SingleSessionHolder.class);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void testBorrow2Connections() throws Exception {
        SessionHolder borrowed1 = fixture.borrowConnection();
        SessionHolder borrowed2 = fixture.borrowConnection();
        assertEquals(2, fixture.pool.getNumActive());
        assertTrue(borrowed1 != borrowed2);
    }

    @Test(expected = JmsConnectionMaxWaitException.class)
    public void testBorrow3Connections() throws Exception {
        fixture.borrowConnection();
        fixture.borrowConnection();
        fixture.borrowConnection();
    }

    @Test
    public void testReturn1Connection() throws Exception {
        SessionHolder borrowed1 = fixture.borrowConnection();
        assertEquals(1, fixture.pool.getNumActive());
        fixture.returnConnection(borrowed1);
        assertEquals(0, fixture.pool.getNumActive());
        assertEquals(1, fixture.pool.getNumIdle());
    }

    @Test
    public void close() throws Exception {
        fixture.close();
        assertTrue(fixture.pool.isClosed());
    }

    @Test
    public void invalidate() throws Exception {
        SessionHolder borrowed = fixture.borrowConnection();
        fixture.invalidate(borrowed);
        verify(borrowed, times(1)).unRef();
        assertEquals(0, fixture.pool.getNumActive());
    }

    @Test
    public void isIdleTimeoutExpired() throws Exception {
        AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis() - 400000);
        when(fixture.getLastAccessTime()).thenReturn(lastAccessTime);
        assertTrue(fixture.isIdleTimeoutExpired());
    }

    @Test
    public void testIdleTimeoutNotExpired() throws Exception {
        AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis() - 250000);
        when(fixture.getLastAccessTime()).thenReturn(lastAccessTime);
        assertFalse(fixture.isIdleTimeoutExpired());
    }

    @Test
    public void testPoolIsActive() throws Exception {
        SessionHolder borrowed = fixture.borrowConnection();
        AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis() - 400000);
        when(fixture.getLastAccessTime()).thenReturn(lastAccessTime);
        assertFalse(fixture.isIdleTimeoutExpired());
    }

}