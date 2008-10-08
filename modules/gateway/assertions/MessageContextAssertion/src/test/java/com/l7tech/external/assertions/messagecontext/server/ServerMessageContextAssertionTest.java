package com.l7tech.external.assertions.messagecontext.server;

import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.messagecontext.MessageContextAssertion;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Oct 7, 2008
 */
public class ServerMessageContextAssertionTest {
    private static ApplicationContext applicationContext;
    private PolicyEnforcementContext policyEnforcementContext;
    private ServerMessageContextAssertion smca;
    private MessageContextAssertion mca0;
    private MessageContextAssertion mca1;

    @Before
    public void setUp() throws Exception {
        applicationContext = ApplicationContexts.getTestApplicationContext();
        policyEnforcementContext = new PolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testDefaultMappings() throws Exception {
        mca0 = new MessageContextAssertion();
        smca = new ServerMessageContextAssertion(mca0, applicationContext);
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
        smca = new ServerMessageContextAssertion(mca0, applicationContext);
        smca.checkRequest(policyEnforcementContext);

        mca1 = new MessageContextAssertion();
        mca1.setMappings(new MessageContextMapping[] {
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "myvar", "Hello!"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "existingVar", "${httpRouting.url}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "notExistingVar", "${abc}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "yourvar", "foo bar")
        });
        smca = new ServerMessageContextAssertion(mca1, applicationContext);
        smca.checkRequest(policyEnforcementContext);

        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (mapping.getMappingType().equals(MessageContextMapping.MappingType.IP_ADDRESS)) {
                fail("The IP Address mapping has been dropped.");
            }
        }
    }

    @Test
    public void testOverrideMappings() throws Exception {
        mca0 = new MessageContextAssertion();
        smca = new ServerMessageContextAssertion(mca0, applicationContext);
        smca.checkRequest(policyEnforcementContext);

        mca1 = new MessageContextAssertion();
        mca1.setMappings(new MessageContextMapping[] {
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "myvar", "Hello!"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "existingVar", "${httpRouting.url}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "notExistingVar", "${abc}"),
            new MessageContextMapping(MessageContextMapping.MappingType.CUSTOM_MAPPING, "myvar", "foo bar")
        });
        smca = new ServerMessageContextAssertion(mca1, applicationContext);
        smca.checkRequest(policyEnforcementContext);

        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (mapping.getKey().equals("myvar") && mapping.getValue().equals("Hello!")) {
                fail("The myvar mapping has been overridden.");
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
        smca = new ServerMessageContextAssertion(mca1, applicationContext);
        smca.checkRequest(policyEnforcementContext);

        for (MessageContextMapping mapping: policyEnforcementContext.getMappings()) {
            if (mapping.getKey().equals("existingVar") && ! mapping.getValue().equals("http://hugh")) {
                fail("The existingVar mapping is supposed to have a preset variable, httpRouting.url with a value, http://hugh.");
            } else if (mapping.getKey().equals("notExistingVar") && ! mapping.getValue().equals("")) {
                fail("The notExistingVar mapping is supposed to have a non-preset variable, abc.");
            }
        }
    }
}

