package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.AssertionAccessManagerStub;
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
import static org.junit.Assert.assertNull;

/**
 *
 */
public class AssertionSecurityZoneRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(AssertionSecurityZoneRestServerGatewayManagementAssertionTest.class.getName());

    private static final AssertionAccess assertionSecurityZone = new AssertionAccess();
    private static AssertionAccessManagerStub assertionSecurityZoneManager;
    private static SecurityZone securityZone;
    private static SecurityZoneManagerStub securityZoneManager;
    private static final String assertionSecurityZoneBasePath = "assertionSecurityZones/";

    @InjectMocks
    protected AssertionSecurityZoneResourceFactory assertionSecurityZoneResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        securityZone = new SecurityZone();
        securityZone.setName("Security Zone Name");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ASSERTION_ACCESS));
        securityZoneManager = applicationContext.getBean("securityZoneManager", SecurityZoneManagerStub.class);
        securityZoneManager.save(securityZone);

        assertionSecurityZoneManager = applicationContext.getBean("assertionAccessManager", AssertionAccessManagerStub.class);
        assertionSecurityZoneManager.setRegisteredAssertions("TestAssertion","Assertion2");
        assertionSecurityZone.setGoid(new Goid(0, 1234L));
        assertionSecurityZone.setName("TestAssertion");
        assertionSecurityZone.setSecurityZone(securityZone);
        assertionSecurityZoneManager.save(assertionSecurityZone);

    }

    @After
    public void after() throws Exception {
        super.after();

        Collection<EntityHeader> entities = new ArrayList<>(assertionSecurityZoneManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            assertionSecurityZoneManager.delete(entity.getGoid());
        }

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
        Response response = processRequest(assertionSecurityZoneBasePath + assertionSecurityZone.getName(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

        assertEquals("Assertion Security Zone identifier:", assertionSecurityZone.getId(), reference.getId());
        assertEquals("Assertion Security Zone name:", assertionSecurityZone.getName(), ((AssertionSecurityZoneMO) reference.getResource()).getName());
        assertEquals("Assertion Security Zone security zone id:", securityZone.getId(), ((AssertionSecurityZoneMO) reference.getResource()).getSecurityZoneId());
    }

    @Test
    public void createEntityTest() throws Exception {

        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
        createObject.setId(null);
        createObject.setName("Assertion2");
        createObject.setSecurityZoneId(securityZone.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(assertionSecurityZoneBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatus());
    }

    @Test
    public void updateEntityTest() throws Exception {

        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
        createObject.setId(null);
        createObject.setName("Assertion2");
        createObject.setSecurityZoneId(securityZone.getId());
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(assertionSecurityZoneBasePath + createObject.getName(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(HttpStatus.SC_OK, response.getStatus());
        AssertionAccess createdConnector = assertionSecurityZoneManager.findByUniqueName(getFirstReferencedGoid(response));

        assertEquals("Assertion Security Zone name:", createdConnector.getName(), createObject.getName());
        assertEquals("Assertion Security Zone security zone id:", createdConnector.getSecurityZone().getId(), createObject.getSecurityZoneId());
    }

    @Test
    public void updateEntityRemoveSecurityZoneTest() throws Exception {

        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
        createObject.setId(assertionSecurityZone.getId());
        createObject.setName(assertionSecurityZone.getName());
        createObject.setSecurityZoneId(null);
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(assertionSecurityZoneBasePath + createObject.getName(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        AssertionAccess createdConnector = assertionSecurityZoneManager.findByUniqueName(getFirstReferencedGoid(response));

        assertEquals("Assertion Security Zone name:", createdConnector.getName(), createObject.getName());
        assertNull("Assertion Security Zone security zone id should be null", createdConnector.getSecurityZone());
    }


    @Test
    public void updateEntityInvalidHardwiredIdFormatTest() throws Exception {

        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
        createObject.setId(assertionSecurityZone.getId());
        createObject.setName(assertionSecurityZone.getName());
        createObject.setSecurityZoneId("not a id");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(assertionSecurityZoneBasePath + createObject.getName(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

//    @Test
//    public void createEntityWithIDTest() throws Exception {
//
//        Goid goid = new Goid(12345678L, 5678);
//        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
//        createObject.setId(null);
//        createObject.setName("New active connector");
//        Document request = ManagedObjectFactory.write(createObject);
//        Response response = processRequest(assertionSecurityZoneBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
//        logger.log(Level.INFO, response.toString());
//
//        assertEquals("Created active connector goid:", goid.toString(), getFirstReferencedGoid(response));
//
//        SsgAssertionSecurityZone createdConnector = assertionSecurityZoneManager.findByPrimaryKey(goid);
//        assertEquals("Assertion Security Zone name:", createdConnector.getName(), createObject.getName());
//        assertEquals("Assertion Security Zone type:", createdConnector.getType(), createObject.getType());
//        assertEquals("Assertion Security Zone hardwired id:", createdConnector.getHardwiredServiceGoid().toString(), createObject.getHardwiredId());
//    }

//    @Test
//    public void updateEntityTest() throws Exception {
//
//        // get
//        Response responseGet = processRequest(assertionSecurityZoneBasePath + assertionSecurityZone.getId(), HttpMethod.GET, null, "");
//        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
//        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
//        AssertionSecurityZoneMO entityGot = (AssertionSecurityZoneMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();
//
//        // update
//        entityGot.setName(entityGot.getName() + "_mod");
//        Response response = processRequest(assertionSecurityZoneBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
//
//        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
//        assertEquals("Created active connector goid:", entityGot.getId(), getFirstReferencedGoid(response));
//
//        // check entity
//        SsgAssertionSecurityZone updatedConnector = assertionSecurityZoneManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
//
//        assertEquals("Assertion Security Zone id:", updatedConnector.getId(), assertionSecurityZone.getId());
//        assertEquals("Assertion Security Zone name:", updatedConnector.getName(), entityGot.getName());
//        assertEquals("Assertion Security Zone type:", updatedConnector.getType(), entityGot.getType());
//        assertEquals("Assertion Security Zone hardwired id:", updatedConnector.getHardwiredServiceGoid().toString(), entityGot.getHardwiredId());
//    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(assertionSecurityZoneBasePath + assertionSecurityZone.getName(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatus());
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(assertionSecurityZoneBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References reference = MarshallingUtils.unmarshal(References.class, source);

        // check entity
        Assert.assertEquals(2, reference.getReferences().size());
        Assert.assertEquals(1, assertionSecurityZoneManager.findAll().size());
    }
}
