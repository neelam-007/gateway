package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.policy.PolicyVersionManager;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 * This was created: 10/23/13 as 4:47 PM
 *
 * @author Victor Kazakov
 */
public class PolicyRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(PolicyRestServerGatewayManagementAssertionTest.class.getName());

    private static final Policy policy1 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy1", "", false);
    private static final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy2", "", false);
    private static final Policy policy3 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy3", "", false);
    private static final Policy policyIdProvider = new Policy(PolicyType.IDENTITY_PROVIDER_POLICY, "Policy ID Provider", "", false);
    private static PolicyManagerStub policyManager;
    private static PolicyVersionManager policyVersionManager;
    private static final String comment = "CreateComment";
    private static final String policyBasePath = "policies/";
    private Folder rootFolder;
    private FolderManagerStub folderManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();
        folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        rootFolder = new Folder("ROOT FOLDER", null);
        folderManager.save(rootFolder);

        policyVersionManager = applicationContext.getBean("policyVersionManager", PolicyVersionManager.class);

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
        policy3.setFolder(rootFolder);
        policy3.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policy3.setGuid(UUID.randomUUID().toString());
        policyIdProvider.setFolder(rootFolder);
        policyIdProvider.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policyIdProvider.setGuid(UUID.randomUUID().toString());
        policyManager = applicationContext.getBean("policyManager", PolicyManagerStub.class);
        policyManager.save(policy1);
        policyManager.save(policy2);
        policyManager.save(policy3);
        policyManager.save(policyIdProvider);
        policyVersionManager.checkpointPolicy(policy1,true,true);
        policyVersionManager.checkpointPolicy(policy2,true,true);
        policyVersionManager.checkpointPolicy(policy3,true,comment,true);
        policyVersionManager.checkpointPolicy(policyIdProvider,true,comment,true);
    }

    @After
    public void after() throws Exception {
        super.after();

        Collection<FolderHeader> folders = new ArrayList<>(folderManager.findAllHeaders());
        for (EntityHeader entity : folders) {
            folderManager.delete(entity.getGoid());
        }

        ArrayList<PolicyHeader> policies = new ArrayList<>(policyManager.findAllHeaders());
        for(EntityHeader policy : policies){
            policyManager.delete(policy.getGoid());
        }

        ArrayList<EntityHeader> versions = new ArrayList<>(policyVersionManager.findAllHeaders());
        for(EntityHeader version : versions){
            policyVersionManager.delete(version.getGoid());
        }
    }

    @Test
    public void getPolicyTest() throws Exception {
        RestResponse response = processRequest(policyBasePath + policy1.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        PolicyMO policyReturned = (PolicyMO) item.getContent();

        Assert.assertEquals(policy1.getId(), policyReturned.getId());
        Assert.assertEquals(policy1.getName(), policyReturned.getPolicyDetail().getName());
    }

    @Test
    public void getPolicyNotExistsTest() throws Exception {
        RestResponse response = processRequest(policyBasePath + new Goid(123, 456), HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(404, response.getStatus());
        Assert.assertEquals("Resource not found {id=" + new Goid(123, 456) + "}", getErrorMessage(response));
    }

    @Test
    public void createPolicyTest() throws Exception {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("My New Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(rootFolder.getId());
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
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
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        String policyMOString = writeMOToString(policyMO);

        RestResponse response = processRequest(policyBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Policy policySaved = policyManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        Assert.assertNotNull(policySaved);
        Assert.assertEquals(policyDetail.getName(), policySaved.getName());
        Assert.assertEquals(AuditDetailAssertion.class, ((AllAssertion)policySaved.getAssertion()).getChildren().get(0).getClass());
    }

    @Test
    public void createPolicyWithCommentTest() throws Exception {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("My New Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(rootFolder.getId());
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
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
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        String policyMOString = writeMOToString(policyMO);

        RestResponse response = processRequest(policyBasePath +"?versionComment="+comment, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Goid policyGoid = new Goid(getFirstReferencedGoid(response));
        Policy policySaved = policyManager.findByPrimaryKey(policyGoid);
        Assert.assertNotNull(policySaved);
        Assert.assertEquals(policyDetail.getName(), policySaved.getName());
        Assert.assertEquals(AuditDetailAssertion.class, ((AllAssertion)policySaved.getAssertion()).getChildren().get(0).getClass());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(policyGoid, 1);
        Assert.assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
    }

    @Test
    public void createPolicyWithIDTest() throws Exception {
        Goid id = new Goid(124124124, 1);

        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("My New Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(rootFolder.getId());
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
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
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        String policyMOString = writeMOToString(policyMO);

        RestResponse response = processRequest(policyBasePath + id, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Policy policySaved = policyManager.findByPrimaryKey(id);
        Assert.assertNotNull(policySaved);
        Assert.assertEquals(policyDetail.getName(), policySaved.getName());
        Assert.assertEquals(AuditDetailAssertion.class, ((AllAssertion)policySaved.getAssertion()).getChildren().get(0).getClass());

        final Goid goidReturned = new Goid(getFirstReferencedGoid(response));
        Assert.assertEquals(id, goidReturned);
    }

    @Test
    public void createPolicyWithIDAndCommentTest() throws Exception {
        Goid id = new Goid(124124124, 1);

        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("My New Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(rootFolder.getId());
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
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
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        String policyMOString = writeMOToString(policyMO);

        RestResponse response = processRequest(policyBasePath + id  +"?versionComment="+comment, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Policy policySaved = policyManager.findByPrimaryKey(id);
        Assert.assertNotNull(policySaved);
        Assert.assertEquals(policyDetail.getName(), policySaved.getName());
        Assert.assertEquals(AuditDetailAssertion.class, ((AllAssertion)policySaved.getAssertion()).getChildren().get(0).getClass());

        final Goid goidReturned = new Goid(getFirstReferencedGoid(response));
        Assert.assertEquals(id, goidReturned);

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(goidReturned, 1);
        Assert.assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
    }

    @Test
    public void deletePolicyTest() throws Exception {
        RestResponse response = processRequest(policyBasePath + policy3.getId(), HttpMethod.DELETE, null, "");
        logger.info(response.toString());

        Assert.assertNull(policyManager.findByPrimaryKey(policy3.getGoid()));
    }

    @Test
    public void updatePolicyTest() throws Exception {
        RestResponse response = processRequest(policyBasePath + policy2.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyMO policyReturned = (PolicyMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        policyReturned.getPolicyDetail().setName("Policy Updated");
        Resource policyResource = policyReturned.getResourceSets().get(0).getResources().get(0);
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
        String policyMOString = writeMOToString(policyReturned);

        logger.info(policyMOString.toString());
        response = processRequest(policyBasePath + policy2.getId() + "?active=false&versionComment="+comment, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Policy policySaved = policyManager.findByPrimaryKey(policy2.getGoid());
        Assert.assertEquals(policySaved.getName(), "Policy Updated");

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(policy2.getGoid(), 3);
        Assert.assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
        assertEquals("Active:", false, version.isActive());

        PolicyVersion oldVersion = policyVersionManager.findPolicyVersionForPolicy(policy2.getGoid(), 1);
        Assert.assertNotNull(oldVersion);
        assertEquals("Active:", true, oldVersion.isActive());
    }

    @Test
    public void listPolicies() throws Exception {
        RestResponse response = processRequest(policyBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource( new StringReader(response.getBody()) );

        ItemsList<PolicyMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(4, item.getContent().size());
    }
}
