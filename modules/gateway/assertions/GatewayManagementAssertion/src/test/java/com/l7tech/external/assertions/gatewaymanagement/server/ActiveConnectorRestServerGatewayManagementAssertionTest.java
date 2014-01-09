package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.transport.SsgActiveConnectorManagerStub;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class ActiveConnectorRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(ActiveConnectorRestServerGatewayManagementAssertionTest.class.getName());

    private static final SsgActiveConnector activeConnector = new SsgActiveConnector();
    private static SsgActiveConnectorManagerStub activeConnectorManager;
    private static final String activeConnectorBasePath = "activeConnectors/";

    @InjectMocks
    protected ActiveConnectorResourceFactory activeConnectorResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        activeConnectorManager = applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManagerStub.class);
        activeConnector.setGoid(new Goid(0, 1234L));
        activeConnector.setName("Test MQ Config 1");
        activeConnector.setHardwiredServiceGoid(new Goid(123, 123));
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false");
        activeConnectorManager.save(activeConnector);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<SsgActiveConnectorHeader> entities = new ArrayList<>(activeConnectorManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            activeConnectorManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(activeConnectorBasePath + activeConnector.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

        assertEquals("Active connector identifier:", activeConnector.getId(), reference.getId());
        assertEquals("Active connector name:", activeConnector.getName(), ((ActiveConnectorMO) reference.getResource()).getName());
        assertEquals("Active connector type:", activeConnector.getType(), ((ActiveConnectorMO) reference.getResource()).getType());
        assertEquals("Active connector hardwired id:", activeConnector.getHardwiredServiceGoid().toString(), ((ActiveConnectorMO) reference.getResource()).getHardwiredId());
    }

    @Test
    public void createEntityTest() throws Exception {

        ActiveConnectorMO createObject = activeConnectorResourceFactory.asResource(activeConnector);
        createObject.setId(null);
        createObject.setName("New active connector");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(activeConnectorBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        SsgActiveConnector createdConnector = activeConnectorManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Active connector name:", createdConnector.getName(), createObject.getName());
        assertEquals("Active connector type:", createdConnector.getType(), createObject.getType());
        assertEquals("Active connector hardwired id:", createdConnector.getHardwiredServiceGoid().toString(), createObject.getHardwiredId());
    }

    @Test
    public void createEntityInvalidHardwiredIdFormatTest() throws Exception {

        ActiveConnectorMO createObject = activeConnectorResourceFactory.asResource(activeConnector);
        createObject.setId(null);
        createObject.setName("New active connector");
        createObject.setHardwiredId("not a id");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(activeConnectorBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        ActiveConnectorMO createObject = activeConnectorResourceFactory.asResource(activeConnector);
        createObject.setId(null);
        createObject.setName("New active connector");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(activeConnectorBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.log(Level.INFO, response.toString());

        assertEquals("Created active connector goid:", goid.toString(), getFirstReferencedGoid(response));

        SsgActiveConnector createdConnector = activeConnectorManager.findByPrimaryKey(goid);
        assertEquals("Active connector name:", createdConnector.getName(), createObject.getName());
        assertEquals("Active connector type:", createdConnector.getType(), createObject.getType());
        assertEquals("Active connector hardwired id:", createdConnector.getHardwiredServiceGoid().toString(), createObject.getHardwiredId());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(activeConnectorBasePath + activeConnector.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ActiveConnectorMO entityGot = (ActiveConnectorMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setName(entityGot.getName() + "_mod");
        RestResponse response = processRequest(activeConnectorBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created active connector goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SsgActiveConnector updatedConnector = activeConnectorManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Active connector id:", updatedConnector.getId(), activeConnector.getId());
        assertEquals("Active connector name:", updatedConnector.getName(), entityGot.getName());
        assertEquals("Active connector type:", updatedConnector.getType(), entityGot.getType());
        assertEquals("Active connector hardwired id:", updatedConnector.getHardwiredServiceGoid().toString(), entityGot.getHardwiredId());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(activeConnectorBasePath + activeConnector.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(activeConnectorManager.findByPrimaryKey(activeConnector.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(activeConnectorBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(1, reference.getResource().getReferences().size());
    }
}
