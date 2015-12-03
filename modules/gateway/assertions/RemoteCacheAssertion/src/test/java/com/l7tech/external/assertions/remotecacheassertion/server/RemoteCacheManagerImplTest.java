package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerConfig.class, TerracottaToolkitClassLoader.class, GemFireClassLoader.class, CoherenceClassLoader.class})
@PowerMockIgnore("javax.management.*")
public class RemoteCacheManagerImplTest {

    private RemoteCachesManager remoteCachesManager = null;


    private ClusterPropertyManager clusterPropertyManager;
    private ServerConfig serverConfig;
    private EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    private TerracottaToolkitClassLoader terracottaToolkitClassLoader;
    private GemFireClassLoader gemFireClassLoader;
    private CoherenceClassLoader coherenceClassLoader;

    private RemoteCacheEntity memCachedEntity;
    private RemoteCacheEntity redisEntity;
    private RemoteCacheEntity gemFireEntity;
    private RemoteCacheEntity coherenceEntity;
    private RemoteCacheEntity terrecottaEntity;
    private final Goid memCachedGoid = new Goid(2, 0);
    private final Goid redisGoid = new Goid(3, 0);
    private final Goid gemfireGoid = new Goid(4, 0);
    private final Goid coherenceGoid = new Goid(5, 0);
    private final Goid terracottaGoid = new Goid(6, 0);

    @Before
    public void setup() throws Exception {
        clusterPropertyManager = mock(ClusterPropertyManager.class);
        serverConfig = PowerMockito.mock(ServerConfig.class);
        entityManager = mock(EntityManager.class);

        terracottaToolkitClassLoader = PowerMockito.mock(TerracottaToolkitClassLoader.class);
        PowerMockito.mockStatic(TerracottaToolkitClassLoader.class);
        coherenceClassLoader = PowerMockito.mock(CoherenceClassLoader.class);
        PowerMockito.mockStatic(CoherenceClassLoader.class);
        gemFireClassLoader = PowerMockito.mock(GemFireClassLoader.class);
        PowerMockito.mockStatic(GemFireClassLoader.class);

        when(serverConfig.getProperty("com.l7tech.server.home")).thenReturn(any(String.class));
        when(CoherenceClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"))).thenReturn(coherenceClassLoader);
        when(TerracottaToolkitClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"))).thenReturn(terracottaToolkitClassLoader);
        when(GemFireClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"))).thenReturn(gemFireClassLoader);

        final String cacheName = "cacheName";
        final int timeOut = 10;
        final String hosts = "localhost:1234";

        //memcached entity
        memCachedEntity = new RemoteCacheEntity();
        memCachedEntity.setEnabled(true);
        memCachedEntity.setName(cacheName);
        memCachedEntity.setTimeout(timeOut);
        memCachedEntity.setType(RemoteCacheTypes.Memcached.getEntityType());
        memCachedEntity.setGoid(memCachedGoid);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, "false");
        properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, hosts);
        memCachedEntity.setProperties(properties);

        //redis entity
        redisEntity = new RemoteCacheEntity();
        redisEntity.setEnabled(true);
        redisEntity.setName(cacheName);
        redisEntity.setTimeout(timeOut);
        redisEntity.setType(RemoteCacheTypes.Redis.getEntityType());
        redisEntity.setGoid(redisGoid);
        properties = new HashMap<>();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, hosts);
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, "false");
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, "");
        redisEntity.setProperties(properties);

//gemfire entity
        gemFireEntity = new RemoteCacheEntity();
        gemFireEntity.setEnabled(true);
        gemFireEntity.setName(cacheName);
        gemFireEntity.setTimeout(timeOut);
        gemFireEntity.setType(RemoteCacheTypes.GemFire.getEntityType());
        gemFireEntity.setGoid(gemfireGoid);
        properties = new HashMap<>();
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_NAME, cacheName);
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "locator");
        properties.put(GemfireRemoteCache.PROPERTY_SERVERS, hosts);
        gemFireEntity.setProperties(properties);

        //coherence entity
        coherenceEntity = new RemoteCacheEntity();
        coherenceEntity.setEnabled(true);
        coherenceEntity.setName(cacheName);
        coherenceEntity.setTimeout(timeOut);
        coherenceEntity.setType(RemoteCacheTypes.Coherence.getEntityType());
        coherenceEntity.setGoid(coherenceGoid);
        properties = new HashMap<>();
        properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, cacheName);
        properties.put(CoherenceRemoteCache.PROP_SERVERS, hosts);
        coherenceEntity.setProperties(properties);

        //terrecotta entity
        terrecottaEntity = new RemoteCacheEntity();
        terrecottaEntity.setEnabled(true);
        terrecottaEntity.setName(cacheName);
        terrecottaEntity.setTimeout(timeOut);
        terrecottaEntity.setType(RemoteCacheTypes.Terracotta.getEntityType());
        terrecottaEntity.setGoid(terracottaGoid);
        properties = new HashMap<>();
        properties.put(TerracottaRemoteCache.PROPERTY_CACHE_NAME, cacheName);
        properties.put(TerracottaRemoteCache.PROPERTY_URLS, hosts);
        terrecottaEntity.setProperties(properties);

        RemoteCachesManagerImpl.createRemoteCachesManager(entityManager, clusterPropertyManager, serverConfig);
        remoteCachesManager = RemoteCachesManagerImpl.getInstance();
    }

    @After
    public void teardown() throws Exception {
        RemoteCachesManagerImpl.setInstance(null);
    }

    /**
     * Test connections are created successfully on startup when entities exist
     *
     * @throws FindException
     */
    @Test
    public void testSuccessfulCreateConnectionsOnStartup() throws FindException {
        RemoteCachesManagerImpl.setInstance(null);
        Collection<RemoteCacheEntity> entities = new ArrayList<>();
        entities.add(redisEntity);
        entities.add(memCachedEntity);

        when(entityManager.findAll()).thenReturn(entities);

        RemoteCachesManagerImpl.createRemoteCachesManager(entityManager, clusterPropertyManager, serverConfig);
        RemoteCachesManager remoteCachesManager = RemoteCachesManagerImpl.getInstance();

        assertEquals(2, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());
    }


    /**
     * This test case will create 5 caches successfully (memcached, redis, coherence, terracotta and gemfire) and will successfully remove each cache.
     *
     * @throws RemoteCacheConnectionException
     */
    @Test
    public void testSuccessfulCreateAndRemoveConnections() throws RemoteCacheConnectionException {
        //create memcached
        remoteCachesManager.connectionAdded(memCachedEntity);
        RemoteCache remoteCacheCreated = remoteCachesManager.getRemoteCache(memCachedGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(1, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //create redis
        remoteCachesManager.connectionAdded(redisEntity);
        remoteCacheCreated = remoteCachesManager.getRemoteCache(redisGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(2, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //create gemfire
        remoteCachesManager.connectionAdded(gemFireEntity);
        remoteCacheCreated = remoteCachesManager.getRemoteCache(gemfireGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(3, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //create coherence
        remoteCachesManager.connectionAdded(coherenceEntity);
        remoteCacheCreated = remoteCachesManager.getRemoteCache(coherenceGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(4, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //create terrecotta
        remoteCachesManager.connectionAdded(terrecottaEntity);
        remoteCacheCreated = remoteCachesManager.getRemoteCache(terracottaGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(5, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //remove memcached
        remoteCachesManager.connectionRemoved(memCachedEntity);
        assertNull(((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().get(memCachedGoid));
        assertEquals(4, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //remove redis
        remoteCachesManager.connectionRemoved(redisEntity);
        assertNull(((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().get(redisGoid));
        assertEquals(3, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //remove gemfire
        remoteCachesManager.connectionRemoved(gemFireEntity);
        assertNull(((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().get(gemfireGoid));
        assertEquals(2, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        //remove coherence
        remoteCachesManager.connectionRemoved(coherenceEntity);
        assertNull(((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().get(coherenceGoid));
        assertEquals(1, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());


        //remove terrecotta
        remoteCachesManager.connectionRemoved(terrecottaEntity);
        assertNull(((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().get(terracottaGoid));
        assertEquals(0, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());
    }

    /**
     * Tesst creating a connection fails for unknown cache type
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateConnectionFailsForUnkwonType() {
        memCachedEntity.setType("Invalid");

        remoteCachesManager.connectionAdded(memCachedEntity);
    }

    /**
     * test creating a connection fails when a connection already exists
     *
     * @throws RemoteCacheConnectionException
     */
    @Test(expected = RuntimeException.class)
    public void testCreateConnectionFailsForAlreadyExistingConnection() throws RemoteCacheConnectionException {
        //create memcached
        remoteCachesManager.connectionAdded(memCachedEntity);
        RemoteCache remoteCacheCreated = remoteCachesManager.getRemoteCache(memCachedGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(1, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        remoteCachesManager.connectionAdded(memCachedEntity);
    }

    /**
     * test creating a connection fails when a connection is disabled
     *
     * @throws RemoteCacheConnectionException
     */
    @Test
    public void testCreateConnectionIsRemovedForDisabledConnection() throws RemoteCacheConnectionException {
        //create memcached
        remoteCachesManager.connectionAdded(memCachedEntity);
        RemoteCache remoteCacheCreated = remoteCachesManager.getRemoteCache(memCachedGoid);
        assertNotNull(remoteCacheCreated);
        assertEquals(1, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        memCachedEntity.setEnabled(false);

        remoteCachesManager.connectionUpdated(memCachedEntity);
        assertEquals(0, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());
    }

    /**
     * Test connection is successfully shutdown and all caches are cleared
     *
     * @throws RemoteCacheConnectionException
     */
    @Test
    public void testSuccessfullShutdownManager() throws RemoteCacheConnectionException {
        remoteCachesManager.connectionAdded(memCachedEntity);
        remoteCachesManager.connectionAdded(redisEntity);
        remoteCachesManager.connectionAdded(coherenceEntity);
        remoteCachesManager.connectionAdded(gemFireEntity);
        remoteCachesManager.connectionAdded(terrecottaEntity);

        assertEquals(5, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());

        RemoteCachesManagerImpl.shutdown();
        assertEquals(0, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());
    }

    /**
     * Test getting a connection fails when entity is not found
     *
     * @throws RemoteCacheConnectionException
     */
    @Test(expected = RemoteCacheConnectionException.class)
    public void testGetRemoteCacheFailsWhenEntityNotFound() throws RemoteCacheConnectionException {
        remoteCachesManager.getRemoteCache(memCachedGoid);
    }

    /**
     * test successfully getting a connection
     *
     * @throws RemoteCacheConnectionException
     * @throws FindException
     */
    @Test
    public void testGetRemoteCache() throws RemoteCacheConnectionException, FindException {
        when(entityManager.findByPrimaryKey(memCachedGoid)).thenReturn(memCachedEntity);
        RemoteCache remoteCache = remoteCachesManager.getRemoteCache(memCachedGoid);

        assertNotNull(remoteCache);
        assertEquals(1, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());
    }

    /**
     * Test a connection is not added to the list of connections when it fails to create a connection
     *
     * @throws Exception
     */
    @Test
    public void testConnectionIsNotCreatedForFailedConnections() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, "");
        memCachedEntity.setProperties(properties);

        properties = new HashMap<>();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, "");
        redisEntity.setProperties(properties);

        when(TerracottaToolkitClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"))).thenReturn(null);
        when(GemFireClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"))).thenReturn(null);
        when(CoherenceClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"))).thenReturn(null);

        remoteCachesManager.connectionAdded(memCachedEntity);
        remoteCachesManager.connectionAdded(redisEntity);
        remoteCachesManager.connectionAdded(gemFireEntity);
        remoteCachesManager.connectionAdded(coherenceEntity);
        remoteCachesManager.connectionAdded(terrecottaEntity);

        assertEquals(0, ((RemoteCachesManagerImpl) remoteCachesManager).getCurrentlyUsedCaches().size());
    }

}
