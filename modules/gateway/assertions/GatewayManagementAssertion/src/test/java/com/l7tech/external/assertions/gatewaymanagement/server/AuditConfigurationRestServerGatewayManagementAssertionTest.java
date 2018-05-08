package com.l7tech.external.assertions.gatewaymanagement.server;

import static org.junit.Assert.assertEquals;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.AuditConfigurationMO;
import com.l7tech.gateway.api.AuditFtpConfig;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.policy.PolicyManagerStub;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class AuditConfigurationRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(AuditConfigurationRestServerGatewayManagementAssertionTest.class.getName());

    private static MockClusterPropertyManager clusterPropertyManagerStub;
    private static PolicyManagerStub policyManagerStub;
    private static final String auditConfigurationBasePath = "auditConfiguration/";

    private static Policy lookupPolicy;
    private static Policy sinkPolicy;
    private static FtpClientConfig ftpClientConfig;

    @Before
    public void before() throws Exception {
        super.before();
        clusterPropertyManagerStub = applicationContext.getBean("clusterPropertyManager", MockClusterPropertyManager.class);
        policyManagerStub = applicationContext.getBean("policyManager", PolicyManagerStub.class);
        lookupPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT,"lookup",null,false);
        lookupPolicy.setGuid(UUID.randomUUID().toString());
        lookupPolicy.setGoid(policyManagerStub.save(lookupPolicy));
        sinkPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT,"sink",null,false);
        sinkPolicy.setGuid(UUID.randomUUID().toString());
        sinkPolicy.setGoid(policyManagerStub.save( sinkPolicy));

        ftpClientConfig = FtpClientConfigImpl.newFtpConfig("host");

    }

    @After
    public void after() throws Exception {
        super.after();

        Collection<EntityHeader> entities = new ArrayList<>(clusterPropertyManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            clusterPropertyManagerStub.delete(entity.getGoid());
        }

        policyManagerStub.delete(lookupPolicy.getGoid());
        policyManagerStub.delete(sinkPolicy.getGoid());
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }


    @Test
    public void updateEntityTest() throws Exception {

        AuditConfigurationMO configurationMO = ManagedObjectFactory.createAuditConfiguration();
        configurationMO.setAlwaysSaveInternal(false);
        configurationMO.setSinkPolicyReference(new ManagedObjectReference(PolicyMO.class,sinkPolicy.getId()));
        configurationMO.setLookupPolicyReference(new ManagedObjectReference(PolicyMO.class,lookupPolicy.getId()));
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

        // update
        String payload = XmlUtil.nodeToString(ManagedObjectFactory.write(configurationMO));
        System.out.print(payload);
        RestResponse response = processRequest(auditConfigurationBasePath+"/default", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), payload);

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity

        assertEquals("AlwaysSaveInternal:", "false", clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK));
        assertEquals("SinkPolicyGuid:", sinkPolicy.getGuid(), clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID));
        assertEquals("LookupPolicyGuid:", lookupPolicy.getGuid(), clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID));

        FtpClientConfig resultFtpConfig = FtpUtils.deserialize(clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION));
        assertEquals(ftpConfigMO.getHost(), resultFtpConfig.getHost());
        assertEquals(ftpConfigMO.getPort(), resultFtpConfig.getPort());
        assertEquals(ftpConfigMO.getTimeout().longValue(), resultFtpConfig.getTimeout());
        assertEquals(ftpConfigMO.getUser(), resultFtpConfig.getUser());
        assertEquals(ftpConfigMO.getPasswordValue(), resultFtpConfig.getPass());
        assertEquals(ftpConfigMO.getDirectory(), resultFtpConfig.getDirectory());
        assertEquals(ftpConfigMO.isVerifyServerCert(), resultFtpConfig.isVerifyServerCert());
        assertEquals(ftpConfigMO.getSecurity().toString(), resultFtpConfig.getSecurity().getWspName());
        assertEquals(ftpConfigMO.isEnabled(), resultFtpConfig.isEnabled());

        // get
        response = processRequest(auditConfigurationBasePath +"/default" , HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Item<AuditConfigurationMO> updatedAuditConfigItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        AuditConfigurationMO updatedAuditConfig = updatedAuditConfigItem.getContent();
        assertEquals(configurationMO.getAlwaysSaveInternal(), updatedAuditConfig.getAlwaysSaveInternal());
        assertEquals(configurationMO.getSinkPolicyReference().getId(), updatedAuditConfig.getSinkPolicyReference().getId());
        assertEquals(configurationMO.getLookupPolicyReference().getId(), updatedAuditConfig.getLookupPolicyReference().getId());

        assertEquals(configurationMO.getFtpConfig().getHost(), updatedAuditConfig.getFtpConfig().getHost());
        assertEquals(configurationMO.getFtpConfig().getPort(), updatedAuditConfig.getFtpConfig().getPort());
        assertEquals(configurationMO.getFtpConfig().getTimeout(), updatedAuditConfig.getFtpConfig().getTimeout());
        assertEquals(configurationMO.getFtpConfig().getUser(), updatedAuditConfig.getFtpConfig().getUser());
        assertEquals(configurationMO.getFtpConfig().getDirectory(), updatedAuditConfig.getFtpConfig().getDirectory());
        assertEquals(configurationMO.getFtpConfig().isVerifyServerCert(), updatedAuditConfig.getFtpConfig().isVerifyServerCert());
        assertEquals(configurationMO.getFtpConfig().getSecurity(), updatedAuditConfig.getFtpConfig().getSecurity());
        assertEquals(configurationMO.getFtpConfig().isEnabled(), updatedAuditConfig.getFtpConfig().isEnabled());

    }

    @Test
    public void clearEntityTest() throws Exception {
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK,"false");
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID, sinkPolicy.getGuid());
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID, lookupPolicy.getGuid());
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION,FtpUtils.serialize(ftpClientConfig));

        AuditConfigurationMO configurationMO = ManagedObjectFactory.createAuditConfiguration();

        // update
        String payload = XmlUtil.nodeToString(ManagedObjectFactory.write(configurationMO));
        RestResponse response = processRequest(auditConfigurationBasePath+"/default", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), payload);

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity

        assertEquals("AlwaysSaveInternal:", "true", clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK));
        assertEquals("SinkPolicyGuid:", null, clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID));
        assertEquals("LookupPolicyGuid:", null, clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID));
        assertEquals("FtpConfig:", null, clusterPropertyManagerStub.getProperty(ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(auditConfigurationBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<AuditConfigurationMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(1, item.getContent().size());
        Item auditItem = item.getContent().get(0);
        Assert.assertEquals(AuditConfiguration.ENTITY_ID.toString(), auditItem.getId());
        Assert.assertEquals(AuditConfiguration.ENTITY_NAME, auditItem.getName());
        Assert.assertEquals(EntityType.AUDIT_CONFIG.toString(), auditItem.getType());
        Assert.assertEquals(1,auditItem.getLinks().size());
        Link auditLink = (Link)auditItem.getLinks().get(0);
        Assert.assertTrue( auditLink.getUri().endsWith("auditConfiguration/default"));

        AuditConfigurationMO mo = (AuditConfigurationMO)auditItem.getContent();
        Assert.assertEquals(true, mo.getAlwaysSaveInternal());
        Assert.assertEquals(null, mo.getSinkPolicyReference());
        Assert.assertEquals(null, mo.getLookupPolicyReference());
        Assert.assertEquals(null, mo.getFtpConfig());

    }

    @Test
    public void clearEntityWithDataTest() throws Exception {
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK,"false");
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID, sinkPolicy.getGuid());
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID, lookupPolicy.getGuid());
        clusterPropertyManagerStub.putProperty(ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION, FtpUtils.serialize(ftpClientConfig));

        RestResponse response = processRequest(auditConfigurationBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<AuditConfigurationMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(1, item.getContent().size());
        Item auditItem = item.getContent().get(0);
        Assert.assertEquals(AuditConfiguration.ENTITY_ID.toString(), auditItem.getId());
        Assert.assertEquals(AuditConfiguration.ENTITY_NAME, auditItem.getName());
        Assert.assertEquals(EntityType.AUDIT_CONFIG.toString(), auditItem.getType());

        AuditConfigurationMO mo = (AuditConfigurationMO)auditItem.getContent();
        Assert.assertEquals(Boolean.FALSE, mo.getAlwaysSaveInternal());
        Assert.assertEquals(sinkPolicy.getId(), mo.getSinkPolicyReference().getId());
        Assert.assertEquals(lookupPolicy.getId(), mo.getLookupPolicyReference().getId());
        Assert.assertEquals(ftpClientConfig.getHost(), mo.getFtpConfig().getHost());
    }

    @Test
    public void getEntitiesTest() throws Exception {

        RestResponse response = processRequest(auditConfigurationBasePath + "/default", HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<AuditConfigurationMO> item = MarshallingUtils.unmarshal(Item.class, source);

        // check entity
        Assert.assertEquals(AuditConfiguration.ENTITY_ID.toString(), item.getId());
        Assert.assertEquals(AuditConfiguration.ENTITY_NAME, item.getName());
        Assert.assertEquals(EntityType.AUDIT_CONFIG.toString(), item.getType());

        Assert.assertEquals(Link.LINK_REL_SELF, item.getLinks().get(0).getRel());
        Assert.assertTrue( item.getLinks().get(0).getUri().endsWith("auditConfiguration/default"));
        Assert.assertEquals(Link.LINK_REL_TEMPLATE, item.getLinks().get(1).getRel());
        Assert.assertTrue( item.getLinks().get(1).getUri().endsWith("auditConfiguration/template"));
        Assert.assertEquals(Link.LINK_REL_LIST, item.getLinks().get(2).getRel());
        Assert.assertTrue( item.getLinks().get(2).getUri().endsWith("auditConfiguration"));
        Assert.assertEquals("dependencies", item.getLinks().get(3).getRel());
        Assert.assertTrue( item.getLinks().get(3).getUri().endsWith("auditConfiguration/default/dependencies"));

        AuditConfigurationMO mo = item.getContent();
        Assert.assertEquals(true, mo.getAlwaysSaveInternal());
        Assert.assertEquals(null, mo.getSinkPolicyReference());
        Assert.assertEquals(null, mo.getLookupPolicyReference());
        Assert.assertEquals(null, mo.getFtpConfig());

    }

    @Test
    public void updateEntityWithNullFtpSecurityTest() throws Exception {

        AuditConfigurationMO configurationMO = ManagedObjectFactory.createAuditConfiguration();
        configurationMO.setAlwaysSaveInternal(false);
        configurationMO.setSinkPolicyReference(new ManagedObjectReference(PolicyMO.class, sinkPolicy.getId()));
        configurationMO.setLookupPolicyReference(new ManagedObjectReference(PolicyMO.class, lookupPolicy.getId()));
        AuditFtpConfig ftpConfigMO = ManagedObjectFactory.createAuditFtpConfig();
        ftpConfigMO.setHost("Host");
        ftpConfigMO.setPort(123);
        ftpConfigMO.setTimeout(123);
        ftpConfigMO.setUser("user");
        ftpConfigMO.setPasswordValue("password");
        ftpConfigMO.setDirectory("directory");
        ftpConfigMO.setVerifyServerCert(true);
        ftpConfigMO.setSecurity(null);
        ftpConfigMO.setEnabled(true);
        configurationMO.setFtpConfig(ftpConfigMO);

        // update
        String payload = XmlUtil.nodeToString(ManagedObjectFactory.write(configurationMO));
        System.out.print(payload);
        RestResponse response = processRequest(auditConfigurationBasePath + "/default", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), payload);

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
    }
}
