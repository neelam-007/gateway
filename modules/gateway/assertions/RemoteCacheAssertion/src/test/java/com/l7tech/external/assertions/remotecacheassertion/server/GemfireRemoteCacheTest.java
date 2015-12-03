package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


public class GemfireRemoteCacheTest {

    private GemfireRemoteCache cache;
    private RemoteCacheEntity entity;
    private GemFireClassLoader gemFireClassLoader;
    private Object gemfireRegion;
    private Object gemfireClientCache;
    private final String cacheKey = "testCacheKey";


    @Before
    public void setup() throws ExecutionException, InterruptedException {
        gemFireClassLoader = mock(GemFireClassLoader.class);
        gemfireRegion = mock(Object.class);
        gemfireClientCache = mock(Object.class);

        entity = new RemoteCacheEntity();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_NAME, "gemfire_cacheName");
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "locator");
        properties.put(GemfireRemoteCache.PROPERTY_SERVERS, "localhost:1234");
        entity.setProperties(properties);
    }

    /**
     * Test to successfully lookup a key
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetByteCacheMessage() throws Exception {
        Object entry = mock(Object.class);
        byte[] value = "\u0000\u0000\u0000\u0016text/xml;charset=UTF-8<gemfire/>".getBytes();

        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);
        when(gemFireClassLoader.getEntry(gemfireRegion, cacheKey)).thenReturn(entry);
        when(gemFireClassLoader.isEntryDestroyed(entry)).thenReturn(Boolean.FALSE);
        when(gemFireClassLoader.getEntryValue(entry)).thenReturn(value);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        CachedMessageData message = cache.get(cacheKey);
        assertEquals("text/xml;charset=UTF-8", message.getContentType());
        assertEquals("<gemfire/>", new String(message.getBodyBytes()));
    }

    /**
     * Test looking up a key fails when region is destroyed
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailedGetByteCacheMessageWhenRegionIsDestroyed() throws Exception {
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.TRUE);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.get(cacheKey);
    }

    /**
     * Test looking up a key fails when entry is null
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailedGetByteCacheMessageWhenEntryIsNotFound() throws Exception {
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);
        when(gemFireClassLoader.getEntry(gemfireRegion, cacheKey)).thenReturn(null);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.get(cacheKey);
    }

    /**
     * Test looking up a key fails when entry is destroyed
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailedGetByteCacheMessageWhenEntryIsDestroyed() throws Exception {
        Object entry = mock(Object.class);
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);
        when(gemFireClassLoader.getEntry(gemfireRegion, cacheKey)).thenReturn(entry);
        when(gemFireClassLoader.isEntryDestroyed(entry)).thenReturn(Boolean.TRUE);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.get(cacheKey);
    }

    /**
     * Test looking up a key fails when message is not of type byte[]
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailedGetByteCacheMessageWhenResultIsNotByte() throws Exception {
        Object entry = mock(Object.class);
        String value = "<gemfire/>";

        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);
        when(gemFireClassLoader.getEntry(gemfireRegion, cacheKey)).thenReturn(entry);
        when(gemFireClassLoader.isEntryDestroyed(entry)).thenReturn(Boolean.FALSE);
        when(gemFireClassLoader.getEntryValue(entry)).thenReturn(value);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.get(cacheKey);
    }

    /**
     * Test successful storing of key-value in cache
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSetCacheMessage() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.set(cacheKey, message, 0);

        verify(gemFireClassLoader).put(gemfireRegion, cacheKey, null);
    }

    /**
     * Test storing of key-value fails when region is destroyed
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSetCacheMessageFailsWhenRegionIsDestroyed() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.TRUE);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
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
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);
        doThrow(Exception.class).when(gemFireClassLoader).put(gemfireRegion, cacheKey, null);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.set(cacheKey, message, 0);
    }

    /**
     * Test successful removal of key-value from cache
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulMessageRemove() throws Exception {
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.remove(cacheKey);

        verify(gemFireClassLoader).remove(gemfireRegion, cacheKey);
    }

    /**
     * Test removal from cache fails when region is destroyed
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testMessageRemoveFailsWhenRegionIsDestroyed() throws Exception {

        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.TRUE);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.remove(cacheKey);
    }

    /**
     * Test removal from cache fails when removing throws an exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testMessageRemoveThrowsException() throws Exception {
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(Boolean.FALSE);
        doThrow(Exception.class).when(gemFireClassLoader).remove(gemfireRegion, cacheKey);

        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.remove(cacheKey);
    }

    /**
     * Test successful server shutdown
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulServerShutdown() throws Exception {
        cache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        cache.shutdown();

        verify(gemFireClassLoader).close(gemfireClientCache);
    }

    /**
     * test creation of  GemfireRemoteCache cache
     * Test the cacheFile creation is correct
     * Test gemfire properties are set
     *
     * @throws Exception
     */
    @Test
    public void testRemoteCacheCreation() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir") + "/SSG-GemFire-Cache/";
        String cacheFile = "GemfireRegionExpiration_" + entity.getProperties().get(GemfireRemoteCache.PROPERTY_CACHE_NAME) + "_.xml";

        Object clientCacheFactory = mock(Object.class);
        when(gemFireClassLoader.createClientCacheFactory()).thenReturn(clientCacheFactory);

        HashMap<String, String> properties = new HashMap<>();
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_NAME, "gemfire_cacheName");
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "server");
        properties.put(GemfireRemoteCache.PROPERTY_SERVERS, "server1:1234, server2:1235");
        properties.put("property1", "value1");
        entity.setProperties(properties);

        cache = new GemfireRemoteCache(entity, gemFireClassLoader);

        assertTrue(new File(tempDir).exists());
        verify(gemFireClassLoader).set(clientCacheFactory, "cache-xml-file", tempDir + cacheFile);
        verify(gemFireClassLoader, times(3)).set(anyObject(), anyString(), anyString());
        verify(gemFireClassLoader).set(clientCacheFactory, "property1", "value1");
        verify(gemFireClassLoader, times(0)).addPoolLocator(anyObject(), anyString(), anyInt());
        verify(gemFireClassLoader, times(2)).addPoolServer(anyObject(), anyString(), anyInt());
        verify(gemFireClassLoader).addPoolServer(clientCacheFactory, "server1", 1234);
        verify(gemFireClassLoader).addPoolServer(clientCacheFactory, "server2", 1235);
    }
}
