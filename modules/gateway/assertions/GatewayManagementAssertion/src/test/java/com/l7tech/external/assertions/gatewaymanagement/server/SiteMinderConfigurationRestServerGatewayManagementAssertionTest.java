package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 *
 */
public class SiteMinderConfigurationRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(SiteMinderConfigurationRestServerGatewayManagementAssertionTest.class.getName());

    private static final SiteMinderConfiguration siteMinderConfiguration = new SiteMinderConfiguration();
    private static SiteMinderConfigurationManagerStub siteMinderConfigurationManager;
    private static final String siteMinderConfigurationBasePath = "siteMinderConfigurations/";

    @InjectMocks
    protected SiteMinderConfigurationResourceFactory siteMinderConfigurationResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        siteMinderConfigurationManager = applicationContext.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManagerStub.class);
        siteMinderConfiguration.setGoid(new Goid(0, 1234L));
        siteMinderConfiguration.setName("Test Config 1");
        siteMinderConfiguration.setUserName("test user name");
        siteMinderConfiguration.setSecret("Test secret");
        siteMinderConfiguration.setFipsmode(1);
        siteMinderConfiguration.setHostname("Test hostname");
        siteMinderConfiguration.setHostConfiguration("test host config");
        siteMinderConfiguration.setAddress("test address");
        siteMinderConfiguration.setProperties(new HashMap<String, String>());
        siteMinderConfiguration.putProperty("prop","value");
        siteMinderConfigurationManager.save(siteMinderConfiguration);
    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(siteMinderConfigurationManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            siteMinderConfigurationManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(siteMinderConfigurationBasePath + siteMinderConfiguration.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        assertEquals("Siteminder Configuration identifier:", siteMinderConfiguration.getId(), item.getId());
        assertEquals("Siteminder Configuration name:", siteMinderConfiguration.getName(), ((SiteMinderConfigurationMO) item.getContent()).getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        SiteMinderConfigurationMO createObject = siteMinderConfigurationResourceFactory.asResource(siteMinderConfiguration);
        createObject.setId(null);
        createObject.setSecret("Test secret1");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(siteMinderConfigurationBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        SiteMinderConfiguration createdConnector = siteMinderConfigurationManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertNotNull("Siteminder Configuration:", createdConnector);
        assertEquals("Siteminder Configuration name:", createdConnector.getName(), createObject.getName());
    }

    @Test
    public void createEntityInvalidPasswordIdFormatTest() throws Exception {

        SiteMinderConfigurationMO createObject = siteMinderConfigurationResourceFactory.asResource(siteMinderConfiguration);
        createObject.setId(null);
        createObject.setSecret("Test secret1");
        createObject.setPasswordId("Bad id");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(siteMinderConfigurationBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        SiteMinderConfigurationMO createObject = siteMinderConfigurationResourceFactory.asResource(siteMinderConfiguration);
        createObject.setId(null);
        createObject.setName("New siteminder config");
        createObject.setSecret("Test secret1");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(siteMinderConfigurationBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.log(Level.INFO, response.toString());

        assertEquals("Created siteminder config goid:", goid.toString(), getFirstReferencedGoid(response));

        SiteMinderConfiguration createdConnector = siteMinderConfigurationManager.findByPrimaryKey(goid);
        assertNotNull("Siteminder Configuration:", createdConnector);
        assertEquals("Siteminder Configuration name:", createdConnector.getName(), createObject.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(siteMinderConfigurationBasePath + siteMinderConfiguration.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        SiteMinderConfigurationMO entityGot = (SiteMinderConfigurationMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.setName(entityGot.getName() + "_mod");
        RestResponse response = processRequest(siteMinderConfigurationBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created siteminder config goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SiteMinderConfiguration updatedConnector = siteMinderConfigurationManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertNotNull("Siteminder Configuration:", updatedConnector);
        assertEquals("Siteminder Configuration id:", updatedConnector.getId(), siteMinderConfiguration.getId());
        assertEquals("Siteminder Configuration name:", updatedConnector.getName(), entityGot.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(siteMinderConfigurationBasePath + siteMinderConfiguration.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(siteMinderConfigurationManager.findByPrimaryKey(siteMinderConfiguration.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(siteMinderConfigurationBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<SiteMinderConfigurationMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(1, item.getContent().size());
    }
}
