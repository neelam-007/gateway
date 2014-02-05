package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.transport.email.EmailListenerManagerStub;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
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
        RestResponse response = processRequest(emailListenerBasePath + emailListener.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        EmailListenerMO result = (EmailListenerMO) item.getContent();

        assertEquals("Email listener identifier:", emailListener.getId(), result.getId());
        assertEquals("Email listener name:", emailListener.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        EmailListenerMO createObject = emailListenerResourceFactory.asResource(emailListener);
        createObject.setId(null);
        createObject.setName("New email listener");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(emailListenerBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        EmailListener createdEntity = emailListenerManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Email listener name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityTestInvalidPortValueFail() throws Exception {

        final String emailListener =
                "<l7:EmailListener version=\"1\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Name>Copy of fake</l7:Name>\n" +
                "    <l7:Active>false</l7:Active>\n" +
                "    <l7:Hostname>saee</l7:Hostname>\n" +
                "    <l7:Port>143dd</l7:Port>\n" +
                "    <l7:ServerType>IMAP</l7:ServerType>\n" +
                "    <l7:UseSsl>false</l7:UseSsl>\n" +
                "    <l7:DeleteOnReceive>false</l7:DeleteOnReceive>\n" +
                "    <l7:Username>dsag</l7:Username>\n" +
                "    <l7:Folder>adsawegaweg</l7:Folder>\n" +
                "    <l7:PollInterval>60</l7:PollInterval>\n" +
                "    <l7:Properties>\n" +
                "        <l7:Property key=\"com.l7tech.server.jms.prop.hardwired.service.bool\">\n" +
                "            <l7:StringValue>false</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "    </l7:Properties>\n" +
                "</l7:EmailListener>";
        Document request = XMLUtils.parse(emailListener);
        RestResponse response = processRequest(emailListenerBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        logger.info(response.toString());
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);

        Assert.assertTrue(error.getDetail().contains("143dd"));
        Assert.assertEquals("BadRequest",error.getType());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        EmailListenerMO createObject = emailListenerResourceFactory.asResource(emailListener);
        createObject.setId(null);
        createObject.setName("New email listener");

        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(emailListenerBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Email listener goid:", goid.toString(), getFirstReferencedGoid(response));

        EmailListener createdEntity = emailListenerManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Email listener name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(emailListenerBasePath + emailListener.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        EmailListenerMO entityGot = (EmailListenerMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.setName("Updated New Email Listener");
        RestResponse response = processRequest(emailListenerBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Email listener goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        EmailListener updatedEntity = emailListenerManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Email listener id:", emailListener.getId(), updatedEntity.getId());
        assertEquals("Email listener name:", emailListener.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(emailListenerBasePath + emailListener.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(emailListenerManagerStub.findByPrimaryKey(emailListener.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(emailListenerBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<EmailListenerMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(emailListenerManagerStub.findAll().size(), item.getContent().size());
    }
}
