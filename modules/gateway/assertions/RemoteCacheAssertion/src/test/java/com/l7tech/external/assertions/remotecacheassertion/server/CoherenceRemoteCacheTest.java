package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


public class CoherenceRemoteCacheTest {

    private CoherenceRemoteCache cache;
    private RemoteCacheEntity entity;
    private CoherenceClassLoader coherenceClassLoader;
    private Object cacheClient;
    private Object configFactory;
    private ClusterPropertyManager propertyManager;
    private final String cacheKey = "testCacheKey";


    @Before
    public void setup() throws Exception {
        coherenceClassLoader = mock(CoherenceClassLoader.class);
        cacheClient = mock(Object.class);
        propertyManager = mock(ClusterPropertyManager.class);
        configFactory = mock(Object.class);

        when(coherenceClassLoader.newConfigurationFactory(anyString())).thenReturn(configFactory);
        when(coherenceClassLoader.ensureCache(configFactory, "coherence_cacheName")).thenReturn(cacheClient);

        entity = new RemoteCacheEntity();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, "coherence_cacheName");
        properties.put(CoherenceRemoteCache.PROP_SERVERS, "localhost:1234");
        entity.setProperties(properties);
    }

    /**
     * Test CoherenceRemoteCache constructor
     *
     * @throws Exception
     */
    @Test
    public void testCreateCoherenceRemoteCache() throws Exception {
        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);

        verify(propertyManager).getProperty(CoherenceRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY);
        verify(coherenceClassLoader).ensureCache(configFactory, "coherence_cacheName");
    }

    /**
     * Test CoherenceRemoteCache fails when classLoader throws an exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testCoherenceRemoteCacheThrowsException() throws Exception {
        when(coherenceClassLoader.newConfigurationFactory(anyString())).thenThrow(Exception.class);

        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);

        verify(propertyManager).getProperty(CoherenceRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY);
        verify(coherenceClassLoader, times(0)).ensureCache(anyObject(), anyString());
    }

    /**
     * Test to successfully lookup a key
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetCacheMessage() throws Exception {
        CachedMessageData value = mock(CachedMessageData.class);
        when(coherenceClassLoader.getCache(cacheClient, cacheKey)).thenReturn(value);

        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);
        CachedMessageData message = cache.get(cacheKey);

        assertEquals(value, message);
        verify(coherenceClassLoader).getCache(cacheClient, cacheKey);
    }

    /**
     * Test looking up a key fails when value is null
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailedGetCacheMessageWhenValueIsNull() throws Exception {
        when(coherenceClassLoader.getCache(cacheClient, cacheKey)).thenReturn(null);

        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);
        cache.get(cacheKey);

        verify(coherenceClassLoader).getCache(cacheClient, cacheKey);
    }

    /**
     * Test looking up a key throws an exception, when value is not of type CachedMessageData
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testGetCacheMessageThrowsExceptionWhenValueIsNotCachedMessageData() throws Exception {
        when(coherenceClassLoader.getCache(cacheClient, cacheKey)).thenReturn("value");

        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);
        cache.get(cacheKey);

        verify(coherenceClassLoader).getCache(cacheClient, cacheKey);
    }


    /**
     * Test successful storing of key-value in cache
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSetCacheMessage() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);

        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);

        //when expiry is 0
        cache.set(cacheKey, message, 0);
        verify(coherenceClassLoader).putCache(cacheClient, cacheKey, message, 0);

        //when expiry is -1
        cache.set(cacheKey, message, -1);
        verify(coherenceClassLoader).putCache(cacheClient, cacheKey, message, -1);

        //when expiry is not 0 or -1
        cache.set(cacheKey, message, 1);
        verify(coherenceClassLoader).putCache(cacheClient, cacheKey, message, 1000);
    }


    /**
     * Test successful removal of key-value from cache
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulMessageRemove() throws Exception {
        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);
        cache.remove(cacheKey);

        verify(coherenceClassLoader).removeFromCache(cacheClient, cacheKey);
    }

    /**
     * Test successful server shutdown
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulServerShutdown() throws Exception {
        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);
        cache.shutdown();

        verify(coherenceClassLoader).release(cacheClient);
    }

    /**
     * Test server shutdown fails when cache is null
     *
     * @throws Exception
     */
    @Test
    public void testServerShutdownWhenClientIsNull() throws Exception {
        when(coherenceClassLoader.ensureCache(configFactory, "coherence_cacheName")).thenReturn(null);

        cache = new CoherenceRemoteCache(entity, propertyManager, coherenceClassLoader);
        cache.shutdown();

        verify(coherenceClassLoader, times(0)).release(cacheClient);
    }

}
