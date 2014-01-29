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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class MigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(MigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<StoredPasswordMO> securePasswordItem;
    private Item<JDBCConnectionMO> jdbcConnectionItem;

    @Before
    public void before() throws Exception {
        //create secure password;
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("MyPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        RestResponse response = getSourceEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);

        securePasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securePasswordItem.setContent(storedPasswordMO);

        //create jdbc connection;
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionMO.setName("MyJDBCConnection");
        jdbcConnectionMO.setEnabled(false);
        jdbcConnectionMO.setDriverClass("com.l7tech.jdbc.mysql.MySQLDriver");
        jdbcConnectionMO.setJdbcUrl("jdbcUrl");
        jdbcConnectionMO.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("password", "${secpass.MyPassword.plaintext}")
                .put("user", "jdbcUserName")
                .map());
        response = getSourceEnvironment().processRequest("jdbcConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jdbcConnectionMO)));

        assertOkCreatedResponse(response);

        jdbcConnectionItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jdbcConnectionItem.setContent(jdbcConnectionMO);

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
                "        <L7p:JdbcQuery>\n" +
                "            <L7p:ConnectionName stringValue=\"MyJDBCConnection\"/>\n" +
                "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
                "            <L7p:SqlQuery stringValue=\"select * from test;\"/>\n" +
                "        </L7p:JdbcQuery>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = getSourceEnvironment().processRequest("jdbcConnections/" + jdbcConnectionItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, jdbcConnection and secure password", 3, bundleItem.getContent().getReferences().size());

        //change the secure password MO to contain a password.
        ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.JDBC_CONNECTION.toString(), jdbcMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
        Assert.assertEquals(jdbcConnectionItem.getId(), jdbcMapping.getSrcId());
        Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        cleanupAll(mappings);
    }

    @Test
    public void testMapToExistingPasswordSameGoid() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        storedPasswordMO.setId(securePasswordItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, jdbcConnection and secure password", 3, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.JDBC_CONNECTION.toString(), jdbcMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
        Assert.assertEquals(jdbcConnectionItem.getId(), jdbcMapping.getSrcId());
        Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        cleanupAll(mappings);
    }

    @Test
    public void testMapToExistingPasswordDifferentGoid() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, jdbcConnection and secure password", 3, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the password to the existing one
        bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

        Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.JDBC_CONNECTION.toString(), jdbcMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
        Assert.assertEquals(jdbcConnectionItem.getId(), jdbcMapping.getSrcId());
        Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        cleanupAll(mappings);
    }
}
