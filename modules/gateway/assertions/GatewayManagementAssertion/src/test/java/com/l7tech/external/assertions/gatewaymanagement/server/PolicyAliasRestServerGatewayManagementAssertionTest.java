package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.service.PolicyAliasManagerStub;
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
public class PolicyAliasRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(PolicyAliasRestServerGatewayManagementAssertionTest.class.getName());

    private static final Policy policy1 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy1", "", false);
    private static final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy2", "", false);
    private static PolicyManagerStub policyManager;
    private static FolderManagerStub folderManager;
    private static Folder rootFolder, folder1, folder2;

    private static PolicyAlias policyAlias;
    private static PolicyAliasManagerStub policyAliasManager;
    private static final String policyAliasBasePath = "policyAliases/";

    @InjectMocks
    protected PolicyAliasResourceFactory policyAliasResourceFactory;

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

        policyAliasManager = applicationContext.getBean("policyAliasManager", PolicyAliasManagerStub.class);

        policyAlias = new PolicyAlias(policy1,folder1);
        policyAliasManager.save(policyAlias);

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

        Collection<AliasHeader<Policy>> aliases = new ArrayList<>(policyAliasManager.findAllHeaders());
        for (EntityHeader alias : aliases) {
            policyAliasManager.delete(alias.getGoid());
        }

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(policyAliasBasePath + policyAlias.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        PolicyAliasMO result = ManagedObjectFactory.read(response.getBody(), PolicyAliasMO.class);

        assertEquals("Policy Alias identifier:", policyAlias.getId(), result.getId());
        assertEquals("Policy Alias ref policy id:", policyAlias.getEntityGoid().toString(), result.getPolicyReference().getId());
    }

    @Test
    public void createEntityTest() throws Exception {

        PolicyAliasMO createObject = policyAliasResourceFactory.asResource(policyAlias);
        createObject.setId(null);
        createObject.setFolderId(folder2.getId());
        createObject.getPolicyReference().setId(policy2.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(policyAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        PolicyAlias createdEntity = policyAliasManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Policy Alias policy ref:", policy2.getId() , createdEntity.getEntityGoid().toString());
        assertEquals("Policy Alias folder:", folder2.getId(), createdEntity.getFolder().getId());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        PolicyAliasMO createObject = policyAliasResourceFactory.asResource(policyAlias);
        createObject.setId(null);
        createObject.setFolderId(folder2.getId());
        createObject.getPolicyReference().setId(policy2.getId());

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(policyAliasBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Policy Alias goid:", goid.toString(), getFirstReferencedGoid(response));

        PolicyAlias createdEntity = policyAliasManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Policy Alias policy ref:", policy2.getId() , createdEntity.getEntityGoid().toString());
        assertEquals("Policy Alias folder:", folder2.getId(), createdEntity.getFolder().getId());
    }

    @Test
    public void createAliasSameFolderTest() throws Exception {

        PolicyAliasMO createObject = policyAliasResourceFactory.asResource(policyAlias);
        createObject.setId(null);
        createObject.setFolderId(folder1.getId());
        createObject.getPolicyReference().setId(policy1.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(policyAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createAliasSameAsSourceFolderTest() throws Exception {

        PolicyAliasMO createObject = policyAliasResourceFactory.asResource(policyAlias);
        createObject.setId(null);
        createObject.setFolderId(rootFolder.getId());
        createObject.getPolicyReference().setId(policy1.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(policyAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createPolicyNotFoundTest() throws Exception {

        PolicyAliasMO createObject = policyAliasResourceFactory.asResource(policyAlias);
        createObject.setId(null);
        createObject.setFolderId(folder1.getId());
        createObject.getPolicyReference().setId("Bad policy id");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(policyAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createFolderNotFoundTest() throws Exception {

        PolicyAliasMO createObject = policyAliasResourceFactory.asResource(policyAlias);
        createObject.setId(null);
        createObject.setFolderId("Bad Folder id");
        createObject.getPolicyReference().setId(policy1.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(policyAliasBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityChangeBackingPolicyTest() throws Exception {

        // get
        Response responseGet = processRequest(policyAliasBasePath + policyAlias.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        PolicyAliasMO entityGot = MarshallingUtils.unmarshal(PolicyAliasMO.class, source);

        // update
        entityGot.getPolicyReference().setId(policy2.getId());
        Response response = processRequest(policyAliasBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(policyAliasBasePath + policyAlias.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        PolicyAliasMO entityGot = MarshallingUtils.unmarshal(PolicyAliasMO.class, source);

        // update
        entityGot.setFolderId(folder2.getId());
        Response response = processRequest(policyAliasBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Updated Policy Alias goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        PolicyAlias updatedEntity = policyAliasManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Policy Alias id:", policyAlias.getId(), updatedEntity.getId());
        assertEquals("Policy Alias folder:", folder2.getId(), updatedEntity.getFolder().getId());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(policyAliasBasePath + policyAlias.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(policyAliasManager.findByPrimaryKey(policyAlias.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(policyAliasBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References references = MarshallingUtils.unmarshal(References.class, source);

        // check entity
        Assert.assertEquals(policyAliasManager.findAll().size(), references.getReferences().size());
    }
}
