package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheLookupAssertion;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheStoreAssertion;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import java.util.HashMap;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Test the RemoteCacheStoreAssertion.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteCachesManagerImpl.class)
public class ServerRemoteCacheStoreAssertionTest {

    private static ApplicationContext applicationContext;
    private static boolean initialized;
    private static Goid memCacheDRemoteCacheGoid = new Goid(1, 0);
    private static Goid coherenceRemoteCacheGoid = new Goid(2, 1);
    private static Goid redisRemoteCacheGoid = new Goid(3, 1);

    @Mock
    private static EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    @Mock
    private static GenericEntityManager genericEntityManager;
    @Mock
    private RemoteCachesManagerImpl rcManagerImpl;
    @Mock
    private MemcachedRemoteCache memcachedRemoteCache;
    @Mock
    private CoherenceRemoteCache coherenceRemoteCache;

    @Mock
    JedisPool redisPool;
    @Mock
    Jedis redisClient;
    @Mock
    JedisCluster redisCluster;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        if (!initialized) {
            StashManagerFactory stashManagerFactory = TestStashManagerFactory.getInstance();
            applicationContext = mock(ApplicationContext.class);
            Mockito.when(applicationContext.getBean("stashManagerFactory", StashManagerFactory.class))
                    .thenReturn(stashManagerFactory);
            Mockito.when(genericEntityManager.getEntityManager(RemoteCacheEntity.class)).thenReturn(entityManager);
        }

        rcManagerImpl = mock(RemoteCachesManagerImpl.class);
        mockStatic(RemoteCachesManagerImpl.class);
        when(RemoteCachesManagerImpl.getInstance()).thenReturn(rcManagerImpl);

        //initialize remote cache entries
        try {
            if (!initialized) {
                // Memcached
                RemoteCacheEntity re = new RemoteCacheEntity();
                re.setType(RemoteCacheTypes.Memcached.getEntityType());
                re.setName("TestMemCacheD");
                re.setTimeout(1000);
                HashMap<String, String> properties = re.getProperties();
                properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, Boolean.toString(false));
                String serverPort = "ext-dep-tactical-teamcity:11211";
                properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, serverPort);
                Mockito.when(entityManager.save(re)).thenReturn(memCacheDRemoteCacheGoid);

                // Coherence
                re = new RemoteCacheEntity();
                re.setType(RemoteCacheTypes.Coherence.getEntityType());
                re.setName("TestCoherence");
                re.setTimeout(10);
                properties = re.getProperties();
                properties.put(CoherenceRemoteCache.PROP_SERVERS, "ext-dep-tactical-teamcity:9099");
                properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, "TestCoherence");
                Mockito.when(entityManager.save(re)).thenReturn(coherenceRemoteCacheGoid);
//                coherenceRemoteCacheGoid = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null).save(re);

                initialized = true;
            }
        } catch (SaveException e) {
            e.printStackTrace();
        }
    }

    /**
     * This test stores an entry in the Memcached remote cache and then retrieves it.
     */
    @Test
    public void testSuccessfulStoreInMemcachedRemoteCacheAndSuccessfulRetrieval() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(memcachedRemoteCache);
        String cacheKey = "testKey";

        RemoteCacheStoreAssertion rcsa = new RemoteCacheStoreAssertion();
        rcsa.setTarget(TargetMessageType.REQUEST);
        rcsa.setRemoteCacheGoid(memCacheDRemoteCacheGoid);
        rcsa.setCacheEntryKey(cacheKey);
        rcsa.setMaxEntryAge(2 + "");
        rcsa.setMaxEntrySizeBytes(2000 + "");
        rcsa.setStoreSoapFaults(false);
        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(rcsa, applicationContext);
        PolicyEnforcementContext peCtx = makeContext("<blah/>", "<response />");
        AssertionStatus status = srcsa.checkRequest(peCtx);
        Assert.assertEquals(status, AssertionStatus.NONE);

        // Retrieve the entry added to cache earlier
        /* Since the value being inserted and retrieved are actually using mocked cached classes
         * and their values are set in the test itself, the usefulness of the Assert checks related
         * to its values is questionable.
         */
        when(memcachedRemoteCache.get(cacheKey)).thenReturn(new CachedMessageData("<blah/>"));
        RemoteCacheLookupAssertion rcla = new RemoteCacheLookupAssertion();
        rcla.setCacheEntryKey(cacheKey);
        rcla.setRemoteCacheGoid(memCacheDRemoteCacheGoid);
        rcla.setTarget(TargetMessageType.RESPONSE);
        ServerRemoteCacheLookupAssertion srcla = new ServerRemoteCacheLookupAssertion(rcla, applicationContext);
        AssertionStatus status1 = srcla.checkRequest(peCtx);
        Assert.assertEquals(status1, AssertionStatus.NONE);
        Object o = peCtx.getVariable("response.mainpart");
        Assert.assertEquals("<blah/>", o.toString());
        //Thread.sleep(2000L);//This time out is required to expire the cache..so that next caching test can check for some different value.
    }

    @Test
    public void testContextVariableSupport() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(memcachedRemoteCache);
        String cacheKey = "testKey";

        RemoteCacheStoreAssertion rcsa = new RemoteCacheStoreAssertion();
        rcsa.setTarget(TargetMessageType.REQUEST);
        rcsa.setRemoteCacheGoid(memCacheDRemoteCacheGoid);
        rcsa.setCacheEntryKey(cacheKey);
        rcsa.setMaxEntryAge("${myAge}");
        rcsa.setMaxEntrySizeBytes("${mySize}");
        rcsa.setStoreSoapFaults(false);
        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(rcsa, applicationContext);
        PolicyEnforcementContext peCtx = makeContext("<testtwo/>", "<response />");
        peCtx.setVariable("myAge", "2");
        peCtx.setVariable("mySize", "20000");
        AssertionStatus status = srcsa.checkRequest(peCtx);
        Assert.assertEquals(status, AssertionStatus.NONE);

        when(memcachedRemoteCache.get(cacheKey)).thenReturn(new CachedMessageData("<testtwo/>"));

        RemoteCacheLookupAssertion rcla = new RemoteCacheLookupAssertion();
        rcla.setCacheEntryKey(cacheKey);
        rcla.setRemoteCacheGoid(memCacheDRemoteCacheGoid);
        rcla.setTarget(TargetMessageType.RESPONSE);
        ServerRemoteCacheLookupAssertion srcla = new ServerRemoteCacheLookupAssertion(rcla, applicationContext);
        AssertionStatus status1 = srcla.checkRequest(peCtx);
        Assert.assertEquals(status1, AssertionStatus.NONE);
        Object o = peCtx.getVariable("response.mainpart");
        System.out.println(o.toString());
        Assert.assertEquals("<testtwo/>", o.toString());
        //Thread.sleep(2000L);//This time out is required to expire the cache..so that next caching test can check for some different value.
    }

    @Test
    public void testCoherenceNotExpired() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(coherenceRemoteCache);
        String cacheKey = "coherenceTestKey";

        RemoteCacheStoreAssertion rcsa = new RemoteCacheStoreAssertion();
        rcsa.setTarget(TargetMessageType.REQUEST);
        rcsa.setRemoteCacheGoid(coherenceRemoteCacheGoid);
        rcsa.setCacheEntryKey(cacheKey);
        rcsa.setMaxEntryAge(2 + "");
        rcsa.setMaxEntrySizeBytes(2000 + "");
        rcsa.setStoreSoapFaults(false);
        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(rcsa, applicationContext);
        PolicyEnforcementContext peCtx = makeContext("<coherence_test/>", "<response />");
        AssertionStatus status = srcsa.checkRequest(peCtx);
        Assert.assertEquals(AssertionStatus.NONE, status);

        when(coherenceRemoteCache.get(cacheKey)).thenReturn(new CachedMessageData("<coherence_test/>"));

        RemoteCacheLookupAssertion rcla = new RemoteCacheLookupAssertion();
        rcla.setCacheEntryKey(cacheKey);
        rcla.setRemoteCacheGoid(coherenceRemoteCacheGoid);
        rcla.setTarget(TargetMessageType.RESPONSE);
        ServerRemoteCacheLookupAssertion srcla = new ServerRemoteCacheLookupAssertion(rcla, applicationContext);
        status = srcla.checkRequest(peCtx);
        Assert.assertEquals(AssertionStatus.NONE, status);
        Object o = peCtx.getVariable("response.mainpart");
        Assert.assertEquals("<coherence_test/>", o.toString());
    }

    @Test
    public void testCoherenceExpired() throws Exception {
        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(coherenceRemoteCache);
        String cacheKey = "coherenceTestKey2";

        RemoteCacheStoreAssertion rcsa = new RemoteCacheStoreAssertion();
        rcsa.setTarget(TargetMessageType.REQUEST);
        rcsa.setRemoteCacheGoid(coherenceRemoteCacheGoid);
        rcsa.setCacheEntryKey(cacheKey);
        rcsa.setMaxEntryAge(2 + "");
        rcsa.setMaxEntrySizeBytes(2000 + "");
        rcsa.setStoreSoapFaults(false);
        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(rcsa, applicationContext);
        PolicyEnforcementContext peCtx = makeContext("<coherence_test2/>", "<response />");
        AssertionStatus status = srcsa.checkRequest(peCtx);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Thread.sleep(4000);
        RemoteCacheLookupAssertion rcla = new RemoteCacheLookupAssertion();
        rcla.setCacheEntryKey(cacheKey);
        rcla.setRemoteCacheGoid(coherenceRemoteCacheGoid);
        rcla.setTarget(TargetMessageType.RESPONSE);
        ServerRemoteCacheLookupAssertion srcla = new ServerRemoteCacheLookupAssertion(rcla, applicationContext);
        status = srcla.checkRequest(peCtx);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    /**
     * Test assertion is successful during successful store into cache
     *
     * @throws Exception
     */
    @Test
    public void testRedisClusterCacheStoreSuccess() throws Exception {
        RemoteCacheEntity entity = new RemoteCacheEntity();
        HashMap<String, String> properties = entity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "true");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);

        String cacheKey = "testKey";
        PolicyEnforcementContext context = makeContext("<redis_test/>", "<response />");

        RemoteCacheStoreAssertion storeAssertion = new RemoteCacheStoreAssertion();
        storeAssertion.setTarget(TargetMessageType.REQUEST);
        storeAssertion.setRemoteCacheGoid(redisRemoteCacheGoid);
        storeAssertion.setCacheEntryKey(cacheKey);
        storeAssertion.setMaxEntryAge("2");
        storeAssertion.setMaxEntrySizeBytes("2000");
        storeAssertion.setStoreSoapFaults(false);

        RedisRemoteCache redisRemoteCache = new RedisRemoteCache(entity, redisPool, redisCluster);

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(redisRemoteCache);
        when(redisCluster.set(anyString(), anyString())).thenReturn("OK");

        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(storeAssertion, applicationContext);
        AssertionStatus status = srcsa.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    /**
     * Test assertion fails if store throws an exception
     *
     * @throws Exception
     */
    @Test
    public void testRedisClientCacheStoreThrowsException() throws Exception {
        RemoteCacheEntity entity = new RemoteCacheEntity();
        HashMap<String, String> properties = entity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);

        String cacheKey = "testKey";
        PolicyEnforcementContext context = makeContext("<redis_test/>", "<response />");

        RemoteCacheStoreAssertion storeAssertion = new RemoteCacheStoreAssertion();
        storeAssertion.setTarget(TargetMessageType.REQUEST);
        storeAssertion.setRemoteCacheGoid(redisRemoteCacheGoid);
        storeAssertion.setCacheEntryKey(cacheKey);
        storeAssertion.setMaxEntryAge("2");
        storeAssertion.setMaxEntrySizeBytes("2000");
        storeAssertion.setStoreSoapFaults(false);

        RedisRemoteCache redisRemoteCache = new RedisRemoteCache(entity, redisPool, redisCluster);

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(redisRemoteCache);
        when(redisPool.getResource()).thenReturn(redisClient);
        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.set(anyString(), anyString())).thenThrow(mock(JedisException.class));

        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(storeAssertion, applicationContext);
        AssertionStatus status = srcsa.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    /**
     * Test assertion fails if getting remote cache throws an error
     *
     * @throws Exception
     */
    @Test
    public void testDisabledRedisEntityThrowsException() throws Exception {
        String cacheKey = "testKey";
        PolicyEnforcementContext context = makeContext("<redis_test/>", "<response />");

        RemoteCacheStoreAssertion storeAssertion = new RemoteCacheStoreAssertion();
        storeAssertion.setTarget(TargetMessageType.REQUEST);
        storeAssertion.setRemoteCacheGoid(redisRemoteCacheGoid);
        storeAssertion.setCacheEntryKey(cacheKey);
        storeAssertion.setMaxEntryAge("2");
        storeAssertion.setMaxEntrySizeBytes("2000");
        storeAssertion.setStoreSoapFaults(false);

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenThrow(RemoteCacheConnectionException.class);

        ServerRemoteCacheStoreAssertion srcsa = new ServerRemoteCacheStoreAssertion(storeAssertion, applicationContext);
        AssertionStatus status = srcsa.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }
}