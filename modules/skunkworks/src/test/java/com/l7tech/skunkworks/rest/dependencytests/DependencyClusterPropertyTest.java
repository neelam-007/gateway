package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.cluster.ClusterPropertyManager;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyClusterPropertyTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyClusterPropertyTest.class.getName());

    private final ClusterProperty clusterProperty =  new ClusterProperty();
    private ClusterPropertyManager clusterPropertyManager;
    private static final String clusterPropName = "testClusterProp";

    @Before
    public void before() throws Exception {
        super.before();

        clusterPropertyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("clusterPropertyManager", ClusterPropertyManager.class);

        //create cluster property
        clusterProperty.setName(clusterPropName);
        clusterProperty.setValue("propValue");
        clusterPropertyManager.save(clusterProperty);

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        clusterPropertyManager.delete(clusterProperty);
    }


    @Test
    public void contextVariableTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"${gateway."+clusterPropName+"}\"/>\n" +
                        "        </L7p:AuditDetailAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2, dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep,clusterProperty);
            }
        });
    }

    @Test
    public void contextVariableDefaultValueTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"${gateway.hostname}\"/>\n" +
                        "        </L7p:AuditDetailAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNull(getDependency(dependencyItem.getContent(),EntityType.CLUSTER_PROPERTY));
            }
        });
    }

    protected void verifyItem(DependencyMO item, ClusterProperty clusterProperty){
        assertEquals(EntityType.CLUSTER_PROPERTY.toString(), item.getType());
        assertEquals(clusterProperty.getId(), item.getId());
        assertEquals(clusterProperty.getName(), item.getName());
    }
}
