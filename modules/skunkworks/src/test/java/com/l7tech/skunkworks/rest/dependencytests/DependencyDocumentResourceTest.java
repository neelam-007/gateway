package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.globalresources.ResourceEntryManager;
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

import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyDocumentResourceTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyDocumentResourceTest.class.getName());

    private final ResourceEntry resourceEntry =  new ResourceEntry();
    private final SecurityZone securityZone =  new SecurityZone();
    private SecurityZoneManager securityZoneManager;
    private ResourceEntryManager resourceEntryManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        resourceEntryManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("resourceEntryManager", ResourceEntryManager.class);


        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        // create resource entry in security zone
        resourceEntry.setUri("http://someurl");
        resourceEntry.setType(ResourceType.XML_SCHEMA);
        resourceEntry.setContentType(ResourceType.XML_SCHEMA.getMimeType());
        resourceEntry.setContent("<schema/>");
        resourceEntry.setSecurityZone(securityZone);
        resourceEntryManager.save(resourceEntry);


    }

    @After
    public void after() throws Exception {
        super.after();
        resourceEntryManager.delete(resourceEntry);
        securityZoneManager.delete(securityZone);

    }

    @Test
    public void ValidateXMLSchemaAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SchemaValidation>\n" +
                "            <L7p:ResourceInfo globalResourceInfo=\"included\">\n" +
                "                <L7p:Id stringValue=\""+resourceEntry.getUri()+"\"/>\n" +
                "            </L7p:ResourceInfo>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "        </L7p:SchemaValidation>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(2,dependencyAnalysisMO.getDependencies().size());

                // verify security zone dependency
                DependencyMO passwordDep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(securityZone.getId(), passwordDep.getId());
                assertEquals(securityZone.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), passwordDep.getType());
                assertNull(passwordDep.getDependencies());

                // verify document resource dependency
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(1);
                assertEquals(resourceEntry.getId(), dep.getId());
                assertEquals(resourceEntry.getUri(), dep.getName());
                assertEquals(EntityType.RESOURCE_ENTRY.toString(), dep.getType());
                assertEquals(1,dep.getDependencies().size());
                assertEquals(securityZone.getId(),dep.getDependencies().get(0).getId());

            }
        });
    }

    @Test
    public void brokenReferenceTest() throws Exception {

        final String brokenReferenceUri = resourceEntry.getUri()+"brokenReference";

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:SchemaValidation>\n" +
                        "            <L7p:ResourceInfo globalResourceInfo=\"included\">\n" +
                        "                <L7p:Id stringValue=\""+brokenReferenceUri+"\"/>\n" +
                        "            </L7p:ResourceInfo>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:SchemaValidation>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(0,dependencyAnalysisMO.getDependencies().size());

                assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                assertNotNull(brokenDep);
                assertEquals(EntityType.RESOURCE_ENTRY.toString(), brokenDep.getType());
                assertEquals(brokenReferenceUri, brokenDep.getName());
            }
        });
    }

}
