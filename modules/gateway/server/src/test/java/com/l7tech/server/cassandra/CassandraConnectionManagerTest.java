package com.l7tech.server.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.test.BugId;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import com.l7tech.util.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraConnectionManagerTest {

    @Mock
    private Config config;
    @Mock
    private SecurePasswordManager securePasswordManager;
    @Mock
    private TrustManager trustManager;
    @Mock
    private SecureRandom secureRandom;

    private CassandraConnectionManagerImpl cassandraConnectionManager;
    private CassandraConnection cassandraConnection1;
    private CassandraConnection cassandraConnection2;
    private CassandraConnection cassandraConnection3;

    @Before
    public void setup() throws Exception {
        when(config.getTimeUnitProperty("cassandra.maxConnectionCacheAge", 0L)).thenReturn(0L);
        when(config.getTimeUnitProperty("cassandra.maxConnectionCacheIdleTime", TimeUnit.MINUTES.toMillis(30))).thenReturn(TimeUnit.MINUTES.toMillis(30));
        when(config.getIntProperty("cassandra.maxConnectionCacheSize", 20)).thenReturn(20);

        cassandraConnection1 = new CassandraConnection();
        cassandraConnection1.setId(getGoid().toString());
        cassandraConnection1.setName("Connection1");
        cassandraConnection1.setKeyspaceName("keyspace1");
        cassandraConnection1.setContactPoints("localhost");
        cassandraConnection1.setPort("9042");
        cassandraConnection1.setUsername("cassandra");
        cassandraConnection1.setCompression(CassandraConnection.COMPRESS_LZ4);
        cassandraConnection1.setSsl(true);
        cassandraConnection1.setEnabled(true);

        cassandraConnection2 = new CassandraConnection();
        cassandraConnection2.setId(getGoid().toString());
        cassandraConnection2.setName("Connection2");
        cassandraConnection2.setKeyspaceName("keyspace2");
        cassandraConnection2.setContactPoints("localhost");
        cassandraConnection2.setPort("9042");
        cassandraConnection2.setUsername("cassandra");
        cassandraConnection2.setCompression(CassandraConnection.COMPRESS_NONE);
        cassandraConnection2.setSsl(true);
        cassandraConnection2.setEnabled(true);

        // Note that connection3 is disabled!
        cassandraConnection3 = new CassandraConnection();
        cassandraConnection3.setId(getGoid().toString());
        cassandraConnection3.setName("Connection3");
        cassandraConnection3.setKeyspaceName("keyspace3");
        cassandraConnection3.setContactPoints("localhost");
        cassandraConnection3.setPort("9042");
        cassandraConnection3.setUsername("cassandra");
        cassandraConnection3.setCompression(CassandraConnection.COMPRESS_LZ4);
        cassandraConnection3.setSsl(false);
        cassandraConnection3.setEnabled(false);

        CassandraConnectionEntityManager mockCassandraConnectionEntityManager = mock(CassandraConnectionEntityManager.class);
        Mockito.doReturn(cassandraConnection1).when(mockCassandraConnectionEntityManager).getCassandraConnectionEntity(cassandraConnection1.getName());
        Mockito.doReturn(cassandraConnection2).when(mockCassandraConnectionEntityManager).getCassandraConnectionEntity(cassandraConnection2.getName());
        Mockito.doReturn(cassandraConnection3).when(mockCassandraConnectionEntityManager).getCassandraConnectionEntity(cassandraConnection3.getName());
        Mockito.doReturn(cassandraConnection1).when(mockCassandraConnectionEntityManager).findByPrimaryKey(cassandraConnection1.getGoid());
        Mockito.doReturn(cassandraConnection2).when(mockCassandraConnectionEntityManager).findByPrimaryKey(cassandraConnection2.getGoid());
        Mockito.doReturn(cassandraConnection3).when(mockCassandraConnectionEntityManager).findByPrimaryKey(cassandraConnection3.getGoid());

        cassandraConnectionManager = new CassandraConnectionManagerTestStub(
                mockCassandraConnectionEntityManager, config, securePasswordManager, trustManager, secureRandom, false);

    }


    @After
    public void after() throws Exception {
        cassandraConnectionManager = null;
    }

    @Test
    public void testAddConnection() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        CassandraConnectionHolder result = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        assertSame("Connection not added with the same settings.", cassandraConnection1, result.getCassandraConnectionEntity());

        cassandraConnectionManager.addConnection(cassandraConnection2);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Connection 3 is set to be disabled.
        cassandraConnectionManager.addConnection(cassandraConnection3);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        assertSame("Connections are different.", result, cache);
    }

    @Test
    public void testRemoveConnection() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        cassandraConnectionManager.addConnection(cassandraConnection3);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        cassandraConnectionManager.removeConnection(cassandraConnection1);
        assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        cassandraConnectionManager.removeConnection(cassandraConnection2);
        assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());
        // Remove non-existing connection
        cassandraConnectionManager.removeConnection(cassandraConnection3);
        assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());

        // By Goid
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        cassandraConnectionManager.addConnection(cassandraConnection3);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        Goid goid = cassandraConnection1.getGoid();
        cassandraConnectionManager.removeConnection(goid);
        assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());

        goid = cassandraConnection2.getGoid();
        cassandraConnectionManager.removeConnection(goid);

        // Remove non-existing connection
        goid = cassandraConnection3.getGoid();
        cassandraConnectionManager.removeConnection(goid);
        assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());
    }

    @Test
    public void testCloseAllConnections() throws Exception {
        cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Getting existing connection, cache size should be the same
        cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        cassandraConnectionManager.closeAllConnections();
        assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());
    }

    @Test
     public void testUpdateConnectionWithNewSettings() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Update connection1
        CassandraConnection updatedConn = new CassandraConnection();
        updatedConn.setId(cassandraConnection1.getId());
        updatedConn.setName(cassandraConnection1.getName());
        updatedConn.setKeyspaceName(cassandraConnection1.getKeyspaceName());
        updatedConn.setContactPoints("10.10.10.10");
        updatedConn.setPort("1234");
        updatedConn.setUsername(cassandraConnection1.getUsername());
        updatedConn.setCompression(cassandraConnection1.getCompression());
        updatedConn.setSsl(cassandraConnection1.isSsl());
        updatedConn.setEnabled(cassandraConnection1.isEnabled());
        cassandraConnectionManager.updateConnection(updatedConn);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        assertEquals(cassandraConnection1.getId(), cache.getCassandraConnectionEntity().getId());
        assertEquals(cassandraConnection1.getName(), cache.getCassandraConnectionEntity().getName());
        assertEquals("10.10.10.10", cache.getCassandraConnectionEntity().getContactPoints());
        assertEquals("1234", cache.getCassandraConnectionEntity().getPort());

        // Connection2 has not changed
        cache = cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        assertSame("Connection not added with the same settings.", cassandraConnection2, cache.getCassandraConnectionEntity());
    }

    @Test
    public void testUpdateConnectionInvalidServer() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Update connection1
        CassandraConnection updatedConn = new CassandraConnection();
        updatedConn.setId(cassandraConnection1.getId());
        updatedConn.setName(cassandraConnection1.getName());
        updatedConn.setKeyspaceName(cassandraConnection1.getKeyspaceName());
        updatedConn.setContactPoints("invalidServer");
        updatedConn.setPort("1234");
        updatedConn.setUsername(cassandraConnection1.getUsername());
        updatedConn.setCompression(cassandraConnection1.getCompression());
        updatedConn.setSsl(cassandraConnection1.isSsl());
        updatedConn.setEnabled(cassandraConnection1.isEnabled());

        ((CassandraConnectionManagerTestStub)cassandraConnectionManager).setReturnNull(true);
        cassandraConnectionManager.updateConnection(updatedConn);
        assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        assertNull(cassandraConnectionManager.getConnection(cassandraConnection1.getName()));

        // Connection2 has not changed
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        assertSame("Connection not added with the same settings.", cassandraConnection2, cache.getCassandraConnectionEntity());
    }

    @Test
    public void testUpdateConnectionWithDifferentName() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        CassandraConnectionHolder result = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        assertSame("Connection not added with the same settings.", cassandraConnection1, result.getCassandraConnectionEntity());
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Update connection1
        CassandraConnection updatedConn = new CassandraConnection();
        updatedConn.setId(cassandraConnection1.getId());
        updatedConn.setName("New Name");
        updatedConn.setKeyspaceName(cassandraConnection1.getKeyspaceName());
        updatedConn.setContactPoints(cassandraConnection1.getContactPoints());
        updatedConn.setPort(cassandraConnection1.getPort());
        updatedConn.setUsername(cassandraConnection1.getUsername());
        updatedConn.setCompression(cassandraConnection1.getCompression());
        updatedConn.setSsl(cassandraConnection1.isSsl());
        updatedConn.setEnabled(cassandraConnection1.isEnabled());

        cassandraConnectionManager.updateConnection(updatedConn);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection("New Name");
        // No new connection created in cache
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());
        assertEquals(cassandraConnection1.getId(), cache.getCassandraConnectionEntity().getId());
        assertEquals("New Name", cache.getCassandraConnectionEntity().getName());

        // Connection2 has not changed
        cache = cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        assertSame("Connection not added with the same settings.", cassandraConnection2, cache.getCassandraConnectionEntity());
    }

    @Test
    public void testUpdateConnectionToDisabled() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Update connection2, set to disabled
        CassandraConnection updatedConn = new CassandraConnection();
        updatedConn.setId(cassandraConnection2.getId());
        updatedConn.setName(cassandraConnection2.getName());
        updatedConn.setKeyspaceName(cassandraConnection2.getKeyspaceName());
        updatedConn.setContactPoints(cassandraConnection2.getContactPoints());
        updatedConn.setPort(cassandraConnection2.getPort());
        updatedConn.setUsername(cassandraConnection2.getUsername());
        updatedConn.setCompression(cassandraConnection2.getCompression());
        updatedConn.setSsl(cassandraConnection1.isSsl());
        updatedConn.setEnabled(false);

        cassandraConnectionManager.updateConnection(updatedConn);
        assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        // What's left in the cache should be just connection1
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        // No new connection created in cache
        assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        assertEquals(cassandraConnection1.getId(), cache.getCassandraConnectionEntity().getId());
        assertEquals(cassandraConnection1.getName(), cache.getCassandraConnectionEntity().getName());
    }

    @BugId("DE363569")
    @Test
    public void testUseDefaultProperties() throws Exception {

        CassandraConnectionManagerImpl realManager = new CassandraConnectionManagerImpl(null, new MockConfig(new HashMap<>()), null, null, null);

        Cluster.Builder clusterBuilder = Cluster.builder();
        realManager.populateCluster(clusterBuilder, cassandraConnection1);
        assertEquals(CassandraConnectionManager.DEFAULT_CONNECTION_TIMEOUT_MS, clusterBuilder.getConfiguration().getSocketOptions().getConnectTimeoutMillis());
        assertEquals(CassandraConnectionManager.DEFAULT_READ_TIMEOUT_MS, clusterBuilder.getConfiguration().getSocketOptions().getReadTimeoutMillis());
        assertEquals(CassandraConnectionManager.DEFAULT_KEEP_ALIVE, clusterBuilder.getConfiguration().getSocketOptions().getKeepAlive());
        assertNull(clusterBuilder.getConfiguration().getSocketOptions().getReuseAddress());
        assertNull(clusterBuilder.getConfiguration().getSocketOptions().getSoLinger());
        assertEquals(CassandraConnectionManager.DEFAULT_TCP_NO_DELAY, clusterBuilder.getConfiguration().getSocketOptions().getTcpNoDelay());
        assertNull(clusterBuilder.getConfiguration().getSocketOptions().getReceiveBufferSize());
        assertNull(clusterBuilder.getConfiguration().getSocketOptions().getSendBufferSize());
    }

    @BugId("DE363569")
    @Test
    public void testUseCustomDefaultProperties() throws Exception {

        int connectionTimeout = 1111;
        int readTimeout = 2222;
        int soLinger = 3333;
        int receiveBufferSize = 4444;
        int sendBufferSize = 5555;
        boolean keepAlive = !CassandraConnectionManager.DEFAULT_KEEP_ALIVE;
        boolean tcpNoDelay = !CassandraConnectionManager.DEFAULT_TCP_NO_DELAY;
        boolean reuseAddress = true;

        // override cluster property defined default values.
        Config config = new MockConfig(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_CASSANDRA_CONNECTION_TIMEOUT, Integer.toString(connectionTimeout))
                .put(ServerConfigParams.PARAM_CASSANDRA_READ_TIMEOUT, Integer.toString(readTimeout))
                .put(ServerConfigParams.PARAM_CASSANDRA_KEEP_ALIVE, Boolean.toString(keepAlive))
                .put(ServerConfigParams.PARAM_CASSANDRA_REUSE_ADDRESS, Boolean.toString(reuseAddress))
                .put(ServerConfigParams.PARAM_CASSANDRA_SO_LINGER, Integer.toString(soLinger))
                .put(ServerConfigParams.PARAM_CASSANDRA_TCP_NO_DELAY, Boolean.toString(tcpNoDelay))
                .put(ServerConfigParams.PARAM_CASSANDRA_RECIEVE_BUFFER_SIZE, Integer.toString(receiveBufferSize))
                .put(ServerConfigParams.PARAM_CASSANDRA_SEND_BUFFER_SIZE, Integer.toString(sendBufferSize))
                .map());
        CassandraConnectionManagerImpl realManager = new CassandraConnectionManagerImpl(null, config, null, null, null);

        Cluster.Builder clusterBuilder = Cluster.builder();
        realManager.populateCluster(clusterBuilder, cassandraConnection1);
        assertEquals(connectionTimeout, clusterBuilder.getConfiguration().getSocketOptions().getConnectTimeoutMillis());
        assertEquals(readTimeout, clusterBuilder.getConfiguration().getSocketOptions().getReadTimeoutMillis());
        assertEquals(keepAlive, clusterBuilder.getConfiguration().getSocketOptions().getKeepAlive());
        assertEquals(reuseAddress, clusterBuilder.getConfiguration().getSocketOptions().getReuseAddress());
        assertEquals(soLinger, clusterBuilder.getConfiguration().getSocketOptions().getSoLinger().intValue());
        assertEquals(tcpNoDelay, clusterBuilder.getConfiguration().getSocketOptions().getTcpNoDelay());
        assertEquals(receiveBufferSize, clusterBuilder.getConfiguration().getSocketOptions().getReceiveBufferSize().intValue());
        assertEquals(sendBufferSize, clusterBuilder.getConfiguration().getSocketOptions().getSendBufferSize().intValue());
    }

    @Test
    public void testInvalidHostDistanceClusterProp() throws Exception {
        Config config = new MockConfig(CollectionUtils.MapBuilder.<String,String>builder()
                .put(ServerConfigParams.PARAM_CASSANDRA_HOST_DISTANCE, "BAD")   // should default to LOCAL
                .put(ServerConfigParams.PARAM_CASSANDRA_MAX_SIMULTANEOUS_REQ_PER_HOST, "1234")
                .map());
        CassandraConnectionManagerImpl realManager = new CassandraConnectionManagerImpl(null,config,null,null,null);

        Cluster.Builder clusterBuilder = Cluster.builder();
        realManager.populateCluster(clusterBuilder, cassandraConnection1);
        assertEquals(clusterBuilder.getConfiguration().getPoolingOptions().getMaxConnectionsPerHost(HostDistance.LOCAL), 1234 );
    }

    private Goid getGoid() {
        ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator =
                new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }
}
