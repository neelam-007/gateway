package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyPolicyTest extends DependencyTestBase {
    private static final Logger logger = Logger.getLogger(DependencyPolicyTest.class.getName());

    private final Policy policy = new Policy(PolicyType.INTERNAL, "Policy", "", false);
    private final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy 2", "", false);
    private final SecurityZone securityZone = new SecurityZone();
    private final SecurityZone securityZone1 = new SecurityZone();
    private PolicyAlias policyAlias;
    private Folder folder;
    private Folder folder2;
    private PolicyManager policyManager;
    private PolicyAliasManager policyAliasManager;
    private SecurityZoneManager securityZoneManager;
    private PolicyCache policyCache;


    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policyCache = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyCache", PolicyCache.class);
        policyAliasManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyAliasManager", PolicyAliasManager.class);

//        rootFolder
//            - folder
//                - policy ( includes policy 2, in securityZone)
//                - folder2
//                    - policy2  ( in securityZone1)
//                    - policyAlias ( in securityZone1)

        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.SERVICE));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        //create security zone
        securityZone1.setName("Test alias security zone");
        securityZone1.setPermittedEntityTypes(CollectionUtils.set(EntityType.SERVICE_ALIAS));
        securityZone1.setDescription("stuff");
        securityZoneManager.save(securityZone1);

        // create folder
        folder = new Folder("Test Folder", rootFolder);
        folderManager.save(folder);

        folder2 = new Folder("Child Folder", folder);
        folderManager.save(folder2);

        // create include policy
        final String policyXml2 =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        policy2.setXml(policyXml2);
        policy2.setGuid(UUID.randomUUID().toString());
        policy2.setFolder(folder2);
        policy2.setSecurityZone(securityZone1);
        policyManager.save(policy2);
        policyGoids.add(policy2.getGoid());
        policyVersionManager.checkpointPolicy(policy2, true, true);

        // create policy
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "        <L7p:Include>\n" +
                        "            <L7p:PolicyGuid stringValue=\"" + policy2.getGuid() + "\"/>\n" +
                        "            <L7p:PolicyName stringValue=\"" + policy2.getName() + "\"/>\n" +
                        "        </L7p:Include>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        policy.setXml(policyXml);
        policy.setGuid(UUID.randomUUID().toString());
        policy.setFolder(folder);
        policy.setSecurityZone(securityZone);
        policyManager.save(policy);
        policyGoids.add(policy.getGoid());
        policyVersionManager.checkpointPolicy(policy, true, true);

        // create service alias
        policyAlias = new PolicyAlias(policy, folder2);
        policyAlias.setSecurityZone(securityZone1);
        policyAliasManager.save(policyAlias);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        policyAliasManager.delete(policyAlias.getGoid());
        policyManager.delete(policy.getGoid());
        //todo: find a better solution sleep for 1 second to let policy cache remove the policy.
        Thread.sleep(1000);
        policyManager.delete(policy2.getGoid());
        super.after();
        folderManager.delete(folder2.getGoid());
        folderManager.delete(folder.getGoid());
        securityZoneManager.delete(securityZone.getGoid());
        securityZoneManager.delete(securityZone1.getGoid());
    }

    @Test
    public void policyVersionTest() throws Exception {

        //create policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Source Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        String comment = "0123456789";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest( "policies/" + "?versionComment="+comment+"&active=false", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),  XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(201, response.getStatus());
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = processRequest("policies/" + policyCreated.getId() + "/versions/", HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
        ItemsList versions = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals(1,versions.getContent().size());
        Item<PolicyVersionMO> version = (Item)versions.getContent().get(0);
        assertEquals(comment, version.getContent().getComment());

        // clean up
        response = getDatabaseBasedRestManagementEnvironment().processRequest( "policies/"+ policyCreated.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    @Test
    public void createServiceWithCommentTest() throws Exception {

        final String serviceMOxml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<l7:Service xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:ServiceDetail folderId=\"0000000000000000ffffffffffffec76\">\n" +
                        "        <l7:Name>blah</l7:Name>\n" +
                        "        <l7:Enabled>true</l7:Enabled>\n" +
                        "        <l7:ServiceMappings>\n" +
                        "            <l7:HttpMapping>\n" +
                        "                <l7:UrlPattern>/blah</l7:UrlPattern>\n" +
                        "                <l7:Verbs>\n" +
                        "                    <l7:Verb>GET</l7:Verb>\n" +
                        "                </l7:Verbs>\n" +
                        "            </l7:HttpMapping>\n" +
                        "        </l7:ServiceMappings>\n" +
                        "        <l7:Properties>\n" +
                        "            <l7:Property key=\"policyRevision\">\n" +
                        "                <l7:LongValue>23</l7:LongValue>\n" +
                        "            </l7:Property>\n" +
                        "            <l7:Property key=\"wssProcessingEnabled\">\n" +
                        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                        "            </l7:Property>\n" +
                        "            <l7:Property key=\"soap\">\n" +
                        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                        "            </l7:Property>\n" +
                        "            <l7:Property key=\"internal\">\n" +
                        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                        "            </l7:Property>\n" +
                        "        </l7:Properties>\n" +
                        "    </l7:ServiceDetail>\n" +
                        "    <l7:Resources>\n" +
                        "        <l7:ResourceSet tag=\"policy\">\n" +
                        "            <l7:Resource type=\"policy\" version=\"24\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                        "        &lt;L7p:AuditDetailAssertion&gt;\n" +
                        "            &lt;L7p:Detail stringValue=&quot;created!&quot;/&gt;\n" +
                        "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
                        "    &lt;/wsp:All&gt;\n" +
                        "&lt;/wsp:Policy&gt;\n" +
                        "            </l7:Resource>\n" +
                        "        </l7:ResourceSet>\n" +
                        "    </l7:Resources>\n" +
                        "</l7:Service>";

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest( "services/" + "?versionComment=newComment&active=false", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),  serviceMOxml);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(201, response.getStatus());
        Item<ServiceMO> serviceCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = processRequest("services/" + serviceCreated.getId() + "/versions/", HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
        ItemsList versions = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals(1,versions.getContent().size());
        Item<PolicyVersionMO> version = (Item)versions.getContent().get(0);
        assertEquals("newComment", version.getContent().getComment());

        // clean up
        response = getDatabaseBasedRestManagementEnvironment().processRequest( "services/"+ serviceCreated.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    @Test
    public void policyTest() throws Exception {

        TestDependency("policies/", policy.getId(), new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(3, dependencyAnalysisMO.getDependencies().size());

                DependencyMO policydep = dependencyAnalysisMO.getSearchObjectItem();
                assertEquals(EntityType.POLICY.toString(), policydep.getType());
                assertEquals(policy.getId(), policydep.getId());
                assertEquals(policy.getName(), policydep.getName());
                assertNotNull( "Missing dependency:"+securityZone.getId(), getDependency(policydep.getDependencies(),securityZone.getId()));
                assertNotNull( "Missing dependency:"+policy2.getId(), getDependency(policydep.getDependencies(),policy2.getId()));

                DependencyMO securityZonedep = getDependency(dependencyAnalysisMO.getDependencies(),securityZone.getId());
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZonedep.getType());
                assertEquals(securityZone.getId(), securityZonedep.getId());
                assertEquals(securityZone.getName(), securityZonedep.getName());

                DependencyMO includePolicydep = getDependency(dependencyAnalysisMO.getDependencies(),policy2.getId());
                assertEquals(EntityType.POLICY.toString(), includePolicydep.getType());
                assertEquals(policy2.getId(), includePolicydep.getId());
                assertEquals(policy2.getName(), includePolicydep.getName());
                assertNotNull( "Missing dependency:"+securityZone1.getId(), getDependency(includePolicydep.getDependencies(),securityZone1.getId()));
            }
        });
    }


    @Test
    public void folderServiceAliasTest() throws Exception {

        TestDependency("folders/", folder2.getId(), new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(5, dependencyAnalysisMO.getDependencies().size());

                DependencyMO policyAliasDep = getDependency(dependencyAnalysisMO, EntityType.POLICY_ALIAS);
                assertNotNull(policyAliasDep);
                assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasDep.getType());
                assertEquals(policyAlias.getId(), policyAliasDep.getId());
                assertEquals(policy.getName() + " alias", policyAliasDep.getName());

                // verify security zone dependency
                DependencyMO securityZoneDep = getDependency(dependencyAnalysisMO.getDependencies(), securityZone1.getId());
                assertNotNull(securityZoneDep);
                assertEquals(securityZone1.getId(), securityZoneDep.getId());
                assertEquals(securityZone1.getName(), securityZoneDep.getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneDep.getType());


                // verify policy dependency
                DependencyMO policydep = getDependency(dependencyAnalysisMO.getDependencies(),policy.getId());
                assertNotNull(policydep);
                assertEquals(EntityType.POLICY.toString(), policydep.getType());
                assertEquals(policy.getId(), policydep.getId());
                assertEquals(policy.getName(), policydep.getName());
                assertNotNull("Missing dependency:" + securityZone.getId(), getDependency(policydep.getDependencies(), securityZone.getId()));
                assertNotNull("Missing dependency:" + policy2.getId(), getDependency(policydep.getDependencies(), policy2.getId()));
            }
        });
    }
}
