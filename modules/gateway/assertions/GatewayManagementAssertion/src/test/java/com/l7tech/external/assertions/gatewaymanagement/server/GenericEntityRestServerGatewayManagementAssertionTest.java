package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.entity.GenericEntityManagerStub;
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
public class GenericEntityRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(GenericEntityRestServerGatewayManagementAssertionTest.class.getName());

    private static final GenericEntity genericEntity = new GenericEntity();
    private static GenericEntityManagerStub genericEntityManagerStub;
    private static final String genericEntityBasePath = "genericEntities/";
    private static final String genericEntityClass = "com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityRestServerGatewayManagementAssertionTest";

    @InjectMocks
    protected GenericEntityResourceFactory genericEntityResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        genericEntityManagerStub = applicationContext.getBean("genericEntityManager", GenericEntityManagerStub.class);
        genericEntityManagerStub.setRegistedClasses(genericEntityClass);
        genericEntity.setGoid(new Goid(0, 1234L));
        genericEntity.setName("Test Generic Entity");
        genericEntity.setEntityClassName(genericEntityClass);
        genericEntity.setValueXml("<xml>xml value</xml>");

        genericEntityManagerStub.save(genericEntity);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<GenericEntityHeader> entities = new ArrayList<>(genericEntityManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            genericEntityManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(genericEntityBasePath + genericEntity.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        GenericEntityMO result = ManagedObjectFactory.read(response.getBody(), GenericEntityMO.class);

        assertEquals("Generic Entity identifier:", genericEntity.getId(), result.getId());
        assertEquals("Generic Entity name:", genericEntity.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        GenericEntityMO createObject = genericEntityResourceFactory.asResource(genericEntity);
        createObject.setId(null);
        createObject.setName("New generic Entity");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(genericEntityBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        GenericEntity createdEntity = genericEntityManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Generic Entity name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        GenericEntityMO createObject = genericEntityResourceFactory.asResource(genericEntity);
        createObject.setId(null);
        createObject.setName("New generic Entity");

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(genericEntityBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Generic Entity goid:", goid.toString(), getFirstReferencedGoid(response));

        GenericEntity createdEntity = genericEntityManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Generic Entity name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(genericEntityBasePath + genericEntity.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        GenericEntityMO entityGot = MarshallingUtils.unmarshal(GenericEntityMO.class, source);

        // update
        entityGot.setName("New Generic Entity");
        Response response = processRequest(genericEntityBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Generic Entity goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        GenericEntity updatedEntity = genericEntityManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Generic Entity id:", genericEntity.getId(), updatedEntity.getId());
        assertEquals("Generic Entity name:", genericEntity.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(genericEntityBasePath + genericEntity.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(genericEntityManagerStub.findByPrimaryKey(genericEntity.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(genericEntityBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References references = MarshallingUtils.unmarshal(References.class, source);

        // check entity
        Assert.assertEquals(genericEntityManagerStub.findAll().size(), references.getReferences().size());
    }
}
