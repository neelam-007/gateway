package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.globalresources.HttpConfigurationManagerStub;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
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
public class HttpConfigurationRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(HttpConfigurationRestServerGatewayManagementAssertionTest.class.getName());

    private static final HttpConfiguration httpConfiguration = new HttpConfiguration();
    private static HttpConfigurationManagerStub httpConfigurationManagerStub;
    private static final String httpConfigurationBasePath = "httpConfigurations/";

    @InjectMocks
    protected HttpConfigurationResourceFactory httpConfigurationResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        SecurePasswordManagerStub passwordManager = applicationContext.getBean("securePasswordManager", SecurePasswordManagerStub.class);
        SecurePassword password = new SecurePassword();
        password.setDescription("Test password");
        password.setName("Test password name");
        password.setEncodedPassword(passwordManager.encryptPassword("password".toCharArray()));
        passwordManager.save(password);

        httpConfigurationManagerStub = applicationContext.getBean("httpConfigurationManager", HttpConfigurationManagerStub.class);
        httpConfiguration.setGoid(new Goid(0, 1234L));
        httpConfiguration.setHost("host");
        httpConfiguration.setPort(1234);
        httpConfiguration.setProtocol(HttpConfiguration.Protocol.HTTPS);
        httpConfiguration.setPath("path");
        httpConfiguration.setUsername("user");
        httpConfiguration.setPasswordGoid(password.getGoid());

        httpConfigurationManagerStub.save(httpConfiguration);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(httpConfigurationManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            httpConfigurationManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(httpConfigurationBasePath + httpConfiguration.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        HttpConfigurationMO result = (HttpConfigurationMO) reference.getResource();

        assertEquals("Http configuration identifier:", httpConfiguration.getId(), result.getId());
        assertEquals("Http configuration username:", httpConfiguration.getUsername(), result.getUsername());
    }

    @Test
    public void createEntityTest() throws Exception {

        HttpConfigurationMO createObject = httpConfigurationResourceFactory.asResource(httpConfiguration);
        createObject.setId(null);
        createObject.setUsername("New user");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(httpConfigurationBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        HttpConfiguration createdEntity = httpConfigurationManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Http configuration username:", createObject.getUsername(), createdEntity.getUsername());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        HttpConfigurationMO createObject = httpConfigurationResourceFactory.asResource(httpConfiguration);
        createObject.setId(null);
        createObject.setUsername("New user");

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(httpConfigurationBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Http configuration goid:", goid.toString(), getFirstReferencedGoid(response));

        HttpConfiguration createdEntity = httpConfigurationManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Http configuration username:", createObject.getUsername(), createdEntity.getUsername());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(httpConfigurationBasePath + httpConfiguration.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        HttpConfigurationMO entityGot = (HttpConfigurationMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setUsername("Updated user");
        Response response = processRequest(httpConfigurationBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Http configuration goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        HttpConfiguration updatedEntity = httpConfigurationManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Http configuration id:", httpConfiguration.getId(), updatedEntity.getId());
        assertEquals("Http configuration username:", httpConfiguration.getUsername(), updatedEntity.getUsername());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(httpConfigurationBasePath + httpConfiguration.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(httpConfigurationManagerStub.findByPrimaryKey(httpConfiguration.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(httpConfigurationBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(httpConfigurationManagerStub.findAll().size(), reference.getResource().getReferences().size());
    }
}
