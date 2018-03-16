package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpSsgActiveConnector;
import com.l7tech.external.assertions.amqpassertion.AmqpSupportAssertion;
import com.l7tech.external.assertions.amqpassertion.console.AMQPDestinationHelper;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
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
import com.l7tech.test.BugId;
import com.l7tech.util.Functions;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 27/02/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
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
            applicationContext = mock(ApplicationContext.class);
            Assert.assertNotNull("Fail - Unable to get applicationContext instance", applicationContext);
        }
        AmqpSupportAssertion assertion = new AmqpSupportAssertion();
        AssertionMetadata assertionMetadata = assertion.meta();
        Functions.Unary<ExtensionInterfaceBinding, ApplicationContext> o = assertionMetadata.get(AssertionMetadata.EXTENSION_INTERFACES_FACTORY);
        o.call(applicationContext);
        when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).
                thenReturn(mock(ClusterPropertyManager.class));
    }

    private void createDestination(List<AMQPDestination> destinations, boolean isInbound) throws FindException, SaveException {
        AMQPDestination destination = new AMQPDestination();
        destination.setName("Test 1");
        destination.setInbound(isInbound);
        destination.setVirtualHost("vhost");
        destination.setAddresses(new String[]{"localhost:1234", "localhost:5678"});
        destination.setCredentialsRequired(true);
        destination.setUsername("user1");
        destination.setPasswordGoid(addPassword("password"));
        destination.setUseSsl(false);
        destination.setExchangeName("exchange1");
        if (isInbound) {
            destination.setQueueName("testQueue");
        }
        else {
            destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.ONE_WAY);
        }

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
        createDestination(destinations, false);
        setDestinationsClusterProperty(destinations);

        ServerConfig config = new ServerConfigStub();
        when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        SsgActiveConnector amqpconnector = prepareAmqpConnector();
        Collection<SsgActiveConnector> connectorList = new ArrayList<>();
        connectorList.add(amqpconnector);
        when(ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(connectorList);
        when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);

        ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        ServerAMQPDestinationManager spyManager = Mockito.spy(manager);

        ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
                .whenNewOutBoundConnectionReturn(new ConnectionBuilder().whenCreateChannelReturn(mock(Channel.class)).build())
                .build();
        Mockito.doReturn(connectionFactory).when(spyManager).createNewConnectionFactory();

        spyManager.loadClusterProperties();
        spyManager.loadDestinations();

        Assert.assertEquals(1, spyManager.destinations.size());
        assertTrue(spyManager.destinations.containsKey(destinations.get(0).getGoid()));
        Assert.assertEquals(0, spyManager.failedProducers.size());
        assertTrue(spyManager.clientChannels.containsKey(destinations.get(0).getGoid()));

        Channel channel = spyManager.clientChannels.get(destinations.get(0).getGoid()).getClientChannel();
        Assert.assertNotNull(channel);
    }

    @BugId("DE350748")
    @Test
    public void testActivateInboundDestinationSuccess() throws Exception {
        final List<AMQPDestination> destinations = new ArrayList<AMQPDestination>();
        createDestination(destinations, true);
        setDestinationsClusterProperty(destinations);

        final ServerConfig config = new ServerConfigStub();
        when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        SsgActiveConnector amqpconnector = prepareAmqpInboundConnector();

        final Collection<SsgActiveConnector> connectorList = new ArrayList<>();
        connectorList.add(amqpconnector);
        when(ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(connectorList);
        when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);
        prepareServiceForInboundTest();

        final ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        final ServerAMQPDestinationManager spyManager = Mockito.spy(manager);
        final ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
                .whenNewInboundConnectionReturn(new ConnectionBuilder().whenCreateChannelReturn(mock(Channel.class)).build())
                .build();
        Mockito.doReturn(connectionFactory).when(spyManager).createNewConnectionFactory();

        spyManager.loadClusterProperties();
        spyManager.loadDestinations();

        Assert.assertEquals(1, spyManager.destinations.size());
        assertTrue(spyManager.destinations.containsKey(destinations.get(0).getGoid()));
        Assert.assertEquals(0, spyManager.failedConsumers.size());
        assertTrue(spyManager.serverChannels.containsKey(destinations.get(0).getGoid()));

        final Channel channel = spyManager.serverChannels.get(destinations.get(0).getGoid());
        Assert.assertNotNull(channel);
        Mockito.verify(spyManager, times(1)).createNewConnectionFactory();

        // DE350748: Connections are always regenerated when reloading the consumer destinations.
        // Simulate reconnecting with the same AMQP destination
        spyManager.loadDestinations();
        // verify that a connection is not created again (ie: createNewConnectionFactory is not called a second time)
        Mockito.verify(spyManager, times(1)).createNewConnectionFactory();
    }

    @BugId("DE350748")
    @Test
    public void testInboundChannelClosedOnError() throws Exception {
        final List<AMQPDestination> destinations = new ArrayList<AMQPDestination>();
        createDestination(destinations, true);
        setDestinationsClusterProperty(destinations);

        final ServerConfig config = new ServerConfigStub();
        when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        final SsgActiveConnector amqpconnector = prepareAmqpInboundConnector();

        final Collection<SsgActiveConnector> connectorList = new ArrayList<>();
        connectorList.add(amqpconnector);
        when(ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(connectorList);
        when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);
        prepareServiceForInboundTest();

        final ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        final ServerAMQPDestinationManager spyManager = Mockito.spy(manager);

        final Channel channel = mock(Channel.class);
        // Simulate channel error
        when(channel.basicConsume(anyString(), anyBoolean(), any(AMQPConsumer.class))).thenThrow(new IOException("mock channel error"));

        final Connection connection = mock(Connection.class);
        when(channel.getConnection()).thenReturn(connection);

        ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
                .whenNewInboundConnectionReturn(new ConnectionBuilder().whenCreateChannelReturn(channel).build())
                .build();
        Mockito.doReturn(connectionFactory).when(spyManager).createNewConnectionFactory();
        spyManager.loadClusterProperties();
        spyManager.loadDestinations();

        // Verify that the channels are closed if an error occurred while trying to consume from the queue
        Mockito.verify(channel, times(1)).close();
        Mockito.verify(connection, times(1)).close();
    }

    @Test
    public void testQueueMessage() throws Exception {
        List<AMQPDestination> destinations = new ArrayList<AMQPDestination>();
        createDestination(destinations, false);
        setDestinationsClusterProperty(destinations);

        ServerConfig config = new ServerConfigStub();
        when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        final SsgActiveConnector amqpconnector = prepareAmqpConnector();
        Collection<SsgActiveConnector> connectorList = new ArrayList<>();
        connectorList.add(amqpconnector);
        when(ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(connectorList);
        when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);

        ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        ServerAMQPDestinationManager spyManager = Mockito.spy(manager);

        ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
                .whenNewOutBoundConnectionReturn(new ConnectionBuilder().whenCreateChannelReturn(mock(Channel.class)).build())
                .build();
        Mockito.doReturn(connectionFactory).when(spyManager).createNewConnectionFactory();

        spyManager.loadClusterProperties();
        spyManager.loadDestinations();

        Assert.assertEquals(1, spyManager.destinations.size());
        assertTrue(spyManager.destinations.containsKey(destinations.get(0).getGoid()));
        Assert.assertEquals(0, spyManager.failedProducers.size());
        assertTrue(spyManager.clientChannels.containsKey(destinations.get(0).getGoid()));

        HashMap amqpProperties = new HashMap();
        String routingKey = "key";
        Message request = new Message();
        Message response = new Message();
        spyManager.queueMessage(destinations.get(0).getGoid(), routingKey, request, response, amqpProperties);
    }

    @Test
    public void testTlsProtocolSelection() throws Exception {
        final ServerConfig config = new ServerConfigStub();
        when(applicationContext.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class)).
                thenReturn(ssgActiveConnectorManager);
        when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);

        final ServerAMQPDestinationManager manager = new ServerAMQPDestinationManager(config, applicationContext);
        final SSLContext[] sslContextHolder = new SSLContext[1];
        final ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
                .trapUsesSslProtocolArgument(sslContextHolder)
                .build();
        final String[] supportedSslProtocols = new String[] {AMQPDestination.PROCOCOL_TLS1, AMQPDestination.PROTOCOL_TLS12};
        final AMQPDestination destination = new DestinationFactory()
                .withIsUseSsl(true).withSslProtocols(supportedSslProtocols)
                .build();

        manager.initSSLSettings(destination, connectionFactory);

        final SSLContext sslContext = sslContextHolder[0];
        final SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        final List<String> supportedList = Arrays.asList(supportedSslProtocols);
        final List<String> returnedList = Arrays.asList(sslSocket.getEnabledProtocols());

        final List<String> supportedButNotReturned = supportedList.stream().filter(new Predicate<String>() {
            @Override
            public boolean test(String i) {
                return !returnedList.contains(i);
            }
        })
                .collect(Collectors.toList());
        assertTrue("Protocols were supposed to be supported but are not: "
                + Arrays.toString(supportedButNotReturned.toArray()),
                supportedButNotReturned.isEmpty());

        final List<String> returnedButNotSupported = returnedList.stream().filter(new Predicate<String>() {
            @Override
            public boolean test(String i) {
                return !supportedList.contains(i);
            }
        })
                .collect(Collectors.toList());
        assertTrue("Protocols were not supposed to be supported but are: "
                        + Arrays.toString(returnedButNotSupported.toArray()),
                returnedButNotSupported.isEmpty());
    }

    private void prepareServiceForInboundTest() {
        final PublishedService service = mock(PublishedService.class);
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isDisabled()).thenReturn(false);
    }

    @NotNull
    private SsgActiveConnector prepareAmqpInboundConnector() {
        final SsgActiveConnector amqpconnector = prepareAmqpConnector();
        when(amqpconnector.getProperty(AmqpSsgActiveConnector.PROPERTIES_KEY_IS_INBOUND)).thenReturn("true");
        when(amqpconnector.isEnabled()).thenReturn(true);
        return amqpconnector;
    }

    @NotNull
    private SsgActiveConnector prepareAmqpConnector() {
        final SsgActiveConnector amqpconnector = mock(SsgActiveConnector.class);
        when(amqpconnector.getName()).thenReturn("AMQP");
        // to match the default GOID when a new AMQPDestination is created
        when(amqpconnector.getGoid()).thenReturn(new Goid(0, -1));
        when(amqpconnector.getType()).thenReturn(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
        when(amqpconnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES)).thenReturn(PROPERTY_KEY_ADDRESS);
        return amqpconnector;
    }

}

class ConnectionFactoryBuilder {
    private final ConnectionFactory connectionFactory;

    ConnectionFactoryBuilder() {
        connectionFactory = mock(ConnectionFactory.class);
    }

    ConnectionFactoryBuilder whenNewOutBoundConnectionReturn(final Connection connection) throws Exception {
        when(connectionFactory.newConnection(any(Address[].class))).thenReturn(connection);
        return this;
    }

    ConnectionFactoryBuilder whenNewInboundConnectionReturn(final Connection connection) throws Exception {
        when(connectionFactory.newConnection(any(ExecutorService.class), any(Address[].class))).thenReturn(connection);
        return this;
    }

    ConnectionFactoryBuilder trapUsesSslProtocolArgument(SSLContext[] contextHolder) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return contextHolder[0] = (SSLContext) invocationOnMock.getArguments()[0];
            }
        }).
                when(connectionFactory).useSslProtocol(any(SSLContext.class));
        return this;
    }

    ConnectionFactory build() {
        return connectionFactory;
    }
}

class ConnectionBuilder {
    private final Connection connection;

    ConnectionBuilder() {
        this.connection = mock(Connection.class);
    }

    ConnectionBuilder whenCreateChannelReturn(Channel channel) throws Exception {
        when(connection.createChannel()).thenReturn(channel);
        return this;
    }

    Connection build() {
        return connection;
    }
}

class DestinationFactory {
    private final AMQPDestination destination;

    DestinationFactory() {
        destination = mock(AMQPDestination.class);
    }

    DestinationFactory withIsUseSsl(boolean useSsl) {
        when(destination.isUseSsl()).thenReturn(useSsl);
        return this;
    }

    public DestinationFactory withSslProtocols(String[] supportedSslProtocols) {
        when(destination.getTlsProtocols()).thenReturn(supportedSslProtocols);
        return this;
    }

    AMQPDestination build() {
        return destination;
    }
}