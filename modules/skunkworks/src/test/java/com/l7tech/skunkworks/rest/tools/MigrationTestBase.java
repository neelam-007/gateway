package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidRange;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Functions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

    protected void assertOkEmptyResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    protected void assertConflictResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(409, response.getStatus());
    }

    protected void cleanupAll(Item<Mappings> mappings) throws Exception {
        List<Mapping> reverseMappingsList = mappings.getContent().getMappings();
        Collections.reverse(reverseMappingsList);
        for (Mapping mapping : reverseMappingsList) {
            if(mapping.getErrorType() == null && !Mapping.ActionTaken.Ignored.equals(mapping.getActionTaken()) && !Mapping.ActionTaken.Deleted.equals(mapping.getActionTaken()) &&
                    ( mapping.getTargetId().length()!=16 || !GoidRange.RESERVED_RANGE.isInRange(Goid.parseGoid(mapping.getTargetId())))
                    && mapping.getActionTaken()== Mapping.ActionTaken.CreatedNew){
                Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
                String uri = getUri(mapping.getTargetUri());
                RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
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
            if(mapping.getErrorType() == null && mapping.getAction()!= Mapping.Action.Ignore){
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

    protected DependencyMO getDependency(List<DependencyMO> dependencies, final String id){
        final DependencyMO dependencyMO = Functions.grepFirst(dependencies, new Functions.Unary<Boolean, DependencyMO>() {
            @Override
            public Boolean call(DependencyMO dependency) {
                return id.equals(dependency.getId());
            }
        });
        return dependencyMO;
    }

    protected DependencyMO getDependencyByName(List<DependencyMO> dependencies, final String name){
        final DependencyMO dependencyMO = Functions.grepFirst(dependencies, new Functions.Unary<Boolean, DependencyMO>() {
            @Override
            public Boolean call(DependencyMO dependency) {
                return name.equals(dependency.getName());
            }
        });
        return dependencyMO;
    }

    protected DependencyMO getDependency(List<DependencyMO> dependencies, final EntityType type){
        return (DependencyMO)CollectionUtils.find(dependencies, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((DependencyMO)o).getType().equals(type.toString());
            }
        });
    }

    @NotNull
    public static <O> O getBundleReference(@NotNull final Bundle bundle, @NotNull final String id) {
        final Item referenceItem = Functions.grepFirst(bundle.getReferences(), new Functions.Unary<Boolean, Item>() {
            @Override
            public Boolean call(Item item) {
                return id.equals(item.getId());
            }
        });
        junit.framework.Assert.assertNotNull(referenceItem);
        return (O) referenceItem.getContent();
    }

    @NotNull
    public static Mapping getMapping(@NotNull final Collection<Mapping> mappings, @NotNull final String id) {
        final Mapping mapping = Functions.grepFirst(mappings, new Functions.Unary<Boolean, Mapping>() {
            @Override
            public Boolean call(Mapping mapping) {
                return id.equals(mapping.getSrcId());
            }
        });
        junit.framework.Assert.assertNotNull(mapping);
        return mapping;
    }

    /**
     * Asserts that the first id is in the mapping list before the second id. This will fail if either id is not in the
     * mapping list
     *
     * @param mappings The mappings list
     * @param firstId  The first id
     * @param secondId The second id
     * @param message  The message to include with any error message.
     */
    public static void assertOrder(@NotNull final List<Mapping> mappings, @NotNull final String firstId, @NotNull final String secondId, @Nullable final String message) {
        boolean foundFirst = false;
        for (Mapping mapping : mappings) {
            if (firstId.equals(mapping.getSrcId())) {
                foundFirst = true;
            } else if (secondId.equals(mapping.getSrcId())) {
                if (!foundFirst) {
                    Assert.fail("Found the second id (" + secondId + ") before the first (" + firstId + ") in the mappings list: " + message);
                } else {
                    return;
                }
            }
        }
        //in this case the second id has not been found
        if (foundFirst) {
            Assert.fail("Did not find the second (" + secondId + ") id in the mapping list: " + message);
        } else {
            Assert.fail("Did not find the first (" + firstId + ") or second (" + secondId + ") id in the mapping list: " + message);
        }
    }
}
