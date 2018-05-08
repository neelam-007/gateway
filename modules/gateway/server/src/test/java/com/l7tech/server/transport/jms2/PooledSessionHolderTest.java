package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import javax.jms.JMSException;
import javax.naming.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PooledSessionHolderTest {

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
    Context mockJndiContext;

    JmsResourceManagerConfig cacheConfig;

    PooledSessionHolder fixture;

    @Before
    public void setUp() throws Exception {
        cacheConfig = new JmsResourceManagerConfig(
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
                50L /*JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT*/);//default max wait is too long for the unit tests

        Properties props = new Properties();
        when(mockJmsConnection.properties()).thenReturn(props);
        when(mockJmsConnection.getVersion()).thenReturn(1);
        when(mockJmsConnection.getName()).thenReturn("mockJmsConnection");
        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(mockJmsEndpointKey);
        when(mockJmsEndpointKey.toString()).thenReturn("mockJmsEndpointKey");
        when(mockJmsBag.getJndiContext()).thenReturn(mockJndiContext);
        fixture = new PooledSessionHolder(mockJmsEndpointConfig, mockJmsBag, cacheConfig) {
            @Override
            protected JmsBag makeJmsBag() throws Exception {
                return mock(JmsBag.class);
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void testBorrow1JmsBag() throws Exception {
        JmsBag jmsBag = fixture.borrowJmsBag();
        assertNotNull(jmsBag);
        long lastAccessedTime1 = fixture.getLastAccessTime().get();
        assertTrue(lastAccessedTime1 >= fixture.getCreatedTime());
        assertEquals(1, fixture.pool.getNumActive());
        assertEquals(0, fixture.pool.getNumIdle());
    }

    @Test
    public void testBorrow2JmsBag() throws Exception {
        JmsBag jmsBag = fixture.borrowJmsBag();
        assertNotNull(jmsBag);
        long lastAccessedTime1 = fixture.getLastAccessTime().get();
        assertTrue(lastAccessedTime1 >= fixture.getCreatedTime());
        JmsBag jmsBag2 = fixture.borrowJmsBag();
        assertNotNull(jmsBag);
        long lastAccessedTime2 = fixture.getLastAccessTime().get();
        assertNotEquals(jmsBag, jmsBag2);
        assertTrue(lastAccessedTime2 >= lastAccessedTime1);
        assertEquals(2, fixture.pool.getNumActive());
        assertEquals(0, fixture.pool.getNumIdle());

    }

    @Test(expected = JmsRuntimeException.class)
    public void testBorrow9JmsBag() throws Exception {
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        assertNotNull( fixture.borrowJmsBag());
        fixture.borrowJmsBag();
    }

    @Test
    public void testReturnJmsBag() throws Exception {
        JmsBag jmsBag = fixture.borrowJmsBag();
        fixture.returnJmsBag(jmsBag);
        assertEquals(0, fixture.pool.getNumActive());
        assertEquals(1, fixture.pool.getNumIdle());
    }

    @Test
    public void testClose() throws Exception {
        assertNotNull(fixture.borrowJmsBag());
        fixture.close();
        assertTrue(fixture.pool.isClosed());
        verify(mockJmsBag, times(1)).close();
    }

    @Test
    public void testDoWithJmsResources() throws Exception {
        fixture.doWithJmsResources(new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                assertNotNull(bag);
                assertEquals(1, fixture.pool.getNumActive());
            }
        });
        assertEquals(1, fixture.pool.getNumIdle());
    }

    @Test
    public void testBorrowJmsBagWhenPoolSizeOne() throws Exception {
        SessionHolder sessionHolder = new PooledSessionHolder(mockJmsEndpointConfig, mockJmsBag, new JmsResourceManagerConfig(
                0L,
                0L, //DEFAULT_CONNECTION_MAX_IDLE
                0L,
                0,
                0,
                0,
                0L,
                0L,
                0,
                1,
                1,
                5000)) {
            @Override
            protected JmsBag makeJmsBag() throws Exception {
                return mock(JmsBag.class);
            }

        };
        JmsBag borrowedBag1 = sessionHolder.borrowJmsBag();
        sessionHolder.returnJmsBag(borrowedBag1);
        JmsBag borrowedBag2 = sessionHolder.borrowJmsBag();
        assertTrue(borrowedBag1 == borrowedBag2);
    }

    @Test
    public void testBorrowJmsBagWhenPoolSizeZero() throws Exception {
        SessionHolder sessionHolder = new NonCachedSessionHolder(mockJmsEndpointConfig, mockJmsBag) {
            @Override
            protected JmsBag makeJmsBag() throws Exception {
                return mock(JmsBag.class);
            }

        };
        JmsBag borrowedBag1 = sessionHolder.borrowJmsBag();
        sessionHolder.returnJmsBag(borrowedBag1);
        JmsBag borrowedBag2 = sessionHolder.borrowJmsBag();
        assertFalse(borrowedBag1 == borrowedBag2);
    }
}