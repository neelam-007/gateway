package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManagerStub;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 * This was created: 10/23/13 as 4:47 PM
 *
 * @author Victor Kazakov
 */
public class PublishedServiceRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(PublishedServiceRestServerGatewayManagementAssertionTest.class.getName());

    private static final PublishedService publishedService = new PublishedService();
    private static ServiceManagerStub serviceManager;
    private static PolicyVersionManager policyVersionManager;
    private static final String basePath = "services/";

    @InjectMocks
    protected ServiceResourceFactory serviceResourceFactory;

    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";
    private Folder testFolder;

    @Before
    public void before() throws Exception {
        super.before();

        FolderManagerStub folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        testFolder = new Folder("Test Folder", null);
        folderManager.save(testFolder);

        policyVersionManager = applicationContext.getBean("policyVersionManager", PolicyVersionManager.class);
        serviceManager = applicationContext.getBean("serviceManager", ServiceManagerStub.class);
        publishedService.setName("Service1");
        publishedService.setRoutingUri("/test");
        publishedService.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", POLICY, false));
        publishedService.setFolder(testFolder);
        publishedService.setSoap(false);
        publishedService.getPolicy().setGuid(UUID.randomUUID().toString());
        serviceManager.save(publishedService);
        policyVersionManager.checkpointPolicy(publishedService.getPolicy(), true, true);
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        ServiceMO result = (ServiceMO) item.getContent();

        assertEquals("Service identifier:", publishedService.getId(), result.getId());
        assertEquals("Service name:", publishedService.getName(), result.getServiceDetail().getName());
        assertEquals("Service folder:", publishedService.getFolder().getId(), result.getServiceDetail().getFolderId());
    }

    @Test
    public void createEntityTest() throws Exception {

        ServiceMO createObject = serviceResourceFactory.asResource(new ServiceResourceFactory.ServiceEntityBag(publishedService, Collections.<ServiceDocument>emptySet()));
        createObject.setId(null);
        createObject.getServiceDetail().setName("Create Service Name");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(basePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        PublishedService createdEntity = serviceManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Service name:", createdEntity.getName(), createObject.getServiceDetail().getName());
        assertEquals("Service folder:", createdEntity.getFolder().getId(), createObject.getServiceDetail().getFolderId());
    }

    @Test
    public void createEntityWithCommentTest() throws Exception {

        ServiceMO createObject = serviceResourceFactory.asResource(new ServiceResourceFactory.ServiceEntityBag(publishedService, Collections.<ServiceDocument>emptySet()));
        createObject.setId(null);
        createObject.getServiceDetail().setName("Create Service Name");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(basePath + "?versionComment=COMMENT!", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        Goid serviceGoid = new Goid(getFirstReferencedGoid(response));
        PublishedService createdEntity = serviceManager.findByPrimaryKey(serviceGoid);

        assertEquals("Service name:", createdEntity.getName(), createObject.getServiceDetail().getName());
        assertEquals("Service folder:", createdEntity.getFolder().getId(), createObject.getServiceDetail().getFolderId());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(createdEntity.getPolicy().getGoid(), 1);
        assertEquals("Comment:", "COMMENT!", version.getName());


    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceMO entityGot = (ServiceMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.getServiceDetail().setName("Updated Service Name");
        RestResponse response = processRequest(basePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        PublishedService updatedEntity = serviceManager.findByPrimaryKey(new Goid(entityGot.getId()));

        assertEquals("Service identifier:", updatedEntity.getId(), entityGot.getId());
        assertEquals("Service name:", updatedEntity.getName(), entityGot.getServiceDetail().getName());
        assertEquals("Service folder:", updatedEntity.getFolder().getId(), entityGot.getServiceDetail().getFolderId());
    }

    @Test
    public void updateEntityWithCommentTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceMO entityGot = (ServiceMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.getServiceDetail().setName("Updated Service Name");
        Resource policyResource = entityGot.getResourceSets().get(0).getResources().get(0);
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:AuditDetailAssertion>\n" +
                "                <L7p:Detail stringValue=\"Policy Fragment: temp\"/>\n" +
                "            </L7p:AuditDetailAssertion>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        RestResponse response = processRequest(basePath + entityGot.getId() + "?versionComment=MYCOMMENT", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        Goid serviceGoid = new Goid(entityGot.getId());
        PublishedService updatedEntity = serviceManager.findByPrimaryKey(serviceGoid);

        assertEquals("Service identifier:", updatedEntity.getId(), entityGot.getId());
        assertEquals("Service name:", updatedEntity.getName(), entityGot.getServiceDetail().getName());
        assertEquals("Service folder:", updatedEntity.getFolder().getId(), entityGot.getServiceDetail().getFolderId());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(updatedEntity.getPolicy().getGoid(), 2);
        Assert.assertNotNull(version);
        assertEquals("Comment:", "MYCOMMENT", version.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(basePath + publishedService.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        RestResponse responseGet = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Status resource not found:", responseGet.getStatus(), HttpStatus.SC_NOT_FOUND);

    }

    @Test
    public void getServiceWithPropertiesTest() throws Exception {
        PublishedService publishedServiceWithProperties = new PublishedService();
        publishedServiceWithProperties.setName("ServiceWithProperties");
        publishedServiceWithProperties.setRoutingUri("/test");
        publishedServiceWithProperties.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", POLICY, false));
        publishedServiceWithProperties.setFolder(testFolder);
        publishedServiceWithProperties.setSoap(false);
        publishedServiceWithProperties.getPolicy().setGuid(UUID.randomUUID().toString());
        publishedServiceWithProperties.putProperty("myPropertyKey", "myPropertyValue");
        serviceManager.save(publishedServiceWithProperties);
        policyVersionManager.checkpointPolicy(publishedServiceWithProperties.getPolicy(), true, true);

        try {

            RestResponse response = processRequest(basePath + publishedServiceWithProperties.getId(), HttpMethod.GET, null, "");
            logger.info(response.toString());
            Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);
            ServiceMO result = (ServiceMO) item.getContent();

            assertEquals("Service identifier:", publishedServiceWithProperties.getId(), result.getId());
            assertEquals("Service name:", publishedServiceWithProperties.getName(), result.getServiceDetail().getName());
            assertEquals("Service folder:", publishedServiceWithProperties.getFolder().getId(), result.getServiceDetail().getFolderId());
            assertEquals("Service myPropertyKey property:", "myPropertyValue", result.getServiceDetail().getProperties().get("property.myPropertyKey"));
        } finally {
            serviceManager.delete(publishedServiceWithProperties);
        }
    }

    @Test
    public void createEntityWithPropertyTest() throws Exception {

        ServiceMO createObject = serviceResourceFactory.asResource(new ServiceResourceFactory.ServiceEntityBag(publishedService, Collections.<ServiceDocument>emptySet()));
        createObject.setId(null);
        createObject.getServiceDetail().setName("Create Service Name");
        createObject.getServiceDetail().getProperties().put("property.myCreatedProperty", "myCreatedPropertyValue");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(basePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        PublishedService createdEntity = serviceManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Service name:", createObject.getServiceDetail().getName(), createdEntity.getName());
        assertEquals("Service folder:", createObject.getServiceDetail().getFolderId(), createdEntity.getFolder().getId());
        assertEquals("Service folder:", "myCreatedPropertyValue", createdEntity.getProperty("myCreatedProperty"));
    }

    @Test
    public void updateEntityPropertiesTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceMO entityGot = (ServiceMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.getServiceDetail().setName("Updated Service Name");
        entityGot.getServiceDetail().getProperties().put("property.myUpdatedProperty", "myUpdatedPropertyValue");
        RestResponse response = processRequest(basePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        PublishedService updatedEntity = serviceManager.findByPrimaryKey(new Goid(entityGot.getId()));

        assertEquals("Service identifier:", updatedEntity.getId(), entityGot.getId());
        assertEquals("Service name:", updatedEntity.getName(), entityGot.getServiceDetail().getName());
        assertEquals("Service folder:", updatedEntity.getFolder().getId(), entityGot.getServiceDetail().getFolderId());
        assertEquals("Service folder:", "myUpdatedPropertyValue", updatedEntity.getProperty("myUpdatedProperty"));
    }
}
