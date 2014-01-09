package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
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
public class SecurePasswordRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(SecurePasswordRestServerGatewayManagementAssertionTest.class.getName());

    private static final SecurePassword securePassword = new SecurePassword();
    private static SecurePasswordManagerStub securePasswordManagerStub;
    private static final String securePasswordBasePath = "passwords/";

    @InjectMocks
    protected SecurePasswordResourceFactory securePasswordResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManagerStub = applicationContext.getBean("securePasswordManager", SecurePasswordManagerStub.class);
        securePassword.setDescription("Test password");
        securePassword.setName("Test password name");
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePassword.setEncodedPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));
        securePasswordManagerStub.save(securePassword);
    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(securePasswordManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            securePasswordManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(securePasswordBasePath + securePassword.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        StoredPasswordMO result = (StoredPasswordMO) reference.getResource();

        assertEquals("Secure Password identifier:", securePassword.getId(), result.getId());
        assertEquals("Secure Password Name:", securePassword.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        StoredPasswordMO createObject = securePasswordResourceFactory.asResource(securePassword);
        createObject.setId(null);
        createObject.setName("New name");
        createObject.setPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(securePasswordBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        logger.log(Level.INFO, response.getBody());

        SecurePassword createdEntity = securePasswordManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Secure Password Name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        StoredPasswordMO createObject = securePasswordResourceFactory.asResource(securePassword);
        createObject.setId(null);
        createObject.setName("New name");
        createObject.setPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));

        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(securePasswordBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Secure Password goid:", goid.toString(), getFirstReferencedGoid(response));

        SecurePassword createdEntity = securePasswordManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Secure Password username:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(securePasswordBasePath + securePassword.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        StoredPasswordMO entityGot = (StoredPasswordMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setName("Updated name");
        RestResponse response = processRequest(securePasswordBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created secure password goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SecurePassword updatedEntity = securePasswordManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Secure Password id:", securePassword.getId(), updatedEntity.getId());
        assertEquals("Secure Password name:", securePassword.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(securePasswordBasePath + securePassword.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(securePasswordManagerStub.findByPrimaryKey(securePassword.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(securePasswordBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(securePasswordManagerStub.findAll().size(), reference.getResource().getReferences().size());
    }
}
