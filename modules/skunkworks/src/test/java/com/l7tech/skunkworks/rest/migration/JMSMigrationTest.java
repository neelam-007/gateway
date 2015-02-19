package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



/**;
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class JMSMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(JMSMigrationTest.class.getName());

    private Item<ServiceMO> serviceItem;
    private Item<StoredPasswordMO> storedPasswordItem;
    private Item<PolicyMO> policyItem;
    private Item<JMSDestinationMO> jmsItem;
    private Item<Mappings> mappingsToClean;
    private Item<StoredPasswordMO> storedPasswordItem2;
    private Item<PrivateKeyMO> privateKeyItem;
    private Item<PrivateKeyMO> privateKeyItem2;
    private Item<PrivateKeyMO> privateKeyItemTarget;
    private Item<PrivateKeyMO> privateKeyItem2Target;

    @Before
    public void before() throws Exception {
        //create service
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMapping = ManagedObjectFactory.createHttpMapping();
        serviceMapping.setUrlPattern("/srcService");
        serviceMapping.setVerbs(Arrays.asList("POST"));
        serviceDetail.setServiceMappings(Arrays.asList(serviceMapping));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml );
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);

        //create secure password
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("SourcePassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        response = getSourceEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);

        storedPasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordItem.setContent(storedPasswordMO);

        storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("SourcePassword2");
        storedPasswordMO.setPassword("password2");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        response = getSourceEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);

        storedPasswordItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordItem2.setContent(storedPasswordMO);

        // create private key
        PrivateKeyCreationContext createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=srcAlias1");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getSourceEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":srcAlias1", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getSourceEnvironment().processRequest("privateKeys/"+ privateKeyItem.getId(), HttpMethod.GET,null,"");
        assertOkResponse(response);
        privateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=srcAlias2");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getSourceEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":srcAlias2", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getSourceEnvironment().processRequest("privateKeys/"+ privateKeyItem2.getId(), HttpMethod.GET,null,"");
        assertOkResponse(response);
        privateKeyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=srcAlias1");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getTargetEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":srcAlias1", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getTargetEnvironment().processRequest("privateKeys/"+ privateKeyItemTarget.getId(), HttpMethod.GET,null,"");
        assertOkResponse(response);
        privateKeyItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=targetAlias2");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getTargetEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":targetAlias2", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyItem2Target = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getTargetEnvironment().processRequest("privateKeys/"+ privateKeyItem2Target.getId(), HttpMethod.GET,null,"");
        assertOkResponse(response);
        privateKeyItem2Target = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create jms
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("Source JMS");
        jmsDetail.setDestinationName("Source JMS Destination");
        jmsDetail.setInbound(false);
        jmsDetail.setEnabled(true);
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProviderType(JMSConnection.JMSProviderType.TIBCO_EMS);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname", "om.context.Classname")
                .put("jndi.providerUrl", "ldap://jndi")
                .put("queue.connectionFactoryName", "qcf")
                .put("password", "${secpass." + storedPasswordItem.getName() + ".plaintext}")
                .map());
        jmsConnection.setContextPropertiesTemplate(CollectionUtils.<String, Object>mapBuilder()
                .put("com.l7tech.server.jms.prop.hardwired.service.id", serviceItem.getId())
                .put("java.naming.security.credentials", "${secpass." + storedPasswordItem2.getName() + ".plaintext}")
                .put("com.l7tech.server.jms.prop.jndi.ssgKeyAlias", privateKeyItem.getContent().getAlias())
                .put("com.l7tech.server.jms.prop.jndi.ssgKeystoreId", privateKeyItem.getContent().getKeystoreId())
                .put("com.tibco.tibjms.naming.security_protocol", "ssl")
                .put("com.l7tech.server.jms.prop.customizer.class", "com.l7tech.server.transport.jms.prov.MQSeriesCustomizer")
                .put("com.l7tech.server.jms.prop.queue.ssgKeyAlias", privateKeyItem2.getContent().getAlias())
                .put("com.l7tech.server.jms.prop.queue.ssgKeystoreId", privateKeyItem2.getContent().getKeystoreId())
                .map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);

        response = getSourceEnvironment().processRequest("jmsDestinations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

        assertOkCreatedResponse(response);

        jmsItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jmsItem.setContent(jmsMO);


        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:JmsRoutingAssertion>\n" +
                "            <L7p:EndpointName stringValue=\"name\"/>\n" +
                "            <L7p:EndpointOid goidValue=\""+jmsItem.getId()+"\"/>\n" +
                "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
                "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
                "        </L7p:JmsRoutingAssertion>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);
    }

    @After
    public void after() throws Exception {
        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("jmsDestinations/" + jmsItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + storedPasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + storedPasswordItem2.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem2.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItemTarget.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItem2Target.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 5 items. A policy, and jms endpoint", 5, bundleItem.getContent().getReferences().size());

        MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem.getId()).setPassword("myPassword");
        getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());
        MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem2.getId()).setPassword("myPassword");
        getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setProperties(Collections.<String,Object>emptyMap());
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), privateKeyItem2.getId()).setTargetId(privateKeyItem2Target.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 8 mappings after the import", 8, mappings.getContent().getMappings().size());
        Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping securePass1Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem2.getId());
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());
        Assert.assertEquals(securePass1Mapping.getSrcId(), securePass1Mapping.getTargetId());

        Mapping securePass2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem.getId());
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());
        Assert.assertEquals(securePass2Mapping.getSrcId(), securePass2Mapping.getTargetId());

        Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem.getId());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

        Mapping privateKey2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem2.getId());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKey2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKey2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKey2Mapping.getActionTaken());
        Assert.assertEquals(privateKeyItem2.getId(), privateKey2Mapping.getSrcId());
        Assert.assertEquals(privateKeyItem2Target.getId(), privateKey2Mapping.getTargetId());

        Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem.getId());
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jmsMapping.getActionTaken());
        Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
        Assert.assertEquals(jmsMapping.getSrcId(), jmsMapping.getTargetId());

        Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies
        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(6, policyDependencies.size());

        DependencyMO jmsDependency = getDependency(policyDependencies,jmsItem.getId());
        Assert.assertNotNull(jmsDependency);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
        Assert.assertEquals(jmsItem.getName(), jmsDependency.getName());
        Assert.assertEquals(jmsItem.getId(), jmsDependency.getId());
        Assert.assertEquals(5, jmsDependency.getDependencies().size());

        validate(mappings);
    }

    @Test
    public void testImportNewEncryptSecrets() throws Exception {
        Item<JMSDestinationMO> jmsItem2 = null;
        Item<PolicyMO> policyItem2 = null;
        Item<PrivateKeyMO> thisPrivateKeyItem = null;

        RestResponse response;
        try {
            // create private key
            PrivateKeyCreationContext createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
            createPrivateKey.setDn("CN=srcAlias16");
            createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                    .put("ecName", "secp384r1")
                    .map());
            response = getSourceEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":srcAlias16", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
            assertOkCreatedResponse(response);
            thisPrivateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            response = getSourceEnvironment().processRequest("privateKeys/"+ thisPrivateKeyItem.getId(), HttpMethod.GET,null,"");
            assertOkResponse(response);
            thisPrivateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //create jms
            JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
            jmsDetail.setName("Source JMS 2");
            jmsDetail.setDestinationName("Source JMS Destination");
            jmsDetail.setInbound(false);
            jmsDetail.setEnabled(true);
            jmsDetail.setTemplate(false);
            JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
            jmsConnection.setTemplate(false);
            jmsConnection.setProviderType(JMSConnection.JMSProviderType.TIBCO_EMS);
            jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                    .put("jndi.initialContextFactoryClassname", "om.context.Classname")
                    .put("jndi.providerUrl", "ldap://jndi")
                    .put("queue.connectionFactoryName", "qcf")
                    .put("password", "pass")
                    .map());
            jmsConnection.setContextPropertiesTemplate(CollectionUtils.<String, Object>mapBuilder()
                    .put("com.l7tech.server.jms.prop.hardwired.service.id", serviceItem.getId())
                    .put("java.naming.security.credentials", "credPass")
                    .put("com.l7tech.server.jms.prop.jndi.ssgKeyAlias", thisPrivateKeyItem.getContent().getAlias())
                    .put("com.l7tech.server.jms.prop.jndi.ssgKeystoreId", thisPrivateKeyItem.getContent().getKeystoreId())
                    .put("com.tibco.tibjms.naming.security_protocol", "ssl")
                    .put("com.l7tech.server.jms.prop.customizer.class", "com.l7tech.server.transport.jms.prov.MQSeriesCustomizer")
                    .put("com.l7tech.server.jms.prop.queue.ssgKeyAlias", thisPrivateKeyItem.getContent().getAlias())
                    .put("com.l7tech.server.jms.prop.queue.ssgKeystoreId", thisPrivateKeyItem.getContent().getKeystoreId())
                    .map());
            JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
            jmsMO.setJmsDestinationDetail(jmsDetail);
            jmsMO.setJmsConnection(jmsConnection);

            response = getSourceEnvironment().processRequest("jmsDestinations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

            assertOkCreatedResponse(response);

            jmsItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            jmsItem2.setContent(jmsMO);

            // create policy
            PolicyMO policyMO = ManagedObjectFactory.createPolicy();
            PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
            policyMO.setPolicyDetail(policyDetail);
            policyDetail.setName("MyPolicy2");
            policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
            policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
            policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                    .put("soap", false)
                    .map());
            ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
            policyMO.setResourceSets(Arrays.asList(resourceSet));
            resourceSet.setTag("policy");
            Resource resource = ManagedObjectFactory.createResource();
            resourceSet.setResources(Arrays.asList(resource));
            resource.setType("policy");
            resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <wsp:All wsp:Usage=\"Required\">\n" +
                    "        <L7p:JmsRoutingAssertion>\n" +
                    "            <L7p:EndpointName stringValue=\"name\"/>\n" +
                    "            <L7p:EndpointOid goidValue=\""+jmsItem2.getId()+"\"/>\n" +
                    "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                    "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                    "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
                    "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                    "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                    "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
                    "        </L7p:JmsRoutingAssertion>" +
                    "    </wsp:All>\n" +
                    "</wsp:Policy>");

            response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

            assertOkCreatedResponse(response);

            policyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            policyItem2.setContent(policyMO);

            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem2.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 4 items. A policy, and jms endpoint", 4, bundleItem.getContent().getReferences().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 5, mappings.getContent().getMappings().size());
            Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

             Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), thisPrivateKeyItem.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, privateKeyMapping.getActionTaken());
            Assert.assertEquals(thisPrivateKeyItem.getId(), privateKeyMapping.getSrcId());
            Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

            Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem2.getId());
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem2.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMapping.getSrcId(), jmsMapping.getTargetId());

            Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem2.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem2.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);
        }finally{
            response = getSourceEnvironment().processRequest("jmsDestinations/" + jmsItem2.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getSourceEnvironment().processRequest("policies/" + policyItem2.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getSourceEnvironment().processRequest("privateKeys/"+ thisPrivateKeyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingJMS() throws Exception {
        //create the jms on the target
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("Target JMS");
        jmsDetail.setDestinationName("Target JMS Destination");
        jmsDetail.setInbound(false);
        jmsDetail.setEnabled(true);
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);

        RestResponse response = getTargetEnvironment().processRequest("jmsDestinations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

        assertOkCreatedResponse(response);
        Item<JMSDestinationMO> jmsCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jmsMO.setId(jmsCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 items. A policy and jms endpoint", 5, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the cert to the existing one
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setAction(Mapping.Action.Ignore);
            getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setAction(Mapping.Action.Ignore);
            getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setProperties(Collections.<String,Object>emptyMap());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setTargetId(jmsMO.getId());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), privateKeyItem2.getId()).setTargetId(privateKeyItem2Target.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 8 mappings after the import", 8, mappings.getContent().getMappings().size());
            Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem2.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
            Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
            Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

            Mapping privateKey2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem2.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKey2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKey2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKey2Mapping.getActionTaken());
            Assert.assertEquals(privateKeyItem2.getId(), privateKey2Mapping.getSrcId());
            Assert.assertEquals(privateKeyItem2Target.getId(), privateKey2Mapping.getTargetId());

            Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem.getId());
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO certDependency = getDependency(policyDependencies,jmsMO.getId());
            Assert.assertNotNull(certDependency);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), certDependency.getType());
            Assert.assertEquals(jmsMO.getJmsDestinationDetail().getName(), certDependency.getName());
            Assert.assertEquals(jmsMO.getId(), certDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("jmsDestinations/" + jmsCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testAlwaysCreateNew() throws Exception{

        //get the bundle
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 5 items. A policy and jms endpoint", 5, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the jms to the existing one
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setAction(Mapping.Action.AlwaysCreateNew);
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnExisting", true).map());
        MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem.getId()).setPassword("myPassword");
        getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());
        MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem2.getId()).setPassword("myPassword");
        getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setProperties(Collections.<String,Object>emptyMap());
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), privateKeyItem2.getId()).setTargetId(privateKeyItem2Target.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 8 mappings after the import", 8, mappings.getContent().getMappings().size());
        Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping securePass1Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem2.getId());
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

        Mapping securePass2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem.getId());
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

        Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem.getId());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

        Mapping privateKey2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem2.getId());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKey2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKey2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKey2Mapping.getActionTaken());
        Assert.assertEquals(privateKeyItem2.getId(), privateKey2Mapping.getSrcId());
        Assert.assertEquals(privateKeyItem2Target.getId(), privateKey2Mapping.getTargetId());

        Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem.getId());
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, jmsMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jmsMapping.getActionTaken());
        Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
        Assert.assertEquals(jmsMapping.getSrcId(), jmsMapping.getTargetId());

        Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

        logger.log(Level.INFO, policyXml);

        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(6, policyDependencies.size());

        DependencyMO jmsDependency = getDependency(policyDependencies,jmsItem.getId());
        Assert.assertNotNull(jmsDependency);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
        Assert.assertEquals(jmsItem.getName(), jmsDependency.getName());
        Assert.assertEquals(jmsItem.getId(), jmsDependency.getId());
        Assert.assertEquals(5, jmsDependency.getDependencies().size());

        validate(mappings);
    }

    @Test
    public void testUpdateExistingSameGoid() throws Exception{
        //create the jms on the target
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setId(jmsItem.getId());
        jmsDetail.setName("Target JMS");
        jmsDetail.setDestinationName("Target JMS Destination");
        jmsDetail.setInbound(false);
        jmsDetail.setEnabled(true);
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setId(jmsItem.getId());
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);

        RestResponse response = getTargetEnvironment().processRequest("jmsDestinations/"+jmsItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

        assertOkCreatedResponse(response);
        Item<JMSDestinationMO> jmsCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jmsMO.setId(jmsCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 items. A policy and jms endpoint", 5, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the cert to the existing one
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setAction(Mapping.Action.NewOrUpdate);
            MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem.getId()).setPassword("myPassword");
            getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());
            MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem2.getId()).setPassword("myPassword");
            getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setProperties(Collections.<String,Object>emptyMap());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), privateKeyItem2.getId()).setTargetId(privateKeyItem2Target.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 8 mappings after the import", 8, mappings.getContent().getMappings().size());
            Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem2.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
            Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
            Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

            Mapping privateKey2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem2.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKey2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKey2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKey2Mapping.getActionTaken());
            Assert.assertEquals(privateKeyItem2.getId(), privateKey2Mapping.getSrcId());
            Assert.assertEquals(privateKeyItem2Target.getId(), privateKey2Mapping.getTargetId());

            Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem.getId());
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(6, policyDependencies.size());

            DependencyMO jmsDependency = getDependency(policyDependencies,jmsItem.getId());
            Assert.assertNotNull(jmsDependency);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
            Assert.assertEquals(jmsMO.getJmsDestinationDetail().getName(), jmsDependency.getName());
            Assert.assertEquals(jmsItem.getId(), jmsDependency.getId());
            Assert.assertEquals(5, jmsDependency.getDependencies().size());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("jmsDestinations/" + jmsCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToUpdateExisting() throws Exception{
        //create the jms on the target
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("Target JMS");
        jmsDetail.setDestinationName("Target JMS Destination");
        jmsDetail.setInbound(false);
        jmsDetail.setEnabled(true);
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProviderType(JMSConnection.JMSProviderType.Weblogic);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);
        RestResponse response = getTargetEnvironment().processRequest("jmsDestinations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

        assertOkCreatedResponse(response);
        Item<JMSDestinationMO> jmsCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jmsMO.setId(jmsCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 5 items. A policy and jms endpoint", 5, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the jms to the existing one
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setAction(Mapping.Action.NewOrUpdate);
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setTargetId(jmsMO.getId());
        MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem.getId()).setPassword("myPassword");
        getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());
        MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem2.getId()).setPassword("myPassword");
        getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setProperties(Collections.<String,Object>emptyMap());
        MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), privateKeyItem2.getId()).setTargetId(privateKeyItem2Target.getId());

        try{
            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 8 mappings after the import", 8, mappings.getContent().getMappings().size());
            Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem2.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
            Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
            Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

            Mapping privateKey2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem2.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKey2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKey2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKey2Mapping.getActionTaken());
            Assert.assertEquals(privateKeyItem2.getId(), privateKey2Mapping.getSrcId());
            Assert.assertEquals(privateKeyItem2Target.getId(), privateKey2Mapping.getTargetId());

            Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem.getId());
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(6, policyDependencies.size());

            DependencyMO jmsDependency = getDependency(policyDependencies,jmsMO.getId());
            Assert.assertNotNull(jmsDependency);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
            Assert.assertEquals(jmsMO.getJmsDestinationDetail().getName(), jmsDependency.getName());
            Assert.assertEquals(jmsMO.getId(), jmsDependency.getId());
            Assert.assertEquals(5, jmsDependency.getDependencies().size());

            // check jms object, associated jms connection is updated and not creating a new one.
            response = getTargetEnvironment().processRequest("jmsDestinations/"+jmsMO.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<JMSDestinationMO> jmsUpdated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(JMSConnection.JMSProviderType.TIBCO_EMS, jmsUpdated.getContent().getJmsConnection().getProviderType());
            Assert.assertNotSame(jmsItem.getContent().getJmsConnection().getId(), jmsUpdated.getContent().getJmsConnection().getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("jmsDestinations/" + jmsCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }

    }

    @Test
    public void testMqMapByName() throws Exception {
        //create the jms on the target
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("Target JMS");
        jmsDetail.setDestinationName("Target JMS Destination");
        jmsDetail.setInbound(false);
        jmsDetail.setEnabled(true);
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);
        RestResponse response = getTargetEnvironment().processRequest("jmsDestinations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

        assertOkCreatedResponse(response);
        Item<JMSDestinationMO> jmsCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jmsMO.setId(jmsCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 items. A policy and jms endpoint", 5, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the cert to the existing one
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setAction(Mapping.Action.NewOrExisting);
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).put("MapBy", "name").put("MapTo", jmsCreated.getName()).map());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), jmsItem.getId()).setTargetId(jmsMO.getId());
            MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem.getId()).setPassword("myPassword");
            getMapping(bundleItem.getContent().getMappings(), storedPasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());
            MigrationTestBase.<StoredPasswordMO>getBundleReference(bundleItem.getContent(), storedPasswordItem2.getId()).setPassword("myPassword");
            getMapping(bundleItem.getContent().getMappings(), storedPasswordItem2.getId()).setProperties(Collections.<String,Object>emptyMap());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), privateKeyItem2.getId()).setTargetId(privateKeyItem2Target.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 8 mappings after the import", 8, mappings.getContent().getMappings().size());
            Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem2.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), storedPasswordItem.getId());
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping privateKeyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
            Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
            Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

            Mapping privateKey2Mapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), privateKeyItem2.getId());
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKey2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKey2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKey2Mapping.getActionTaken());
            Assert.assertEquals(privateKeyItem2.getId(), privateKey2Mapping.getSrcId());
            Assert.assertEquals(privateKeyItem2Target.getId(), privateKey2Mapping.getTargetId());

            Mapping jmsMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), jmsItem.getId());
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO jmsDependency = getDependency(policyDependencies,jmsMO.getId());
            Assert.assertNotNull(jmsDependency);
            Assert.assertEquals(jmsMO.getJmsDestinationDetail().getName(), jmsDependency.getName());
            Assert.assertEquals(jmsMO.getId(), jmsDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("jmsDestinations/" + jmsCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        //create the jms on the target
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("Target JMS");
        jmsDetail.setDestinationName("Target JMS Destination");
        jmsDetail.setInbound(false);
        jmsDetail.setEnabled(true);
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);
        RestResponse response = getTargetEnvironment().processRequest("jmsDestinations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jmsMO)));

        assertOkCreatedResponse(response);
        Item<JMSDestinationMO> jmsCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jmsMO.setId(jmsCreated.getId());
        jmsCreated.setContent(jmsMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(jmsCreated.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(jmsCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(jmsCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(jmsCreated));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping activeConnectorMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(jmsCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("jmsDestinations/"+jmsCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }
}
