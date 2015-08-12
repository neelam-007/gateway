package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class FirewallRuleMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(FirewallRuleMigration.class.getName());
    private Item<FirewallRuleMO> firewallRuleItem;

    @Before
    public void before() throws Exception {
        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setName("Test Firewall Rule created");
        firewallRule.setOrdinal(4);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getSourceEnvironment().processRequest("firewallRules", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        assertOkCreatedResponse(response);

        firewallRuleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        firewallRuleItem.setContent(firewallRule);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("firewallRules/" + firewallRuleItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?firewallRule=" + firewallRuleItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A firewallRule", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items. A firewallRule", 1, bundleItem.getContent().getMappings().size());
    }
}
