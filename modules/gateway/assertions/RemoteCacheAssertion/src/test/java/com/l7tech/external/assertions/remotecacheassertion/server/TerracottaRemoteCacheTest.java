package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


public class TerracottaRemoteCacheTest {


    private TerracottaRemoteCache cache;
    private RemoteCacheEntity entity;
    private TerracottaToolkitClassLoader terracottaClassLoader;
    private Object cacheClient;
    private Object cacheManager;
    private final String cacheKey = "testCacheKey";


    @Before
    public void setup() throws ExecutionException, InterruptedException {
        terracottaClassLoader = mock(TerracottaToolkitClassLoader.class);
        cacheClient = mock(Object.class);
        cacheManager = mock(Object.class);

        entity = new RemoteCacheEntity();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(TerracottaRemoteCache.PROPERTY_CACHE_NAME, "terracotta_cacheName");
        properties.put(TerracottaRemoteCache.PROPERTY_URLS, "localhost:1234");
        entity.setProperties(properties);
    }

    /**
     * Test create terracotta remote cache successfully
     *
     * @throws Exception
     */
    @Test
    public void testCreateTerracottaRemoteCache() throws Exception {
        ClusterPropertyManager propertyManager = mock(ClusterPropertyManager.class);

        cache = new TerracottaRemoteCache(entity, propertyManager, terracottaClassLoader);

        verify(propertyManager).getProperty(TerracottaRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY);
    }

    /**
     * test no connection is created when no server is provided
     *
     * @throws Exception
     */
    @Test
    public void testTerracottaRemoteCacheIsNotCreatedWhenServerListIsBlank() throws Exception {
        entity = new RemoteCacheEntity();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(TerracottaRemoteCache.PROPERTY_URLS, "");
        entity.setProperties(properties);

        ClusterPropertyManager propertyManager = mock(ClusterPropertyManager.class);
        cache = new TerracottaRemoteCache(entity, propertyManager, terracottaClassLoader);

        verify(propertyManager, Mockito.times(0)).getProperty(TerracottaRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY);
    }

    /**
     * Test to successfully lookup a key
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetCacheMessage() throws Exception {
        Object element = mock(Object.class);
        CachedMessageData value = mock(CachedMessageData.class);
        when(terracottaClassLoader.cacheGet(cacheClient, cacheKey)).thenReturn(element);
        when(terracottaClassLoader.elementGetObjectValue(element)).thenReturn(value);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        CachedMessageData message = cache.get(cacheKey);

        assertEquals(value, message);
        verify(terracottaClassLoader).elementGetObjectValue(element);
        verify(terracottaClassLoader).cacheGet(cacheClient, cacheKey);
    }

    /**
     * Test looking up a key fails when cacheClient is null
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailedGetCacheMessageWhenCacheClientIsNull() throws Exception {
        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, null);
        cache.get(cacheKey);

        verify(terracottaClassLoader, times(0)).elementGetObjectValue(anyObject());
        verify(terracottaClassLoader, times(0)).cacheGet(cacheClient, cacheKey);
    }

    /**
     * Test looking up a key throws an exception, when there is no element in cache for the given key
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testGetCacheMessageThrowsExceptionWhenKeyIsNotFound() throws Exception {
        when(terracottaClassLoader.cacheGet(cacheClient, cacheKey)).thenReturn(null);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.get(cacheKey);

        verify(terracottaClassLoader).cacheGet(cacheClient, cacheKey);
    }

    /**
     * Test looking up a key fails when value is empty
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testGetCacheMessageThrowsExceptionWhenValueIsNull() throws Exception {
        Object element = mock(Object.class);
        when(terracottaClassLoader.cacheGet(cacheClient, cacheKey)).thenReturn(element);
        when(terracottaClassLoader.elementGetObjectValue(element)).thenReturn(null);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.get(cacheKey);

        verify(terracottaClassLoader).elementGetObjectValue(element);
        verify(terracottaClassLoader).cacheGet(cacheClient, cacheKey);
    }


    /**
     * Test successful storing of key-value in cache
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSetCacheMessage() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);
        Object element = mock(Object.class);
        when(terracottaClassLoader.newElement(cacheKey, message)).thenReturn(element);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.set(cacheKey, message, 0);

        verify(terracottaClassLoader).newElement(cacheKey, message);
        verify(terracottaClassLoader).elementSetTimeToLive(element, 0);
        verify(terracottaClassLoader).cachePut(cacheClient, element);
    }

    /**
     * Test storing of key-value fails when cacheClient is null
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void testSetCacheMessageFailsWhenRegionIsDestroyed() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, null);
        cache.set(cacheKey, message, 0);
    }

    /**
     * Test storing of key-value fails when storing key-value throws exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSetCacheMessageThrowsException() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);
        Object element = mock(Object.class);
        when(terracottaClassLoader.newElement(cacheKey, message)).thenReturn(element);
        doThrow(Exception.class).when(terracottaClassLoader).cachePut(cacheClient, element);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.set(cacheKey, message, 0);

        verify(terracottaClassLoader).newElement(cacheKey, message);
        verify(terracottaClassLoader).elementSetTimeToLive(element, 0);
    }

    /**
     * Test successful removal of key-value from cache
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulMessageRemove() throws Exception {
        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.remove(cacheKey);

        verify(terracottaClassLoader).cacheRemove(cacheClient, cacheKey);
    }

    /**
     * Test removal from cache fails when cache is null
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void testMessageRemoveFailsWhenCacheIsNull() throws Exception {
        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, null);
        cache.remove(cacheKey);
    }

    /**
     * Test removal from cache fails when removing throws an exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testMessageRemoveThrowsException() throws Exception {
        doThrow(Exception.class).when(terracottaClassLoader).cacheRemove(cacheClient, cacheKey);

        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.remove(cacheKey);
    }

    /**
     * Test successful server shutdown
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulServerShutdown() throws Exception {
        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, cacheClient);
        cache.shutdown();

        verify(terracottaClassLoader).cacheManagerShutdown(cacheManager);
    }

    /**
     * Test server shutdown fails when cache is null
     *
     * @throws Exception
     */
    @Test
    public void testServerShutdownFailsWhenCacheIsNull() throws Exception {
        cache = new TerracottaRemoteCache(terracottaClassLoader, cacheManager, null);
        cache.shutdown();

        verify(terracottaClassLoader, times(0)).cacheManagerShutdown(cacheManager);

    }
}
