package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.service.ServiceAliasManagerStub;
import com.l7tech.server.service.ServiceManagerStub;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class ServiceAliasRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(ServiceAliasRestServerGatewayManagementAssertionTest.class.getName());

    private static final PublishedService publishedService1 = new PublishedService();
    private static final PublishedService publishedService2 = new PublishedService();
    private static ServiceManagerStub serviceManager;
    private static FolderManagerStub folderManager;
    private static Folder rootFolder, folder1, folder2;

    private static PublishedServiceAlias serviceAlias;
    private static ServiceAliasManagerStub serviceAliasManager;
    private static final String serviceAliasBasePath = "serviceAliases/";

    private static final String POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<exp:Export Version=\"3.0\"\n" +
            "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
            "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <exp:References/>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "        </wsp:All>\n" +
            "    </wsp:Policy>\n" +
            "</exp:Export>\n";

    @InjectMocks
    protected ServiceAliasResourceFactory serviceAliasResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        rootFolder = new Folder("ROOT FOLDER", null);
        folderManager.save(rootFolder);

        folder1= new Folder("Folder 1", rootFolder);
        folder2= new Folder("Folder 2", rootFolder);
        folderManager.save(folder1);
        folderManager.save(folder2);

        publishedService1.setName("Service1");
        publishedService1.setRoutingUri("/test1");
        publishedService1.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", POLICY_XML, false));
        publishedService1.setFolder(rootFolder);
        publishedService1.setSoap(false);
        publishedService1.getPolicy().setGuid(UUID.randomUUID().toString());

        publishedService2.setName("Service2");
        publishedService2.setRoutingUri("/test2");
        publishedService2.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service2 Policy", POLICY_XML, false));
        publishedService2.setFolder(rootFolder);
        publishedService2.setSoap(false);
        publishedService2.getPolicy().setGuid(UUID.randomUUID().toString());

        serviceManager = applicationContext.getBean("serviceManager", ServiceManagerStub.class);
        serviceManager.save(publishedService1);
        serviceManager.save(publishedService2);

        serviceAliasManager = applicationContext.getBean("serviceAliasManager", ServiceAliasManagerStub.class);

        serviceAlias = new PublishedServiceAlias(publishedService1,folder1);
        serviceAliasManager.save(serviceAlias);

    }

    @After
    public void after() throws Exception {
        super.after();

        Collection<FolderHeader> folders = new ArrayList<>(folderManager.findAllHeaders());
        for (EntityHeader entity : folders) {
            folderManager.delete(entity.getGoid());
        }

        ArrayList<ServiceHeader> policies = new ArrayList<>(serviceManager.findAllHeaders());
        for (EntityHeader policy : policies) {
            serviceManager.delete(policy.getGoid());
        }

        Collection<AliasHeader<PublishedService>> aliases = new ArrayList<>(serviceAliasManager.findAllHeaders());
        for (EntityHeader alias : aliases) {
            serviceAliasManager.delete(alias.getGoid());
        }

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(serviceAliasBasePath + serviceAlias.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ServiceAliasMO result = (ServiceAliasMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        assertEquals("Service Alias identifier:", serviceAlias.getId(), result.getId());
        assertEquals("Service Alias ref service id:", serviceAlias.getEntityGoid().toString(), result.getServiceReference().getId());
    }

    @Test
    public void createEntityTest() throws Exception {

        ServiceAliasMO createObject = serviceAliasResourceFactory.asResource(serviceAlias);
        createObject.setId(null);
        createObject.setFolderId(folder2.getId());
        createObject.getServiceReference().setId(publishedService2.getId());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(serviceAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        PublishedServiceAlias createdEntity = serviceAliasManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Service Alias policy ref:", publishedService2.getId() , createdEntity.getEntityGoid().toString());
        assertEquals("Service Alias folder:", folder2.getId(), createdEntity.getFolder().getId());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        ServiceAliasMO createObject = serviceAliasResourceFactory.asResource(serviceAlias);
        createObject.setId(null);
        createObject.setFolderId(folder2.getId());
        createObject.getServiceReference().setId(publishedService2.getId());

        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(serviceAliasBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Service Alias goid:", goid.toString(), getFirstReferencedGoid(response));

        PublishedServiceAlias createdEntity = serviceAliasManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Service Alias policy ref:", publishedService2.getId() , createdEntity.getEntityGoid().toString());
        assertEquals("Service Alias folder:", folder2.getId(), createdEntity.getFolder().getId());
    }

    @Test
    public void createAliasSameFolderTest() throws Exception {

        ServiceAliasMO createObject = serviceAliasResourceFactory.asResource(serviceAlias);
        createObject.setId(null);
        createObject.setFolderId(folder1.getId());
        createObject.getServiceReference().setId(publishedService1.getId());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(serviceAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }


    @Test
    public void createAliasSameAsSourceFolderTest() throws Exception {

        ServiceAliasMO createObject = serviceAliasResourceFactory.asResource(serviceAlias);
        createObject.setId(null);
        createObject.setFolderId(rootFolder.getId());
        createObject.getServiceReference().setId(publishedService1.getId());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(serviceAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createPolicyNotFoundTest() throws Exception {

        ServiceAliasMO createObject = serviceAliasResourceFactory.asResource(serviceAlias);
        createObject.setId(null);
        createObject.setFolderId(folder1.getId());
        createObject.getServiceReference().setId("Bad policy id");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(serviceAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createFolderNotFoundTest() throws Exception {

        ServiceAliasMO createObject = serviceAliasResourceFactory.asResource(serviceAlias);
        createObject.setId(null);
        createObject.setFolderId("Bad Folder id");
        createObject.getServiceReference().setId(publishedService1.getId());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(serviceAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityChangeBackingPolicyTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(serviceAliasBasePath + serviceAlias.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceAliasMO entityGot = (ServiceAliasMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.getServiceReference().setId(publishedService2.getId());
        RestResponse response = processRequest(serviceAliasBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(serviceAliasBasePath + serviceAlias.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceAliasMO entityGot = (ServiceAliasMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.setFolderId(folder2.getId());
        RestResponse response = processRequest(serviceAliasBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Updated Service Alias goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        PublishedServiceAlias updatedEntity = serviceAliasManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Service Alias id:", serviceAlias.getId(), updatedEntity.getId());
        assertEquals("Service Alias folder:", folder2.getId(), updatedEntity.getFolder().getId());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(serviceAliasBasePath + serviceAlias.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(serviceAliasManager.findByPrimaryKey(serviceAlias.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(serviceAliasBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<ServiceAliasMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(serviceAliasManager.findAll().size(), item.getContent().size());
    }
}
