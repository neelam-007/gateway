package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.store.CustomKeyValueStoreManagerStub;
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
import static org.junit.Assert.assertArrayEquals;

/**
 *
 */
public class CustomKeyValueStoreRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(CustomKeyValueStoreRestServerGatewayManagementAssertionTest.class.getName());

    private static final CustomKeyValueStore customKeyValStore = new CustomKeyValueStore();
    private static CustomKeyValueStoreManagerStub customKeyValueStoreManager;
    private static final String customKeyValStoreBasePath = "customKeyValues/";

    @InjectMocks
    protected CustomKeyValueStoreResourceFactory customKeyValueStoreResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        customKeyValueStoreManager = applicationContext.getBean("customKeyValueStoreManager", CustomKeyValueStoreManagerStub.class);
        customKeyValStore.setGoid(new Goid(0, 1234L));
        customKeyValStore.setName("Test Key");
        customKeyValStore.setValue("Test Value".getBytes());
        customKeyValueStoreManager.save(customKeyValStore);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(customKeyValueStoreManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            customKeyValueStoreManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(customKeyValStoreBasePath + customKeyValStore.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        CustomKeyValueStoreMO result = (CustomKeyValueStoreMO) reference.getResource();

        assertEquals("Custom Key Value identifier:", customKeyValStore.getId(), result.getId());
        assertEquals("Custom Key Value key:", customKeyValStore.getName(), result.getKey());
        assertArrayEquals("Custom Key Value value:", customKeyValStore.getValue(), result.getValue());
    }

    @Test
    public void createEntityTest() throws Exception {

        CustomKeyValueStoreMO createObject = customKeyValueStoreResourceFactory.asResource(customKeyValStore);
        createObject.setId(null);
        createObject.setKey("New custom key value");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(customKeyValStoreBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        CustomKeyValueStore createdEntity = customKeyValueStoreManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Custom Key Value key:", createdEntity.getName(), createObject.getKey());
        assertArrayEquals("Custom Key Value value:", createdEntity.getValue(), createObject.getValue());
    }


    @Test
    public void createEntityBadStoreTest() throws Exception {

        CustomKeyValueStoreMO createObject = customKeyValueStoreResourceFactory.asResource(customKeyValStore);
        createObject.setId(null);
        createObject.setKey("New custom key value");
        createObject.setStoreName("Bad store");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(customKeyValStoreBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        CustomKeyValueStoreMO createObject = customKeyValueStoreResourceFactory.asResource(customKeyValStore);
        createObject.setId(null);
        createObject.setKey("New custom key value");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(customKeyValStoreBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Custom Key Value goid:", goid.toString(), getFirstReferencedGoid(response));

        CustomKeyValueStore createdConnector = customKeyValueStoreManager.findByPrimaryKey(goid);
        assertEquals("Custom Key Value key:", createdConnector.getName(), createObject.getKey());
        assertArrayEquals("Custom Key Value value:", createdConnector.getValue(), createObject.getValue());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(customKeyValStoreBasePath + customKeyValStore.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        CustomKeyValueStoreMO entityGot = (CustomKeyValueStoreMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setKey(entityGot.getKey() + "_mod");
        RestResponse response = processRequest(customKeyValStoreBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Custom Key Value goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        CustomKeyValueStore updatedConnector = customKeyValueStoreManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Custom Key Value id:", updatedConnector.getId(), customKeyValStore.getId());

        assertEquals("Custom Key Value key:", updatedConnector.getName(), entityGot.getKey());
        assertArrayEquals("Custom Key Value value:", updatedConnector.getValue(), entityGot.getValue());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(customKeyValStoreBasePath + customKeyValStore.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(customKeyValueStoreManager.findByPrimaryKey(customKeyValStore.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(customKeyValStoreBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(1, reference.getResource().getReferences().size());
    }
}
