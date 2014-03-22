package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.globalresources.ResourceEntryManager;
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
public class DocumentRestEntityResourceTest extends RestEntityTests<ResourceEntry, ResourceDocumentMO> {
    private ResourceEntryManager resourceEntryManager;

    private List<ResourceEntry> defaultResourceEntries = new ArrayList<>();

    private List<ResourceEntry> createdResourceEntries = new ArrayList<>();

    private List<ResourceEntry> allResourceEntries = new ArrayList<>();

    @Before
    public void before() throws SaveException, FindException {
        resourceEntryManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("resourceEntryManager", ResourceEntryManager.class);

        defaultResourceEntries = new ArrayList<>(resourceEntryManager.findAll());


        ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setContent("MyResourceContent");
        resourceEntry.setType(ResourceType.XML_SCHEMA);
        resourceEntry.setUri("MyURI");

        resourceEntryManager.save(resourceEntry);

        createdResourceEntries.add(resourceEntry);

        allResourceEntries = new ArrayList<>(resourceEntryManager.findAll());

    }

    @After
    public void after() throws FindException, DeleteException {
        //delete all resources that are not the defaults
        Collection<ResourceEntry> all = resourceEntryManager.findAll();
        for(final ResourceEntry resourceEntry : all) {
            if(!Functions.exists(defaultResourceEntries, new Functions.Unary<Boolean, ResourceEntry>() {
                @Override
                public Boolean call(ResourceEntry rs) {
                    return rs.getId().equals(resourceEntry.getId());
                }
            })) {
                resourceEntryManager.delete(resourceEntry.getGoid());
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(defaultResourceEntries, new Functions.Unary<String, ResourceEntry>() {
            @Override
            public String call(ResourceEntry resourceEntry) {
                return resourceEntry.getId();
            }
        });
    }

    @Override
    public List<ResourceDocumentMO> getCreatableManagedObjects() {
        List<ResourceDocumentMO> resourceDocumentMOs = new ArrayList<>();
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        resourceDocumentMO.setId(getGoid().toString());
        resourceDocumentMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("publicIdentifier", "myDTDID")
                .map());
        Resource resource = ManagedObjectFactory.createResource();
        resource.setSourceUrl("sourceUri");
        resource.setType("dtd");
        resource.setContent("ResourceContentDTD");
        resourceDocumentMO.setResource(resource);
        resourceDocumentMOs.add(resourceDocumentMO);
        return resourceDocumentMOs;
    }

    @Override
    public List<ResourceDocumentMO> getUpdateableManagedObjects() {
        List<ResourceDocumentMO> resourceDocumentMOs = new ArrayList<>();
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        resourceDocumentMO.setId(createdResourceEntries.get(0).getId());
        resourceDocumentMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("publicIdentifier", "myDTDID")
                .map());
        Resource resource = ManagedObjectFactory.createResource();
        resource.setSourceUrl("Updated" + createdResourceEntries.get(0).getUri());
        resource.setType("dtd");
        resource.setContent("Updated" + createdResourceEntries.get(0).getContent());
        resourceDocumentMO.setResource(resource);
        resourceDocumentMOs.add(resourceDocumentMO);
        return resourceDocumentMOs;
    }

    @Override
    public Map<ResourceDocumentMO, Functions.BinaryVoid<ResourceDocumentMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<ResourceDocumentMO, Functions.BinaryVoid<ResourceDocumentMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        resourceDocumentMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("publicIdentifier", "myDTDID")
                .map());
        Resource resource = ManagedObjectFactory.createResource();
        resource.setSourceUrl("MyURI");
        resource.setType("dtd");
        resource.setContent("ResourceContentDTD");
        resourceDocumentMO.setResource(resource);

        builder.put(resourceDocumentMO, new Functions.BinaryVoid<ResourceDocumentMO, RestResponse>() {
            @Override
            public void call(ResourceDocumentMO resourceDocumentMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        resource = ManagedObjectFactory.createResource();
        resource.setSourceUrl("sourceUriDifferent");
        resource.setType("dtd");
        resource.setContent("ResourceContentDTD");
        resourceDocumentMO.setResource(resource);

        builder.put(resourceDocumentMO, new Functions.BinaryVoid<ResourceDocumentMO, RestResponse>() {
            @Override
            public void call(ResourceDocumentMO resourceDocumentMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<ResourceDocumentMO, Functions.BinaryVoid<ResourceDocumentMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<ResourceDocumentMO, Functions.BinaryVoid<ResourceDocumentMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
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
        return CollectionUtils.MapBuilder.<String, Functions.BinaryVoid<String, RestResponse>>builder()
                .put("type=badType", new Functions.BinaryVoid<String, RestResponse>() {
                    @Override
                    public void call(String s, RestResponse restResponse) {
                        Assert.assertEquals(400, restResponse.getStatus());
                    }
                })
                .map();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(createdResourceEntries, new Functions.Unary<String, ResourceEntry>() {
            @Override
            public String call(ResourceEntry resourceEntry) {
                return resourceEntry.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "resources";
    }

    @Override
    public EntityType getType() {
        return EntityType.RESOURCE_ENTRY;
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        ResourceEntry entity = resourceEntryManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getUri();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        ResourceEntry entity = resourceEntryManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, ResourceDocumentMO managedObject) throws FindException {
        ResourceEntry entity = resourceEntryManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getUri(), managedObject.getResource().getSourceUrl());
            switch (entity.getType()) {
                case XML_SCHEMA:
                    Assert.assertEquals("xmlschema", managedObject.getResource().getType());
                    break;
                case DTD:
                    Assert.assertEquals("dtd", managedObject.getResource().getType());
                    break;
                default:
                    Assert.assertTrue("Unknown resource type", false);
            }
            Assert.assertEquals(entity.getContent(), managedObject.getResource().getContent());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(allResourceEntries, new Functions.Unary<String, ResourceEntry>() {
                    @Override
                    public String call(ResourceEntry resourceEntry) {
                        return resourceEntry.getId();
                    }
                }))
                .put("uri=" + URLEncoder.encode(allResourceEntries.get(0).getUri()), Arrays.asList(allResourceEntries.get(0).getId()))
                .put("type=dtd", Functions.map(Functions.grep(allResourceEntries, new Functions.Unary<Boolean, ResourceEntry>() {
                    @Override
                    public Boolean call(ResourceEntry resourceEntry) {
                        return ResourceType.DTD.equals(resourceEntry.getType());
                    }
                }), new Functions.Unary<String, ResourceEntry>() {
                    @Override
                    public String call(ResourceEntry resourceEntry) {
                        return resourceEntry.getId();
                    }
                }))
                .put("type=xmlschema", Functions.map(Functions.grep(allResourceEntries, new Functions.Unary<Boolean, ResourceEntry>() {
                    @Override
                    public Boolean call(ResourceEntry resourceEntry) {
                        return ResourceType.XML_SCHEMA.equals(resourceEntry.getType());
                    }
                }), new Functions.Unary<String, ResourceEntry>() {
                    @Override
                    public String call(ResourceEntry resourceEntry) {
                        return resourceEntry.getId();
                    }
                }))
                .put("uri=banName", Collections.<String>emptyList())
                .map();
    }
}
