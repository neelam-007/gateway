package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
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
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("jmsDestinations/" + jmsItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + storedPasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + storedPasswordItem2.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 5 items. A policy, and jms endpoint", 5, bundleItem.getContent().getReferences().size());

        ((StoredPasswordMO)bundleItem.getContent().getReferences().get(1).getContent()).setPassword("myPassword");
        ((StoredPasswordMO)bundleItem.getContent().getReferences().get(2).getContent()).setPassword("myPassword");

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
        Mapping serviceMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping securePass1Mapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());
        Assert.assertEquals(securePass1Mapping.getSrcId(), securePass1Mapping.getTargetId());

        Mapping securePass2Mapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());
        Assert.assertEquals(securePass2Mapping.getSrcId(), securePass2Mapping.getTargetId());

        Mapping jmsMapping = mappings.getContent().getMappings().get(4);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jmsMapping.getActionTaken());
        Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
        Assert.assertEquals(jmsMapping.getSrcId(), jmsMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(5);
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
        Assert.assertEquals(4, policyDependencies.size());

        DependencyMO jmsDependency = getDependency(policyDependencies,jmsItem.getId());
        Assert.assertNotNull(jmsDependency);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
        Assert.assertEquals(jmsItem.getName(), jmsDependency.getName());
        Assert.assertEquals(jmsItem.getId(), jmsDependency.getId());
        Assert.assertEquals(3, jmsDependency.getDependencies().size());

        validate(mappings);
    }

    @Test
    public void testMapToExistingJMSCert() throws Exception {
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
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.Ignore);
            bundleItem.getContent().getMappings().get(3).setAction(Mapping.Action.Ignore);
            bundleItem.getContent().getMappings().get(4).setTargetId(jmsMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping jmsMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(5);
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
            assertOkDeleteResponse(response);
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
        bundleItem.getContent().getMappings().get(4).setAction(Mapping.Action.AlwaysCreateNew);
        bundleItem.getContent().getMappings().get(4).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnExisting", true).map());
        ((StoredPasswordMO)bundleItem.getContent().getReferences().get(1).getContent()).setPassword("myPassword");
        ((StoredPasswordMO)bundleItem.getContent().getReferences().get(2).getContent()).setPassword("myPassword");

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
        Mapping serviceMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping securePass1Mapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

        Mapping securePass2Mapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
        Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

        Mapping jmsMapping = mappings.getContent().getMappings().get(4);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, jmsMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jmsMapping.getActionTaken());
        Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
        Assert.assertEquals(jmsMapping.getSrcId(), jmsMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(5);
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
        Assert.assertEquals(4, policyDependencies.size());

        DependencyMO jmsDependency = getDependency(policyDependencies,jmsItem.getId());
        Assert.assertNotNull(jmsDependency);
        Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
        Assert.assertEquals(jmsItem.getName(), jmsDependency.getName());
        Assert.assertEquals(jmsItem.getId(), jmsDependency.getId());
        Assert.assertEquals(3, jmsDependency.getDependencies().size());

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
            bundleItem.getContent().getMappings().get(4).setAction(Mapping.Action.NewOrUpdate);
            ((StoredPasswordMO)bundleItem.getContent().getReferences().get(1).getContent()).setPassword("myPassword");
            ((StoredPasswordMO)bundleItem.getContent().getReferences().get(2).getContent()).setPassword("myPassword");

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping jmsMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(5);
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
            Assert.assertEquals(4, policyDependencies.size());

            DependencyMO jmsDependency = getDependency(policyDependencies,jmsItem.getId());
            Assert.assertNotNull(jmsDependency);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
            Assert.assertEquals(jmsItem.getName(), jmsDependency.getName());
            Assert.assertEquals(jmsItem.getId(), jmsDependency.getId());
            Assert.assertEquals(3, jmsDependency.getDependencies().size());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("jmsDestinations/" + jmsCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
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
        bundleItem.getContent().getMappings().get(4).setAction(Mapping.Action.NewOrUpdate);
        bundleItem.getContent().getMappings().get(4).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
        bundleItem.getContent().getMappings().get(4).setTargetId(jmsMO.getId());
        ((StoredPasswordMO)bundleItem.getContent().getReferences().get(1).getContent()).setPassword("myPassword");
        ((StoredPasswordMO)bundleItem.getContent().getReferences().get(2).getContent()).setPassword("myPassword");

        try{
            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping jmsMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(5);
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
            Assert.assertEquals(4, policyDependencies.size());

            DependencyMO jmsDependency = getDependency(policyDependencies,jmsMO.getId());
            Assert.assertNotNull(jmsDependency);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDependency.getType());
            Assert.assertEquals(jmsItem.getName(), jmsDependency.getName());
            Assert.assertEquals(jmsMO.getId(), jmsDependency.getId());
            Assert.assertEquals(3, jmsDependency.getDependencies().size());

            // check jms object, associated jms connection is updated and not creating a new one.
            response = getTargetEnvironment().processRequest("jmsDestinations/"+jmsMO.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<JMSDestinationMO> jmsUpdated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(JMSConnection.JMSProviderType.TIBCO_EMS, jmsUpdated.getContent().getJmsConnection().getProviderType());
            Assert.assertNotSame(jmsItem.getContent().getJmsConnection().getId(), jmsUpdated.getContent().getJmsConnection().getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("jmsDestinations/" + jmsCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
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
            bundleItem.getContent().getMappings().get(4).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(4).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).put("MapBy", "name").put("MapTo", jmsCreated.getName()).map());
            bundleItem.getContent().getMappings().get(4).setTargetId(jmsMO.getId());
            ((StoredPasswordMO)bundleItem.getContent().getReferences().get(1).getContent()).setPassword("myPassword");
            ((StoredPasswordMO)bundleItem.getContent().getReferences().get(2).getContent()).setPassword("myPassword");

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping securePass1Mapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass1Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass1Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass1Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem2.getId(), securePass1Mapping.getSrcId());

            Mapping securePass2Mapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), securePass2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securePass2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securePass2Mapping.getActionTaken());
            Assert.assertEquals(storedPasswordItem.getId(), securePass2Mapping.getSrcId());

            Mapping jmsMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jmsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, jmsMapping.getActionTaken());
            Assert.assertEquals(jmsItem.getId(), jmsMapping.getSrcId());
            Assert.assertEquals(jmsMO.getId(), jmsMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(5);
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
            assertOkDeleteResponse(response);
        }
    }
}
