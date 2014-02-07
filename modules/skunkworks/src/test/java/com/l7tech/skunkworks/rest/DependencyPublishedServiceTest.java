package com.l7tech.skunkworks.rest;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyTreeMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
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
public class DependencyPublishedServiceTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyPublishedServiceTest.class.getName());

    private final Policy policy =  new Policy(PolicyType.PRIVATE_SERVICE, "Policy for service","",false);
    private final PublishedService service = new PublishedService();
    private final SecurityZone securityZone =  new SecurityZone();
    private final SecurityZone securityZone1 =  new SecurityZone();
    private PublishedServiceAlias serviceAlias;
    private Folder folder;
    private ServiceManager serviceManager;
    private ServiceAliasManager serviceAliasManager;
    private SecurityZoneManager securityZoneManager;


    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);
        serviceAliasManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceAliasManager", ServiceAliasManager.class);

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

        policy.setXml(policyXml);
        policy.setName("Policy for Test Service");
        policy.setGuid(UUID.randomUUID().toString());
        policyManager.save(policy);
        policyGoids.add(policy.getGoid());

        // create service
        service.setName("Test Service");
        service.setPolicy(policy);
        service.setFolder(rootFolder);
        service.setSecurityZone(securityZone);
        serviceManager.save(service);

        // create folder
        folder = new Folder("Test Folder", rootFolder);
        folderManager.save(folder);

        // create service alias
        serviceAlias = new PublishedServiceAlias(service,folder);
        serviceAlias.setSecurityZone(securityZone1);
        serviceAliasManager.save(serviceAlias);
    }

    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        serviceAliasManager.delete(serviceAlias);
        folderManager.delete(folder);
        serviceManager.delete(service);
        securityZoneManager.delete(securityZone);
        securityZoneManager.delete(securityZone1);
    }


    @Test
    public void serviceTest() throws Exception {

        TestDependency( "services/", service.getId() ,new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1, dependencyAnalysisMO.getDependencies().size());

                DependencyMO securityZonedep = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZonedep.getDependentObject().getType());
                assertEquals(securityZone.getId(), securityZonedep.getDependentObject().getId());
                assertEquals(securityZone.getName(), securityZonedep.getDependentObject().getName());
            }
        });
    }


    @Test
    public void folderServiceAliasTest() throws Exception {

        TestDependency( "folders/", folder.getId() ,new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1, dependencyAnalysisMO.getDependencies().size());

                DependencyMO serviceAliasDep = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasDep.getDependentObject().getType());
                assertEquals(serviceAlias.getId(), serviceAliasDep.getDependentObject().getId());
                assertEquals(service.getId(), serviceAliasDep.getDependentObject().getName());

                // verify security zone dependency
                assertEquals(2,serviceAliasDep.getDependencies().size());
                DependencyMO securityZoneDep  =  getDependency(serviceAliasDep.getDependencies(),EntityType.SECURITY_ZONE);
                assertEquals(securityZone1.getId(), securityZoneDep.getDependentObject().getId());
                assertEquals(securityZone1.getName(), securityZoneDep.getDependentObject().getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneDep.getDependentObject().getType());


                // verify service dependency
                DependencyMO serviceDep  = getDependency(serviceAliasDep.getDependencies(),EntityType.SERVICE);
                assertEquals(service.getId(), serviceDep.getDependentObject().getId());
                assertEquals(service.getName(), serviceDep.getDependentObject().getName());
                assertEquals(EntityType.SERVICE.toString(), serviceDep.getDependentObject().getType());
            }
        });
    }

}
