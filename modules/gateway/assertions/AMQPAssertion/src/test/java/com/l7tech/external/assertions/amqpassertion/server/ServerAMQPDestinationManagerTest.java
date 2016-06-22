package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpSsgActiveConnector;
import com.l7tech.external.assertions.amqpassertion.AmqpSupportAssertion;
import com.l7tech.external.assertions.amqpassertion.console.AMQPDestinationHelper;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.util.Functions;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 27/02/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
@PowerMockIgnore({"org.xml.*", "javax.xml.*"})
@RunWith(PowerMockRunner.class)
public class ServerAMQPDestinationManagerTest {
    private static final Logger log = Logger.getLogger(ServerRouteViaAMQPAssertionTest.class.getName());
    private static final String PROPERTY_KEY_ADDRESS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<java version=\"1.7.0_75\" class=\"java.beans.XMLDecoder\">\n" +
            " <array class=\"java.lang.String\" length=\"2\">\n" +
            "  <void index=\"0\">\n" +
            "   <string>localhost:1234</string>\n" +
            "  </void>\n" +
            "  <void index=\"1\">\n" +
            "   <string>localhost:5678</string>\n" +
            "  </void>\n" +
            " </array>\n" +
            "</java>\n";
    private static ApplicationContext applicationContext;
    @Mock
    private SecurePasswordManager securePasswordManager;
    @Mock
    private SsgActiveConnectorManager ssgActiveConnectorManager;
    @Mock
    private ServiceCache serviceCache;

    @Before
    public void setUp() {
        // Get the spring app context
        if (applicationContext == null) {
            applicationContext = Mockito.mock(ApplicationContext.class);
            Assert.assertNotNull("Fail - Unable to get applicationContext instance", applicationContext);
        }
        AmqpSupportAssertion assertion = new AmqpSupportAssertion();
        AssertionMetadata assertionMetadata = assertion.meta();
        Functions.Unary<ExtensionInterfaceBinding, ApplicationContext> o = assertionMetadata.get(AssertionMetadata.EXTENSION_INTERFACES_FACTORY);
        o.call(applicationContext);
        Mockito.when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).
                thenReturn(Mockito.mock(ClusterPropertyManager.class));
    }

    private void createOutboundDestination1(List<AMQPDestination> destinations) throws FindException, SaveException {
        AMQPDestination destination = new AMQPDestination();
        destination.setName("Test 1");
        destination.setInbound(false);
        destination.setVirtualHost("vhost");
        destination.setAddresses(new String[]{"localhost:1234", "localhost:5678"});
        destination.setCredentialsRequired(true);
        destination.setUsername("user1");
        destination.setPasswordGoid(addPassword("password"));
        destination.setUseSsl(false);
        destination.setExchangeName("exchange1");
        destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.ONE_WAY);

        destinations.add(destination);
    }

    private void setDestinationsClusterProperty(List<AMQPDestination> destinations) throws IOException, SaveException {
        AMQPDestination[] destinationsArray = destinations.toArray(new AMQPDestination[destinations.size()]);
        try {
            for (AMQPDestination destination : destinationsArray) {
                SsgActiveConnector ssgActiveConnector = AMQPDestinationHelper.amqpDestinationToSsgActiveConnector(destination);
                ssgActiveConnectorManager.save(ssgActiveConnector);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Goid addPassword(String password) throws FindException, SaveException {
        SecurePassword sp = new SecurePassword("dummyVal");
        sp.setEncodedPassword(securePasswordManager.encryptPassword(password.toCharArray()));
        return securePasswordManager.save(sp);
    }

    @Test
    public void activateOutboundDestination1() throws Exception {
        List<AMQPDestination> destinations = new ArrayList<AMQPDestination>();
        createOutboundDestination1(destinations);
        setDestinationsClusterProperty(destinations);

        ServerConfig config = new ServerConfigStub();
        Mockito.when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        SsgActiveConnector amqpconnector = Mockito.mock(SsgActiveConnector.class);
        Mockito.when(amqpconnector.getName()).thenReturn("AMQP");
        // to match the default GOID when a new AMQPDestination is created
        Mockito.when(amqpconnector.getGoid()).thenReturn(new Goid(0, -1));
        Mockito.when(amqpconnector.getType()).thenReturn(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
        Mockito.when(amqpconnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES)).thenReturn(PROPERTY_KEY_ADDRESS);
        Collection<SsgActiveConnector> connectorList = new ArrayList<>();
        connectorList.add(amqpconnector);
        Mockito.when(ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(connectorList);
        Mockito.when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);

        ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        ServerAMQPDestinationManager spyManager = Mockito.spy(manager);

        AmqpSupportServer amqpSupportServer = Mockito.mock(AmqpSupportServer.class);
        Channel amqpDestinationChannel = Mockito.mock(Channel.class);
        ConnectionFactory cf = Mockito.mock(ConnectionFactory.class);
        Mockito.doReturn(cf).when(spyManager).createNewConnectionFactory();
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.createChannel()).thenReturn(amqpDestinationChannel);
        Mockito.when(cf.newConnection(Mockito.any(Address[].class))).thenReturn(connection);

//        Mockito.when(amqpSupportServer.activateProducerDestination(Mockito.any(AMQPDestination.class))).thenReturn(amqpDestinationChannel);

        spyManager.loadClusterProperties();
        spyManager.loadDestinations();

        Assert.assertEquals(1, spyManager.destinations.size());
        Assert.assertTrue(spyManager.destinations.containsKey(destinations.get(0).getGoid()));
        Assert.assertEquals(0, spyManager.failedProducers.size());
        Assert.assertTrue(spyManager.clientChannels.containsKey(destinations.get(0).getGoid()));

        Channel channel = spyManager.clientChannels.get(destinations.get(0).getGoid()).getClientChannel();
        Assert.assertNotNull(channel);
    }

    @Test
    public void testQueueMessage() throws Exception {
        List<AMQPDestination> destinations = new ArrayList<AMQPDestination>();
        createOutboundDestination1(destinations);
        setDestinationsClusterProperty(destinations);

        ServerConfig config = new ServerConfigStub();
        Mockito.when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        SsgActiveConnector amqpconnector = Mockito.mock(SsgActiveConnector.class);
        Mockito.when(amqpconnector.getName()).thenReturn("AMQP");
        // to match the default GOID when a new AMQPDestination is created
        Mockito.when(amqpconnector.getGoid()).thenReturn(new Goid(0, -1));
        Mockito.when(amqpconnector.getType()).thenReturn(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
        Mockito.when(amqpconnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES)).thenReturn(PROPERTY_KEY_ADDRESS);
        Collection<SsgActiveConnector> connectorList = new ArrayList<>();
        connectorList.add(amqpconnector);
        Mockito.when(ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(connectorList);
        Mockito.when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);

        ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        ServerAMQPDestinationManager spyManager = Mockito.spy(manager);

        Channel amqpDestinationChannel = Mockito.mock(Channel.class);
        ConnectionFactory cf = Mockito.mock(ConnectionFactory.class);
        Mockito.doReturn(cf).when(spyManager).createNewConnectionFactory();
        Connection connection = Mockito.mock(Connection.class);

        Mockito.when(connection.createChannel()).thenReturn(amqpDestinationChannel);
        Mockito.when(cf.newConnection(Mockito.any(Address[].class))).thenReturn(connection);


        spyManager.loadClusterProperties();
        spyManager.loadDestinations();

        Assert.assertEquals(1, spyManager.destinations.size());
        Assert.assertTrue(spyManager.destinations.containsKey(destinations.get(0).getGoid()));
        Assert.assertEquals(0, spyManager.failedProducers.size());
        Assert.assertTrue(spyManager.clientChannels.containsKey(destinations.get(0).getGoid()));

        HashMap amqpProperties = new HashMap();
        String routingKey = "key";
        Message request = new Message();
        Message response = new Message();
        spyManager.queueMessage(destinations.get(0).getGoid(), routingKey, request, response, amqpProperties);
    }
}
