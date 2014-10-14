package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.SampleMessageManager;
import com.l7tech.server.service.ServiceManager;
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
public class SampleMessageRestEntityResourceTest extends RestEntityTests<SampleMessage, SampleMessageMO> {
    private SampleMessageManager sampleMessageManager;
    private List<SampleMessage> messages = new ArrayList<>();
    private List<PublishedService> services = new ArrayList<>();
    private ServiceManager serviceManager;
    private PolicyVersionManager policyVersionManager;
    private Folder rootFolder;

    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";


    @Before
    public void before() throws Exception {
        sampleMessageManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("sampleMessageManager", SampleMessageManager.class);
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);

        FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        //create the published service
        for (int i = 0; i < 2; i++) {
            PublishedService service = new PublishedService();
            service.setName("Service" + i);
            service.setRoutingUri("/test" + i);
            service.getPolicy().setXml(POLICY);
            service.setFolder(rootFolder);
            service.setSoap(false);
            service.getPolicy().setGuid(UUID.randomUUID().toString());
            serviceManager.save(service);
            policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
            services.add(service);
        }

        //Create the sample messages

        SampleMessage sampleMessage = new SampleMessage(services.get(0).getGoid(), "message 1", "getAAA", "<xml/>");
        sampleMessageManager.save(sampleMessage);
        messages.add(sampleMessage);

        sampleMessage = new SampleMessage(services.get(0).getGoid(), "message 2", "getBBB", "<xml/>");
        sampleMessageManager.save(sampleMessage);
        messages.add(sampleMessage);

        sampleMessage = new SampleMessage(services.get(1).getGoid(), "message 3", "getCCC", "<xml/>");
        sampleMessageManager.save(sampleMessage);
        messages.add(sampleMessage);


    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<SampleMessage> all = sampleMessageManager.findAll();
        for (SampleMessage sampleMessage : all) {
            sampleMessageManager.delete(sampleMessage.getGoid());
        }

        for (PublishedService publishedService : services) {
            serviceManager.delete(publishedService.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(messages, new Functions.Unary<String, SampleMessage>() {
            @Override
            public String call(SampleMessage message) {
                return message.getId();
            }
        });
    }

    @Override
    public List<SampleMessageMO> getCreatableManagedObjects() {
        List<SampleMessageMO> messages = new ArrayList<>();

        SampleMessageMO sampleMessage = ManagedObjectFactory.createSampleMessageMO();
        sampleMessage.setId(getGoid().toString());
        sampleMessage.setName("Test Sample Message created");
        sampleMessage.setOperation("operationTest");
        sampleMessage.setServiceId(services.get(0).getId());
        sampleMessage.setXml("<stuff/>");

        messages.add(sampleMessage);

        return messages;
    }

    @Override
    public List<SampleMessageMO> getUpdateableManagedObjects() {
        List<SampleMessageMO> sampleMessages = new ArrayList<>();

        SampleMessage message = this.messages.get(0);
        SampleMessageMO sampleMessage = ManagedObjectFactory.createSampleMessageMO();
        sampleMessage.setId(message.getId());
        sampleMessage.setVersion(message.getVersion());
        sampleMessage.setName(message.getName() + " Updated");
        sampleMessage.setOperation(message.getOperationName());
        sampleMessage.setServiceId(message.getServiceGoid().toString());
        sampleMessage.setXml(message.getXml());
        sampleMessages.add(sampleMessage);

        //update twice
        sampleMessage = ManagedObjectFactory.createSampleMessageMO();
        sampleMessage.setId(message.getId());
        sampleMessage.setVersion(message.getVersion());
        sampleMessage.setName(message.getName() + " Updated");
        sampleMessage.setOperation(message.getOperationName());
        sampleMessage.setServiceId(message.getServiceGoid().toString());
        sampleMessage.setXml("<updated/>");
        sampleMessages.add(sampleMessage);

        return sampleMessages;
    }

    @Override
    public Map<SampleMessageMO, Functions.BinaryVoid<SampleMessageMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<SampleMessageMO, Functions.BinaryVoid<SampleMessageMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        SampleMessageMO sampleMessage = ManagedObjectFactory.createSampleMessageMO();
        sampleMessage.setName("UnCreatable Sample Message");
        sampleMessage.setOperation("operation");
        sampleMessage.setServiceId(getGoid().toString());
        sampleMessage.setXml("<xml/>");

        builder.put(sampleMessage, new Functions.BinaryVoid<SampleMessageMO, RestResponse>() {
            @Override
            public void call(SampleMessageMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<SampleMessageMO, Functions.BinaryVoid<SampleMessageMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<SampleMessageMO, Functions.BinaryVoid<SampleMessageMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        SampleMessageMO sampleMessage = ManagedObjectFactory.createSampleMessageMO();
        sampleMessage.setId(messages.get(0).getId());
        sampleMessage.setVersion(messages.get(0).getVersion());
        sampleMessage.setName(messages.get(0).getName());
        sampleMessage.setOperation("operation");
        sampleMessage.setServiceId(getGoid().toString());
        sampleMessage.setXml("<xml/>");

        builder.put(sampleMessage, new Functions.BinaryVoid<SampleMessageMO, RestResponse>() {
            @Override
            public void call(SampleMessageMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf"+getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
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
        return Functions.map(messages, new Functions.Unary<String, SampleMessage>() {
            @Override
            public String call(SampleMessage message) {
                return message.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "sampleMessages";
    }

    @Override
    public String getType() {
        return EntityType.SAMPLE_MESSAGE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        SampleMessage entity = sampleMessageManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        SampleMessage entity = sampleMessageManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, SampleMessageMO managedObject) throws FindException {
        SampleMessage entity = sampleMessageManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getOperationName(), managedObject.getOperation());
            Assert.assertEquals(entity.getServiceGoid().toString(), managedObject.getServiceId());
            Assert.assertEquals(entity.getXml(), managedObject.getXml());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(messages, new Functions.Unary<String, SampleMessage>() {
                    @Override
                    public String call(SampleMessage message) {
                        return message.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(messages.get(0).getName()), Arrays.asList(messages.get(0).getId()))
                .put("name=" + URLEncoder.encode(messages.get(0).getName()) + "&name=" + URLEncoder.encode(messages.get(1).getName()), Functions.map(messages.subList(0, 2), new Functions.Unary<String, SampleMessage>() {
                    @Override
                    public String call(SampleMessage message) {
                        return message.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("operationName=" + URLEncoder.encode(messages.get(0).getOperationName()), Arrays.asList(messages.get(0).getId()))
                .put("operationName=" + URLEncoder.encode(messages.get(0).getOperationName()) + "&operationName=" + URLEncoder.encode(messages.get(1).getOperationName()), Functions.map(messages.subList(0, 2), new Functions.Unary<String, SampleMessage>() {
                    @Override
                    public String call(SampleMessage message) {
                        return message.getId();
                    }
                }))
                .put("service.id="+ services.get(0).getGoid(), Arrays.asList(messages.get(0).getId(),messages.get(1).getId()))
                .put("name=" + URLEncoder.encode(messages.get(0).getName()) + "&name=" + URLEncoder.encode(messages.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(messages.get(1).getId(), messages.get(0).getId()))
                .map();
    }
}
