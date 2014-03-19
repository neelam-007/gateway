package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidRange;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.junit.*;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Ignore
public abstract class MigrationTestBase {
    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static JVMDatabaseBasedRestManagementEnvironment sourceEnvironment;
    private static JVMDatabaseBasedRestManagementEnvironment targetEnvironment;

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (!IgnoreOnDaily.isDaily()) {
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
            final CountDownLatch enviromentsStarted = new CountDownLatch(2);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sourceEnvironment = new JVMDatabaseBasedRestManagementEnvironment("srcgateway");
                    } catch (IOException e) {
                        exceptions.add(e);
                    }
                    enviromentsStarted.countDown();
                }
            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        targetEnvironment = new JVMDatabaseBasedRestManagementEnvironment("trggateway");
                    } catch (IOException e) {
                        exceptions.add(e);
                    }
                    enviromentsStarted.countDown();
                }
            }).start();
            enviromentsStarted.await(5, TimeUnit.MINUTES);
            if(!exceptions.isEmpty()){
                throw exceptions.get(0);
            }
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

    protected void assertNotFoundResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(404, response.getStatus());
    }

    protected void assertOkDeleteResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    protected void cleanupAll(Item<Mappings> mappings) throws Exception {
        List<Mapping> reverseMappingsList = mappings.getContent().getMappings();
        Collections.reverse(reverseMappingsList);
        for (Mapping mapping : reverseMappingsList) {
            if(mapping.getErrorType() == null && !GoidRange.RESERVED_RANGE.isInRange(Goid.parseGoid(mapping.getTargetId())) && mapping.getActionTaken()== Mapping.ActionTaken.CreatedNew){
                Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
                String uri = getUri(mapping.getTargetUri());
                RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.DELETE, null, "");
                assertOkDeleteResponse(response);
            }
        }
    }

    protected void validateNotFound(Item<Mappings> mappings) throws Exception {
        for (Mapping mapping : mappings.getContent().getMappings()) {
            if(mapping.getErrorType() == null){
                Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
                String uri = getUri(mapping.getTargetUri());
                RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.GET, null, "");
                assertNotFoundResponse(response);
            }
        }
    }

    protected void validate(Item<Mappings> mappings) throws Exception {
        for (Mapping mapping : mappings.getContent().getMappings()) {
            if(mapping.getErrorType() == null){
                Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
                String uri = getUri(mapping.getTargetUri());
                RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.GET, null, "");
                assertOkResponse(response);
            }
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
