package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.util.Config;
import com.l7tech.util.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.jms.JMSException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JmsResourceManager.class)
@SuppressStaticInitializationFor("com.l7tech.server.transport.jms2.JmsResourceManager")
public class JmsResourceManagerTest {
    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    JmsBag mockJmsBag;
    @Mock
    JmsResourceManagerConfig mockResourceManagerConfig;
    @Mock
    Config mockConfig;

    JmsResourceManager fixture;
    private CachedConnection mockConnection;
    private SessionHolder mockSessionHolder;

    @Before
    public void setUp() throws Exception {

        JmsEndpointConfig.JmsEndpointKey jmsEndpointKey = mock(JmsEndpointConfig.JmsEndpointKey.class);
        JmsEndpoint jmsEndpoint = mock(JmsEndpoint.class);
        JmsConnection jmsConnection = mock(JmsConnection.class);
        Properties mockJmsConnectionProps = mock(Properties.class);

        when(mockJmsConnectionProps.getProperty(eq(JmsConnection.PROP_CONNECTION_POOL_SIZE), anyString())).thenReturn(String.valueOf(JmsConnection.DEFAULT_CONNECTION_POOL_SIZE));
        when(mockJmsConnectionProps.getProperty(eq(JmsConnection.PROP_CONNECTION_EVICTABLE_TIME), anyString())).thenReturn(String.valueOf(JmsConnection.DEFAULT_CONNECTION_MAX_AGE));
        when(mockJmsConnectionProps.getProperty(eq(JmsConnection.PROP_CONNECTION_POOL_ENABLE), anyString())).thenReturn("false");

        when(mockConfig.getTimeUnitProperty( "ioJmsConnectionCacheMaxIdleTime", JmsResourceManager.DEFAULT_CONNECTION_MAX_IDLE )).thenReturn(JmsResourceManager.DEFAULT_CONNECTION_MAX_IDLE);
        when(mockConfig.getTimeUnitProperty("ioJmsConnectionIdleTime", JmsResourceManager.DEFAULT_CONNECTION_IDLE_TIME)).thenReturn(JmsResourceManager.DEFAULT_CONNECTION_IDLE_TIME);
        when(mockConfig.getIntProperty("ioJmsConnectionPoolSize", JmsConnection.DEFAULT_CONNECTION_POOL_SIZE)).thenReturn(JmsConnection.DEFAULT_CONNECTION_POOL_SIZE);
        when(mockConfig.getIntProperty("ioJmsConnectionMinIdle", JmsConnection.DEFAULT_CONNECTION_POOL_MIN_IDLE)).thenReturn(JmsConnection.DEFAULT_CONNECTION_POOL_MIN_IDLE);
        when(mockConfig.getTimeUnitProperty("ioJmsConnectionMaxWait", JmsConnection.DEFAULT_CONNECTION_POOL_MAX_WAIT)).thenReturn(JmsConnection.DEFAULT_CONNECTION_POOL_MAX_WAIT);
        when(mockConfig.getTimeUnitProperty("ioJmsConnectionTimeBetweenEviction", JmsConnection.DEFAULT_CONNECTION_POOL_EVICT_INTERVAL)).thenReturn(JmsConnection.DEFAULT_CONNECTION_POOL_EVICT_INTERVAL);
        when(mockConfig.getIntProperty("ioJmsConnectionEvictionBatchSize", JmsConnection.DEFAULT_CONNECTION_POOL_SIZE)).thenReturn(JmsConnection.DEFAULT_CONNECTION_POOL_SIZE);
        when(mockConfig.getIntProperty("ioJmsSessionPoolSize", JmsConnection.DEFAULT_SESSION_POOL_SIZE)).thenReturn(JmsConnection.DEFAULT_SESSION_POOL_SIZE);
        when(mockConfig.getIntProperty("ioJmsSessionMaxIdle", JmsConnection.DEFAULT_SESSION_POOL_SIZE)).thenReturn(JmsConnection.DEFAULT_SESSION_POOL_SIZE);
        when(mockConfig.getTimeUnitProperty("ioJmsSessionMaxWait", JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT)).thenReturn(JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT);
        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(jmsEndpointKey);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(jmsEndpoint);
        when(mockJmsEndpointConfig.getConnection()).thenReturn(jmsConnection);
        when(jmsEndpointKey.toString()).thenReturn("JmsEndpointKey");
        when(jmsEndpoint.getVersion()).thenReturn(1234);
        when(jmsConnection.getVersion()).thenReturn(5678);
        when(jmsConnection.properties()).thenReturn(mockJmsConnectionProps);
        when(jmsConnection.getInitialContextFactoryClassname()).thenReturn("javax.jms.InitialContextFactory");

        Whitebox.setInternalState(JmsResourceManager.class, "CACHE_CLEAN_INTERVAL", 27937L);
        Whitebox.setInternalState(JmsResourceManager.class, "DEFAULT_CONNECTION_MAX_AGE", TimeUnit.MINUTES.toMillis( 30 ));
        Whitebox.setInternalState(JmsResourceManager.class,"DEFAULT_CONNECTION_MAX_IDLE", TimeUnit.MINUTES.toMillis( 5 ));
        Whitebox.setInternalState(JmsResourceManager.class, "DEFAULT_CONNECTION_IDLE_TIME", TimeUnit.MINUTES.toMillis(2));
        Whitebox.setInternalState(JmsResourceManager.class, "DEFAULT_CONNECTION_CACHE_SIZE", 100);
        Whitebox.setInternalState(JmsResourceManager.class, "logger", Logger.getLogger(JmsResourceManager.class.getName()));

        fixture = PowerMockito.spy(new JmsResourceManager("testJmsResourceManager", mockConfig));

        Whitebox.setInternalState(fixture, "cacheConfigReference", new AtomicReference<JmsResourceManagerConfig>( mockResourceManagerConfig ));
        Whitebox.setInternalState(fixture, "active", new AtomicBoolean(true));

        mockConnection = mock(CachedConnection.class);
        mockSessionHolder = mock(SessionHolder.class);
        when(mockConnection.borrowConnection()).thenReturn(mockSessionHolder);
        when(mockSessionHolder.borrowJmsBag()).thenReturn(mockJmsBag);
        doNothing().when(mockSessionHolder).doWithJmsResources(any(JmsResourceManager.JmsResourceCallback.class));
        PowerMockito.doReturn(mockConnection).when(fixture).getCachedConnection(any(JmsEndpointConfig.class), any(JmsResourceManagerConfig.class), eq(false));
    }

    @Test
    public void testDoWithJmsResources() throws Exception {
        final JmsResourceManager.JmsResourceCallback testCallback = new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                fail("This should not be called");
            }
        };
        fixture.doWithJmsResources(mockJmsEndpointConfig, testCallback);

        verify(mockSessionHolder, times(1)).doWithJmsResources(testCallback);
        verify(mockConnection, times(1)).returnConnection(mockSessionHolder);

        assertTrue(fixture.connectionHolder.containsValue(mockConnection));
    }

    @Test(expected = JmsRuntimeException.class)
    public void testDoWithJmsResourcesNotActive() throws Exception {
        Whitebox.setInternalState(fixture, "active", new AtomicBoolean(false));
        final JmsResourceManager.JmsResourceCallback testCallback = new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                fail("This should not be called");
            }
        };
        fixture.doWithJmsResources(mockJmsEndpointConfig, testCallback);
    }

    @Test
    public void testBorrowJmsBag() throws Exception {
        assertEquals(mockJmsBag, fixture.borrowJmsBag(mockJmsEndpointConfig));
        verify(mockSessionHolder, times(1)).borrowJmsBag();

    }

    @Test
    public void testReturnJmsBag() throws Exception {
        when(mockJmsBag.getBagOwner()).thenReturn(mockSessionHolder);
        JmsBag jmsBag = fixture.borrowJmsBag(mockJmsEndpointConfig);
        fixture.returnJmsBag(jmsBag);
        verify(mockSessionHolder, times(1)).returnJmsBag(mockJmsBag);
        verify(mockSessionHolder, times(1)).unRef();
    }

    @Test
    public void testInvalidate() throws Exception {
        fixture.borrowJmsBag(mockJmsEndpointConfig);
        fixture.invalidate(mockJmsEndpointConfig);
        verify(mockConnection, times(1)).close();
    }

    @Test
    public void testDestroy() throws Exception {
        fixture.borrowJmsBag(mockJmsEndpointConfig);
        fixture.destroy();
        verify(mockConnection).close();
        assertEquals(0,fixture.connectionHolder.size());
    }
}