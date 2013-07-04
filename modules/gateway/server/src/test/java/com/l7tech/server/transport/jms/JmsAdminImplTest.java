package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class JmsAdminImplTest {

    private JmsAdmin jmsAdmin;


    @Before
    public void setup() {
        jmsAdmin = new JmsAdminImpl(null, null, null, null);
    }

    @Test
    public void testIsValidProperty() throws Exception {
        JmsMessagePropertyRule rule = new JmsMessagePropertyRule("JMSXUserID", false, "string");
        assertTrue(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule("JMSXDeliveryCount", false, "string");
        assertFalse(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule("JMSXDeliveryCount", true, "string");
        assertTrue(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule("JMSXDeliveryCount", false, "1234");
        assertTrue(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule("NotPredefinedProperty", false, "string");
        assertTrue(jmsAdmin.isValidProperty(rule));
    }

    @Test
    public void testIsDedicatedThreadPool() {
        assertFalse(jmsAdmin.isValidThreadPoolSize("A"));
        assertFalse(jmsAdmin.isValidThreadPoolSize("1"));
        assertFalse(jmsAdmin.isValidThreadPoolSize("100"));
        assertTrue(jmsAdmin.isValidThreadPoolSize("10"));
    }
}
