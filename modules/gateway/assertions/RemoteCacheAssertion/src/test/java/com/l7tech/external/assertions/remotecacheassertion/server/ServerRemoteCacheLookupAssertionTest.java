package com.l7tech.external.assertions.remotecacheassertion.server;


import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheLookupAssertion;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteCachesManagerImpl.class)
public class ServerRemoteCacheLookupAssertionTest {

    private static ApplicationContext applicationContext;

    @Mock
    private RemoteCachesManagerImpl rcManagerImpl;

    @Mock
    JedisPool redisPool;
    @Mock
    Jedis redisClient;
    @Mock
    JedisCluster redisCluster;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        applicationContext = mock(ApplicationContext.class);

        rcManagerImpl = PowerMockito.mock(RemoteCachesManagerImpl.class);
        mockStatic(RemoteCachesManagerImpl.class);
        when(RemoteCachesManagerImpl.getInstance()).thenReturn(rcManagerImpl);
    }

    /**
     * Test assertion is successful if lookup is successful
     *
     * @throws Exception
     */
    @Test
    public void testRedisClientCacheLookupSuccess() throws Exception {
        RemoteCacheLookupAssertion lookupAssertion = new RemoteCacheLookupAssertion();
        RemoteCacheEntity entity = new RemoteCacheEntity();
        HashMap<String, String> properties = entity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);
        entity.setProperties(properties);

        PolicyEnforcementContext context = makeContext("<redis_test/>", "<response />");

        RedisRemoteCache redisRemoteCache = new RedisRemoteCache(entity, redisPool, redisCluster);

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(redisRemoteCache);
        when(redisPool.getResource()).thenReturn(redisClient);
        when(redisClient.isConnected()).thenReturn(Boolean.TRUE);
        when(redisClient.get(anyString())).thenReturn("\u0000\u0000\u0000\u0016text/xml;charset=UTF-8<redis_test/>");

        ServerRemoteCacheLookupAssertion srcla = new ServerRemoteCacheLookupAssertion(lookupAssertion, applicationContext);
        AssertionStatus status = srcla.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    /**
     * Test assertion fails if lookup from cache throws an exception
     *
     * @throws Exception
     */
    @Test
    public void testRedisClusterCacheLookupThrowsException() throws Exception {
        RemoteCacheLookupAssertion lookupAssertion = new RemoteCacheLookupAssertion();

        RemoteCacheEntity entity = new RemoteCacheEntity();
        HashMap<String, String> properties = entity.getProperties();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "localhost:6379");
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "true");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, null);
        entity.setProperties(properties);

        RedisRemoteCache redisRemoteCache = new RedisRemoteCache(entity, redisPool, redisCluster);

        PolicyEnforcementContext context = makeContext("<redis_test/>", "<response />");

        when(rcManagerImpl.getRemoteCache(Mockito.any(Goid.class))).thenReturn(redisRemoteCache);
        when(redisCluster.get(anyString())).thenThrow(mock(JedisException.class));

        ServerRemoteCacheLookupAssertion srcla = new ServerRemoteCacheLookupAssertion(lookupAssertion, applicationContext);
        AssertionStatus status = srcla.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}
