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
    //This is used to see if the test is run from a test suite. If it is it will not perform its before and after class actions
    private static boolean runInSuite = false;
    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static JVMDatabaseBasedRestManagementEnvironment sourceEnvironment;
    private static JVMDatabaseBasedRestManagementEnvironment targetEnvironment;

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (!IgnoreOnDaily.isDaily() && !runInSuite) {
            createMigrationEnvironments();
        }

        System.out.println("============================================================================================");
        System.out.println("Environment startup times:");
        System.out.println("============================================================================================");
        System.out.println("Source: " + String.valueOf(getSourceEnvironmentStartupTime() == -1 ? "NOT STARTED" : getSourceEnvironmentStartupTime()) + " ms.");
        System.out.println("Target: " + String.valueOf(getTargetEnvironmentStartupTime() == -1 ? "NOT STARTED" : getTargetEnvironmentStartupTime()) + " ms.");
        System.out.println("============================================================================================");
    }

    protected static void createMigrationEnvironments() throws Exception {
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        final CountDownLatch enviromentsStarted = new CountDownLatch(2);

        long waitTime;
        try {
            waitTime = Math.max(Long.parseLong(System.getProperty("test.migration.waitTimeMinutes", "10")), 5L);
        } catch (Exception e) {
            // on any error (should only be NumberFormatException though) use a default of 10 min
            waitTime = 10L;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sourceEnvironment = new JVMDatabaseBasedRestManagementEnvironment("8003");
                } catch (Exception e) {
                    exceptions.add(e);
                }
                enviromentsStarted.countDown();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    targetEnvironment = new JVMDatabaseBasedRestManagementEnvironment("8004");
                } catch (Exception e) {
                    exceptions.add(e);
                }
                enviromentsStarted.countDown();
            }
        }).start();
        enviromentsStarted.await(waitTime, TimeUnit.MINUTES);
        if(!exceptions.isEmpty()){
            throw exceptions.get(0);
        }
    }

    @AfterClass
    public static void afterClass() {
        if(!IgnoreOnDaily.isDaily() && !runInSuite) {
            destroyMigrationEnvironments();
        }
    }

    protected static void destroyMigrationEnvironments() {
        if (sourceEnvironment != null) {
            sourceEnvironment.close();
        }
        if (targetEnvironment != null) {
            targetEnvironment.close();
        }
    }

    public static JVMDatabaseBasedRestManagementEnvironment getSourceEnvironment() {
        if ( sourceEnvironment == null )
            throw new NullPointerException( "sourceEnvironment not set up" );
        return sourceEnvironment;
    }

    public static long getSourceEnvironmentStartupTime() {
        if ( sourceEnvironment != null )
            return sourceEnvironment.getStartupTimeInMs();
        return -1;
    }

    public static JVMDatabaseBasedRestManagementEnvironment getTargetEnvironment() {
        if ( targetEnvironment == null )
            throw new NullPointerException( "targetEnvironment not set up" );
        return targetEnvironment;
    }

    public static long getTargetEnvironmentStartupTime() {
        if ( targetEnvironment != null )
            return targetEnvironment.getStartupTimeInMs();
        return -1;
    }

    protected void assertOkCreatedResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response:\n" + response.getBody(), 201, response.getStatus());
        Assert.assertNotNull(response.getBody());
    }

    protected void assertOkResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response:\n" + response.getBody(), 200, response.getStatus());
    }

    protected void assertNotFoundResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response:\n" + response.getBody(), 404, response.getStatus());
    }

    protected void assertOkEmptyResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response:\n" + response.getBody(), 204, response.getStatus());
    }

    protected void assertConflictResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response:\n" + response.getBody(), 409, response.getStatus());
    }

    protected void cleanupAll(Item<Mappings> mappings) throws Exception {
        List<Mapping> reverseMappingsList = mappings.getContent().getMappings();
        Collections.reverse(reverseMappingsList);
        for (Mapping mapping : reverseMappingsList) {
            if(mapping.getErrorType() == null && !Mapping.ActionTaken.Ignored.equals(mapping.getActionTaken()) && !Mapping.ActionTaken.Deleted.equals(mapping.getActionTaken()) &&
                    ( mapping.getTargetId().length()!=16 || !GoidRange.RESERVED_RANGE.isInRange(Goid.parseGoid(mapping.getTargetId())))
                    && mapping.getActionTaken()== Mapping.ActionTaken.CreatedNew && !EntityType.ASSERTION_ACCESS.name().equals(mapping.getType())){
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
            if(mapping.getErrorType() == null && mapping.getAction()!= Mapping.Action.Ignore && mapping.getActionTaken() != Mapping.ActionTaken.Ignored && !("INTERFACE_TAG".equals(mapping.getType()) && "742a7604-699c-368a-a19a-0ddd73085575".equals(mapping.getSrcId()))){
                Assert.assertNotNull("The target uri cannot be null", mapping.getTargetUri());
                String uri = getUri(mapping.getTargetUri());
                RestResponse response = targetEnvironment.processRequest(uri, HttpMethod.GET, null, "");
                if(mapping.getActionTaken() == Mapping.ActionTaken.Deleted && !"ASSERTION_ACCESS".equals(mapping.getType())){
                    assertNotFoundResponse(response);
                } else {
                    assertOkResponse(response);
                }
            }
        }
    }

    protected void validateDependency(final DependencyMO dependency, final String id, final String name, final EntityType entityType) {
        Assert.assertNotNull(dependency);
        Assert.assertNull(dependency.getDependencies());
        Assert.assertEquals(name, dependency.getName());
        Assert.assertEquals(entityType.toString(), dependency.getType());
    }

    protected void validateMapping(final Mapping mapping, final EntityType entityType, final Mapping.Action action, final Mapping.ActionTaken actionTaken, final String srcId, final String targetId) {
        Assert.assertEquals(entityType.toString(), mapping.getType());
        Assert.assertEquals(action, mapping.getAction());
        Assert.assertEquals(actionTaken, mapping.getActionTaken());
        Assert.assertEquals(srcId, mapping.getSrcId());
        Assert.assertEquals(targetId, mapping.getTargetId());
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

    public static void runInSuite(boolean runInSuite) {
        MigrationTestBase.runInSuite = runInSuite;
    }
}
