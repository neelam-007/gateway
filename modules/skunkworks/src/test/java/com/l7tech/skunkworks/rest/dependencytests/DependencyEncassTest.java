package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyEncassTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyEncassTest.class.getName());

    private final Policy encassPolicy =  new Policy(PolicyType.INTERNAL, "EncassPolicy","",false);
    private final EncapsulatedAssertionConfig encassConfig = new EncapsulatedAssertionConfig();
    private final SecurityZone securityZone =  new SecurityZone();
    private EncapsulatedAssertionConfigManager encassConfigManager;
    private SecurityZoneManager securityZoneManager;


    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        encassConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);

        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        // create policy
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI\"/>\n" +
                        "        </L7p:AuditDetailAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        encassPolicy.setXml(policyXml);
        encassPolicy.setGoid(policyManager.save(encassPolicy));

        // create encass config
        encassConfig.setName("Test Encass");
        encassConfig.setPolicy(encassPolicy);
        encassConfig.setSecurityZone(securityZone);
        encassConfig.setGuid(UUID.randomUUID().toString());
        encassConfigManager.save(encassConfig);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        encassConfigManager.delete(encassConfig);
        policyManager.delete(encassPolicy);
        securityZoneManager.delete(securityZone);
    }


    @Test
    public void encassAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Encapsulated>\n" +
                "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\""+ encassConfig.getGuid() +"\"/>\n" +
                "            <L7p:EncapsulatedAssertionConfigName stringValue=\""+ encassConfig.getName() +"\"/>\n" +
                "        </L7p:Encapsulated>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(3, dependencyAnalysisMO.getDependencies().size());

                DependencyMO policydep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.POLICY.toString(), policydep.getType());
                assertEquals(encassPolicy.getId(), policydep.getId());
                assertEquals(encassPolicy.getName(), policydep.getName());

                DependencyMO securityZonedep  = dependencyAnalysisMO.getDependencies().get(1);
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZonedep.getType());
                assertEquals(securityZone.getId(), securityZonedep.getId());
                assertEquals(securityZone.getName(), securityZonedep.getName());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(2);
                assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), dep.getType());
                assertEquals(encassConfig.getId(), dep.getId());
                assertEquals(encassConfig.getName(), dep.getName());
            }
        });
    }

    @Test
    public void testCircularDependency() throws Exception {
        // update policy
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Encapsulated>\n" +
                        "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\""+ encassConfig.getGuid() +"\"/>\n" +
                        "            <L7p:EncapsulatedAssertionConfigName stringValue=\""+ encassConfig.getName() +"\"/>\n" +
                        "        </L7p:Encapsulated>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        Policy policy = policyManager.findByPrimaryKey(encassPolicy.getGoid());
        policy.setXml(assXml);
        policyManager.update(policy);

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(3, dependencyAnalysisMO.getDependencies().size());

                DependencyMO policydep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.POLICY.toString(), policydep.getType());
                assertEquals(encassPolicy.getId(), policydep.getId());
                assertEquals(encassPolicy.getName(), policydep.getName());
                assertEquals(1, policydep.getDependencies().size());
                assertNotNull("Missing dependency:" + encassConfig.getId(), getDependency(policydep.getDependencies(), encassConfig.getId()));

                DependencyMO securityZonedep  = dependencyAnalysisMO.getDependencies().get(1);
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZonedep.getType());
                assertEquals(securityZone.getId(), securityZonedep.getId());
                assertEquals(securityZone.getName(), securityZonedep.getName());
                assertNull(securityZonedep.getDependencies());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(2);
                assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), dep.getType());
                assertEquals(encassConfig.getId(), dep.getId());
                assertEquals(encassConfig.getName(), dep.getName());
                assertEquals(2, dep.getDependencies().size());
                assertNotNull( "Missing dependency:"+securityZone.getId(), getDependency(dep.getDependencies(),securityZone.getId()));
                assertNotNull( "Missing dependency:"+encassPolicy.getId(), getDependency(dep.getDependencies(),encassPolicy.getId()));
            }
        });
    }
}
