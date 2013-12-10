package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.security.rbac.SecurityZoneManagerStub;
import com.l7tech.util.CollectionUtils;
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
public class SecurityZoneRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(SecurityZoneRestServerGatewayManagementAssertionTest.class.getName());

    private static SecurityZone securityZone;
    private static SecurityZoneManagerStub securityZoneManager;
    private static final String securityZoneBasePath = "securityZones/";



    @InjectMocks
    protected SecurityZoneResourceFactory securityZoneResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        securityZone = new SecurityZone();
        securityZone.setName("Security Zone Name");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.POLICY));
        securityZoneManager = applicationContext.getBean("securityZoneManager", SecurityZoneManagerStub.class);
        securityZoneManager.save(securityZone);
    }

    @After
    public void after() throws Exception {
        super.after();

        Collection<EntityHeader> zones = new ArrayList<>(securityZoneManager.findAllHeaders());
        for (EntityHeader zone : zones) {
            securityZoneManager.delete(zone.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(securityZoneBasePath + securityZone.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        SecurityZoneMO result = ManagedObjectFactory.read(response.getBody(), SecurityZoneMO.class);

        assertEquals("Security Zone identifier:", securityZone.getId(), result.getId());
        assertEquals("Security Zone name:", securityZone.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        SecurityZoneMO createObject = securityZoneResourceFactory.asResource(securityZone);
        createObject.setId(null);
        createObject.setName("New Zone");
        createObject.setPermittedEntityTypes(CollectionUtils.list(EntityType.FOLDER.toString()));
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(securityZoneBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        SecurityZone createdEntity = securityZoneManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Security Zone name:", createObject.getName() , createdEntity.getName());
        assertEquals("Security Zone permitted entities size:", createObject.getPermittedEntityTypes().size(), createdEntity.getPermittedEntityTypes().size());
        assertEquals("Security Zone permitted entity:",EntityType.FOLDER, createdEntity.getPermittedEntityTypes().toArray()[0]);
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        SecurityZoneMO createObject = securityZoneResourceFactory.asResource(securityZone);
        createObject.setId(null);
        createObject.setName("New Zone");
        createObject.setPermittedEntityTypes(CollectionUtils.list(EntityType.FOLDER.toString()));

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(securityZoneBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Security Zone goid:", goid.toString(), getFirstReferencedGoid(response));

        SecurityZone createdEntity = securityZoneManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Security Zone name:", createObject.getName() , createdEntity.getName());
        assertEquals("Security Zone permitted entities size:", createObject.getPermittedEntityTypes().size(), createdEntity.getPermittedEntityTypes().size());
        assertEquals("Security Zone permitted entity:",EntityType.FOLDER, createdEntity.getPermittedEntityTypes().toArray()[0]);
    }

    @Test
    public void createInvalidEntityTypeTest() throws Exception {

        SecurityZoneMO createObject = securityZoneResourceFactory.asResource(securityZone);
        createObject.setId(null);
        createObject.setName("New Zone");
        createObject.setPermittedEntityTypes(CollectionUtils.list("Bad Entity Type"));
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(securityZoneBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }


    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(securityZoneBasePath + securityZone.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        SecurityZoneMO entityGot = MarshallingUtils.unmarshal(SecurityZoneMO.class, source);

        // update
        entityGot.setName("New Name");
        Response response = processRequest(securityZoneBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Updated Security Zone goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SecurityZone updatedEntity = securityZoneManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Security Zone id:", securityZone.getId(), updatedEntity.getId());
        assertEquals("Security Zone name:", securityZone.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(securityZoneBasePath + securityZone.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(securityZoneManager.findByPrimaryKey(securityZone.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(securityZoneBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References references = MarshallingUtils.unmarshal(References.class, source);

        // check entity
        Assert.assertEquals(securityZoneManager.findAll().size(), references.getReferences().size());
    }
}
