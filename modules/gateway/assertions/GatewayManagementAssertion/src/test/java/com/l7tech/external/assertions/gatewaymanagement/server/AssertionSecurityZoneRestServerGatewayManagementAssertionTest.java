package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
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
    private static AssertionRegistry assertionRegistry;
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

        assertionRegistry = applicationContext.getBean("assertionRegistry", AssertionRegistry.class);
        assertionRegistry.registerAssertion(JmsRoutingAssertion.class);
        assertionRegistry.registerAssertion(GatewayManagementAssertion.class);

        assertionSecurityZoneManager = applicationContext.getBean("assertionAccessManager", AssertionAccessManagerStub.class);
        assertionSecurityZoneManager.setRegisteredAssertions(JmsRoutingAssertion.class.getName(),GatewayManagementAssertion.class.getName());
        assertionSecurityZone.setGoid(new Goid(0, 1234L));
        assertionSecurityZone.setName(JmsRoutingAssertion.class.getName());
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
        RestResponse response = processRequest(assertionSecurityZoneBasePath + assertionSecurityZone.getName(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        assertEquals("Assertion Security Zone identifier:", assertionSecurityZone.getId(), ((AssertionSecurityZoneMO) item.getContent()).getId());
        assertEquals("Assertion Security Zone name:", assertionSecurityZone.getName(), ((AssertionSecurityZoneMO) item.getContent()).getName());
        assertEquals("Assertion Security Zone security zone id:", securityZone.getId(), ((AssertionSecurityZoneMO) item.getContent()).getSecurityZoneId());
    }

    @Test
    public void createEntityTest() throws Exception {

        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
        createObject.setId(null);
        createObject.setName(GatewayManagementAssertion.class.getName());
        createObject.setSecurityZoneId(securityZone.getId());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(assertionSecurityZoneBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);

        Assert.assertTrue(error.getDetail().contains("Method Not Allowed"));
        Assert.assertEquals("NotAllowed",error.getType());
    }

    @Test
    public void updateEntityTest() throws Exception {

        AssertionSecurityZoneMO createObject = assertionSecurityZoneResourceFactory.asResource(assertionSecurityZone);
        createObject.setId(null);
        createObject.setName(GatewayManagementAssertion.class.getName());
        createObject.setSecurityZoneId(securityZone.getId());
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(assertionSecurityZoneBasePath + createObject.getName(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
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
        RestResponse response = processRequest(assertionSecurityZoneBasePath + createObject.getName(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
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
        RestResponse response = processRequest(assertionSecurityZoneBasePath + createObject.getName(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);

        Assert.assertTrue(error.getDetail().contains("INVALID_VALUES"));
        Assert.assertEquals("InvalidResource",error.getType());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(assertionSecurityZoneBasePath + assertionSecurityZone.getName(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatus());
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(assertionSecurityZoneBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<AssertionSecurityZoneMO> reference = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(2, reference.getContent().size());
        Assert.assertEquals(1, assertionSecurityZoneManager.findAll().size());
    }
}
