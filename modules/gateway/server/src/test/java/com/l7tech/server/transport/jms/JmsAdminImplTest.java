package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import com.l7tech.test.BugId;
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

    @BugId("SSG-8556")
    @Test
    public void shouldReturnFalseWhenJmsPropertyNameIsBlank() throws Exception {
        JmsMessagePropertyRule rule = new JmsMessagePropertyRule("   ", false, "string");
        assertFalse(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule("\t", false, "string");
        assertFalse(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule("", false, "");
        assertFalse(jmsAdmin.isValidProperty(rule));
        rule = new JmsMessagePropertyRule(null, false, null);
        assertFalse(jmsAdmin.isValidProperty(rule));
    }
}
