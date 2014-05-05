package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.external.assertions.whichmodule.DemoGenericEntity;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.entity.TestDemoGenericEntity;
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
public class GenericEntityRestEntityResourceTest extends RestEntityTests<GenericEntity, GenericEntityMO> {
    private GenericEntityManager genericEntityManager;
    private List<GenericEntity> genericEntities = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        genericEntityManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(TestDemoGenericEntity.class);

        DemoGenericEntity genericEntity = new DemoGenericEntity();
        genericEntity.setName("Generic Entity 1");
        genericEntity.setAge(1);
        genericEntity.setPlaysTrombone(true);
        genericEntity.setServiceId(new Goid(1,2));
        genericEntity.setEnabled(true);

        Goid id = genericEntityManager.getEntityManager(DemoGenericEntity.class).save(genericEntity);
        genericEntity.setId(id.toString());
        genericEntities.add(genericEntity);

        genericEntity = new DemoGenericEntity();
        genericEntity.setName("Generic Entity 2");
        genericEntity.setAge(34);
        genericEntity.setPlaysTrombone(false);
        genericEntity.setServiceId(new Goid(4,6));
        genericEntity.setEnabled(false);

        id = genericEntityManager.getEntityManager(DemoGenericEntity.class).save(genericEntity);
        genericEntity.setId(id.toString());
        genericEntities.add(genericEntity);

        TestDemoGenericEntity testDemoGenericEntity = new TestDemoGenericEntity();
        testDemoGenericEntity.setName("Generic Entity 3");
        testDemoGenericEntity.setAge(1);
        testDemoGenericEntity.setPlaysTrombone(true);
        testDemoGenericEntity.setEnabled(true);

        id = genericEntityManager.getEntityManager(TestDemoGenericEntity.class).save(testDemoGenericEntity);
        testDemoGenericEntity.setId(id.toString());
        genericEntities.add(testDemoGenericEntity);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<GenericEntity> all = genericEntityManager.findAll();
        for (GenericEntity genericEntity : all) {
            genericEntityManager.delete(genericEntity.getGoid());
        }

        genericEntityManager.unRegisterClass(TestDemoGenericEntity.class.getName());
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(genericEntities, new Functions.Unary<String, GenericEntity>() {
            @Override
            public String call(GenericEntity genericEntity) {
                return genericEntity.getId();
            }
        });
    }

    @Override
    public List<GenericEntityMO> getCreatableManagedObjects() {
        List<GenericEntityMO> genericEntityMOs = new ArrayList<>();

        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setId(getGoid().toString());
        genericEntityMO.setName("New Generic Entity");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(0).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(0).getValueXml());
        genericEntityMOs.add(genericEntityMO);

        //same name but different class
        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setId(getGoid().toString());
        genericEntityMO.setName(genericEntities.get(2).getName());
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(0).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(0).getValueXml());
        genericEntityMOs.add(genericEntityMO);

        return genericEntityMOs;
    }

    @Override
    public List<GenericEntityMO> getUpdateableManagedObjects() {
        List<GenericEntityMO> genericEntityMOs = new ArrayList<>();

        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setId(genericEntities.get(0).getId());
        genericEntityMO.setName(genericEntities.get(0).getName()+"Updated");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(1).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(1).getValueXml());
        genericEntityMOs.add(genericEntityMO);

        //update twice
        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setId(genericEntities.get(0).getId());
        genericEntityMO.setName(genericEntities.get(0).getName()+"Updated");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(1).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(1).getValueXml());
        genericEntityMOs.add(genericEntityMO);

        return genericEntityMOs;
    }

    @Override
    public Map<GenericEntityMO, Functions.BinaryVoid<GenericEntityMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<GenericEntityMO, Functions.BinaryVoid<GenericEntityMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName(null);
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(1).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(1).getValueXml());

        builder.put(genericEntityMO, new Functions.BinaryVoid<GenericEntityMO, RestResponse>() {
            @Override
            public void call(GenericEntityMO genericEntityMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8226
        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("aaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddzzzzzzzzzzaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddzzzzzzzzzz");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(1).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(1).getValueXml());

        builder.put(genericEntityMO, new Functions.BinaryVoid<GenericEntityMO, RestResponse>() {
            @Override
            public void call(GenericEntityMO genericEntityMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("enabled is missing");
        genericEntityMO.setEntityClassName(genericEntities.get(1).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(1).getValueXml());

        builder.put(genericEntityMO, new Functions.BinaryVoid<GenericEntityMO, RestResponse>() {
            @Override
            public void call(GenericEntityMO genericEntityMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName(genericEntities.get(1).getName());
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName(genericEntities.get(1).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(1).getValueXml());

        builder.put(genericEntityMO, new Functions.BinaryVoid<GenericEntityMO, RestResponse>() {
            @Override
            public void call(GenericEntityMO genericEntityMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<GenericEntityMO, Functions.BinaryVoid<GenericEntityMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<GenericEntityMO, Functions.BinaryVoid<GenericEntityMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setId(genericEntities.get(0).getId());
        genericEntityMO.setName(genericEntities.get(1).getName());
        genericEntityMO.setEnabled(genericEntities.get(0).isEnabled());
        genericEntityMO.setEntityClassName(genericEntities.get(0).getEntityClassName());
        genericEntityMO.setValueXml(genericEntities.get(0).getValueXml());

        builder.put(genericEntityMO, new Functions.BinaryVoid<GenericEntityMO, RestResponse>() {
            @Override
            public void call(GenericEntityMO genericEntityMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
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
        return Functions.map(genericEntities, new Functions.Unary<String, GenericEntity>() {
            @Override
            public String call(GenericEntity genericEntity) {
                return genericEntity.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "genericEntities";
    }

    @Override
    public String getType() {
        return EntityType.GENERIC.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        GenericEntity entity = genericEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        GenericEntity entity = genericEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, GenericEntityMO managedObject) throws FindException {
        GenericEntity entity = genericEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.isEnabled(), managedObject.getEnabled());
            Assert.assertEquals(entity.getEntityClassName(), managedObject.getEntityClassName());
            Assert.assertEquals(entity.getValueXml(), managedObject.getValueXml());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(genericEntities, new Functions.Unary<String, GenericEntity>() {
                    @Override
                    public String call(GenericEntity genericEntity) {
                        return genericEntity.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(genericEntities.get(0).getName()), Arrays.asList(genericEntities.get(0).getId()))
                .put("name=" + URLEncoder.encode(genericEntities.get(0).getName()) + "&name=" + URLEncoder.encode(genericEntities.get(1).getName()), Functions.map(genericEntities.subList(0, 2), new Functions.Unary<String, GenericEntity>() {
                    @Override
                    public String call(GenericEntity genericEntity) {
                        return genericEntity.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("enabled=false", Arrays.asList(genericEntities.get(1).getId()))
                .put("enabled=true", Arrays.asList(genericEntities.get(0).getId(), genericEntities.get(2).getId()))
                .put("entityClassName=" + URLEncoder.encode(genericEntities.get(0).getEntityClassName()), Arrays.asList(genericEntities.get(0).getId(), genericEntities.get(1).getId()))
                .put("entityClassName=" + URLEncoder.encode(genericEntities.get(2).getEntityClassName()), Arrays.asList(genericEntities.get(2).getId()))
                .put("name=" + URLEncoder.encode(genericEntities.get(0).getName()) + "&name=" + URLEncoder.encode(genericEntities.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(genericEntities.get(1).getId(), genericEntities.get(0).getId()))
                .map();
    }
}
