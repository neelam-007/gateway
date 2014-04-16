package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PrivateKeyMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(PrivateKeyMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<PrivateKeyMO> privateKeyItem;
    private Item<ActiveConnectorMO> mqNativeItem;
    private Item<Mappings> mappingsToClean;

    private Item<PrivateKeyMO> targetPrivateKeyItem;

    @Before
    public void before() throws Exception {
        RestResponse response;

        // create private key
        PrivateKeyCreationContext createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=srcAlias");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getSourceEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":srcAlias", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getSourceEnvironment().processRequest("privateKeys/"+ privateKeyItem.getId(), HttpMethod.GET,null,"");
        assertOkResponse(response);
        privateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));


        //create mq connector;
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("MyMQConfig");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeIsSslEnabled", "true")
                .put("MqNativeIsSslKeystoreUsed", "true")
                .put("MqNativeSslKeystoreAlias", privateKeyItem.getContent().getAlias() )
                .put("MqNativeSslKeystoreId",privateKeyItem.getContent().getKeystoreId() )
                .put("MqNativePort", "1234")
                .put("MqNativeHostName", "host")
                .map());
        response = getSourceEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);

        mqNativeItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mqNativeItem.setContent(activeConnectorMO);

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
                "        <L7p:MqNativeRouting>\n" +
                "            <L7p:RequestMessageAdvancedProperties mapValueNull=\"null\"/>\n" +
                "            <L7p:RequestMqNativeMessagePropertyRuleSet mappingRuleSet=\"included\"/>\n" +
                "            <L7p:ResponseMessageAdvancedProperties mapValueNull=\"null\"/>\n" +
                "            <L7p:ResponseMqNativeMessagePropertyRuleSet mappingRuleSet=\"included\"/>\n" +
                "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
                "            </L7p:ResponseTarget>\n" +
                "            <L7p:SsgActiveConnectorGoid goidValue=\""+mqNativeItem.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorId goidValue=\""+mqNativeItem.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorName stringValue=\""+mqNativeItem.getName()+"\"/>\n" +
                "        </L7p:MqNativeRouting>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);

        // create target private key
        PrivateKeyCreationContext targetPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        targetPrivateKey.setDn("CN=targetAlias");
        targetPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getTargetEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":targetAlias", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(targetPrivateKey)));
        assertOkCreatedResponse(response);
        targetPrivateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

    }

    @After
    public void after() throws Exception {

        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response;

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("activeConnectors/" + mqNativeItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getTargetEnvironment().processRequest("policies/" , HttpMethod.GET, null, "");
        response = getTargetEnvironment().processRequest("privateKeys/" , HttpMethod.GET, null, "");
        response = getTargetEnvironment().processRequest("activeConnectors/" , HttpMethod.GET, null, "");
    }

    @Test
    public void testImportNewFail() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 item. A policy, active connector", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import fail
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(409, response.getStatus());
        Item<Mappings> mappingsReturned = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals(Mapping.ErrorType.TargetNotFound, mappingsReturned.getContent().getMappings().get(0).getErrorType());
    }

    @Test
    public void testImportMap() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 item. A policy, active connector", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        // update mapping
        bundleItem.getContent().getMappings().get(0).setTargetId(targetPrivateKeyItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(targetPrivateKeyItem.getId(), privateKeyMapping.getTargetId());

        Mapping mqMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, mqMapping.getActionTaken());
        Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
        Assert.assertEquals(mqMapping.getSrcId(), mqMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        mappingsToClean = mappings;

        // validate dependency
        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(2, policyDependencies.size());

        DependencyMO privateKeyDependency = getDependency(policyDependencies, privateKeyMapping.getTargetId());
        Assert.assertNotNull(privateKeyDependency);
        Assert.assertEquals(targetPrivateKeyItem.getName(), privateKeyDependency.getName());
        Assert.assertEquals(targetPrivateKeyItem.getId(), privateKeyDependency.getId());

        validate(mappings);
    }

    @Test
    public void testImportAlreadyExisting() throws Exception {
        RestResponse response;

        // create target private key
        PrivateKeyCreationContext targetPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        targetPrivateKey.setDn("CN=alias");
        targetPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getTargetEnvironment().processRequest("privateKeys/"+ privateKeyItem.getId(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(targetPrivateKey)));
        assertOkCreatedResponse(response);
        Item<PrivateKeyMO> createdKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 item. A policy, active connector", 2, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
            Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
            Assert.assertEquals(createdKeyItem.getId(), privateKeyMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(mqMapping.getSrcId(), mqMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getSrcId());
            Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            mappingsToClean = mappings;

            // validate dependency
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO privateKeyDependency = getDependency(policyDependencies, privateKeyMapping.getTargetId());
            Assert.assertNotNull(privateKeyDependency);
            Assert.assertEquals(createdKeyItem.getName(), privateKeyDependency.getName());
            Assert.assertEquals(createdKeyItem.getId(), privateKeyDependency.getId());

            validate(mappings);

        }finally{
            response = getTargetEnvironment().processRequest("privateKeys/" + createdKeyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
        }
    }

    @Test
    public void mapFtpsRoutingAssertionTest() throws Exception{
        // update policy to use FtpsRoutingAssertion
        RestResponse response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        PolicyMO policyMO = policyItem.getContent();
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:FtpRoutingAssertion>\n" +
                "            <L7p:Arguments stringValue=\"hr\"/>\n" +
                "            <L7p:ClientCertKeyAlias stringValue=\""+ privateKeyItem.getContent().getAlias()+"\"/>\n" +
                "            <L7p:ClientCertKeystoreId goidValue=\""+ privateKeyItem.getContent().getKeystoreId()+"\"/>\n" +
                "            <L7p:CredentialsSource credentialsSource=\"passThru\"/>\n" +
                "            <L7p:Directory stringValue=\"hr\"/>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "            <L7p:HostName stringValue=\"rdhzf\"/>\n" +
                "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
                "            </L7p:ResponseTarget>\n" +
                "            <L7p:Security security=\"ftpsExplicit\"/>\n" +
                "            <L7p:UseClientCert booleanValue=\"true\"/>\n" +
                "            <L7p:UserName stringValue=\"\"/>\n" +
                "        </L7p:FtpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        resource.setContent(assXml);
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        policyItem.setContent(policyMO);

        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 3 mappings. A policy, a folder and private key", 3, bundleItem.getContent().getMappings().size());

        // update mapping
        bundleItem.getContent().getMappings().get(0).setTargetId(targetPrivateKeyItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(targetPrivateKeyItem.getId(), privateKeyMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        mappingsToClean = mappings;

        // validate dependency
        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO privateKeyDependency = getDependency(policyDependencies, privateKeyMapping.getTargetId());
        Assert.assertNotNull(privateKeyDependency);
        Assert.assertEquals(targetPrivateKeyItem.getName(), privateKeyDependency.getName());
        Assert.assertEquals(targetPrivateKeyItem.getId(), privateKeyDependency.getId());

        validate(mappings);
    }
}
