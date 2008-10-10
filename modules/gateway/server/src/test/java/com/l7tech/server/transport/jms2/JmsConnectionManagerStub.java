/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms2;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsProvider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * New Sutbbed JmsConnectionManager for Jms-subsystem unit tests.
 *
 * @author: vchan
 */
public class JmsConnectionManagerStub extends EntityManagerStub<JmsConnection, EntityHeader> implements JmsConnectionManager {


    public static final int TEST_CONFIG_AMQ_IN  = 1000;
    public static final int TEST_CONFIG_AMQ_OUT = 1001;
    public static final int TEST_CONFIG_MQS_IN  = 1002;
    public static final int TEST_CONFIG_MQS_OUT = 1003;
    public static final int TEST_CONFIG_FMQ_IN  = 1004;
    public static final int TEST_CONFIG_FMQ_OUT = 1005;

    private static int TEST_CONFIG = TEST_CONFIG_AMQ_IN;

    public static void setTestConfig(int which) {
        TEST_CONFIG = which;
    }

    // Configuration for apache ActiveMQ
    protected static final String QPROVIDER_AMQ = "activeMQ";
    protected static final String AMQ_INITIAL_CONTEXT_FACTORY_CLASS = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    protected static final String AMQ_DEFAULT_QUEUE_FACTORY_URL = "QueueConnectionFactory";
    protected static final String AMQ_QUEUE_DEFAULT = "dynamicQueues/JMS.JUNIT.Q";
    protected static final String AMQ_JNDI_URL = "tcp://localhost:61616";

    // Configuration for apache MQSeries over LDAP
    protected static final String QPROVIDER_MQS = "MQSeries";
    protected static final String MQS_INITIAL_CONTEXT_FACTORY_CLASS = "com.sun.jndi.ldap.LdapCtxFactory";
    protected static final String MQS_DEFAULT_QUEUE_FACTORY_URL = "cn=vcQueueConnectionFactory";
    protected static final String MQS_QUEUE_DEFAULT = "cn=VCTEST.Q.IN";
    protected static final String MQS_JNDI_URL = "ldap://soran.l7tech.com/dc=layer7-tech,dc=com";
    protected static final String MQS_CONN_PROPERTIES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.customizer.class\">com.l7tech.server.transport.jms.prov.MQSeriesCustomizer</entry>\n" +
            "<entry key=\"java.naming.security.principal\"/>\n" +
            "<entry key=\"java.naming.security.credentials\"/>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.queue.useClientAuth\">false</entry>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.hardwired.service.bool\">false</entry>\n" +
            "</properties>";

    // Configuration for apache Fiorano MQ
    protected static final String QPROVIDER_FMQ = "fioranoMQ";
    protected static final String FMQ_INITIAL_CONTEXT_FACTORY_CLASS = "fiorano.jms.runtime.naming.FioranoInitialContextFactory";
    protected static final String FMQ_DEFAULT_QUEUE_FACTORY_URL = "primaryJMXQCF";
    protected static final String FMQ_QUEUE_DEFAULT = "vchan_in";
    protected static final String FMQ_JNDI_URL = "http://fioranowindows:1856";
    protected static final String FMQ_CONN_PROPERTIES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.customizer.class\">com.l7tech.server.transport.jms.prov.FioranoConnectionFactoryCustomizer</entry>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.queue.ssgKeyAlias\">SSL</entry>\n" +
            "<entry key=\"java.naming.security.protocol\">SUN_SSL</entry>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.queue.useClientAuth\">true</entry>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.hardwired.service.bool\">false</entry>\n" +
            "<entry key=\"SecurityManager\">com.l7tech.server.transport.jms.prov.fiorano.proxy.FioranoProxySecurityManager</entry>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.queue.ssgKeystoreId\">0</entry>\n" +
            "</properties>";

    protected String DEFAULT_QUEUE_PROVIDER = QPROVIDER_AMQ;

    protected String queueProvider;

    protected JmsProvider provider;

    public JmsConnectionManagerStub() {
        createTestConnections(1);
    }

    @Override
    public Collection<JmsProvider> findAllProviders() throws FindException {
        return createTestConnections(1);
    }

    public String getQueueProvider() {
        String result;

//        if (queueProvider == null) {
            if (TEST_CONFIG == TEST_CONFIG_AMQ_IN || TEST_CONFIG == TEST_CONFIG_AMQ_OUT)
                result = QPROVIDER_AMQ;
            else if (TEST_CONFIG == TEST_CONFIG_MQS_IN || TEST_CONFIG == TEST_CONFIG_MQS_OUT)
                result = QPROVIDER_MQS;
            else if (TEST_CONFIG == TEST_CONFIG_FMQ_IN || TEST_CONFIG == TEST_CONFIG_FMQ_OUT)
                result = QPROVIDER_FMQ;
            else
                result = DEFAULT_QUEUE_PROVIDER;
//        }

        return result;
    }

    public void setQueueProvider(String queueProvider) {
        this.queueProvider = queueProvider;
    }


    public Collection<JmsConnection> findAll() throws FindException {

        Collection<JmsConnection> connList = new ArrayList<JmsConnection>();

        JmsConnection conn = getConnection(TEST_CONFIG);
        connList.add(conn);
        return connList;
    }

    public JmsConnection findByPrimaryKey(long oid) throws FindException {

        return getConnection((int) oid);
    }

    protected Collection<JmsProvider> createTestConnections(int count) {

        Collection<JmsProvider> list = new ArrayList<JmsProvider>();

        String qProv = getQueueProvider();
        if (provider == null && QPROVIDER_AMQ.equals(qProv)) {
            provider = new JmsProvider("TestJmsProvider", AMQ_INITIAL_CONTEXT_FACTORY_CLASS, AMQ_DEFAULT_QUEUE_FACTORY_URL);
        }
        else if (provider == null && QPROVIDER_MQS.equals(qProv)) {
            provider = new JmsProvider("TestJmsProvider", MQS_INITIAL_CONTEXT_FACTORY_CLASS, MQS_DEFAULT_QUEUE_FACTORY_URL);
        }
        else if (provider == null && QPROVIDER_FMQ.equals(qProv)) {
            provider = new JmsProvider("TestJmsProvider", FMQ_INITIAL_CONTEXT_FACTORY_CLASS, FMQ_DEFAULT_QUEUE_FACTORY_URL);
        }

        list.add(provider);
        return list;
    }


    private JmsConnection getConnection(int which) {
        JmsConnection conn;
        switch(which) {

            case TEST_CONFIG_AMQ_IN: {
                conn = provider.createConnection("dynamicQueues/JMS.JUNIT.IN.Q", AMQ_JNDI_URL);
                conn.setOid(TEST_CONFIG_AMQ_IN);
                break;
            }
            case TEST_CONFIG_AMQ_OUT: {
                conn = provider.createConnection("dynamicQueues/JMS.JUNIT.OUT.Q", AMQ_JNDI_URL);
                conn.setOid(TEST_CONFIG_AMQ_OUT);
                break;
            }
            case TEST_CONFIG_MQS_IN: {
                conn = provider.createConnection("cn=VCTEST.Q.IN", MQS_JNDI_URL);
                conn.setOid(TEST_CONFIG_MQS_IN);
//                conn.setProperties(MQS_CONN_PROPERTIES); // for ssl
                break;
            }
            case TEST_CONFIG_MQS_OUT: {
                conn = provider.createConnection("cn=VCTEST.Q.OUT", MQS_JNDI_URL);
                conn.setOid(TEST_CONFIG_MQS_OUT);
//                conn.setProperties(MQS_CONN_PROPERTIES);
                break;
            }
            case TEST_CONFIG_FMQ_IN: {
                conn = provider.createConnection("vchan_in", FMQ_JNDI_URL);
                conn.setOid(TEST_CONFIG_FMQ_IN);
//                conn.setProperties(FMQ_CONN_PROPERTIES); // for ssl
                break;
            }
            case TEST_CONFIG_FMQ_OUT: {
                conn = provider.createConnection("vchan_out", FMQ_JNDI_URL);
                conn.setOid(TEST_CONFIG_FMQ_OUT);
//                conn.setProperties(FMQ_CONN_PROPERTIES);
                break;
            }
            default: {
                conn = provider.createConnection(AMQ_QUEUE_DEFAULT, AMQ_JNDI_URL);
                conn.setOid(6666);
                break;
            }
        }
        return conn;
    }
}