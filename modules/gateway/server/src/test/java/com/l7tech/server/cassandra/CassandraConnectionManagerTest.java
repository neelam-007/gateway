package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.util.Config;
import com.l7tech.util.TimeUnit;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;

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
        cassandraConnection3.setCompression(CassandraConnection.COMPRESS_SNAPPY);
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
                mockCassandraConnectionEntityManager, config, securePasswordManager, trustManager, secureRandom);

    }


    @After
    public void after() throws Exception {
        cassandraConnectionManager = null;
    }

    @Test
    public void testAddConnection() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        Assert.assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        CassandraConnectionHolder result = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        Assert.assertSame("Connection not added with the same settings.", cassandraConnection1, result.getCassandraConnectionEntity());

        cassandraConnectionManager.addConnection(cassandraConnection2);
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Connection 3 is set to be disabled.
        cassandraConnectionManager.addConnection(cassandraConnection3);
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        Assert.assertSame("Connections are different.", result, cache);
    }

    @Test
    public void testRemoveConnection() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        cassandraConnectionManager.addConnection(cassandraConnection3);
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        cassandraConnectionManager.removeConnection(cassandraConnection1);
        Assert.assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        cassandraConnectionManager.removeConnection(cassandraConnection2);
        Assert.assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());
        // Remove non-existing connection
        cassandraConnectionManager.removeConnection(cassandraConnection3);
        Assert.assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());

        // By Goid
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        cassandraConnectionManager.addConnection(cassandraConnection3);
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        Goid goid = cassandraConnection1.getGoid();
        cassandraConnectionManager.removeConnection(goid);
        Assert.assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());

        goid = cassandraConnection2.getGoid();
        cassandraConnectionManager.removeConnection(goid);

        // Remove non-existing connection
        goid = cassandraConnection3.getGoid();
        cassandraConnectionManager.removeConnection(goid);
        Assert.assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());
    }

    @Test
    public void testCloseAllConnections() throws Exception {
        cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        // Getting existing connection, cache size should be the same
        cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

        cassandraConnectionManager.closeAllConnections();
        Assert.assertEquals("Cache size does not match.", 0, cassandraConnectionManager.getConnectionCacheSize());
    }

    @Test
    public void testUpdateConnectionWithNewSettings() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

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
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        Assert.assertEquals(cassandraConnection1.getId(), cache.getCassandraConnectionEntity().getId());
        Assert.assertEquals(cassandraConnection1.getName(), cache.getCassandraConnectionEntity().getName());
        Assert.assertEquals("10.10.10.10", cache.getCassandraConnectionEntity().getContactPoints());
        Assert.assertEquals("1234", cache.getCassandraConnectionEntity().getPort());

        // Connection2 has not changed
        cache = cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        Assert.assertSame("Connection not added with the same settings.", cassandraConnection2, cache.getCassandraConnectionEntity());
    }

    @Test
    public void testUpdateConnectionWithDifferentName() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        CassandraConnectionHolder result = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        Assert.assertSame("Connection not added with the same settings.", cassandraConnection1, result.getCassandraConnectionEntity());
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

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
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection("New Name");
        // No new connection created in cache
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());
        Assert.assertEquals(cassandraConnection1.getId(), cache.getCassandraConnectionEntity().getId());
        Assert.assertEquals("New Name", cache.getCassandraConnectionEntity().getName());

        // Connection2 has not changed
        cache = cassandraConnectionManager.getConnection(cassandraConnection2.getName());
        Assert.assertSame("Connection not added with the same settings.", cassandraConnection2, cache.getCassandraConnectionEntity());
    }

    @Test
    public void testUpdateConnectionToDisabled() throws Exception {
        cassandraConnectionManager.addConnection(cassandraConnection1);
        cassandraConnectionManager.addConnection(cassandraConnection2);
        Assert.assertEquals("Cache size does not match.", 2, cassandraConnectionManager.getConnectionCacheSize());

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
        Assert.assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        // What's left in the cache should be just connection1
        CassandraConnectionHolder cache = cassandraConnectionManager.getConnection(cassandraConnection1.getName());
        // No new connection created in cache
        Assert.assertEquals("Cache size does not match.", 1, cassandraConnectionManager.getConnectionCacheSize());
        Assert.assertEquals(cassandraConnection1.getId(), cache.getCassandraConnectionEntity().getId());
        Assert.assertEquals(cassandraConnection1.getName(), cache.getCassandraConnectionEntity().getName());
    }

    private Goid getGoid() {
        ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator =
                new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }
}
