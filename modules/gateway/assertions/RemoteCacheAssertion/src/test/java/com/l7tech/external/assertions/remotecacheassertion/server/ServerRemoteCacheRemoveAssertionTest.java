package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheRemoveAssertion;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.lang.reflect.InvocationTargetException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Test the RemoteCacheRemoveAssertion.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteCachesManagerImpl.class)
public class ServerRemoteCacheRemoveAssertionTest {

    @Mock
    ApplicationContext applicationContext;

    @Mock
    PolicyEnforcementContext policyEnforcementContext;

    @Mock
    RemoteCachesManagerImpl rcManagerImpl;

    @Mock
    RemoteCacheEntity remoteCacheEntity;

    @Mock
    MemcachedClient memcachedClient;

    @InjectMocks
    MemcachedRemoteCache memcachedRemoteCache;

    @Mock
    CoherenceClassLoader coherenceClassLoader;

    @Mock
    Object coherenceNamedCache;

    CoherenceRemoteCache coherenceRemoteCache;

    @Mock
    GemFireClassLoader gemFireClassLoader;

    @Mock
    Object gemfireRegion;

    @Mock
    Object gemfireClientCache;

    GemfireRemoteCache gemfireRemoteCache;

    @Mock
    TerracottaToolkitClassLoader terracottaToolkitClassLoader;

    @Mock
    Object terracottaCacheManager;

    @Mock
    Object terracottaCache;

    TerracottaRemoteCache terracottaRemoteCache;

    @InjectMocks
    RedisRemoteCache redisRemoteCache;
    @Mock
    JedisPool redisPool;
    @Mock
    Jedis redisClient;
    @Mock
    JedisCluster redisCluster;
    @Mock
    RemoteCacheEntity entity;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(ServerRemoteCacheRemoveAssertionTest.class);
        rcManagerImpl = mock(RemoteCachesManagerImpl.class);
        mockStatic(RemoteCachesManagerImpl.class);
        when(RemoteCachesManagerImpl.getInstance()).thenReturn(rcManagerImpl);
        applicationContext = mock(ApplicationContext.class);
        policyEnforcementContext = mock(PolicyEnforcementContext.class);

        coherenceRemoteCache = new CoherenceRemoteCache(remoteCacheEntity, coherenceClassLoader, coherenceNamedCache);
        gemfireRemoteCache = new GemfireRemoteCache(gemFireClassLoader, gemfireRegion, gemfireClientCache);
        terracottaRemoteCache = new TerracottaRemoteCache(terracottaToolkitClassLoader, terracottaCacheManager, terracottaCache);
    }

    /**
     * Memcached test with MemcachedClient mocked *
     */
    @Test
    public void testRemoveFromMemcached() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(memcachedRemoteCache);
        OperationFuture future = mock(OperationFuture.class);
        when(memcachedClient.delete(Mockito.anyString())).thenReturn(future);

        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.NONE, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromMemcachedThrowsException() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(memcachedRemoteCache);
        OperationFuture future = mock(OperationFuture.class);
        when(memcachedClient.delete(Mockito.anyString())).thenReturn(future);

        // The exceptions that are thrown by OperationFuture
        when(future.get(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenThrow(new CancellationException())
                .thenThrow(mock(ExecutionException.class))
                .thenThrow(new InterruptedException())
                .thenThrow(new TimeoutException());

        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    /**
     * Coherence test with CoherenceClassloader mocked *
     */
    @Test
    public void testRemoveFromCoherence() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(coherenceRemoteCache);

        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.NONE, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromCoherenceThrowsException() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(coherenceRemoteCache);
        String cacheKey = "testCacheKey";

        // Following exceptions can be thrown
        when(coherenceClassLoader.removeFromCache(coherenceNamedCache, cacheKey))
                .thenThrow(new ConcurrentModificationException())
                .thenThrow(mock(InvocationTargetException.class))
                .thenThrow(new IllegalAccessException());

        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    /**
     * Gemfire test with GemfireClassloader mocked *
     */
    @Test
    public void testRemoveFromGemfire() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(gemfireRemoteCache);

        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.NONE, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromGemfireRegionDestroyed() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(gemfireRemoteCache);
        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        // Following exceptions can be thrown
        when(gemFireClassLoader.isDestroyed(gemfireRegion)).thenReturn(true);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromGemfireThrowsException() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(gemfireRemoteCache);
        String cacheKey = "testCacheKey";

        // Following exceptions can be thrown
        doThrow(new IllegalAccessException()).doThrow(new IllegalArgumentException()).doThrow(mock(InvocationTargetException.class))
                .when(gemFireClassLoader).remove(gemfireRegion, cacheKey);

        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromTerracotta() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(terracottaRemoteCache);

        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.NONE, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromTerracottaNullCache() throws Exception {
        TerracottaRemoteCache tcRemoteCache = new TerracottaRemoteCache(terracottaToolkitClassLoader, terracottaCacheManager, null);
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(tcRemoteCache);

        String cacheKey = "testCacheKey";
        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    @Test
    public void testRemoveFromTerracottaThrowsException() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(terracottaRemoteCache);
        String cacheKey = "testCacheKey";

        // Following exceptions can be thrown
        doThrow(new IllegalAccessException()).doThrow(new IllegalArgumentException()).doThrow(mock(InvocationTargetException.class))
                .when(terracottaToolkitClassLoader).cacheRemove(terracottaCache, cacheKey);

        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));

        serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        assertEquals(AssertionStatus.FAILED, serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext));
    }

    /**
     * Test successful removal of the key for Redis Cache *
     */
    @Test
    public void testRemoveFromRedis() throws Exception {
        String cacheKey = "testCacheKey";

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(redisRemoteCache);
        when(redisPool.getResource()).thenReturn(redisClient);
        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.del(cacheKey)).thenReturn(1L);

        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        AssertionStatus status = serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext);

        assertEquals(AssertionStatus.NONE, status);
    }

    /**
     * Test removal of the key for Redis Cache when it throws exception *
     */
    @Test
    public void testRemoveFromRedisThrowsException() throws Exception {
        String cacheKey = "testCacheKey";

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(redisRemoteCache);
        when(redisPool.getResource()).thenReturn(redisClient);
        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.del(cacheKey)).thenThrow(mock(JedisException.class));

        RemoteCacheRemoveAssertion rcRemoveAssertion = new RemoteCacheRemoveAssertion();
        rcRemoveAssertion.setCacheEntryKey(cacheKey);

        ServerRemoteCacheRemoveAssertion serverRemoteCacheRemoveAssertion = new ServerRemoteCacheRemoveAssertion(rcRemoveAssertion, applicationContext);
        AssertionStatus status = serverRemoteCacheRemoveAssertion.checkRequest(policyEnforcementContext);

        assertEquals(AssertionStatus.FAILED, status);
    }
}