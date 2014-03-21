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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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


    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policyAliasManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyAliasManager", PolicyAliasManager.class);

//        rootFolder
//            - folder
//                - policy ( includes policy 2, in securityZone)
//                - folder2
//                    - policy2  ( in securityZone2)
//                    - policyAlias

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
        policy.setFolder(folder2);
        policy.setSecurityZone(securityZone1);
        policyManager.save(policy2);
        policyGoids.add(policy2.getGoid());

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
        policyAliasManager.delete(policyAlias);
        super.after();
        folderManager.delete(folder2);
        folderManager.delete(folder);
        securityZoneManager.delete(securityZone);
        securityZoneManager.delete(securityZone1);
    }

    @Test
    public void policyVersionTest() throws Exception {

        RestResponse versionCheck = processRequest("policies/" + policy.getId() + "/versions/", HttpMethod.GET, null, "");
        logger.info("versions" + versionCheck.toString());

        // get
        RestResponse responseGet = processRequest("policies/" + policy.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        PolicyMO entityGot = (PolicyMO) MarshallingUtils.unmarshal(Item.class, source).getContent();
        entityGot.setSecurityZoneId(null);
        getDatabaseBasedRestManagementEnvironment().processRequest( "policies/" + policy.getId() , HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),  XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));



        responseGet = processRequest("policies/" + policy.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        entityGot = (PolicyMO) MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(responseGet.getBody()))).getContent();
        final String newPolicyXML =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"new policy\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";
        final List<ResourceSet> resourceSets = new ArrayList<ResourceSet>();
        entityGot.setResourceSets( resourceSets );
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSets.add( resourceSet );
        resourceSet.setTag( "policy" );
        final Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources( Collections.singletonList(resource) );
        resource.setType( "policy" );
        resource.setContent( newPolicyXML );

        String comment = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest( "policies/" + policy.getId() + "?versionComment="+comment+"&active=false", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),  XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info("update response" + response.toString());

        RestResponse updateGet = processRequest("policies/" + policy.getId(), HttpMethod.GET, null, "");
        logger.info("check updated" + updateGet.toString());

        RestResponse versionsGet = processRequest("policies/" + policy.getId() + "/versions/", HttpMethod.GET, null, "");
        logger.info("check versions" + versionsGet.toString());
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
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest( "services/" + policy.getId() + "?versionComment=newComment&active=false", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),  serviceMOxml);
        logger.info("update response" + response.toString());

        RestResponse versionsGet = processRequest("services/" + policy.getId() + "/versions/", HttpMethod.GET, null, "");
        logger.info("check versions" + versionsGet.toString());
    }

    @Test
    public void policyTest() throws Exception {

        TestDependency("policies/", policy.getId(), new Functions.UnaryVoid<Item<DependencyTreeMO>>() {

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2, dependencyAnalysisMO.getDependencies().size());

                DependencyMO securityZonedep = getDependency(dependencyAnalysisMO.getDependencies(),EntityType.SECURITY_ZONE);
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZonedep.getDependentObject().getType());
                assertEquals(securityZone.getId(), securityZonedep.getDependentObject().getId());
                assertEquals(securityZone.getName(), securityZonedep.getDependentObject().getName());

                DependencyMO includePolicydep = getDependency(dependencyAnalysisMO.getDependencies(),EntityType.POLICY);
                assertEquals(EntityType.POLICY.toString(), includePolicydep.getDependentObject().getType());
                assertEquals(policy2.getId(), includePolicydep.getDependentObject().getId());
                assertEquals(policy2.getName(), includePolicydep.getDependentObject().getName());
            }
        });
    }


    @Test
    public void folderServiceAliasTest() throws Exception {

        TestDependency("folders/", folder2.getId(), new Functions.UnaryVoid<Item<DependencyTreeMO>>() {

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1, dependencyAnalysisMO.getDependencies().size());

                DependencyMO policyAliasDep = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasDep.getDependentObject().getType());
                assertEquals(policyAlias.getId(), policyAliasDep.getDependentObject().getId());
                assertEquals(policy.getId(), policyAliasDep.getDependentObject().getName());

                // verify security zone dependency
                assertEquals(2, policyAliasDep.getDependencies().size());
                DependencyMO securityZoneDep = getDependency(policyAliasDep.getDependencies(), EntityType.SECURITY_ZONE);
                assertEquals(securityZone1.getId(), securityZoneDep.getDependentObject().getId());
                assertEquals(securityZone1.getName(), securityZoneDep.getDependentObject().getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneDep.getDependentObject().getType());


                // verify policy dependency
                DependencyMO policydep = getDependency(policyAliasDep.getDependencies(),EntityType.POLICY);
                assertEquals(EntityType.POLICY.toString(), policydep.getDependentObject().getType());
                assertEquals(policy.getId(), policydep.getDependentObject().getId());
                assertEquals(policy.getName(), policydep.getDependentObject().getName());
            }
        });
    }

}
