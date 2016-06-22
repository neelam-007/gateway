package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpSsgActiveConnector;
import com.l7tech.external.assertions.amqpassertion.console.AMQPDestinationHelper;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 20/03/12
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class AmqpDestinationHelperTest {
    private static final Logger log = Logger.getLogger(AmqpDestinationHelperTest.class.getName());
    private static ApplicationContext applicationContext;
    @Mock
    private SecurePasswordManager securePasswordManager;

    @Before
    public void setUp() {
        // Get the spring app context
        if (applicationContext == null) {
            applicationContext = Mockito.mock(ApplicationContext.class);

            Assert.assertNotNull("Fail - Unable to get applicationContext instance", applicationContext);
        }
    }

    private Goid addPassword(String password) throws FindException, SaveException {
        SecurePassword sp = new SecurePassword("dummyVal");
        sp.setEncodedPassword(securePasswordManager.encryptPassword(password.toCharArray()));
        return securePasswordManager.save(sp);
    }

    @Test
    public void testAMQPDestinationsToSsgConnector() throws FindException, SaveException {
        AMQPDestination destination = new AMQPDestination();
        destination.setName("Test 1");
        destination.setInbound(false);
        destination.setVirtualHost("vhost");
        destination.setAddresses(new String[]{"host1:1234", "host2:5678"});
        destination.setCredentialsRequired(true);
        destination.setUsername("user1");
        destination.setPasswordGoid(addPassword("password"));
        destination.setUseSsl(false);
        destination.setExchangeName("exchange1");
        destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.ONE_WAY);
        SsgActiveConnector ssgConnector = AMQPDestinationHelper.amqpDestinationToSsgActiveConnector(destination);
        Assert.assertEquals("Test 1", ssgConnector.getName());
        Assert.assertEquals("exchange1", ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME));
        Assert.assertEquals("user1", ssgConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USERNAME));
    }

    @Test
    public void testSsgConnectorToAMQPDestination() {
        SsgActiveConnector ssgActiveConnector = new SsgActiveConnector();
        ssgActiveConnector.setName("Test 1");
        ssgActiveConnector.setType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
        String[] addresses = new String[]{"host1:1234", "host2:5678"};
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(bos);
        enc.writeObject(addresses);
        enc.close();
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES, bos.toString());
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTIES_KEY_IS_INBOUND, "false");
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_VIRTUALHOST, "vhost");
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CREDENTIALS_REQUIRED, "true");
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USERNAME, "user1");
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME, "exchange1");
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USESSL, "false");
        ssgActiveConnector.setProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR, "ONE_WAY");
        AMQPDestination destination = AMQPDestinationHelper.ssgConnectorToAmqpDestination(ssgActiveConnector);
        Assert.assertEquals("Test 1", destination.getName());
        Assert.assertFalse(destination.isInbound());
        Assert.assertEquals("vhost", destination.getVirtualHost());
        Assert.assertEquals("user1", destination.getUsername());
        Assert.assertEquals("exchange1", destination.getExchangeName());
        Assert.assertFalse(destination.isUseSsl());
        Assert.assertEquals(AMQPDestination.OutboundReplyBehaviour.ONE_WAY, destination.getOutboundReplyBehaviour());
        String[] str = destination.getAddresses();
        Assert.assertEquals(2, str.length);
        Assert.assertEquals("host1:1234", str[0]);
        Assert.assertEquals("host2:5678", str[1]);
    }
}
