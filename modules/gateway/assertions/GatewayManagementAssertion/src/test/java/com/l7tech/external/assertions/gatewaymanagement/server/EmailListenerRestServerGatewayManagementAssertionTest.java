package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.transport.email.EmailListenerManagerStub;
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
public class EmailListenerRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(EmailListenerRestServerGatewayManagementAssertionTest.class.getName());

    private static final EmailListener emailListener = new EmailListener();
    private static EmailListenerManagerStub emailListenerManagerStub;
    private static final String emailListenerBasePath = "emailListeners/";

    @InjectMocks
    protected EmailListenerResourceFactory emailListenerResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        emailListenerManagerStub = applicationContext.getBean("emailListenerManager", EmailListenerManagerStub.class);
        emailListener.setGoid(new Goid(0, 1234L));
        emailListener.setName("Test Email Listener");
        emailListener.setHost("host");
        emailListener.setPort(1234);
        emailListener.setUsername("user");
        emailListener.setPassword("password");
        emailListener.setFolder("folder");
        emailListener.setServerType(EmailServerType.POP3);

        emailListenerManagerStub.save(emailListener);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(emailListenerManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            emailListenerManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(emailListenerBasePath + emailListener.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        EmailListenerMO result = ManagedObjectFactory.read(response.getBody(), EmailListenerMO.class);

        assertEquals("Email listener identifier:", emailListener.getId(), result.getId());
        assertEquals("Email listener name:", emailListener.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        EmailListenerMO createObject = emailListenerResourceFactory.asResource(emailListener);
        createObject.setId(null);
        createObject.setName("New email listener");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(emailListenerBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        EmailListener createdEntity = emailListenerManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Email listener name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        EmailListenerMO createObject = emailListenerResourceFactory.asResource(emailListener);
        createObject.setId(null);
        createObject.setName("New email listener");

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(emailListenerBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Email listener goid:", goid.toString(), getFirstReferencedGoid(response));

        EmailListener createdEntity = emailListenerManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Email listener name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(emailListenerBasePath + emailListener.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        EmailListenerMO entityGot = MarshallingUtils.unmarshal(EmailListenerMO.class, source);

        // update
        entityGot.setName("Updated New Email Listener");
        Response response = processRequest(emailListenerBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Email listener goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        EmailListener updatedEntity = emailListenerManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Email listener id:", emailListener.getId(), updatedEntity.getId());
        assertEquals("Email listener name:", emailListener.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(emailListenerBasePath + emailListener.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(emailListenerManagerStub.findByPrimaryKey(emailListener.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(emailListenerBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References references = MarshallingUtils.unmarshal(References.class, source);

        // check entity
        Assert.assertEquals(emailListenerManagerStub.findAll().size(), references.getReferences().size());
    }
}
