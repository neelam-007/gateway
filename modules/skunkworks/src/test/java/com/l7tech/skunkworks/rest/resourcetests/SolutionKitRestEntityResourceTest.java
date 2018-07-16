package com.l7tech.skunkworks.rest.resourcetests;


import com.l7tech.common.http.HttpConstants;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.SolutionKitResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SolutionKitTransformer;
import com.l7tech.gateway.api.EntityOwnershipDescriptorMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SolutionKitMO;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class SolutionKitRestEntityResourceTest extends RestEntityTests<SolutionKit, SolutionKitMO> {

    private SolutionKitManager solutionKitManager;
    private List<SolutionKit> solutionKits = new ArrayList<>();
    private SortedMap<String, String> sortedGuidMap = new TreeMap<>();

    @Before
    public void before() throws Exception {
        //get a reference to the SK Manager from the gateway
        solutionKitManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("solutionKitManager", SolutionKitManager.class);
        Assert.assertThat(solutionKitManager, Matchers.notNullValue());

        //create sample SK 1 - simple SK with Entity Owner Descriptors
        SolutionKit solutionKit = new SolutionKit();
        solutionKit.setName("3Solution Kit 1");
        solutionKit.setSolutionKitGuid(UUID.randomUUID().toString());
        solutionKit.setSolutionKitVersion("1.0");
        solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, "SK with EntityOwnershipDescriptors");
        solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, new Date().toString());
        solutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false");
        solutionKit.addEntityOwnershipDescriptors(
                CollectionUtils.set(
                        new EntityOwnershipDescriptor(solutionKit, "id1", EntityType.FOLDER, true),
                        new EntityOwnershipDescriptor(solutionKit, "id2", EntityType.SECURE_PASSWORD, false),
                        new EntityOwnershipDescriptor(solutionKit, "id3", EntityType.SERVICE, true) {{
                            setVersionStamp(12);
                        }},
                        new EntityOwnershipDescriptor(solutionKit, "id4", EntityType.SERVER_MODULE_FILE, false)
                )
        );
        solutionKit.setMappings("mappings go here");
        solutionKit.setUninstallBundle("uninstall bundle goes here");

        solutionKitManager.save(solutionKit);
        Assert.assertNotNull(solutionKit.getId());
        sortedGuidMap.put(solutionKit.getSolutionKitGuid(), solutionKit.getId());
        solutionKits.add(solutionKit);

        //create sample SK 2 - with parent/child relationship
        SolutionKit parentSolutionKit = new SolutionKit();
        parentSolutionKit.setName("1Solution Kit 2 - Parent");
        parentSolutionKit.setSolutionKitGuid(UUID.randomUUID().toString());
        parentSolutionKit.setSolutionKitVersion("1.0");
        parentSolutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, "SK for parent/child unit test");
        parentSolutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, new Date().toString());
        parentSolutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true");
        parentSolutionKit.setMappings("mappings go here");
        parentSolutionKit.setUninstallBundle("uninstall bundle goes here");

        Goid parentGoid = solutionKitManager.save(parentSolutionKit);
        Assert.assertNotNull(parentSolutionKit.getId());
        sortedGuidMap.put(parentSolutionKit.getSolutionKitGuid(), parentSolutionKit.getId());
        solutionKits.add(parentSolutionKit);

        SolutionKit childSK1 = new SolutionKit();
        childSK1.setName("2Solution Kit 2 - Child 1");
        childSK1.setSolutionKitGuid(UUID.randomUUID().toString());
        childSK1.setParentGoid(parentGoid);
        childSK1.setSolutionKitVersion("1.0");
        childSK1.setProperty(SolutionKit.SK_PROP_DESC_KEY, "SK for parent/child unit test");
        childSK1.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, new Date().toString());
        childSK1.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false");
        childSK1.setMappings("mappings go here");
        childSK1.setUninstallBundle("uninstall bundle goes here");

        solutionKitManager.save(childSK1);
        Assert.assertNotNull(childSK1.getId());
        sortedGuidMap.put(childSK1.getSolutionKitGuid(), childSK1.getId());
        solutionKits.add(childSK1);


        SolutionKit childSK2 = new SolutionKit();
        childSK2.setName("4Solution Kit 2 - Child 2");
        childSK2.setSolutionKitGuid(UUID.randomUUID().toString());
        childSK2.setParentGoid(parentGoid);
        childSK2.setSolutionKitVersion("1.0");
        childSK2.setProperty(SolutionKit.SK_PROP_DESC_KEY, "SK for parent/child unit test");
        childSK2.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, new Date().toString());
        childSK2.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false");
        childSK2.setMappings("mappings go here");
        childSK2.setUninstallBundle("uninstall bundle goes here");

        solutionKitManager.save(childSK2);
        Assert.assertNotNull(childSK2.getId());
        sortedGuidMap.put(childSK2.getSolutionKitGuid(), childSK2.getId());
        solutionKits.add(childSK2);
    }

    @After
    public void after() throws Exception {
        // clean up all added SK entities
        Collection<SolutionKit> allSolutionKitsInstalled = solutionKitManager.findAll();
        for (SolutionKit solutionKit : allSolutionKitsInstalled) {
            solutionKitManager.delete(solutionKit.getGoid());
        }
    }

    @Override
    public String getResourceUri() {
        return SolutionKitResource.SolutionKits_URI;
    }

    @Override
    public String getType() {
        return EntityType.SOLUTION_KIT.name();
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(solutionKits, new Functions.Unary<String, SolutionKit>() {
            @Override
            public String call(SolutionKit solutionKit) {
                return solutionKit.getId();
            }
        });
    }

    @Override
    public List<SolutionKitMO> getCreatableManagedObjects() {
        return Collections.emptyList();
    }

    @Override
    public List<SolutionKitMO> getUpdateableManagedObjects() {
        return Collections.emptyList();
    }

    @Override
    public Map<SolutionKitMO, Functions.BinaryVoid<SolutionKitMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<SolutionKitMO, Functions.BinaryVoid<SolutionKitMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        SolutionKitMO solutionKitMO = ManagedObjectFactory.createSolutionKitMO();
        solutionKitMO.setName("SK MO 1");
        solutionKitMO.setSkGuid(UUID.randomUUID().toString());
        solutionKitMO.setSkVersion("1.0");
        solutionKitMO.setParentReference(null);
        Map<String, String> properties = new HashMap<>();
        properties.put(SolutionKit.SK_PROP_DESC_KEY, "Solution Kit MO 1");
        properties.put(SolutionKit.SK_PROP_TIMESTAMP_KEY, new Date().toString());
        properties.put(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false");
        properties.put(SolutionKit.SK_PROP_FEATURE_SET_KEY, "");
        properties.put(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, "");
        properties.put(SolutionKit.SK_PROP_CUSTOM_UI_KEY, "");
        properties.put(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "");
        properties.put(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, "");
        solutionKitMO.setProperties(properties);
        solutionKitMO.setInstallProperties(properties);
        solutionKitMO.setUninstallBundle("SK MO Uninstall Bundle");
        solutionKitMO.setMappings("SK MO Mappings");
        solutionKitMO.setLastUpdateTime(System.currentTimeMillis());

        builder.put(solutionKitMO, new Functions.BinaryVoid<SolutionKitMO, RestResponse>() {
            @Override
            public void call(SolutionKitMO skMO, RestResponse restResponse) {
                Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, restResponse.getStatus()); //not implemented yet
            }
        });

        return builder.map();
    }

    @Override
    public Map<SolutionKitMO, Functions.BinaryVoid<SolutionKitMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<SolutionKitMO, Functions.BinaryVoid<SolutionKitMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        try {
            List<SolutionKit> solutionKitList = solutionKitManager.findBySolutionKitGuid(solutionKits.get(0).getSolutionKitGuid());
            Assert.assertNotNull(solutionKitList);
            Assert.assertTrue(solutionKitList.size() > 0);
            SolutionKitTransformer solutionKitTransformer = new SolutionKitTransformer();

            SolutionKitMO solutionKitMO = solutionKitTransformer.convertToMO(solutionKitList.get(0));
            builder.put(solutionKitMO, new Functions.BinaryVoid<SolutionKitMO, RestResponse>() {
                @Override
                public void call(SolutionKitMO skMO, RestResponse restResponse) {
                    Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, restResponse.getStatus()); //not implemented yet
                }
            });
        } catch (FindException e) {
            throw new RuntimeException(e);
        }

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        for (SolutionKit solutionKit : solutionKits) {
            builder.put(solutionKit.getId(), new Functions.BinaryVoid<String, RestResponse>() {
                @Override
                public void call(String s, RestResponse restResponse) {
                    Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, restResponse.getStatus()); //not implemented
                }
            });
        }
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Collections.emptyList();
    }

    @Override
    public String getExpectedTitle(String id) throws Exception {
        SolutionKit entity = solutionKitManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws Exception {
        SolutionKit entity = solutionKitManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    protected void verifyDeleteNoExistingEntity(final RestResponse response) throws Exception {
        Assert.assertNotNull(response);
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected Method not found", HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
    }

    @Override
    public void verifyEntity(String id, SolutionKitMO managedObject) throws Exception {
        SolutionKit entity = solutionKitManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getSolutionKitGuid(), managedObject.getSkGuid());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            if (managedObject.getParentReference() == null) {
                // either the parent goid is null or if DEFAULT_GOID
                Assert.assertThat(entity.getParentGoid(), Matchers.anyOf(Matchers.nullValue(Goid.class), Matchers.equalTo(Goid.DEFAULT_GOID)));
            } else {
                Assert.assertNotNull(entity.getParentGoid());
                // we want Goid.parseGoid to fail on non-goid id
                Assert.assertEquals(entity.getParentGoid(), Goid.parseGoid(managedObject.getParentReference().getId()));
            }
            Assert.assertEquals(entity.getSolutionKitVersion(), managedObject.getSkVersion());
            for (String propertyName : SolutionKit.getPropertyKeys()) {
                Assert.assertEquals(entity.getProperty(propertyName), managedObject.getProperties().get(propertyName));
            }
            for (String propertyName : SolutionKit.getInstallPropertyKeys()) {
                Assert.assertEquals(entity.getInstallationProperty(propertyName), managedObject.getInstallProperties().get(propertyName));
            }
            Assert.assertEquals(entity.getMappings(), managedObject.getMappings());
            Assert.assertEquals(entity.getUninstallBundle(), managedObject.getUninstallBundle());

            if (entity.getEntityOwnershipDescriptors() == null || entity.getEntityOwnershipDescriptors().isEmpty()) {
                Assert.assertThat(
                        managedObject.getEntityOwnershipDescriptors(),
                        Matchers.anyOf(
                                Matchers.nullValue(Collection.class),
                                Matchers.<EntityOwnershipDescriptorMO>empty()
                        )
                );
            } else {
                Assert.assertThat(
                        managedObject.getEntityOwnershipDescriptors(),
                        Matchers.allOf(
                                Matchers.notNullValue(Collection.class),
                                Matchers.hasSize(entity.getEntityOwnershipDescriptors().size())
                        )
                );
                for (EntityOwnershipDescriptor eod : entity.getEntityOwnershipDescriptors()) {
                    boolean matches = false;
                    for (EntityOwnershipDescriptorMO eodMO : managedObject.getEntityOwnershipDescriptors()) {
                        if (eod.getEntityId().equals(eodMO.getEntityId()) && eod.getEntityType().equals(EntityType.valueOf(eodMO.getEntityType())) && eod.isReadOnly() == eodMO.isReadOnly() && eod.getVersionStamp() == eodMO.getVersionStamp()) {
                            matches = true;
                            break;
                        }
                    }
                    Assert.assertTrue(matches);
                }
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        try {
            final String[] idsOrderedByGoids = sortedGuidMap.values().toArray(new String[sortedGuidMap.size()]);
            final String[] sortedGuids = sortedGuidMap.keySet().toArray(new String[sortedGuidMap.size()]);

            return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(solutionKits, new Functions.Unary<String, SolutionKit>() {
                    @Override
                    public String call(SolutionKit solutionKit) {
                        return solutionKit.getId();
                    }
                }))
                .put("guid=" + URLEncoder.encode(solutionKits.get(0).getSolutionKitGuid(), HttpConstants.ENCODING_UTF8), Arrays.asList(solutionKits.get(0).getId()))
                .put("guid=" + URLEncoder.encode(sortedGuids[0], HttpConstants.ENCODING_UTF8) + "&guid=" + URLEncoder.encode(sortedGuids[1], HttpConstants.ENCODING_UTF8),
                        Arrays.asList(idsOrderedByGoids[0], idsOrderedByGoids[1]))
                .put("guid=" + URLEncoder.encode(sortedGuids[0], HttpConstants.ENCODING_UTF8) + "&guid=" + URLEncoder.encode(sortedGuids[1], HttpConstants.ENCODING_UTF8) + "&sort=guid&order=asc",
                        Arrays.asList(idsOrderedByGoids[0], idsOrderedByGoids[1]))
                .put("guid=" + URLEncoder.encode(sortedGuids[1], HttpConstants.ENCODING_UTF8) + "&guid=" + URLEncoder.encode(sortedGuids[0], HttpConstants.ENCODING_UTF8) + "&sort=guid&order=desc",
                        Arrays.asList(idsOrderedByGoids[1], idsOrderedByGoids[0]))
                .put("name=" + URLEncoder.encode(solutionKits.get(0).getName(), HttpConstants.ENCODING_UTF8), Arrays.asList(solutionKits.get(0).getId()))
                .put("name=" + URLEncoder.encode(solutionKits.get(0).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(solutionKits.get(1).getName(), HttpConstants.ENCODING_UTF8),
                        Arrays.asList(solutionKits.get(0).getId(), solutionKits.get(1).getId()))
                .put("name=" + URLEncoder.encode(solutionKits.get(1).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(solutionKits.get(2).getName(), HttpConstants.ENCODING_UTF8) + "&sort=name&order=asc",
                        Arrays.asList(solutionKits.get(1).getId(), solutionKits.get(2).getId()))
                .put("name=" + URLEncoder.encode(solutionKits.get(2).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(solutionKits.get(1).getName(), HttpConstants.ENCODING_UTF8) + "&sort=name&order=desc",
                        Arrays.asList(solutionKits.get(2).getId(), solutionKits.get(1).getId()))
                .put("name=unknownName", Collections.<String>emptyList())
                .put("sort=name&order=desc", Arrays.asList(solutionKits.get(3).getId(), solutionKits.get(0).getId(), solutionKits.get(2).getId(), solutionKits.get(1).getId()))
                .put("sort=name&order=asc", Arrays.asList(solutionKits.get(1).getId(), solutionKits.get(2).getId(), solutionKits.get(0).getId(), solutionKits.get(3).getId()))
                .put("sort=guid&order=desc", Arrays.asList(idsOrderedByGoids[3], idsOrderedByGoids[2], idsOrderedByGoids[1], idsOrderedByGoids[0]))
                .put("sort=guid&order=asc", Arrays.asList(idsOrderedByGoids[0], idsOrderedByGoids[1], idsOrderedByGoids[2], idsOrderedByGoids[3]))
                .map();
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
