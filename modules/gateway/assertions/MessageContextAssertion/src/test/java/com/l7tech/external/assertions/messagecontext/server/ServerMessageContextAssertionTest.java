package com.l7tech.external.assertions.messagecontext.server;

import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.external.assertions.messagecontext.MessageContextAssertion;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.message.Message;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @Author: ghuang
 * @Date: Oct 7, 2008
 */
public class ServerMessageContextAssertionTest {
    private PolicyEnforcementContext policyEnforcementContext;
    private ServerMessageContextAssertion smca;
    private MessageContextAssertion mca0;
    private MessageContextAssertion mca1;

    @Before
    public void setUp() throws Exception {
        policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testDefaultMappings() throws Exception {
        mca0 = new MessageContextAssertion();
        smca = new ServerMessageContextAssertion(mca0);
        smca.checkRequest(policyEnforcementContext);

        int idx = 0;
        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (idx == 0) assertEquals(mapping.getMappingType(), MessageContextMapping.MappingType.IP_ADDRESS);
            else assertEquals(mapping.getMappingType(), MessageContextMapping.MappingType.AUTH_USER);

            idx++;
        }
    }

    @Test
    public void testTooManyMappings() throws Exception {
        mca0 = new MessageContextAssertion();
        smca = new ServerMessageContextAssertion(mca0);
        smca.checkRequest(policyEnforcementContext);

        mca1 = new MessageContextAssertion();
        mca1.setMappings(new MessageContextMapping[] {
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "myvar", "Hello!"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "existingVar", "${httpRouting.url}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "notExistingVar", "${abc}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "yourvar", "foo bar")
        });
        smca = new ServerMessageContextAssertion(mca1);
        smca.checkRequest(policyEnforcementContext);

        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (mapping.getMappingType().equals(MessageContextMapping.MappingType.IP_ADDRESS)) {
                fail("The IP Address mapping has been dropped.");
            } else if (! mapping.getMappingType().equals(MessageContextMapping.MappingType.AUTH_USER)) {
                assertTrue("The max distinct number of mappings is five",
                    (mapping.getKey().equals("myvar") || mapping.getKey().equals("existingVar") ||
                        mapping.getKey().equals("notExistingVar") || mapping.getKey().equals("yourvar")));
            }
        }
    }

    @Test
    public void testOverrideMappings() throws Exception {
        mca0 = new MessageContextAssertion();
        smca = new ServerMessageContextAssertion(mca0);
        smca.checkRequest(policyEnforcementContext);

        mca1 = new MessageContextAssertion();
        mca1.setMappings(new MessageContextMapping[] {
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "myvar", "Hello!"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "existingVar", "${httpRouting.url}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "notExistingVar", "${abc}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "myvar", "foo bar")
        });
        smca = new ServerMessageContextAssertion(mca1);
        smca.checkRequest(policyEnforcementContext);

        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (mapping.getKey().equals("myvar")) {
                assertFalse("The myvar mapping has been overridden.", mapping.getValue().equals("Hello!"));
                assertTrue("The myvar mapping has been overridden.", mapping.getValue().equals("foo bar"));
            }
        }
    }

    @Test
    public void testVariables() throws Exception {
        policyEnforcementContext.setVariable("httpRouting.url", "http://hugh");

        mca1 = new MessageContextAssertion();
        mca1.setMappings(new MessageContextMapping[] {
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "existingVar", "${httpRouting.url}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "notExistingVar", "${abc}")
        });
        smca = new ServerMessageContextAssertion(mca1);
        smca.checkRequest(policyEnforcementContext);

        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (mapping.getKey().equals("existingVar")) {
                assertEquals("The existingVar mapping is supposed to have a preset variable, httpRouting.url " +
                    "with a value, http://hugh.", mapping.getValue(), "http://hugh");
            } else if (mapping.getKey().equals("notExistingVar")) {
                assertEquals("The notExistingVar mapping is supposed to have a non-preset variable, abc.", mapping.getValue(), "");
            } else {
                fail("Invalid mapping: " + mapping.getMappingType().getName());
            }
        }
    }
}

