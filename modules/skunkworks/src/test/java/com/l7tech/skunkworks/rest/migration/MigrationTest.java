package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.skunkworks.rest.tools.JVMDatabaseBasedRestManagementEnvironment;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.RunOnNightly;
import com.l7tech.util.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = RunOnNightly.class)
public class MigrationTest {
    private static final Logger logger = Logger.getLogger(MigrationTest.class.getName());

    private static JVMDatabaseBasedRestManagementEnvironment sourceEnvironment;
    private static JVMDatabaseBasedRestManagementEnvironment targetEnvironment;
    private Item policyItem;
    private Item securePasswordItem;
    private Item jdbcConnectionItem;

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    @BeforeClass
    public static void beforeClass() throws IOException, IllegalAccessException, InstantiationException {
        if (!RunOnNightly.class.newInstance().isSatisfied()) {
            sourceEnvironment = new JVMDatabaseBasedRestManagementEnvironment("srcgateway");
            targetEnvironment = new JVMDatabaseBasedRestManagementEnvironment("trggateway");
        }
    }

    @AfterClass
    public static void afterClass() {
        if (sourceEnvironment != null) {
            sourceEnvironment.close();
        }
        if(targetEnvironment != null){
            targetEnvironment.close();
        }
    }

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
        RestResponse response = sourceEnvironment.processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);

        securePasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

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
        response = sourceEnvironment.processRequest("jdbcConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jdbcConnectionMO)));

        assertOkCreatedResponse(response);

        jdbcConnectionItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

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

        response = sourceEnvironment.processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }

    private void assertOkCreatedResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(201, response.getStatus());
        Assert.assertNotNull(response.getBody());
    }

    private void assertOkResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
    }

    @After
    public void after() throws Exception {
        RestResponse response = sourceEnvironment.processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = sourceEnvironment.processRequest("jdbcConnections/" + jdbcConnectionItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = sourceEnvironment.processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);
    }

    private void assertOKDeleteResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    @Test
    public void test() throws Exception {
        RestResponse response = sourceEnvironment.processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        for (Item reference : bundleItem.getContent().getReferences()) {
            if (securePasswordItem.getId().equals(reference.getId())) {
                ((StoredPasswordMO) reference.getContent()).setPassword("password");
                break;
            }
        }

        response = targetEnvironment.processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        cleanup(mappings);
    }

    private void cleanup(Item<Mappings> mappings) throws Exception {
        List<Mapping> reverseMappingsList = mappings.getContent().getMappings();
        Collections.reverse(reverseMappingsList);
        for (Mapping mapping : reverseMappingsList) {
            if (Mapping.ActionTaken.CreatedNew.equals(mapping.getActionTaken())) {
                Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
                String uri = getUri(mapping.getTargetUri());
                RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.DELETE, null, "");
                assertOKDeleteResponse(response);
            }
        }
    }

    private String getUri(String uri) {
        return uri==null?null:uri.substring(uri.indexOf("/restman/1.0/") + 13);
    }

    private String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }
}
