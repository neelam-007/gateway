package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.RestResponse;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.tools.RestEntityTests;
import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.*;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.RunOnNightly;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = RunOnNightly.class)
public class ActiveConnectorRestEntityResourceTest extends RestEntityTests<SsgActiveConnector, ActiveConnectorMO> {
    private SsgActiveConnectorManager ssgActiveConnectorManager;
    private List<SsgActiveConnector> activeConnectors = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        ssgActiveConnectorManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class);
        //Create the active connectors

        SsgActiveConnector activeConnector = new SsgActiveConnector();
        activeConnector.setName("Test MQ Config 1");
        activeConnector.setHardwiredServiceGoid(new Goid(123, 123));
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnector.setEnabled(true);
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false");

        ssgActiveConnectorManager.save(activeConnector);
        activeConnectors.add(activeConnector);

        activeConnector = new SsgActiveConnector();
        activeConnector.setName("Test MQ Config 2");
        activeConnector.setHardwiredServiceGoid(new Goid(123, 125));
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP);
        activeConnector.setEnabled(false);
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false");

        ssgActiveConnectorManager.save(activeConnector);
        activeConnectors.add(activeConnector);

        activeConnector = new SsgActiveConnector();
        activeConnector.setName("Test MQ Config 3");
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnector.setEnabled(true);
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false");

        ssgActiveConnectorManager.save(activeConnector);
        activeConnectors.add(activeConnector);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<SsgActiveConnector> all = ssgActiveConnectorManager.findAll();
        for (SsgActiveConnector activeConnector : all) {
            ssgActiveConnectorManager.delete(activeConnector.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(activeConnectors, new Functions.Unary<String, SsgActiveConnector>() {
            @Override
            public String call(SsgActiveConnector ssgActiveConnector) {
                return ssgActiveConnector.getId();
            }
        });
    }

    @Override
    public List<ActiveConnectorMO> getCreatableManagedObjects() {
        List<ActiveConnectorMO> activeConnectors = new ArrayList<>();

        ActiveConnectorMO activeConnector = ManagedObjectFactory.createActiveConnector();
        activeConnector.setId(getGoid().toString());
        activeConnector.setName("Test MQ Config created");
        activeConnector.setHardwiredId(new Goid(123, 567).toString());
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnector.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false")
                .map());
        activeConnectors.add(activeConnector);

        return activeConnectors;
    }

    @Override
    public List<ActiveConnectorMO> getUpdateableManagedObjects() {
        List<ActiveConnectorMO> activeConnectors = new ArrayList<>();

        SsgActiveConnector ssgActiveConnector = this.activeConnectors.get(0);
        ActiveConnectorMO activeConnector = ManagedObjectFactory.createActiveConnector();
        activeConnector.setId(ssgActiveConnector.getId());
        activeConnector.setName(ssgActiveConnector.getName() + " Updated");
        activeConnector.setHardwiredId(new Goid(999, 567).toString());
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP);
        activeConnector.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "hostUpdated")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "23499")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManagerUpdated")
                .put("newProperty", "value")
                .map());
        activeConnectors.add(activeConnector);
        return activeConnectors;
    }

    @Override
    public Map<ActiveConnectorMO, Functions.BinaryVoid<ActiveConnectorMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<ActiveConnectorMO, Functions.BinaryVoid<ActiveConnectorMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        ActiveConnectorMO activeConnector = ManagedObjectFactory.createActiveConnector();
        activeConnector.setName(activeConnectors.get(0).getName());
        activeConnector.setHardwiredId(new Goid(123, 567).toString());
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnector.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false")
                .map());

        builder.put(activeConnector, new Functions.BinaryVoid<ActiveConnectorMO, RestResponse>() {
            @Override
            public void call(ActiveConnectorMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(403, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<ActiveConnectorMO, Functions.BinaryVoid<ActiveConnectorMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<ActiveConnectorMO, Functions.BinaryVoid<ActiveConnectorMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        ActiveConnectorMO activeConnector = ManagedObjectFactory.createActiveConnector();
        activeConnector.setId(activeConnectors.get(0).getId());
        activeConnector.setName(activeConnectors.get(1).getName());
        activeConnector.setHardwiredId(new Goid(999, 567).toString());
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP);
        activeConnector.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "hostUpdated")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "23499")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManagerUpdated")
                .put("newProperty", "value")
                .map());

        builder.put(activeConnector, new Functions.BinaryVoid<ActiveConnectorMO, RestResponse>() {
            @Override
            public void call(ActiveConnectorMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(405, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
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
        return Functions.map(activeConnectors, new Functions.Unary<String, SsgActiveConnector>() {
            @Override
            public String call(SsgActiveConnector ssgActiveConnector) {
                return ssgActiveConnector.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "activeConnectors";
    }

    @Override
    public EntityType getType() {
        return EntityType.SSG_ACTIVE_CONNECTOR;
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        SsgActiveConnector entity = ssgActiveConnectorManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        SsgActiveConnector entity = ssgActiveConnectorManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, ActiveConnectorMO managedObject) throws FindException {
        SsgActiveConnector entity = ssgActiveConnectorManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getHardwiredServiceGoid() == null ? null : entity.getHardwiredServiceGoid().toString(), managedObject.getHardwiredId());
            Assert.assertEquals(entity.getType(), managedObject.getType());
            Assert.assertEquals(entity.isEnabled(), managedObject.isEnabled());
            Assert.assertEquals(entity.getPropertyNames().size(), managedObject.getProperties().size());
            for (String key : entity.getPropertyNames()) {
                Assert.assertEquals(entity.getProperty(key), managedObject.getProperties().get(key));
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(activeConnectors, new Functions.Unary<String, SsgActiveConnector>() {
                    @Override
                    public String call(SsgActiveConnector ssgActiveConnector) {
                        return ssgActiveConnector.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(activeConnectors.get(0).getName()), Arrays.asList(activeConnectors.get(0).getId()))
                .put("name=" + URLEncoder.encode(activeConnectors.get(0).getName()) + "&name=" + URLEncoder.encode(activeConnectors.get(1).getName()), Functions.map(activeConnectors.subList(0, 2), new Functions.Unary<String, SsgActiveConnector>() {
                    @Override
                    public String call(SsgActiveConnector ssgActiveConnector) {
                        return ssgActiveConnector.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("enabled=false", Arrays.asList(activeConnectors.get(1).getId()))
                .put("enabled=true", Arrays.asList(activeConnectors.get(0).getId(), activeConnectors.get(2).getId()))
                .put("type=MqNative", Arrays.asList(activeConnectors.get(0).getId(), activeConnectors.get(2).getId()))
                .put("type=SFTP", Arrays.asList(activeConnectors.get(1).getId()))
                .put("hardwiredServiceGoid=" + new Goid(123, 123).toString(), Arrays.asList(activeConnectors.get(0).getId()))
                .put("hardwiredServiceGoid", Arrays.asList(activeConnectors.get(2).getId()))
                .put("name=" + URLEncoder.encode(activeConnectors.get(0).getName()) + "&name=" + URLEncoder.encode(activeConnectors.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(activeConnectors.get(1).getId(), activeConnectors.get(0).getId()))
                .map();
    }
}
