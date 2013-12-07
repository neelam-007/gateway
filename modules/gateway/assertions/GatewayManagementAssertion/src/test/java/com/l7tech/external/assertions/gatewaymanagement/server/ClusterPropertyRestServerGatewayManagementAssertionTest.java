package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MockClusterPropertyManager;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class ClusterPropertyRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(ClusterPropertyRestServerGatewayManagementAssertionTest.class.getName());

    private static final ClusterProperty clusterProperty = new ClusterProperty();
    private static MockClusterPropertyManager mockClusterPropertyManager;
    private static final String clusterPropertyBasePath = "clusterProperties/";

    @InjectMocks
    protected ClusterPropertyResourceFactory clusterPropertyResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        mockClusterPropertyManager = applicationContext.getBean("clusterPropertyManager", MockClusterPropertyManager.class);
        clusterProperty.setName("cluster property name");
        clusterProperty.setValue("cluster property value");

        mockClusterPropertyManager.save(clusterProperty);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(mockClusterPropertyManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            mockClusterPropertyManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(clusterPropertyBasePath + clusterProperty.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        ClusterPropertyMO result = ManagedObjectFactory.read(response.getBody(), ClusterPropertyMO.class);

        assertEquals("Cluster property identifier:", clusterProperty.getId(), result.getId());
        assertEquals("Cluster property name:", clusterProperty.getName(), result.getName());
        assertEquals("Cluster property value:", clusterProperty.getValue(), result.getValue());
    }

    @Test
    public void createEntityTest() throws Exception {

        ClusterPropertyMO createObject = clusterPropertyResourceFactory.asResource(clusterProperty);
        createObject.setId(null);
        createObject.setValue("New value");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(clusterPropertyBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        ClusterProperty createdEntity = mockClusterPropertyManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Cluster property value:", createObject.getValue(), createdEntity.getValue());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        ClusterPropertyMO createObject = clusterPropertyResourceFactory.asResource(clusterProperty);
        createObject.setId(null);
        createObject.setValue("New user");

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(clusterPropertyBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Cluster property goid:", goid.toString(), getFirstReferencedGoid(response));

        ClusterProperty createdEntity = mockClusterPropertyManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Cluster property username:", createObject.getValue(), createdEntity.getValue());
    }


    @Test
    public void createHiddenClusterPropertyTest() throws Exception {

        ClusterPropertyMO createObject = clusterPropertyResourceFactory.asResource(clusterProperty);
        createObject.setId(null);
        createObject.setName("license");

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(clusterPropertyBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(clusterPropertyBasePath + clusterProperty.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ClusterPropertyMO entityGot = MarshallingUtils.unmarshal(ClusterPropertyMO.class, source);

        // update
        entityGot.setValue("Updated user");
        Response response = processRequest(clusterPropertyBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Cluster property goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        ClusterProperty updatedEntity = mockClusterPropertyManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Cluster property id:", clusterProperty.getId(), updatedEntity.getId());
        assertEquals("Cluster property username:", clusterProperty.getValue(), updatedEntity.getValue());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(clusterPropertyBasePath + clusterProperty.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(mockClusterPropertyManager.findByPrimaryKey(clusterProperty.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(clusterPropertyBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References references = MarshallingUtils.unmarshal(References.class, source);

        // check entity
        Assert.assertEquals(mockClusterPropertyManager.findAll().size(), references.getReferences().size());
    }
}
