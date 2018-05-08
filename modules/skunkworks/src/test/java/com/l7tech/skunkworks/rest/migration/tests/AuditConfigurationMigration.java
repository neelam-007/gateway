package com.l7tech.skunkworks.rest.migration.tests;

import static org.junit.Assert.*;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.AuditConfigurationMO;
import com.l7tech.gateway.api.AuditFtpConfig;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.PolicyDetail;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import java.io.StringReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import org.junit.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class AuditConfigurationMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(AuditConfigurationMigration.class.getName());

    private Item<PolicyMO> sinkPolicyItem;
    private Item<PolicyMO> lookupPolicyItem;
    private Item<AuditConfigurationMO> auditConfigurationItem;
    private Item<Mappings> mappingsToClean;

    private static ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator = new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();

    @Before
    public void before() throws Exception {
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        //create sink policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("SinkPolicy");
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
        resource.setContent(assXml);

        RestResponse response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        sinkPolicyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        sinkPolicyItem.setContent(policyMO);

        //create lookup policy;
        policyMO = ManagedObjectFactory.createPolicy();
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("LookupPolicy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent(assXml);

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        lookupPolicyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        lookupPolicyItem.setContent(policyMO);

        // create audit config:
        AuditConfigurationMO configurationMO = ManagedObjectFactory.createAuditConfiguration();
        configurationMO.setAlwaysSaveInternal(false);
        configurationMO.setSinkPolicyReference(new ManagedObjectReference(PolicyMO.class,sinkPolicyItem.getId()));
        configurationMO.setLookupPolicyReference(new ManagedObjectReference(PolicyMO.class,lookupPolicyItem.getId()));
        AuditFtpConfig ftpConfigMO = ManagedObjectFactory.createAuditFtpConfig();
        ftpConfigMO.setHost("Host");
        ftpConfigMO.setPort(123);
        ftpConfigMO.setTimeout(123);
        ftpConfigMO.setUser("user");
        ftpConfigMO.setPasswordValue("password");
        ftpConfigMO.setDirectory("directory");
        ftpConfigMO.setVerifyServerCert(true);
        ftpConfigMO.setSecurity(AuditFtpConfig.SecurityType.ftpsExplicit);
        ftpConfigMO.setEnabled(true);
        configurationMO.setFtpConfig(ftpConfigMO);
        response = getSourceEnvironment().processRequest("auditConfiguration/default", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(configurationMO)));

        assertOkResponse(response);

        auditConfigurationItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        auditConfigurationItem.setContent(configurationMO);



    }

    @After
    public void after() throws Exception {
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + sinkPolicyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + lookupPolicyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportInclude() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?identityProvider=0000000000000000fffffffffffffffe&includeGatewayConfiguration=true" , HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 4 items. ", 4, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 5 items.", 5, bundleItem.getContent().getMappings().size());

        assertEquals( EntityType.ID_PROVIDER_CONFIG.toString(), bundleItem.getContent().getMappings().get(0).getType());
        assertEquals( EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        assertEquals( EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());
        assertEquals( sinkPolicyItem.getId(), bundleItem.getContent().getMappings().get(2).getSrcId());
        assertEquals( EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(3).getType());
        assertEquals( lookupPolicyItem.getId(), bundleItem.getContent().getMappings().get(3).getSrcId());
        assertEquals( EntityType.AUDIT_CONFIG.toString(), bundleItem.getContent().getMappings().get(4).getType());
        assertEquals( Mapping.Action.NewOrExisting, bundleItem.getContent().getMappings().get(4).getAction());
        assertTrue( bundleItem.getContent().getMappings().get(4).getSrcUri().endsWith("auditConfiguration/default"));
    }

    @Test
    public void testExportAll() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?all=true" , HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertTrue("Audit config not exported", Functions.exists(bundleItem.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
            @Override
            public Boolean call(Mapping mapping) {
                return mapping.getType().equals(EntityType.AUDIT_CONFIG.toString());
            }
        }));;
    }

    @Test
    public void testMigration() throws Exception {
        // export
        RestResponse response = getSourceEnvironment().processRequest("bundle?identityProvider=0000000000000000fffffffffffffffe&includeGatewayConfiguration=true" , HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // update mappping for import
        bundleItem.getContent().getMappings().get(4).setAction(Mapping.Action.NewOrUpdate);

        // import
        response = getTargetEnvironment().processRequest("bundle" , HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        //verify the mappings
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;
        Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getSrcId());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getTargetId());

        Mapping sinkMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), sinkMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, sinkMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, sinkMapping.getActionTaken());
        Assert.assertEquals(sinkPolicyItem.getId(), sinkMapping.getSrcId());
        Assert.assertEquals(sinkMapping.getSrcId(), sinkMapping.getTargetId());

        Mapping lookupMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), lookupMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, lookupMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, lookupMapping.getActionTaken());
        Assert.assertEquals(lookupPolicyItem.getId(), lookupMapping.getSrcId());
        Assert.assertEquals(lookupMapping.getSrcId(), lookupMapping.getTargetId());

        Mapping auditConfigMapping = mappings.getContent().getMappings().get(4);
        Assert.assertEquals(EntityType.AUDIT_CONFIG.toString(), auditConfigMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, auditConfigMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, auditConfigMapping.getActionTaken());
        Assert.assertEquals(auditConfigurationItem.getId(), auditConfigMapping.getSrcId());
        Assert.assertEquals(auditConfigMapping.getSrcId(), auditConfigMapping.getTargetId());

        // verify audit config settings
        response = getTargetEnvironment().processRequest("auditConfiguration" , HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        ItemsList<AuditConfigurationMO> auditConfigList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals(1, auditConfigList.getContent().size());
        AuditConfigurationMO targetAuditConfig = auditConfigList.getContent().get(0).getContent();
        assertEquals(auditConfigurationItem.getContent().getAlwaysSaveInternal(), targetAuditConfig.getAlwaysSaveInternal());
        assertEquals(auditConfigurationItem.getContent().getSinkPolicyReference().getId(), targetAuditConfig.getSinkPolicyReference().getId());
        assertEquals(auditConfigurationItem.getContent().getLookupPolicyReference().getId(), targetAuditConfig.getLookupPolicyReference().getId());

        assertEquals(auditConfigurationItem.getContent().getFtpConfig().getHost(), targetAuditConfig.getFtpConfig().getHost());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().getPort(), targetAuditConfig.getFtpConfig().getPort());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().getTimeout(), targetAuditConfig.getFtpConfig().getTimeout());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().getUser(), targetAuditConfig.getFtpConfig().getUser());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().getDirectory(), targetAuditConfig.getFtpConfig().getDirectory());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().isVerifyServerCert(), targetAuditConfig.getFtpConfig().isVerifyServerCert());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().getSecurity(), targetAuditConfig.getFtpConfig().getSecurity());
        assertEquals(auditConfigurationItem.getContent().getFtpConfig().isEnabled(), targetAuditConfig.getFtpConfig().isEnabled());



    }


    @Test
    public void getDependenciesTest() throws Exception {

        // get dependencies
        RestResponse response = getSourceEnvironment().processRequest("auditConfiguration/default/dependencies", HttpMethod.GET, null, "");

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<DependencyListMO> dependencyResource = MarshallingUtils.unmarshal(Item.class, source);

        // check results
        assertEquals(2, dependencyResource.getContent().getSearchObjectItem().getDependencies().size());
        assertEquals(EntityType.POLICY.toString(), dependencyResource.getContent().getSearchObjectItem().getDependencies().get(0).getType());
        assertEquals(EntityType.POLICY.toString(), dependencyResource.getContent().getSearchObjectItem().getDependencies().get(1).getType());
    }

    protected Goid getGoid() {
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }
}
