package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
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
public class CustomKeyValueRestEntityResourceTest extends RestEntityTests<CustomKeyValueStore, CustomKeyValueStoreMO> {
    private CustomKeyValueStoreManager customKeyValueStoreManager;
    private List<CustomKeyValueStore> customKeyValueStores = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        customKeyValueStoreManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("customKeyValueStoreManager", CustomKeyValueStoreManager.class);
        //Create the active connectors

        CustomKeyValueStore customKeyValueStore = new CustomKeyValueStore();
        customKeyValueStore.setName("My Custom Key 1");
        customKeyValueStore.setValue("My Value".getBytes());

        customKeyValueStoreManager.save(customKeyValueStore);
        customKeyValueStores.add(customKeyValueStore);

        customKeyValueStore = new CustomKeyValueStore();
        customKeyValueStore.setName("My Custom Key 2");
        customKeyValueStore.setValue("My Value 2".getBytes());

        customKeyValueStoreManager.save(customKeyValueStore);
        customKeyValueStores.add(customKeyValueStore);

        customKeyValueStore = new CustomKeyValueStore();
        customKeyValueStore.setName("My Custom Key 3");
        customKeyValueStore.setValue("My Value 3".getBytes());

        customKeyValueStoreManager.save(customKeyValueStore);
        customKeyValueStores.add(customKeyValueStore);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<CustomKeyValueStore> all = customKeyValueStoreManager.findAll();
        for (CustomKeyValueStore customKeyValueStore : all) {
            customKeyValueStoreManager.delete(customKeyValueStore.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(customKeyValueStores, new Functions.Unary<String, CustomKeyValueStore>() {
            @Override
            public String call(CustomKeyValueStore customKeyValueStore) {
                return customKeyValueStore.getId();
            }
        });
    }

    @Override
    public List<CustomKeyValueStoreMO> getCreatableManagedObjects() {
        List<CustomKeyValueStoreMO> customKeyValueStoreMOs = new ArrayList<>();

        CustomKeyValueStoreMO customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setId(getGoid().toString());
        customKeyValueStoreMO.setKey("My Key");
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue("My Value".getBytes());
        customKeyValueStoreMOs.add(customKeyValueStoreMO);

        return customKeyValueStoreMOs;
    }

    @Override
    public List<CustomKeyValueStoreMO> getUpdateableManagedObjects() {
        List<CustomKeyValueStoreMO> customKeyValueStoreMOs = new ArrayList<>();

        CustomKeyValueStore customKeyValueStore = this.customKeyValueStores.get(0);
        CustomKeyValueStoreMO customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setId(customKeyValueStore.getId());
        customKeyValueStoreMO.setKey("Updated" + customKeyValueStore.getName());
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue(customKeyValueStore.getValue());
        customKeyValueStoreMOs.add(customKeyValueStoreMO);
        return customKeyValueStoreMOs;
    }

    @Override
    public Map<CustomKeyValueStoreMO, Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<CustomKeyValueStoreMO, Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        CustomKeyValueStoreMO customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setKey(customKeyValueStores.get(0).getName());
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue(customKeyValueStores.get(0).getValue());

        builder.put(customKeyValueStoreMO, new Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>() {
            @Override
            public void call(CustomKeyValueStoreMO customKeyValueStoreMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        String longName = "";
        for(int i = 0 ; i<=128; i++){
            longName+="a";
        }
        customKeyValueStoreMO.setKey(longName);
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue(customKeyValueStores.get(0).getValue());

        builder.put(customKeyValueStoreMO, new Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>() {
            @Override
            public void call(CustomKeyValueStoreMO customKeyValueStoreMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setKey("An Ok name with a bad keystore");
        customKeyValueStoreMO.setStoreName("other");
        customKeyValueStoreMO.setValue(customKeyValueStores.get(0).getValue());

        builder.put(customKeyValueStoreMO, new Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>() {
            @Override
            public void call(CustomKeyValueStoreMO customKeyValueStoreMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<CustomKeyValueStoreMO, Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<CustomKeyValueStoreMO, Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        CustomKeyValueStoreMO customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setId(customKeyValueStores.get(0).getId());
        customKeyValueStoreMO.setKey(customKeyValueStores.get(1).getName());
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue(customKeyValueStores.get(0).getValue());

        builder.put(customKeyValueStoreMO, new Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>() {
            @Override
            public void call(CustomKeyValueStoreMO customKeyValueStoreMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setId(customKeyValueStores.get(0).getId());
        String longName = "";
        for(int i = 0 ; i<=128; i++){
            longName+="a";
        }
        customKeyValueStoreMO.setKey(longName);
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue(customKeyValueStores.get(0).getValue());

        builder.put(customKeyValueStoreMO, new Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>() {
            @Override
            public void call(CustomKeyValueStoreMO customKeyValueStoreMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setId(customKeyValueStores.get(0).getId());
        customKeyValueStoreMO.setKey("An Ok name with a bad keystore");
        customKeyValueStoreMO.setStoreName("other");
        customKeyValueStoreMO.setValue(customKeyValueStores.get(0).getValue());

        builder.put(customKeyValueStoreMO, new Functions.BinaryVoid<CustomKeyValueStoreMO, RestResponse>() {
            @Override
            public void call(CustomKeyValueStoreMO customKeyValueStoreMO, RestResponse restResponse) {
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
        return Functions.map(customKeyValueStores, new Functions.Unary<String, CustomKeyValueStore>() {
            @Override
            public String call(CustomKeyValueStore customKeyValueStore) {
                return customKeyValueStore.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "customKeyValues";
    }

    @Override
    public String getType() {
        return EntityType.CUSTOM_KEY_VALUE_STORE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        CustomKeyValueStore entity = customKeyValueStoreManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        CustomKeyValueStore entity = customKeyValueStoreManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, CustomKeyValueStoreMO managedObject) throws FindException {
        CustomKeyValueStore entity = customKeyValueStoreManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getKey());
            org.junit.Assert.assertArrayEquals(entity.getValue(), managedObject.getValue());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(customKeyValueStores, new Functions.Unary<String, CustomKeyValueStore>() {
                    @Override
                    public String call(CustomKeyValueStore customKeyValueStore) {
                        return customKeyValueStore.getId();
                    }
                }))
                .put("key=" + URLEncoder.encode(customKeyValueStores.get(0).getName()), Arrays.asList(customKeyValueStores.get(0).getId()))
                .put("key=" + URLEncoder.encode(customKeyValueStores.get(0).getName()) + "&key=" + URLEncoder.encode(customKeyValueStores.get(1).getName()), Functions.map(customKeyValueStores.subList(0, 2), new Functions.Unary<String, CustomKeyValueStore>() {
                    @Override
                    public String call(CustomKeyValueStore customKeyValueStore) {
                        return customKeyValueStore.getId();
                    }
                }))
                .put("key=banName", Collections.<String>emptyList())
                .put("key=" + URLEncoder.encode(customKeyValueStores.get(0).getName()) + "&key=" + URLEncoder.encode(customKeyValueStores.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(customKeyValueStores.get(1).getId(), customKeyValueStores.get(0).getId()))
                .map();
    }
}
