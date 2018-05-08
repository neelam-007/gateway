package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class RedisRemoteCacheTest {

    private RedisRemoteCache redisRemoteCache;
    private JedisPool redisPool;
    private Jedis redisClient;
    private JedisCluster redisCluster;
    private RemoteCacheEntity clientEntity;
    private RemoteCacheEntity clusterEntity;
    private final String cacheKey = "testCacheKey";

    @Before
    public void setup() {
        redisPool = mock(JedisPool.class);
        redisClient = mock(Jedis.class);
        redisCluster = mock(JedisCluster.class);

        clientEntity = new RemoteCacheEntity();
        HashMap<String, String> properties = clientEntity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        clientEntity.setProperties(properties);

        clusterEntity = new RemoteCacheEntity();
        properties = clusterEntity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "true");
        clusterEntity.setProperties(properties);

        when(redisPool.getResource()).thenReturn(redisClient);
    }

    /**
     * Test successful removal of the key for Redis Cache
     */
    @Test
    public void testRemoveFromRedisClient() throws Exception {
        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.del(cacheKey)).thenReturn(1L);

        redisRemoteCache = new RedisRemoteCache(clientEntity, redisPool, redisCluster);
        redisRemoteCache.remove(cacheKey);

        verify(redisClient).del(cacheKey);
        verify(redisCluster, times(0)).del(cacheKey);
        verify(redisClient).close();
    }

    /**
     * Test successful removal of the key for Redis Cache
     */
    @Test
    public void testRemoveFromRedisCluster() throws Exception {
        when(redisCluster.del(cacheKey)).thenReturn(1L);

        redisRemoteCache = new RedisRemoteCache(clusterEntity, redisPool, redisCluster);
        redisRemoteCache.remove(cacheKey);

        verify(redisCluster).del(cacheKey);
        verify(redisClient, times(0)).del(cacheKey);
        verify(redisClient, times(0)).close();
    }

    /**
     * Test shutting down redis
     */
    @Test
    public void testRedisCacheShutdown() throws Exception {
        redisRemoteCache = new RedisRemoteCache(clientEntity, redisPool, redisCluster);
        redisRemoteCache.shutdown();

        verify(redisCluster).close();
        verify(redisPool).close();
    }

    /**
     * Test to successfully store a key-value in a redis cluster
     */
    @Test
    public void testRedisClusterCacheStoreSuccess() throws Exception {
        CachedMessageData data = mock(CachedMessageData.class);
        when(data.toByteArray()).thenReturn(cacheKey.getBytes());
        when(redisCluster.set(anyString(), anyString())).thenReturn("OK");
        when(redisCluster.expire(anyString(), anyInt())).thenReturn(0L);

        redisRemoteCache = new RedisRemoteCache(clusterEntity, redisPool, redisCluster);
        redisRemoteCache.set(cacheKey, data, 0);

        verify(redisCluster).set(anyString(), anyString());
        verify(redisCluster).expire(anyString(), anyInt());
        verify(redisPool, times(0)).getResource();
        verify(redisClient, times(0)).get(cacheKey);
        verify(redisClient, times(0)).disconnect();
    }

    /**
     * Test to successfully store a key-value in a redis client
     */
    @Test
    public void testRedisClientCacheStoreSuccess() throws Exception {
        CachedMessageData data = mock(CachedMessageData.class);
        when(data.toByteArray()).thenReturn(cacheKey.getBytes());
        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.set(anyString(), anyString())).thenReturn("OK");
        when(redisClient.expire(anyString(), anyInt())).thenReturn(0L);

        redisRemoteCache = new RedisRemoteCache(clientEntity, redisPool, redisCluster);
        redisRemoteCache.set(cacheKey, data, 0);

        verify(redisPool).getResource();
        verify(redisClient).set(anyString(), anyString());
        verify(redisClient).expire(anyString(), anyInt());
        verify(redisClient).set(anyString(), anyString());
        verify(redisClient).close();
    }

    /**
     * Test to successfully lookup a key in a redis client
     */
    @Test
    public void testRedisClientCacheLookupSuccess() throws Exception {

        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.get(cacheKey)).thenReturn("\u0000\u0000\u0000\u0016text/xml;charset=UTF-8<redis_test/>");

        redisRemoteCache = new RedisRemoteCache(clientEntity, redisPool, redisCluster);
        redisRemoteCache.get(cacheKey);

        verify(redisClient).get(cacheKey);
        verify(redisPool).getResource();
        verify(redisClient).close();
        verify(redisCluster, times(0)).get(cacheKey);
    }

    /**
     * Test to successfully lookup a key in a redis cluster
     */
    @Test
    public void testRedisClusterCacheLookupSuccess() throws Exception {
        when(redisCluster.get(cacheKey)).thenReturn("\u0000\u0000\u0000\u0016text/xml;charset=UTF-8<redis_test/>");

        redisRemoteCache = new RedisRemoteCache(clusterEntity, redisPool, redisCluster);
        redisRemoteCache.get(cacheKey);

        verify(redisCluster).get(cacheKey);
        verify(redisClient, times(0)).get(cacheKey);
        verify(redisPool, times(0)).getResource();
        verify(redisClient, times(0)).close();
    }

    /**
     * test setting apache config properties are set correctly.
     * @throws Exception
     */
    @Test
    public void testRedis() throws Exception {
        GenericObjectPoolConfig newPoolConfig = new GenericObjectPoolConfig();
        newPoolConfig.setMaxIdle(9);
        newPoolConfig.setMaxTotal(10);
        newPoolConfig.setMinIdle(11);
        newPoolConfig.setFairness(Boolean.TRUE);
        newPoolConfig.setLifo(Boolean.FALSE);
        newPoolConfig.setMaxWaitMillis(2L);
        newPoolConfig.setMinEvictableIdleTimeMillis(1L);
        newPoolConfig.setSoftMinEvictableIdleTimeMillis(3L);
        newPoolConfig.setNumTestsPerEvictionRun(4);
        newPoolConfig.setTestOnBorrow(Boolean.TRUE);
        newPoolConfig.setTestOnCreate(Boolean.FALSE);
        newPoolConfig.setTestOnReturn(Boolean.TRUE);
        newPoolConfig.setTestWhileIdle(Boolean.TRUE);
        newPoolConfig.setTimeBetweenEvictionRunsMillis(4L);
        newPoolConfig.setBlockWhenExhausted(Boolean.FALSE);
        newPoolConfig.setJmxEnabled(Boolean.FALSE);
        newPoolConfig.setJmxNamePrefix("testPrefix");
        newPoolConfig.setJmxNameBase("testName");

        clientEntity = new RemoteCacheEntity();
        HashMap<String, String> properties = clientEntity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        properties.put(RedisRemoteCache.APACHE_POOL_MAX_IDLE, "9" );
        properties.put(RedisRemoteCache.APACHE_POOL_MAX_TOTAL, "10");
        properties.put(RedisRemoteCache.APACHE_POOL_MIN_IDLE, "11");
        properties.put(RedisRemoteCache.APACHE_POOL_FAIRNESS, "true");
        properties.put(RedisRemoteCache.APACHE_POOL_LIFO, "false");
        properties.put(RedisRemoteCache.APACHE_POOL_MAX_WAIT_MILLIS, "2");
        properties.put(RedisRemoteCache.APACHE_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, "1");
        properties.put(RedisRemoteCache.APACHE_POOL_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS, "3");
        properties.put(RedisRemoteCache.APACHE_POOL_NUM_TESTS_PER_EVICTION_RUN, "4");
        properties.put(RedisRemoteCache.APACHE_POOL_TEST_ON_RETURN, "true");
        properties.put(RedisRemoteCache.APACHE_POOL_TEST_ON_CREATE, "false");
        properties.put(RedisRemoteCache.APACHE_POOL_TEST_ON_BORROW, "true");
        properties.put(RedisRemoteCache.APACHE_POOL_TEST_WHILE_IDLE, "true");
        properties.put(RedisRemoteCache.APACHE_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS, "4");
        properties.put(RedisRemoteCache.APACHE_POOL_BLOCK_WHEN_EXHAUSTED, "false");
        properties.put(RedisRemoteCache.APACHE_POOL_JMX_ENABLED, "false");
        properties.put(RedisRemoteCache.APACHE_POOL_JMX_NAME_BASE, "testName");
        properties.put(RedisRemoteCache.APACHE_POOL_JMX_NAME_PREFIX, "testPrefix");
        clientEntity.setProperties(properties);

        RedisRemoteCache redisCache = new RedisRemoteCache(clientEntity);
        GenericObjectPoolConfig poolConfig = redisCache.createGenericObjectPoolConfig();

        assertEquals(newPoolConfig.getMaxIdle(), poolConfig.getMaxIdle());
        assertEquals(newPoolConfig.getMaxTotal(), poolConfig.getMaxTotal());
        assertEquals(newPoolConfig.getMinIdle(), poolConfig.getMinIdle());
        assertEquals(newPoolConfig.getNumTestsPerEvictionRun(), poolConfig.getNumTestsPerEvictionRun());
        assertEquals(newPoolConfig.getBlockWhenExhausted(), poolConfig.getBlockWhenExhausted());
        assertEquals(newPoolConfig.getEvictionPolicyClassName(), poolConfig.getEvictionPolicyClassName());
        assertEquals(newPoolConfig.getFairness(), poolConfig.getFairness());
        assertEquals(newPoolConfig.getLifo(), poolConfig.getLifo());
        assertEquals(newPoolConfig.getTimeBetweenEvictionRunsMillis(), poolConfig.getTimeBetweenEvictionRunsMillis());
        assertEquals(newPoolConfig.getTestWhileIdle(), poolConfig.getTestWhileIdle());
        assertEquals(newPoolConfig.getTestOnReturn(), poolConfig.getTestOnReturn());
        assertEquals(newPoolConfig.getTestOnCreate(), poolConfig.getTestOnCreate());
        assertEquals(newPoolConfig.getTestOnBorrow(), poolConfig.getTestOnBorrow());
        assertEquals(newPoolConfig.getSoftMinEvictableIdleTimeMillis(), poolConfig.getSoftMinEvictableIdleTimeMillis());
        assertEquals(newPoolConfig.getMinEvictableIdleTimeMillis(), poolConfig.getMinEvictableIdleTimeMillis());
        assertEquals(newPoolConfig.getMaxWaitMillis(), poolConfig.getMaxWaitMillis());
        assertEquals(newPoolConfig.getJmxNamePrefix(), poolConfig.getJmxNamePrefix());
        assertEquals(newPoolConfig.getJmxNameBase(), poolConfig.getJmxNameBase());
        assertEquals(newPoolConfig.getJmxEnabled(), poolConfig.getJmxEnabled());
    }
}