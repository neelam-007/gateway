package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 10/04/12
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class AmqpSupportServer {
    private static final Logger logger = Logger.getLogger(AmqpSupportServer.class.getName());

    private SecurePasswordManager securePasswordManager;
    private ApplicationContext context;
    private static AmqpSupportServer instance;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private SsgKeyStoreManager keyStoreManager;
    private DefaultKey defaultKey;
    private static ServerAMQPDestinationManager serverAMQPDestinationManager;

    public static AmqpSupportServer getInstance(final ApplicationContext context) {
        if (instance == null) {
            instance = new AmqpSupportServer(context);
        }
        return instance;
    }

    private AmqpSupportServer(final ApplicationContext _context) {
        this.init(_context);
        securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        defaultKey = context.getBean("defaultKey", DefaultKey.class);
        trustManager = context.getBean("trustManager", SslClientTrustManager.class);
        secureRandom = context.getBean("secureRandom", SecureRandom.class);
        serverAMQPDestinationManager = ServerAMQPDestinationManager.getInstance(_context);
        instance = this;
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<AmqpAdmin>(AmqpAdmin.class, null, new AmqpAdmin() {

            @Override
            public boolean testSettings(AMQPDestination destination) throws AmqpTestException {
                Connection connection = null;
                try {
                    ConnectionFactory connectionFactory = serverAMQPDestinationManager.createNewConnectionFactory();
                    if (destination.getUsername() != null) {
                        connectionFactory.setUsername(destination.getUsername());
                        try {
                            connectionFactory.setPassword(new String(securePasswordManager.decryptPassword(
                                    securePasswordManager.findByPrimaryKey(destination.getPasswordGoid()).getEncodedPassword())));
                        } catch (FindException | ParseException e) {
                            logger.log(Level.WARNING, e.getMessage(), e);
                            return false;
                        }
                    }
                    if (destination.getVirtualHost() != null) {
                        connectionFactory.setVirtualHost(destination.getVirtualHost());
                    }
                    serverAMQPDestinationManager.initSSLSettings(destination, connectionFactory);
                    Address[] addresses = serverAMQPDestinationManager.getAddresses(destination);
                    connection = connectionFactory.newConnection(addresses);
                    return true;
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                } finally {
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (IOException e) {
                            //Ignore..
                        }
                        connection = null;
                    }


                }
                return false;
            }
        });

        return Collections.singletonList(binding);
    }

    private void init(final ApplicationContext context) {
        this.context = context;
    }
}

