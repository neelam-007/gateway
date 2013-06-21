package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SecurityZoneRbacInterceptorTest {
    private SecurityZoneRbacInterceptor interceptor;
    @Mock
    private ZoneUpdateSecurityChecker checker;
    @Mock
    private MockAdmin mockAdmin;
    private List<Long> oids;
    private Map<EntityType, Collection<Long>> oidsMap;
    private InternalUser user;
    private Method bulkUpdate;
    private Method bulkUpdateMap;


    @Before
    public void setup() throws Exception {
        interceptor = new SecurityZoneRbacInterceptor();
        ApplicationContexts.inject(interceptor, CollectionUtils.<String, Object>mapBuilder()
                .put("zoneUpdateSecurityChecker", checker)
                .unmodifiableMap(), false);
        oids = new ArrayList<>();
        oidsMap = new HashMap<>();
        user = new InternalUser("test");
        interceptor.setUser(user);
        bulkUpdate = MockAdmin.class.getMethod("bulkUpdate", Long.class, EntityType.class, Collection.class);
        bulkUpdateMap = MockAdmin.class.getMethod("bulkUpdateMap", Long.class, Map.class);
    }

    @Test
    public void invokeSingleEntityTypeAllowed() throws Throwable {
        interceptor.invoke(new StubMethodInvocation(bulkUpdate, new Object[]{null, EntityType.POLICY, oids}, null, mockAdmin));
        verify(checker).checkBulkUpdatePermitted(user, null, EntityType.POLICY, oids);
    }

    @Test(expected = PermissionDeniedException.class)
    public void invokeSingleEntityTypeNotAllowed() throws Throwable {
        doThrow(new PermissionDeniedException(OperationType.UPDATE, EntityType.POLICY, "mocking exception")).when(checker).checkBulkUpdatePermitted(user, null, EntityType.POLICY, oids);
        interceptor.invoke(new StubMethodInvocation(bulkUpdate, new Object[]{null, EntityType.POLICY, oids}, null, mockAdmin));
    }

    @Test
    public void invokeMultipleEntityTypesAllowed() throws Throwable {
        interceptor.invoke(new StubMethodInvocation(bulkUpdateMap, new Object[]{null, oidsMap}, null, mockAdmin));
        verify(checker).checkBulkUpdatePermitted(user, null, oidsMap);
    }

    @Test(expected = PermissionDeniedException.class)
    public void invokeMultipleEntityTypesNotAllowed() throws Throwable {
        doThrow(new PermissionDeniedException(OperationType.UPDATE, EntityType.POLICY, "mocking exception")).when(checker).checkBulkUpdatePermitted(user, null, oidsMap);
        interceptor.invoke(new StubMethodInvocation(bulkUpdateMap, new Object[]{null, oidsMap}, null, mockAdmin));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCollectionType() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdate, new Object[]{null, EntityType.POLICY, Arrays.asList("notLong")}, null, mockAdmin),
                "oid is not a Long: notLong");
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooFewArguments() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdate, new Object[]{null}, null, mockAdmin),
                "Expected two or three arguments. Received: 1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyArguments() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdate, new Object[]{null, EntityType.POLICY, oids, "extra arg"}, null, mockAdmin),
                "Expected two or three arguments. Received: 4");
    }

    @Test(expected = IllegalArgumentException.class)
    public void firstArgNotLong() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdate, new Object[]{"notLong", EntityType.POLICY, oids}, null, mockAdmin),
                "Expected a Long or null. Received: notLong");
    }

    @Test(expected = IllegalArgumentException.class)
    public void secondArgNotEntityType() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdate, new Object[]{null, "notEntityType", oids}, null, mockAdmin),
                "Expected an EntityType. Received: notEntityType");
    }

    @Test(expected = IllegalArgumentException.class)
    public void secondArgNotMap() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdate, new Object[]{null, "notMap"}, null, mockAdmin),
                "Expected a Map. Received: notMap");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMapKey() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdateMap, new Object[]{null, Collections.singletonMap("notEntityType", Collections.singletonList(1234L))}, null, mockAdmin),
                "Expected a map key of EntityType. Received: notEntityType");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMapValue() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdateMap, new Object[]{null, Collections.singletonMap(EntityType.POLICY, "notCollection")}, null, mockAdmin),
                "Expected a map value of Collection. Received: notCollection");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMapValueCollectionType() throws Throwable {
        invokeWithError(new StubMethodInvocation(bulkUpdateMap, new Object[]{null, Collections.singletonMap(EntityType.POLICY, Collections.singletonList("notLong"))}, null, mockAdmin),
                "oid is not a Long: notLong");
    }

    private void invokeWithError(final StubMethodInvocation invocation, final String expectedError) throws Throwable {
        try {
            interceptor.invoke(invocation);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals(expectedError, e.getMessage());
            throw e;
        }
    }

    private interface MockAdmin {
        void bulkUpdate(final Long securityZoneOid, final EntityType entityType, final Collection<Long> oids);

        void bulkUpdateMap(final Long securityZoneOid, final Map<EntityType, Collection<Long>> oids);
    }
}
