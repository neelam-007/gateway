package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.CassandraConnectionMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.cassandra.CassandraConnection;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class CassandraConnectionRestEntityResourceTest extends RestEntityTests<CassandraConnection, CassandraConnectionMO> {
    private CassandraConnectionEntityManager cassandraConnectionEntityManager;
    private List<CassandraConnection> cassandraConnections = new ArrayList<>();

    @Before
    public void before() throws Exception {
        cassandraConnectionEntityManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("cassandraConnectionEntityManager", CassandraConnectionEntityManager.class);

        //Create new connections
        CassandraConnection cc1 = new CassandraConnection();
        cc1.setId(getGoid().toString());
        cc1.setName("Test Cassandra connection 1");
        cc1.setKeyspaceName("test.keyspace1");
        cc1.setContactPoints("localhost");
        cc1.setPort("9042");
        cc1.setUsername("gateway");
        cc1.setCompression("ProtocolOptions.Compression.NONE");
        cc1.setSsl(true);
        cc1.setTlsEnabledCipherSuites("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cc1.setEnabled(true);
        cassandraConnections.add(cc1);
        cassandraConnectionEntityManager.save(cc1);

        CassandraConnection cc2 = new CassandraConnection();
        cc2.setId(getGoid().toString());
        cc2.setName("Test Cassandra connection 2");
        cc2.setKeyspaceName("test.keyspace2");
        cc2.setContactPoints("127.0.0.1");
        cc2.setPort("9043");
        cc2.setUsername("gateway2");
        cc2.setCompression("ProtocolOptions.Compression.LZ4");
        cc2.setSsl(true);
        cc2.setTlsEnabledCipherSuites("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cc2.setEnabled(true);
        cassandraConnections.add(cc2);
        cassandraConnectionEntityManager.save(cc2);
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<CassandraConnection> all = cassandraConnectionEntityManager.findAll();
        for (CassandraConnection cassandraConnection : all) {
            cassandraConnectionEntityManager.delete(cassandraConnection.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(cassandraConnections, new Functions.Unary<String, CassandraConnection>() {
            @Override
            public String call(CassandraConnection cassandraConnection) {
                return cassandraConnection.getId();
            }
        });
    }

    @Override
    public List<CassandraConnectionMO> getCreatableManagedObjects() {
        List<CassandraConnectionMO> cassandraConnectionMOs = new ArrayList<>();

        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(getGoid().toString());
        cassandraConnectionMO.setName("Test Cassandra connection created");
        cassandraConnectionMO.setKeyspace("test.keyspace");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setPort("9042");
        cassandraConnectionMO.setUsername("gateway");
        cassandraConnectionMO.setCompression("ProtocolOptions.Compression.NONE");
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setTlsciphers("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("test", "test").map());

        cassandraConnectionMOs.add(cassandraConnectionMO);

        return cassandraConnectionMOs;
    }

    @Override
    public List<CassandraConnectionMO> getUpdateableManagedObjects() {
        List<CassandraConnectionMO> cassandraConnectionMOs = new ArrayList<>();

        CassandraConnection cassandraConnection = this.cassandraConnections.get(0);
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnection.getId());
        cassandraConnectionMO.setVersion(cassandraConnection.getVersion());
        cassandraConnectionMO.setName(cassandraConnection.getName() + " Updated 1");
        cassandraConnectionMO.setKeyspace("test.keyspace");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setPort("1234");
        cassandraConnectionMO.setUsername("gateway");
        cassandraConnectionMO.setCompression("ProtocolOptions.Compression.NONE");
        cassandraConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("test", "test").map());
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setTlsciphers("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMOs.add(cassandraConnectionMO);

        //update twice
        cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnection.getId());
        cassandraConnectionMO.setVersion(cassandraConnection.getVersion());
        cassandraConnectionMO.setName(cassandraConnection.getName() + " Updated 2");
        cassandraConnectionMO.setKeyspace("test.keyspace");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setPort("4321");
        cassandraConnectionMO.setUsername("gateway");
        cassandraConnectionMO.setCompression("ProtocolOptions.Compression.NONE");
        cassandraConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("test2", "test2").map());
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setTlsciphers("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMOs.add(cassandraConnectionMO);

        return cassandraConnectionMOs;
    }

    @Override
    public Map<CassandraConnectionMO, Functions.BinaryVoid<CassandraConnectionMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<CassandraConnectionMO, Functions.BinaryVoid<CassandraConnectionMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName("UnCreatable Cassandra connection");
        cassandraConnectionMO.setKeyspace("test.keyspace");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setPort("9042");
        cassandraConnectionMO.setUsername("gateway");
        cassandraConnectionMO.setCompression("ProtocolOptions.Compression.NONE");
        cassandraConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("test", "test").map());
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setTlsciphers("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMO.setSecurityZoneId("12345");

        builder.put(cassandraConnectionMO, new Functions.BinaryVoid<CassandraConnectionMO, RestResponse>() {
            @Override
            public void call(CassandraConnectionMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<CassandraConnectionMO, Functions.BinaryVoid<CassandraConnectionMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<CassandraConnectionMO, Functions.BinaryVoid<CassandraConnectionMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnections.get(0).getId());
        cassandraConnectionMO.setVersion(cassandraConnections.get(0).getVersion());
        cassandraConnectionMO.setName(cassandraConnections.get(0).getName() + " Updated");
        cassandraConnectionMO.setKeyspace("test.keyspace");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setPort("1234");
        cassandraConnectionMO.setUsername("gateway");
        cassandraConnectionMO.setCompression("ProtocolOptions.Compression.NONE");
        cassandraConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("test", "test").map());
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setTlsciphers("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMO.setSecurityZoneId("12345");

        builder.put(cassandraConnectionMO, new Functions.BinaryVoid<CassandraConnectionMO, RestResponse>() {
            @Override
            public void call(CassandraConnectionMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf" + getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 400, restResponse.getStatus());
            }
        });
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(cassandraConnections, new Functions.Unary<String, CassandraConnection>() {
            @Override
            public String call(CassandraConnection cassandraConnection) {
                return cassandraConnection.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "cassandraConnections";
    }

    @Override
    public String getType() {
        return EntityType.CASSANDRA_CONFIGURATION.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        CassandraConnection entity = cassandraConnectionEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        CassandraConnection entity = cassandraConnectionEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, CassandraConnectionMO managedObject) throws FindException {
        CassandraConnection entity = cassandraConnectionEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getKeyspaceName(), managedObject.getKeyspace());
            Assert.assertEquals(entity.getContactPoints(), managedObject.getContactPoint());
            Assert.assertEquals(entity.getPort(), managedObject.getPort());
            Assert.assertEquals(entity.getUsername(), managedObject.getUsername());
            Assert.assertEquals(entity.getCompression(), managedObject.getCompression());
            Assert.assertEquals(entity.isSsl(), managedObject.isSsl());
            Assert.assertEquals(entity.isEnabled(), managedObject.isEnabled());
            Assert.assertEquals(entity.getTlsEnabledCipherSuites(),managedObject.getTlsciphers());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(cassandraConnections, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection message) {
                        return message.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(cassandraConnections.get(0).getName()),
                        Arrays.asList(cassandraConnections.get(0).getId()))
                .put("name=" + URLEncoder.encode(cassandraConnections.get(0).getName()) + "&name=" + URLEncoder.encode(cassandraConnections.get(1).getName()),
                        Functions.map(cassandraConnections.subList(0, 2), new Functions.Unary<String, CassandraConnection>() {
                            @Override
                            public String call(CassandraConnection cassandraConnection) {
                                return cassandraConnection.getId();
                            }
                        }))
                .put("name=banName", Collections.<String>emptyList())
                .put("name=" + URLEncoder.encode(cassandraConnections.get(0).getName()) + "&name=" + URLEncoder.encode(cassandraConnections.get(1).getName()) + "&sort=name&order=desc",
                        Arrays.asList(cassandraConnections.get(1).getId(), cassandraConnections.get(0).getId()))
                .put("keyspace=" + URLEncoder.encode(cassandraConnections.get(0).getKeyspaceName()), Arrays.asList(cassandraConnections.get(0).getId()))
                .put("keyspace=" + URLEncoder.encode(cassandraConnections.get(0).getKeyspaceName()) + "&keyspace=" + URLEncoder.encode(cassandraConnections.get(1).getKeyspaceName()),
                        Functions.map(cassandraConnections.subList(0, 2), new Functions.Unary<String, CassandraConnection>() {
                            @Override
                            public String call(CassandraConnection cassandraConnection) {
                                return cassandraConnection.getId();
                            }
                        }))
                .put("contactPoint=" + URLEncoder.encode(cassandraConnections.get(0).getContactPoints()), Arrays.asList(cassandraConnections.get(0).getId()))
                .put("port=" + URLEncoder.encode(cassandraConnections.get(0).getPort()), Arrays.asList(cassandraConnections.get(0).getId()))
                .put("username=" + URLEncoder.encode(cassandraConnections.get(0).getUsername()), Arrays.asList(cassandraConnections.get(0).getId()))
                .put("compression=" + URLEncoder.encode(cassandraConnections.get(0).getCompression()), Arrays.asList(cassandraConnections.get(0).getId()))
                .map();
    }
}
