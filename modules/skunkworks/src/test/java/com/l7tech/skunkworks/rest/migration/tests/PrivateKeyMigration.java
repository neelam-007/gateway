package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.api.impl.PrivateKeyExportContext;
import com.l7tech.gateway.api.impl.PrivateKeyExportResult;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PrivateKeyMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(PrivateKeyMigration.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<PrivateKeyMO> privateKeyItem;
    private Item<PrivateKeyMO> privateKeyZonedItem;
    private Item<SecurityZoneMO> securityZoneItem;
    private Key privateKeyItemKey;
    private Key targetPrivateKeyItemKey;
    private Item<ActiveConnectorMO> mqNativeItem;
    private Item<Mappings> mappingsToClean;

    private String password = "password";

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

        //get the private key data
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setPassword(password);

        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem.getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));
        assertOkResponse(response);

        Item<PrivateKeyExportResult> privateKeyItemExport = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(privateKeyItemExport.getContent().getPkcs12Data()), password.toCharArray());
        privateKeyItemKey = keyStore.getKey(privateKeyItem.getContent().getAlias(), password.toCharArray());

        // create security zone
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("MySecurityZone");
        securityZoneMO.setDescription("MySecurityZone description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        response = getSourceEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        securityZoneItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securityZoneItem.setContent(securityZoneMO);

        // create private key with zone
        createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=srcAliasZone");
        createPrivateKey.setSecurityZoneId(securityZoneItem.getId());
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        response = getSourceEnvironment().processRequest("privateKeys/"+ new Goid(0,2).toString() + ":srcAliasZone", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyZonedItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getSourceEnvironment().processRequest("privateKeys/"+ privateKeyZonedItem.getId(), HttpMethod.GET,null,"");
        assertOkResponse(response);
        privateKeyZonedItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));


        //create mq connector;
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("MyMQConfig");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeIsSslEnabled", "true")
                .put("MqNativeIsSslKeystoreUsed", "true")
                .put("MqNativeSslKeystoreAlias", privateKeyItem.getContent().getAlias())
                .put("MqNativeSslKeystoreId", privateKeyItem.getContent().getKeystoreId())
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
                "            <L7p:SsgActiveConnectorGoid goidValue=\"" + mqNativeItem.getId() + "\"/>\n" +
                "            <L7p:SsgActiveConnectorId goidValue=\"" + mqNativeItem.getId() + "\"/>\n" +
                "            <L7p:SsgActiveConnectorName stringValue=\"" + mqNativeItem.getName() + "\"/>\n" +
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
        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        targetPrivateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //get the private key data
        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));
        assertOkResponse(response);

        Item<PrivateKeyExportResult> targetPrivateKeyItemExport = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(targetPrivateKeyItemExport.getContent().getPkcs12Data()), password.toCharArray());
        targetPrivateKeyItemKey = keyStore.getKey(targetPrivateKeyItem.getContent().getAlias(), password.toCharArray());
    }

    @After
    public void after() throws Exception {

        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response;

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("activeConnectors/" + mqNativeItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyZonedItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?privateKey=" + privateKeyItem.getId()+ "&encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnorePrivateKeyDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?privateKey=" + privateKeyItem.getId() + "&requirePrivateKey=" + privateKeyItem.getId()+ "&encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A privateKey", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A privateKey", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
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
    public void testImportNewEncryptPasswords() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, active connector, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getTargetId());

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
        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        validate(mappings);
    }

    @Test
    public void testImportNewEncryptPasswordsWithSecurityZone() throws Exception {
        // update policy to use private key with zone
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
                        "            <L7p:ClientCertKeyAlias stringValue=\""+ privateKeyZonedItem.getContent().getAlias()+"\"/>\n" +
                        "            <L7p:ClientCertKeystoreId goidValue=\""+ privateKeyZonedItem.getContent().getKeystoreId()+"\"/>\n" +
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

        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, security zone, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, security zone and private key", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

        Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
        Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
        Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

        Mapping privateKeyMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyZonedItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(privateKeyZonedItem.getId(), privateKeyMapping.getTargetId());

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
        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyZonedItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        Item<PrivateKeyMO> targetKey =  MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertNotNull(targetKey.getContent().getSecurityZoneId());
        Assert.assertEquals(securityZoneItem.getId(), targetKey.getContent().getSecurityZoneId());

        validate(mappings);

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
    public void testImportUpdateEncryptPasswords() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, active connector, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        // update mapping
        bundleItem.getContent().getMappings().get(0).setTargetId(targetPrivateKeyItem.getId());
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, privateKeyMapping.getActionTaken());
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

        validate(mappings);

        // validate the the private key data is the same as the source
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setPassword(password);

        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));
        assertOkResponse(response);

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> item = MarshallingUtils.unmarshal(Item.class, source);
        KeyStore inks = KeyStore.getInstance("PKCS12");
        inks.load(new ByteArrayInputStream(item.getContent().getPkcs12Data()), password.toCharArray());
        Key key = inks.getKey(targetPrivateKeyItem.getContent().getAlias(), password.toCharArray());

        assertArrayEquals(privateKeyItemKey.getEncoded(), key.getEncoded());

    }

    @Test
    public void testImportExistingEncryptPasswords() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, active connector, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        // update mapping
        bundleItem.getContent().getMappings().get(0).setTargetId(targetPrivateKeyItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
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

        validate(mappings);

        // validate the the private key data is the same as the target
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setPassword(password);

        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId() + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));
        assertOkResponse(response);

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> item = MarshallingUtils.unmarshal(Item.class, source);
        KeyStore inks = KeyStore.getInstance("PKCS12");
        inks.load(new ByteArrayInputStream(item.getContent().getPkcs12Data()), password.toCharArray());
        Key key = inks.getKey(targetPrivateKeyItem.getContent().getAlias(), password.toCharArray());

        assertArrayEquals(targetPrivateKeyItemKey.getEncoded(), key.getEncoded());

    }

    @BugId("SSG-10583")
    @Test
    public void testImportCreateEncryptSecretTest() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, active connector, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        response = getTargetEnvironment().processRequest("privateKeys/", HttpMethod.GET, null,"");
        assertOkResponse(response);

        ItemsList<PrivateKeyMO> items = MarshallingUtils.unmarshal(ItemsList.class,  new StreamSource(new StringReader(response.getBody())));
        final int originalKeyCount = items.getContent().size();


        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", "test=true", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(privateKeyMapping.getSrcId(), privateKeyMapping.getTargetId());

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

        // validate the the private key is not created
        response = getTargetEnvironment().processRequest("privateKeys/", HttpMethod.GET, null,"");
        assertOkResponse(response);

        items = MarshallingUtils.unmarshal(ItemsList.class,  new StreamSource(new StringReader(response.getBody())));
        assertEquals(originalKeyCount,items.getContent().size());
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
            assertOkEmptyResponse(response);
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

    @Test
    public void deleteMappingTest() throws Exception {
        // create target private key
        PrivateKeyCreationContext targetPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        targetPrivateKey.setDn("CN=targetAlias2");
        targetPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("privateKeys/" + new Goid(0, 2).toString() + ":targetAlias2", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(targetPrivateKey)));
        assertOkCreatedResponse(response);
        Item targetPrivateKeyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // create target private key
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setName("temp");
        folderMO.setId(Goid.DEFAULT_GOID.toString());
        Item<FolderMO> folderItem = new ItemBuilder<FolderMO>("temp", EntityType.FOLDER.toString()).setContent(folderMO).build();
        folderItem.setId(Goid.DEFAULT_GOID.toString());

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(targetPrivateKeyItem2.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(targetPrivateKeyItem2.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString()+":asd");
        mappingNotExisting.setType(targetPrivateKeyItem2.getType());

        Mapping mappingInvalidId = ManagedObjectFactory.createMapping();
        mappingInvalidId.setAction(Mapping.Action.Delete);
        mappingInvalidId.setSrcId("asdslkjdhfakd");
        mappingInvalidId.setType(targetPrivateKeyItem2.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting, mappingInvalidId));
        bundle.setReferences(Arrays.<Item>asList(folderItem));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mapping after the import", 3, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, privateKeyMapping.getActionTaken());
        Assert.assertEquals(targetPrivateKeyItem2.getId(), privateKeyMapping.getTargetId());

        Mapping privateKeyMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, privateKeyMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, privateKeyMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, privateKeyMappingNotExisting.getTargetId());

        Mapping privateKeyMappingInvalidId = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMappingInvalidId.getType());
        Assert.assertEquals(Mapping.Action.Delete, privateKeyMappingInvalidId.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, privateKeyMappingInvalidId.getActionTaken());
        Assert.assertEquals(null, privateKeyMappingInvalidId.getTargetId());

        response = getTargetEnvironment().processRequest("privateKeys/"+targetPrivateKeyItem2.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }

    @Test
    public void testImportCreateNewMapByIdDifferentAlias() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, active connector, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        // update mapping
        String targetAlias = "CreateNewMapById";
        bundleItem.getContent().getMappings().get(0).setTargetId(targetPrivateKeyItem.getContent().getKeystoreId() + ":" + targetAlias);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(privateKeyItem.getContent().getKeystoreId() + ":" + targetAlias, privateKeyMapping.getTargetId());

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

        validate(mappings);

        //validate that the alias was changed correctly
        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItem.getContent().getKeystoreId() + ":" + targetAlias, HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyMO> item = MarshallingUtils.unmarshal(Item.class, source);
        Assert.assertEquals(targetAlias, item.getContent().getAlias());


        // validate the the private key data is the same as the target
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setPassword(password);

        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItem.getContent().getKeystoreId() + ":" + targetAlias + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));
        assertOkResponse(response);

        source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> itemPrivateKeyExportResult = MarshallingUtils.unmarshal(Item.class, source);
        KeyStore inks = KeyStore.getInstance("PKCS12");
        inks.load(new ByteArrayInputStream(itemPrivateKeyExportResult.getContent().getPkcs12Data()), password.toCharArray());
        Key key = inks.getKey(targetAlias, password.toCharArray());

        assertArrayEquals(privateKeyItemKey.getEncoded(), key.getEncoded());

    }

    @Test
    public void testImportCreateNewMapByNameDifferentAlias() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, active connector, private key", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, a folder, active connector and private key", 4, bundleItem.getContent().getMappings().size());

        // update mapping
        String targetAlias = "CreateNewMapById";
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.MapBuilder.<String,Object>builder().put("MapBy", "name").put("MapTo", targetAlias).map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));

        // import success
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertOkResponse(response);
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, privateKeyMapping.getActionTaken());
        Assert.assertEquals(privateKeyItem.getId(), privateKeyMapping.getSrcId());
        Assert.assertEquals(targetPrivateKeyItem.getContent().getKeystoreId() + ":" + targetAlias, privateKeyMapping.getTargetId());

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

        validate(mappings);

        //validate that the alias was changed correctly
        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItem.getContent().getKeystoreId() + ":" + targetAlias, HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyMO> item = MarshallingUtils.unmarshal(Item.class, source);
        Assert.assertEquals(targetAlias, item.getContent().getAlias());


        // validate the the private key data is the same as the target
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setPassword(password);

        response = getTargetEnvironment().processRequest("privateKeys/" + privateKeyItem.getContent().getKeystoreId() + ":" + targetAlias + "/export", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(privateKeyExportContext));
        assertOkResponse(response);

        source = new StreamSource(new StringReader(response.getBody()));
        Item<PrivateKeyExportResult> itemPrivateKeyExportResult = MarshallingUtils.unmarshal(Item.class, source);
        KeyStore inks = KeyStore.getInstance("PKCS12");
        inks.load(new ByteArrayInputStream(itemPrivateKeyExportResult.getContent().getPkcs12Data()), password.toCharArray());
        Key key = inks.getKey(targetAlias, password.toCharArray());

        assertArrayEquals(privateKeyItemKey.getEncoded(), key.getEncoded());

    }
}
