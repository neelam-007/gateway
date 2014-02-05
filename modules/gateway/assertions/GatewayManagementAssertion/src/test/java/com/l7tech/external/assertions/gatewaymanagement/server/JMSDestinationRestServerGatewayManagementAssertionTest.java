package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.transport.jms.JmsConnectionManagerStub;
import com.l7tech.server.transport.jms.JmsEndpointManagerStub;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class JMSDestinationRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(JMSDestinationRestServerGatewayManagementAssertionTest.class.getName());

    private static final JmsConnection jmsConnection = new JmsConnection();
    private static final JmsEndpoint jmsEndpoint = new JmsEndpoint();
    private static JmsEndpointManagerStub jmsEndpointManagerStub;
    private static JmsConnectionManagerStub jmsConnectionManagerStub;
    private static final String jmsDestinationBasePath = "jmsDestinations/";

    @InjectMocks
    protected JMSDestinationResourceFactory jmsDestinationResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        jmsEndpointManagerStub = applicationContext.getBean("jmsEndpointManager", JmsEndpointManagerStub.class);
        jmsConnectionManagerStub = applicationContext.getBean("jmsConnectionManager", JmsConnectionManagerStub.class);

        jmsConnection(jmsConnection,  "Test Endpoint", "com.context.Classname", "qcf", "ldap://jndi", null);
        jmsConnectionManagerStub.save(jmsConnection);
        jmsEndpoint( jmsEndpoint, jmsConnection.getGoid(), "Test Endpoint");
        jmsEndpointManagerStub.save(jmsEndpoint);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<JmsEndpointHeader> endpoints = new ArrayList<>(jmsEndpointManagerStub.findAllHeaders());
        for (EntityHeader endpoint : endpoints) {
            jmsEndpointManagerStub.delete(endpoint.getGoid());
        }
        Collection<EntityHeader> connections = new ArrayList<>(jmsConnectionManagerStub.findAllHeaders());
        for (EntityHeader connection : connections) {
            jmsConnectionManagerStub.delete(connection.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    private static void jmsConnection(final JmsConnection connection, final String name, final String contextClassname, final String queueFactory, final String jndiUrl, JmsProviderType providerType) {
        connection.setName( name );
        connection.setQueueFactoryUrl( queueFactory );
        connection.setInitialContextFactoryClassname( contextClassname );
        connection.setJndiUrl( jndiUrl );
        connection.setProviderType(providerType);
        connection.setUsername("user");
        connection.setPassword("password");
        Properties connectionProperties = connection.properties();
        connectionProperties.setProperty("java.naming.security.credentials","jndi-password");
        connectionProperties.setProperty("com.l7tech.server.jms.prop.hardwired.service.bool","false");
        connection.properties(connectionProperties);
    }

    private static void jmsEndpoint( final JmsEndpoint endpoint, final Goid connectionGoid, final String queueName) {
        endpoint.setConnectionGoid(connectionGoid);
        endpoint.setName( queueName );
        endpoint.setDestinationName( queueName );
        endpoint.setUsername("user");
        endpoint.setPassword("password");
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(jmsDestinationBasePath + jmsEndpoint.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        JMSDestinationMO result = (JMSDestinationMO) item.getContent();

        assertEquals("JMS Destination identifier:", jmsEndpoint.getId(), result.getId());
        assertEquals("JMS Destination name:", jmsEndpoint.getName(), result.getJmsDestinationDetail().getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        JMSDestinationMO createObject = jmsDestinationResourceFactory.asResource(new JMSDestinationResourceFactory.JmsEntityBag( jmsEndpoint, jmsConnection ));
        createObject.setId(null);
        createObject.getJmsDestinationDetail().setId(null);
        createObject.getJmsDestinationDetail().setName("New jms name");
        createObject.getJmsConnection().setId(null);
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(jmsDestinationBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        JmsEndpoint createdEntity = jmsEndpointManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("JMS Destination name:", createObject.getJmsDestinationDetail().getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        JMSDestinationMO createObject = jmsDestinationResourceFactory.asResource(new JMSDestinationResourceFactory.JmsEntityBag( jmsEndpoint, jmsConnection ));
        createObject.setId(null);
        createObject.getJmsDestinationDetail().setId(null);
        createObject.getJmsDestinationDetail().setName("New jms name");
        createObject.getJmsConnection().setId(null);

        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(jmsDestinationBasePath + goid.toString(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created JMS Destination goid:", goid.toString(), getFirstReferencedGoid(response));

        JmsEndpoint createdEntity = jmsEndpointManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("JMS Destination name:", createObject.getJmsDestinationDetail().getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(jmsDestinationBasePath + jmsEndpoint.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        JMSDestinationMO entityGot = (JMSDestinationMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.getJmsDestinationDetail().setName("Updated New jms");
        RestResponse response = processRequest(jmsDestinationBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created JMS Destination goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        JmsEndpoint updatedEntity = jmsEndpointManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("JMS Destination id:", jmsEndpoint.getId(), updatedEntity.getId());
        assertEquals("JMS Destination name:", jmsEndpoint.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(jmsDestinationBasePath + jmsEndpoint.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(jmsEndpointManagerStub.findByPrimaryKey(jmsEndpoint.getGoid()));
        Assert.assertNull(jmsConnectionManagerStub.findByPrimaryKey(jmsConnection.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(jmsDestinationBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<JMSDestinationMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(jmsEndpointManagerStub.findAll().size(), item.getContent().size());
    }
}
