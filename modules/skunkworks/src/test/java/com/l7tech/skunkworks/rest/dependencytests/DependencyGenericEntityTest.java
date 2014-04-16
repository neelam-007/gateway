package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyGenericEntityTest extends DependencyTestBase {
    private static final Logger logger = Logger.getLogger(DependencyGenericEntityTest.class.getName());

    private GenericEntityManager genericEntityManager;
    private GenericEntity genericEntity;

    @Before
    public void before() throws Exception {
        super.before();

        genericEntityManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("genericEntityManager", GenericEntityManager.class);

        //create generic entity
        genericEntity = new GenericEntity();
        genericEntity.setName("MyEntity");
        genericEntity.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntity.setValueXml("<xml>xml value</xml>");
        genericEntityManager.save(genericEntity);

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        genericEntityManager.delete(genericEntity);
    }

    @Test
     public void WhichModuleTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:GenericEntityManagerDemo>\n" +
                "            <L7p:GenericEntityId goidValue=\""+ genericEntity.getId() +"\"/>\n" +
                "            <L7p:GenericEntityClass stringValue=\""+ genericEntity.getEntityClassName() +"\"/>\n" +
                "        </L7p:GenericEntityManagerDemo>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(genericEntity.getName(), dep.getName());
                assertEquals(EntityType.GENERIC.toString(), dep.getType());
            }
        });
    }
}
