package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.policy.MockEncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManagerStub;
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
public class EncapsulatedAssertionRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionRestServerGatewayManagementAssertionTest.class.getName());

    private static final Policy policy1 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy1", "", false);
    private static final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy2", "", false);
    private static PolicyManagerStub policyManager;
    private FolderManagerStub folderManager;

    private static final EncapsulatedAssertionConfig encassConfig = new EncapsulatedAssertionConfig();
    private static MockEncapsulatedAssertionConfigManager encassManager;
    private static final String encassBasePath = "encapsulatedAssertions/";

    @InjectMocks
    protected EncapsulatedAssertionResourceFactory encassConfigResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        Folder rootFolder = new Folder("ROOT FOLDER", null);
        folderManager.save(rootFolder);

        policy1.setFolder(rootFolder);
        policy1.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policy1.setGuid(UUID.randomUUID().toString());
        policy2.setFolder(rootFolder);
        policy2.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policy2.setGuid(UUID.randomUUID().toString());
        policyManager = applicationContext.getBean("policyManager", PolicyManagerStub.class);
        policyManager.save(policy1);
        policyManager.save(policy2);

        encassManager = applicationContext.getBean("encapsulatedAssertionConfigManager", MockEncapsulatedAssertionConfigManager.class);

        encassConfig.setName("Encass name");
        encassConfig.setGuid("Encass Guid");
        encassConfig.setPolicy(policy1);

        encassManager.save(encassConfig);

    }

    @After
    public void after() throws Exception {
        super.after();

        Collection<FolderHeader> folders = new ArrayList<>(folderManager.findAllHeaders());
        for (EntityHeader entity : folders) {
            folderManager.delete(entity.getGoid());
        }

        ArrayList<PolicyHeader> policies = new ArrayList<>(policyManager.findAllHeaders());
        for (EntityHeader policy : policies) {
            policyManager.delete(policy.getGoid());
        }

        Collection<GuidEntityHeader> entities = new ArrayList<>(encassManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            encassManager.delete(entity.getGoid());
        }

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(encassBasePath + encassConfig.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        EncapsulatedAssertionMO result = (EncapsulatedAssertionMO) reference.getResource();

        assertEquals("Encapsulated Assertion identifier:", encassConfig.getId(), result.getId());
        assertEquals("Encapsulated Assertion name:", encassConfig.getName(), result.getName());
        assertEquals("Encapsulated Assertion guid:", encassConfig.getGuid(), result.getGuid());
        assertEquals("Encapsulated Assertion value:", encassConfig.getPolicy().getId(), result.getPolicyReference().getId());
    }

    @Test
    public void createEntityTest() throws Exception {

        EncapsulatedAssertionMO createObject = encassConfigResourceFactory.asResource(encassConfig);
        createObject.setId(null);
        createObject.setName("New encass name");
        createObject.getPolicyReference().setId(policy2.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(encassBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        EncapsulatedAssertionConfig createdEntity = encassManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Encapsulated Assertion policy ref:", createObject.getPolicyReference().getId(), createdEntity.getPolicy().getId());
        assertEquals("Encapsulated Assertion name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        EncapsulatedAssertionMO createObject = encassConfigResourceFactory.asResource(encassConfig);
        createObject.setId(null);
        createObject.setName("New encass name");
        createObject.getPolicyReference().setId(policy2.getId());

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(encassBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Encapsulated Assertion goid:", goid.toString(), getFirstReferencedGoid(response));

        EncapsulatedAssertionConfig createdEntity = encassManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Encapsulated Assertion policy ref:", createObject.getPolicyReference().getId(), createdEntity.getPolicy().getId());
        assertEquals("Encapsulated Assertion name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createInvalidPolicyIdTest() throws Exception {

        EncapsulatedAssertionMO createObject = encassConfigResourceFactory.asResource(encassConfig);
        createObject.setId(null);
        createObject.setName("New encass name");
        createObject.getPolicyReference().setId("bad id");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(encassBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createPolicyNotFoundTest() throws Exception {

        EncapsulatedAssertionMO createObject = encassConfigResourceFactory.asResource(encassConfig);
        createObject.setId(null);
        createObject.setName("New encass name");
        createObject.getPolicyReference().setId(new Goid(6363, 63463).toString());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(encassBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityChangeBackingPolicyTest() throws Exception {

        // get
        Response responseGet = processRequest(encassBasePath + encassConfig.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        EncapsulatedAssertionMO entityGot = (EncapsulatedAssertionMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.getPolicyReference().setId(policy2.getId());
        Response response = processRequest(encassBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityChangeGuidTest() throws Exception {

        // get
        Response responseGet = processRequest(encassBasePath + encassConfig.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        EncapsulatedAssertionMO entityGot = (EncapsulatedAssertionMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setGuid("Updated GUID");
        Response response = processRequest(encassBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(encassBasePath + encassConfig.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        EncapsulatedAssertionMO entityGot = (EncapsulatedAssertionMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setName("Update Encass Name");
        Response response = processRequest(encassBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Updated Encapsulated Assertion goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        EncapsulatedAssertionConfig updatedEntity = encassManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Encapsulated Assertion id:", encassConfig.getId(), updatedEntity.getId());
        assertEquals("Encapsulated Assertion name:", encassConfig.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(encassBasePath + encassConfig.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(encassManager.findByPrimaryKey(encassConfig.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(encassBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(encassManager.findAll().size(), reference.getResource().getReferences().size());
    }
}
