package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.DependencyAnalysisMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManagerStub;
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
    private static final String basePath = "services/";

    @InjectMocks
    protected ServiceResourceFactory serviceResourceFactory;

    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";

    @Before
    public void before() throws Exception {
        super.before();

        FolderManagerStub folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        Folder testFolder = new Folder("Test Folder", null);
        folderManager.save(testFolder);

        serviceManager = applicationContext.getBean("serviceManager", ServiceManagerStub.class);
        publishedService.setName("Service1");
        publishedService.setRoutingUri("/test");
        publishedService.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", POLICY, false));
        publishedService.setFolder(testFolder);
        publishedService.setSoap(false);
        publishedService.getPolicy().setGuid(UUID.randomUUID().toString());
        serviceManager.save(publishedService);
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
    public void getServiceDependenciesTest() throws Exception {
        Response response = processRequest(basePath + publishedService.getId() + "/dependencies", HttpMethod.GET, null, "");
        logger.info(response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource( new StringReader(response.getBody()) );
        Reference<DependencyAnalysisMO> dependencyResultsMO = MarshallingUtils.unmarshal(Reference.class, source);

        dependencyResultsMO.toString();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        ServiceMO result = (ServiceMO) reference.getResource();

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
        Response response = processRequest(basePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertTrue(response.toString().indexOf("l7:Reference") > 0);

        PublishedService createdEntity = serviceManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Service name:", createdEntity.getName(), createObject.getServiceDetail().getName());
        assertEquals("Service folder:", createdEntity.getFolder().getId(), createObject.getServiceDetail().getFolderId());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceMO entityGot = (ServiceMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.getServiceDetail().setName("Updated Service Name");
        Response response = processRequest(basePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertTrue(response.toString().indexOf("l7:Reference") > 0);
        Assert.assertEquals(entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        PublishedService updatedEntity = serviceManager.findByPrimaryKey(new Goid(entityGot.getId()));

        assertEquals("Service identifier:", updatedEntity.getId(), entityGot.getId());
        assertEquals("Service name:", updatedEntity.getName(), entityGot.getServiceDetail().getName());
        assertEquals("Service folder:", updatedEntity.getFolder().getId(), entityGot.getServiceDetail().getFolderId());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(basePath + publishedService.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Response responseGet = processRequest(basePath + publishedService.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Status resource not found:", responseGet.getStatus(), HttpStatus.SC_NOT_FOUND);

    }
}
