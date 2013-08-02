/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsProvider;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.GoidEntityManagerStub;
import com.l7tech.server.transport.jms.JmsConnectionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

/**
 * New Stubbed JmsConnectionManager for Jms-subsystem unit tests.
 *
 * @author: vchan
 */
public class JmsConnectionManagerStub extends GoidEntityManagerStub<JmsConnection, EntityHeader> implements JmsConnectionManager {

    public static final int TEST_CONFIG_AMQ_IN  = 1000;
    public static final int TEST_CONFIG_AMQ_OUT = 1001;
    public static final int TEST_CONFIG_MQS_IN  = 1002;
    public static final int TEST_CONFIG_MQS_OUT = 1003;

    /*
     * new stuff for dynamic jms routing
     * (i.e. routing destination defined/derived dynamically during policy execution)
     */
    public static final int TEST_CONFIG_DYNAMIC_IN  = 1666;
    public static final int TEST_CONFIG_DYNAMIC_OUT = 2666;

    /*
     * Set this to the preferred Test Config
     */
    private static int TEST_CONFIG = TEST_CONFIG_AMQ_IN;

    /*
     * Set this to the exected JMS Provider to be used
     * - ActiveMQ
     * - WebSphere MQ
     * - Tibco EMS
     * - Dynamic (new)
     */
    protected String DEFAULT_QUEUE_PROVIDER = QPROVIDER_DYNAMIC;

    public static void setTestConfig(int which) {
        TEST_CONFIG = which;
    }

    // Configuration for apache ActiveMQ
    protected static final String QPROVIDER_AMQ = "activeMQ";
    protected static final String AMQ_INITIAL_CONTEXT_FACTORY_CLASS = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    protected static final String AMQ_DEFAULT_QUEUE_FACTORY_URL = "QueueConnectionFactory";
    protected static final String AMQ_QUEUE_DEFAULT = "dynamicQueues/JMS.JUNIT.Q";
    protected static final String AMQ_JNDI_URL = "tcp://localhost:61616";

    // Configuration for MQSeries over LDAP
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

    // Configuration for Dynamic JMS routing (using Tibco EMS - can be anything else)
    protected static final String QPROVIDER_DYNAMIC = "tibDynamicMQ";
    protected static final String DYNAMIC_INITIAL_CONTEXT_FACTORY_CLASS = "com.tibco.tibjms.naming.TibjmsInitialContextFactory";
    protected static final String DYNAMIC_DEFAULT_QUEUE_FACTORY_URL = "ilonaQCF"; // ilonaQCF || ${jmsQCF}
    protected static final String DYNAMIC_QUEUE_DEFAULT = "ilona.in1"; // ilona.in1 || ${jmsQ}
    protected static final String DYNAMIC_JNDI_URL = "tibjmsnaming://qatibcomq:7222";
    protected static final String DYNAMIC_CONN_PROPERTIES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.hardwired.service.bool\">false</entry>\n" +
            "<entry key=\"com.tibco.tibjms.ssl.enable_verify_host\">com.l7tech.server.jms.prop.boolean.false</entry>\n" +
            "<entry key=\"com.tibco.tibjms.ssl.auth_only\">com.l7tech.server.jms.prop.boolean.false</entry>\n" +
            "<entry key=\"com.tibco.tibjms.naming.ssl_enable_verify_host\">com.l7tech.server.jms.prop.boolean.false</entry>\n" +
            "<entry key=\"com.tibco.tibjms.ssl.enable_verify_hostname\">com.l7tech.server.jms.prop.boolean.false</entry>\n" +
            "<entry key=\"com.tibco.tibjms.naming.ssl_auth_only\">com.l7tech.server.jms.prop.boolean.false</entry>\n" +
            "<entry key=\"com.tibco.tibjms.naming.ssl_enable_verify_hostname\">com.l7tech.server.jms.prop.boolean.false</entry>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.contentType.source\"/>\n" +
            "<entry key=\"com.l7tech.server.jms.prop.contentType.value\"/>\n" +
            "</properties>";

    protected String queueProvider;

    protected JmsProvider provider;

    public JmsConnectionManagerStub() {
        createTestConnections();
    }

    public EnumSet<JmsProviderType> findAllProviders() throws FindException {
        return EnumSet.noneOf(JmsProviderType.class);
    }

    public String getQueueProvider() {
        String result;

//        if (queueProvider == null) {
            if (TEST_CONFIG == TEST_CONFIG_AMQ_IN || TEST_CONFIG == TEST_CONFIG_AMQ_OUT)
                result = QPROVIDER_AMQ;
            else if (TEST_CONFIG == TEST_CONFIG_MQS_IN || TEST_CONFIG == TEST_CONFIG_MQS_OUT)
                result = QPROVIDER_MQS;
            else if (TEST_CONFIG == TEST_CONFIG_DYNAMIC_IN || TEST_CONFIG == TEST_CONFIG_DYNAMIC_OUT)
                result = QPROVIDER_DYNAMIC;
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

    public JmsConnection findByPrimaryKey(Goid oid) throws FindException {
        return getConnection((int)oid.getLow());
    }

    protected Collection<JmsProvider> createTestConnections() {

        Collection<JmsProvider> list = new ArrayList<JmsProvider>();

        String qProv = getQueueProvider();
        if (provider == null && QPROVIDER_AMQ.equals(qProv)) {
            provider = new JmsProvider("TestJmsProvider", AMQ_INITIAL_CONTEXT_FACTORY_CLASS, AMQ_DEFAULT_QUEUE_FACTORY_URL);
        }
        else if (provider == null && QPROVIDER_MQS.equals(qProv)) {
            provider = new JmsProvider("TestJmsProvider", MQS_INITIAL_CONTEXT_FACTORY_CLASS, MQS_DEFAULT_QUEUE_FACTORY_URL);
        }
        else if (provider == null && QPROVIDER_DYNAMIC.equals(qProv)) {
            provider = new JmsProvider("TestJmsProvider", DYNAMIC_INITIAL_CONTEXT_FACTORY_CLASS, DYNAMIC_DEFAULT_QUEUE_FACTORY_URL);
        }

        list.add(provider);
        return list;
    }


    private JmsConnection getConnection(int which) {
        JmsConnection conn;
        switch(which) {

            case TEST_CONFIG_AMQ_IN: {
                conn = provider.createConnection("dynamicQueues/JMS.JUNIT.IN.Q", AMQ_JNDI_URL);
                conn.setGoid(new Goid(0,TEST_CONFIG_AMQ_IN));
                break;
            }
            case TEST_CONFIG_AMQ_OUT: {
                conn = provider.createConnection("dynamicQueues/JMS.JUNIT.OUT.Q", AMQ_JNDI_URL);
                conn.setGoid(new Goid(0,TEST_CONFIG_AMQ_OUT));
                break;
            }
            case TEST_CONFIG_MQS_IN: {
                conn = provider.createConnection("cn=VCTEST.Q.IN", MQS_JNDI_URL);
                conn.setGoid(new Goid(0,TEST_CONFIG_MQS_IN));
//                conn.setProperties(MQS_CONN_PROPERTIES); // for ssl
                break;
            }
            case TEST_CONFIG_MQS_OUT: {
                conn = provider.createConnection("cn=VCTEST.Q.OUT", MQS_JNDI_URL);
                conn.setGoid(new Goid(0,TEST_CONFIG_MQS_OUT));
//                conn.setProperties(MQS_CONN_PROPERTIES);
                break;
            }
            case TEST_CONFIG_DYNAMIC_IN: {
                conn = provider.createConnection("ilona.in1", DYNAMIC_JNDI_URL);
                conn.setGoid(new Goid(0,TEST_CONFIG_DYNAMIC_IN));
//                conn.setProperties(DYNAMIC_CONN_PROPERTIES); // for ssl
                break;
            }
            case TEST_CONFIG_DYNAMIC_OUT: {
                conn = provider.createConnection("ilona.in1", DYNAMIC_JNDI_URL);
                conn.setGoid(new Goid(0,TEST_CONFIG_DYNAMIC_OUT));
//                conn.setProperties(DYNAMIC_CONN_PROPERTIES);
                break;
            }
            default: {
                conn = provider.createConnection(AMQ_QUEUE_DEFAULT, AMQ_JNDI_URL);
                conn.setGoid(new Goid(0,6666));
                break;
            }
        }
        return conn;
    }
}
