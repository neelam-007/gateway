package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.RunOnNightly;
import org.junit.*;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Ignore
public abstract class MigrationTestBase {
    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static JVMDatabaseBasedRestManagementEnvironment sourceEnvironment;
    private static JVMDatabaseBasedRestManagementEnvironment targetEnvironment;

    @BeforeClass
    public static void beforeClass() throws IOException, IllegalAccessException, InstantiationException {
        if (RunOnNightly.isNightly()) {
            sourceEnvironment = new JVMDatabaseBasedRestManagementEnvironment("srcgateway");
            targetEnvironment = new JVMDatabaseBasedRestManagementEnvironment("trggateway");
        }
    }

    @AfterClass
    public static void afterClass() {
        if (sourceEnvironment != null) {
            sourceEnvironment.close();
        }
        if (targetEnvironment != null) {
            targetEnvironment.close();
        }
    }

    public static JVMDatabaseBasedRestManagementEnvironment getSourceEnvironment() {
        return sourceEnvironment;
    }

    public static JVMDatabaseBasedRestManagementEnvironment getTargetEnvironment() {
        return targetEnvironment;
    }

    protected void assertOkCreatedResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(201, response.getStatus());
        Assert.assertNotNull(response.getBody());
    }

    protected void assertOkResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
    }

    protected void assertOKDeleteResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    protected void cleanupAll(Item<Mappings> mappings) throws Exception {
        List<Mapping> reverseMappingsList = mappings.getContent().getMappings();
        Collections.reverse(reverseMappingsList);
        for (Mapping mapping : reverseMappingsList) {
            Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
            String uri = getUri(mapping.getTargetUri());
            RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.DELETE, null, "");
            assertOKDeleteResponse(response);
        }
    }

    private String getUri(String uri) {
        return uri == null ? null : uri.substring(uri.indexOf("/restman/1.0/") + 13);
    }

    protected String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }
}
