package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class MemcachedRemoteCacheTest {

    private MemcachedRemoteCache cache;
    private MemcachedClient client;
    private RemoteCacheEntity entity;
    private GetFuture<Object> future;
    private OperationFuture<Boolean> futureOperation;
    private final String cacheKey = "testCacheKey";


    @Before
    public void setup() throws ExecutionException, InterruptedException {
        client = mock(MemcachedClient.class);
        future = mock(GetFuture.class);
        futureOperation = mock(OperationFuture.class);

        entity = new RemoteCacheEntity();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, "false");
        properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, "localhost:11211");
        entity.setProperties(properties);
    }

    /**
     * test Successful lookup a key of type byte[]
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetByteCacheMessage() throws Exception {
        when(client.asyncGet(cacheKey)).thenReturn(future);
        byte[] result = "\u0000\u0000\u0000\u0016text/xml;charset=UTF-8<memcached/>".getBytes();
        when(future.get(entity.getTimeout(), TimeUnit.SECONDS)).thenReturn(result);

        cache = new MemcachedRemoteCache(entity, client);
        CachedMessageData message = cache.get(cacheKey);
        assertEquals("text/xml;charset=UTF-8", message.getContentType());
        assertEquals("<memcached/>", new String(message.getBodyBytes()));
    }

    /**
     * test successful lookup a key of type json
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulGetJsonCacheMessage() throws Exception {
        when(client.asyncGet(cacheKey)).thenReturn(future);
        String result = "<memcached/>";
        when(future.get(entity.getTimeout(), TimeUnit.SECONDS)).thenReturn(result);

        cache = new MemcachedRemoteCache(entity, client);
        CachedMessageData message = cache.get(cacheKey);
        assertEquals("application/json; charset=utf-8", message.getContentType());
        assertEquals("<memcached/>", new String(message.getBodyBytes()));
    }

    /**
     * Test lookup fails when key is not found
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailNoCachedMessageFound() throws Exception {
        when(client.asyncGet(cacheKey)).thenReturn(future);
        when(future.get(entity.getTimeout(), TimeUnit.SECONDS)).thenReturn(null);

        cache = new MemcachedRemoteCache(entity, client);
        cache.get(cacheKey);
    }

    /**
     * test lookup fails when cache throws an exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testGetCacheMessageThrowsException() throws Exception {
        when(client.asyncGet(cacheKey)).thenThrow(TimeoutException.class);
        when(future.get(entity.getTimeout(), TimeUnit.SECONDS)).thenThrow(new CancellationException());

        cache = new MemcachedRemoteCache(entity, client);
        cache.get(cacheKey);
    }

    /**
     * test successful store of key-value of type byte[]
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSetByteCacheMessage() throws Exception {
        int expiryTime = (int) (System.currentTimeMillis() / 1000) + entity.getTimeout();
        CachedMessageData message = mock(CachedMessageData.class);
        when(message.getValueType()).thenReturn(CachedMessageData.ValueType.BYTE_ARRAY);
        when(client.set(anyString(), anyInt(), anyObject())).thenReturn(futureOperation);

        cache = new MemcachedRemoteCache(entity, client);
        cache.set(cacheKey, message, entity.getTimeout());

        verify(client).set(cacheKey, expiryTime, null);
        verify(futureOperation).get(entity.getTimeout(), TimeUnit.SECONDS);
    }

    /**
     * test successful store of key-value of type json
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulSetJsonCacheMessage() throws Exception {
        int expiryTime = (int) (System.currentTimeMillis() / 1000) + entity.getTimeout();
        CachedMessageData message = mock(CachedMessageData.class);
        when(message.getValueType()).thenReturn(CachedMessageData.ValueType.JSON);
        when(client.set(anyString(), anyInt(), anyObject())).thenReturn(futureOperation);

        cache = new MemcachedRemoteCache(entity, client);
        cache.set(cacheKey, message, entity.getTimeout());

        verify(client).set(cacheKey, expiryTime, null);
        verify(futureOperation).get(entity.getTimeout(), TimeUnit.SECONDS);
    }

    /**
     * test key-value is not set when cache throws an exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSetCacheMessageThrowsException() throws Exception {
        CachedMessageData message = mock(CachedMessageData.class);
        when(message.getValueType()).thenReturn(CachedMessageData.ValueType.JSON);
        when(client.set(anyString(), anyInt(), anyObject())).thenReturn(futureOperation);
        when(futureOperation.get(entity.getTimeout(), TimeUnit.SECONDS)).thenThrow(new CancellationException());

        cache = new MemcachedRemoteCache(entity, client);
        cache.set(cacheKey, message, entity.getTimeout());
    }

    /**
     * test successful message removal
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulMessageRemove() throws Exception {
        when(client.delete(cacheKey)).thenReturn(futureOperation);

        cache = new MemcachedRemoteCache(entity, client);
        cache.remove(cacheKey);

        verify(client).delete(cacheKey);
        verify(futureOperation).get(entity.getTimeout(), TimeUnit.SECONDS);
    }

    /**
     * test successful server shutdown
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulServerShutdown() throws Exception {
        cache = new MemcachedRemoteCache(entity, client);
        cache.shutdown();

        verify(client).shutdown(60, TimeUnit.SECONDS);
    }

    /**
     * test server list is correctly created
     *
     * @throws Exception
     */
    @Test
    public void testCreateServerList() throws Exception {
        cache = new MemcachedRemoteCache(entity);
        ArrayList addresses = cache.getServerList();

        assertEquals(1, addresses.size());
        assertEquals(new InetSocketAddress("localhost", 11211), addresses.get(0));
    }
}
