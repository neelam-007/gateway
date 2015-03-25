package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.transport.SsgConnectorManagerStub;
import org.apache.http.entity.ContentType;
import org.junit.*;
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
public class ListenPortRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(ListenPortRestServerGatewayManagementAssertionTest.class.getName());

    private static final SsgConnector ssgConnector = new SsgConnector();
    private static SsgConnectorManagerStub ssgConnectorManagerStub;
    private static final String listenPortBasePath = "listenPorts/";

    protected ListenPortResourceFactory listenPortResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        ssgConnectorManagerStub = applicationContext.getBean("ssgConnectorManager", SsgConnectorManagerStub.class);
        ssgConnector.setGoid(new Goid(0, 1234L));
        ssgConnector.setName("Test Email Listener");
        ssgConnector.setPort(1234);

        ssgConnectorManagerStub.save(ssgConnector);

        listenPortResourceFactory = assertionContext.getBean("listenPortResourceFactory",ListenPortResourceFactory.class);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(ssgConnectorManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            ssgConnectorManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(listenPortBasePath + ssgConnector.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        ListenPortMO result = (ListenPortMO) item.getContent();

        assertEquals("Listen Port identifier:", ssgConnector.getId(), result.getId());
        assertEquals("Listen Port name:", ssgConnector.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        ListenPortMO createObject = listenPortResourceFactory.asResource(ssgConnector);
        createObject.setId(null);
        createObject.setName("New listen port");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(listenPortBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        SsgConnector createdEntity = ssgConnectorManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Listen Port name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        ListenPortMO createObject = listenPortResourceFactory.asResource(ssgConnector);
        createObject.setId(null);
        createObject.setName("New listen port");

        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(listenPortBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Listen Port goid:", goid.toString(), getFirstReferencedGoid(response));

        SsgConnector createdEntity = ssgConnectorManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Listen Port name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(listenPortBasePath + ssgConnector.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        logger.info(responseGet.toString());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ListenPortMO entityGot = (ListenPortMO)MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.setName("Updated New listen port");
        RestResponse response = processRequest(listenPortBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Listen Port goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SsgConnector updatedEntity = ssgConnectorManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Listen Port id:", ssgConnector.getId(), updatedEntity.getId());
        assertEquals("Listen Port name:", ssgConnector.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(listenPortBasePath + ssgConnector.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(ssgConnectorManagerStub.findByPrimaryKey(ssgConnector.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(listenPortBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<ListenPortMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(ssgConnectorManagerStub.findAll().size(), item.getContent().size());
    }
}
