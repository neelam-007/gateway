package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyRestEntityResourceTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(PolicyRestEntityResourceTest.class.getName());

    private PolicyVersionManager policyVersionManager;
    private static final Policy policy1 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy1", "", false);
    private static final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy2", "", false);
    private static PolicyManager policyManager;
    private static List<Goid> policyGoids = new ArrayList<Goid>();
    private Folder rootFolder;

    private static final String comment = "MyComment1";
    private static final String policyBasePath = "policies/";

    @Before
    public void before() throws Exception {
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);

        FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

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
        policyManager.save(policy1);
        policyVersionManager.checkpointPolicy(policy1,true,comment,true);

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
        policyManager.save(policy2);
        policyVersionManager.checkpointPolicy(policy2,true,comment,true);

    }

    @After
    public void after() throws FindException, DeleteException {
        policyManager.delete(policy1);
        policyManager.delete(policy2);

        for(Goid id: policyGoids){
            policyManager.delete(id);
        }
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
        policyGoids.add(policyGoid);
        Policy policySaved = policyManager.findByPrimaryKey(policyGoid);
        assertNotNull(policySaved);
        assertEquals(policyDetail.getName(), policySaved.getName());
        assertEquals(AuditDetailAssertion.class, ((AllAssertion) policySaved.getAssertion()).getChildren().get(0).getClass());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(policyGoid, 1);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());

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

        policyGoids.add(id);
        Policy policySaved = policyManager.findByPrimaryKey(id);
        assertNotNull(policySaved);
        assertEquals(policyDetail.getName(), policySaved.getName());
        assertEquals(AuditDetailAssertion.class, ((AllAssertion) policySaved.getAssertion()).getChildren().get(0).getClass());

        final Goid goidReturned = new Goid(getFirstReferencedGoid(response));
        assertEquals(id, goidReturned);

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(goidReturned, 1);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
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
        assertEquals("Policy Updated",policySaved.getName());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(policy2.getGoid(), 2);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
        assertEquals("Active:", false, version.isActive());

        PolicyVersion oldVersion = policyVersionManager.findPolicyVersionForPolicy(policy2.getGoid(), 1);
        assertNotNull(oldVersion);
        assertEquals("Active:", true, oldVersion.isActive());
    }


    protected String writeMOToString(ManagedObject mo) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ManagedObjectFactory.write(mo, bout);
        return bout.toString();
    }

    protected String getFirstReferencedGoid(RestResponse response) throws IOException {
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        List<Link> links = item.getLinks();

        for(Link link : links){
            if("self".equals(link.getRel())){
                return link.getUri().substring(link.getUri().lastIndexOf('/') + 1);
            }
        }
        return null;
    }
}
