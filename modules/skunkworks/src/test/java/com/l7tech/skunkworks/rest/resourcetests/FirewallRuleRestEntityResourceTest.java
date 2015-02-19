package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.transport.firewall.SsgFirewallRuleManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class FirewallRuleRestEntityResourceTest extends RestEntityTests<SsgFirewallRule, FirewallRuleMO> {
    private SsgFirewallRuleManager ssgFirewallRuleManager;
    private List<SsgFirewallRule> rules = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        ssgFirewallRuleManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgFirewallRuleManager", SsgFirewallRuleManager.class);
        //Create the active connectors

        SsgFirewallRule firewallRule = new SsgFirewallRule();
        firewallRule.setName("Firewall Rule 1");
        firewallRule.setOrdinal(1);
        firewallRule.setEnabled(true);
        firewallRule.putProperty("protocol", "tcp");
        firewallRule.putProperty("jump", "REDIRECT");

        ssgFirewallRuleManager.save(firewallRule);
        rules.add(firewallRule);

        firewallRule = new SsgFirewallRule();
        firewallRule.setName("Firewall Rule 2");
        firewallRule.setOrdinal(2);
        firewallRule.setEnabled(false);
        firewallRule.putProperty("protocol", "udp");
        firewallRule.putProperty("jump", "ACCEPT");

        ssgFirewallRuleManager.save(firewallRule);
        rules.add(firewallRule);

        firewallRule = new SsgFirewallRule();
        firewallRule.setName("Firewall Rule 3");
        firewallRule.setOrdinal(3);
        firewallRule.setEnabled(false);
        firewallRule.putProperty("protocol", "icmp");
        firewallRule.putProperty("jump", "DRP{");

        ssgFirewallRuleManager.save(firewallRule);
        rules.add(firewallRule);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<SsgFirewallRule> all = ssgFirewallRuleManager.findAll();
        for (SsgFirewallRule firewallRule : all) {
            ssgFirewallRuleManager.delete(firewallRule.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(rules, new Functions.Unary<String, SsgFirewallRule>() {
            @Override
            public String call(SsgFirewallRule rule) {
                return rule.getId();
            }
        });
    }

    @Override
    public List<FirewallRuleMO> getCreatableManagedObjects() {
        List<FirewallRuleMO> rules = new ArrayList<>();

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(getGoid().toString());
        firewallRule.setName("Test Firewall Rule created");
        firewallRule.setOrdinal(4);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());
        rules.add(firewallRule);

        return rules;
    }

    @Override
    public List<FirewallRuleMO> getUpdateableManagedObjects() {
        List<FirewallRuleMO> rules = new ArrayList<>();

        SsgFirewallRule rule = this.rules.get(0);
        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rule.getId());
        firewallRule.setVersion(rule.getVersion());
        firewallRule.setName(rule.getName() + " Updated");
        firewallRule.setOrdinal(rule.getOrdinal());
        firewallRule.setEnabled(rule.isEnabled());
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "ACCEPT")
                .put("newProperty", "value")
                .map());
        rules.add(firewallRule);

        //update twice
        firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rule.getId());
        firewallRule.setVersion(rule.getVersion());
        firewallRule.setName(rule.getName() + " Updated");
        firewallRule.setOrdinal(rule.getOrdinal());
        firewallRule.setEnabled(rule.isEnabled());
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "DROP")
                .put("newProperty", "value")
                .map());
        rules.add(firewallRule);

        return rules;
    }

    @Override
    public Map<FirewallRuleMO, Functions.BinaryVoid<FirewallRuleMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<FirewallRuleMO, Functions.BinaryVoid<FirewallRuleMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setName(rules.get(0).getName());
        firewallRule.setOrdinal(1);
        firewallRule.setEnabled(false);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "DROP")
                .put("newProperty", "value")
                .map());

        builder.put(firewallRule, new Functions.BinaryVoid<FirewallRuleMO, RestResponse>() {
            @Override
            public void call(FirewallRuleMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<FirewallRuleMO, Functions.BinaryVoid<FirewallRuleMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<FirewallRuleMO, Functions.BinaryVoid<FirewallRuleMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rules.get(0).getId());
        firewallRule.setName(rules.get(1).getName());
        firewallRule.setOrdinal(1);
        firewallRule.setEnabled(false);
        firewallRule.setVersion(rules.get(0).getVersion());
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "DROP")
                .put("newProperty", "value")
                .map());

        builder.put(firewallRule, new Functions.BinaryVoid<FirewallRuleMO, RestResponse>() {
            @Override
            public void call(FirewallRuleMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf"+getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 400, restResponse.getStatus());
            }
        });
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(rules, new Functions.Unary<String, SsgFirewallRule>() {
            @Override
            public String call(SsgFirewallRule rule) {
                return rule.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "firewallRules";
    }

    @Override
    public String getType() {
        return EntityType.FIREWALL_RULE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        SsgFirewallRule entity = ssgFirewallRuleManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        SsgFirewallRule entity = ssgFirewallRuleManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, FirewallRuleMO managedObject) throws FindException {
        SsgFirewallRule entity = ssgFirewallRuleManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getOrdinal(), managedObject.getOrdinal());
            Assert.assertEquals(entity.isEnabled(), managedObject.isEnabled());
            Assert.assertEquals(entity.getPropertyNames().size(), managedObject.getProperties().size());
            for (String key : entity.getPropertyNames()) {
                Assert.assertEquals(entity.getProperty(key), managedObject.getProperties().get(key));
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(rules, new Functions.Unary<String, SsgFirewallRule>() {
                    @Override
                    public String call(SsgFirewallRule rule) {
                        return rule.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(rules.get(0).getName()), Arrays.asList(rules.get(0).getId()))
                .put("name=" + URLEncoder.encode(rules.get(0).getName()) + "&name=" + URLEncoder.encode(rules.get(1).getName()), Functions.map(rules.subList(0, 2), new Functions.Unary<String, SsgFirewallRule>() {
                    @Override
                    public String call(SsgFirewallRule rule) {
                        return rule.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("enabled=false", Arrays.asList(rules.get(1).getId(), rules.get(2).getId()))
                .put("enabled=true", Arrays.asList(rules.get(0).getId()))
                .put("ordinal="+ rules.get(0).getOrdinal(), Arrays.asList(rules.get(0).getId()))
                .put("name=" + URLEncoder.encode(rules.get(0).getName()) + "&name=" + URLEncoder.encode(rules.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(rules.get(1).getId(), rules.get(0).getId()))
                .map();
    }

    @Test
    public void testCreateMiddleOrdinalRule() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setName("Test Firewall Rule create middle ordinal");
        firewallRule.setOrdinal(2);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 201, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item createdItem = MarshallingUtils.unmarshal(Item.class, source);

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(),"sort=ordinal", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        final List<String> expectedIds = Arrays.asList(rules.get(0).getId(), createdItem.getId(), rules.get(1).getId(), rules.get(2).getId());
        source = new StreamSource(new StringReader(response.getBody()));
        ItemsList itemsList = MarshallingUtils.unmarshal(ItemsList.class, source);

        List<Item> references = itemsList.getContent();
        for (int i = 0; i < expectedIds.size(); i++) {
            Assert.assertEquals("Ordinals in incorrect order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), references.get(i).getId());
            Assert.assertEquals("Ordinals should be in sequential order.", i+1, (((FirewallRuleMO)references.get(i).getContent()).getOrdinal()));
        }
    }

    @Test
    public void testCreateFirstOrdinalRule() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setName("Test Firewall Rule create middle ordinal");
        firewallRule.setOrdinal(-6);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 201, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item createdItem = MarshallingUtils.unmarshal(Item.class, source);

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ createdItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        source = new StreamSource(new StringReader(response.getBody()));
        Item createdRule = MarshallingUtils.unmarshal(Item.class, source);

        Assert.assertEquals("Expected ordinal",((FirewallRuleMO)createdRule.getContent()).getOrdinal(),1);
    }

    @Test
    public void testCreateLastOrdinalRule() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setName("Test Firewall Rule create middle ordinal");
        firewallRule.setOrdinal(10);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 201, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item createdItem = MarshallingUtils.unmarshal(Item.class, source);

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ createdItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        source = new StreamSource(new StringReader(response.getBody()));
        Item createdRule = MarshallingUtils.unmarshal(Item.class, source);

        Assert.assertEquals("Expected ordinal",((FirewallRuleMO)createdRule.getContent()).getOrdinal(),4);
    }

    @Test
    public void testChangeRuleOrder() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rules.get(0).getId());
        firewallRule.setName(rules.get(0).getName());
        firewallRule.setVersion(rules.get(0).getVersion());
        firewallRule.setEnabled(rules.get(0).isEnabled());
        firewallRule.setOrdinal(2);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ firewallRule.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(),"sort=ordinal", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        final List<String> expectedIds = Arrays.asList(rules.get(1).getId(), rules.get(0).getId(), rules.get(2).getId());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList itemsList = MarshallingUtils.unmarshal(ItemsList.class, source);

        List<Item> references = itemsList.getContent();
        for (int i = 0; i < expectedIds.size(); i++) {
            Assert.assertEquals("Ordinals in incorrect order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), references.get(i).getId());
            Assert.assertEquals("Ordinals should be in sequential order.", i+1, (((FirewallRuleMO)references.get(i).getContent()).getOrdinal()));
        }
    }

    @Test
    public void testChangeRuleOrderToTop() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rules.get(2).getId());
        firewallRule.setName(rules.get(2).getName());
        firewallRule.setVersion(rules.get(2).getVersion());
        firewallRule.setEnabled(rules.get(2).isEnabled());
        firewallRule.setOrdinal(0);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ firewallRule.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(),"sort=ordinal", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        final List<String> expectedIds = Arrays.asList(rules.get(2).getId(), rules.get(0).getId(), rules.get(1).getId());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList itemsList = MarshallingUtils.unmarshal(ItemsList.class, source);

        List<Item> references = itemsList.getContent();
        for (int i = 0; i < expectedIds.size(); i++) {
            Assert.assertEquals("Ordinals in incorrect order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), references.get(i).getId());
            Assert.assertEquals("Ordinals should be in sequential order.", i+1, (((FirewallRuleMO)references.get(i).getContent()).getOrdinal()));
        }
    }

    @Test
    public void testUpdateOrderNotChanged() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rules.get(1).getId());
        firewallRule.setName(rules.get(1).getName());
        firewallRule.setVersion(rules.get(1).getVersion());
        firewallRule.setEnabled(!rules.get(1).isEnabled());
        firewallRule.setOrdinal(rules.get(1).getOrdinal());
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ firewallRule.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(),"sort=ordinal", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        final List<String> expectedIds = Arrays.asList(rules.get(0).getId(), rules.get(1).getId(), rules.get(2).getId());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList itemsList = MarshallingUtils.unmarshal(ItemsList.class, source);

        List<Item> references = itemsList.getContent();
        for (int i = 0; i < expectedIds.size(); i++) {
            Assert.assertEquals("Ordinals in incorrect order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), references.get(i).getId());
            Assert.assertEquals("Ordinals should be in sequential order.", i+1, (((FirewallRuleMO)references.get(i).getContent()).getOrdinal()));
        }
    }

    @Test
    public void testChangeRuleOrderLast() throws Exception {

        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setId(rules.get(0).getId());
        firewallRule.setName(rules.get(0).getName());
        firewallRule.setVersion(rules.get(0).getVersion());
        firewallRule.setEnabled(rules.get(0).isEnabled());
        firewallRule.setOrdinal(10);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ firewallRule.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(),"sort=ordinal", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        final List<String> expectedIds = Arrays.asList(rules.get(1).getId(), rules.get(2).getId(), rules.get(0).getId());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList itemsList = MarshallingUtils.unmarshal(ItemsList.class, source);

        List<Item> references = itemsList.getContent();
        for (int i = 0; i < expectedIds.size(); i++) {
            Assert.assertEquals("Ordinals in incorrect order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), references.get(i).getId());
            Assert.assertEquals("Ordinals should be in sequential order.", i+1, (((FirewallRuleMO)references.get(i).getContent()).getOrdinal()));
        }
    }

    @Test
    public void testDeleteMiddleOrdinalRule() throws Exception {

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri()+ "/"+ rules.get(1).getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), null);
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 204, response.getStatus());
        Assert.assertEquals("Expected empty response body", "", response.getBody());

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(),"sort=ordinal", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), null);

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList itemsList = MarshallingUtils.unmarshal(ItemsList.class, source);

        List<Item> references = itemsList.getContent();
        for (int i = 0; i < references.size(); i++) {
            Assert.assertEquals("Ordinals should be in sequential order.", i+1, (((FirewallRuleMO)references.get(i).getContent()).getOrdinal()));
        }
    }


    @Test
    public void testCreateFirstRule() throws Exception {

        // delete all rules
        Collection<SsgFirewallRule> all = ssgFirewallRuleManager.findAll();
        for (SsgFirewallRule firewallRule : all) {
            ssgFirewallRuleManager.delete(firewallRule.getGoid());
        }

        // create rule
        FirewallRuleMO firewallRule = ManagedObjectFactory.createFirewallRuleMO();
        firewallRule.setName("First rule");
        firewallRule.setOrdinal(-2);
        firewallRule.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("jump", "REDIRECT")
                .put("protocol", "tcp")
                .map());

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(firewallRule)));
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 201, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item createdItem = MarshallingUtils.unmarshal(Item.class, source);

        SsgFirewallRule createdRule = ssgFirewallRuleManager.findByPrimaryKey(Goid.parseGoid(createdItem.getId()));

        Assert.assertEquals(firewallRule.getName(), createdRule.getName());
        Assert.assertEquals(1, createdRule.getOrdinal());
        Assert.assertEquals(firewallRule.isEnabled(), createdRule.isEnabled());
        Assert.assertEquals(firewallRule.getProperties().size(), createdRule.getPropertyNames().size());
        for (String key : firewallRule.getProperties().keySet()) {
            Assert.assertEquals(firewallRule.getProperties().get(key),createdRule.getProperty(key) );
        }

    }
}
