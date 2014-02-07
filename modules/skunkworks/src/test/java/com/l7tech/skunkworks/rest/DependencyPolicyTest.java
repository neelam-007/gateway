package com.l7tech.skunkworks.rest;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyTreeMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
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
