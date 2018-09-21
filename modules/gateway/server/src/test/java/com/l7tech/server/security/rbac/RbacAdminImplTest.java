package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.ObjectIdentityPredicate;
import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.ResolvedEntityHeader;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleEntityHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.annotation.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RbacAdminImplTest {
    private static final Goid ZONE_GOID = new Goid(0, 1234L);
    private static final Goid GOID = new Goid(0, 5678L);
    private RbacAdminImpl admin;
    @Mock
    private RoleManager roleManager;
    @Mock
    private EntityCrud entityCrud;
    @Mock
    private SecurityZoneManager securityZoneManager;
    @Mock
    private AssertionAccessManager assertionAccessManager;
    @Mock
    private AssertionRegistry assertionRegistry;
    @Mock
    private IdentityProviderFactory identityProviderFactory;
    @Mock
    private ProtectedEntityTracker protectedEntityTracker;
    @Mock
    private EntityNameResolver entityNameResolver;
    @Mock
    private IdentityAdmin identityAdmin;
    private List<SecurityZone> zones;
    private SecurityZone zone;

    @Before
    public void setup() {
        admin = new RbacAdminImpl(roleManager);
        zone = createSecurityZone(ZONE_GOID);
        zones = new ArrayList<SecurityZone>();
        zones.add(zone);
        ApplicationContexts.inject(admin, CollectionUtils.<String, Object>mapBuilder()
                .put("securityZoneManager", securityZoneManager)
                .put("assertionRegistry", assertionRegistry)
                .put("assertionAccessManager", assertionAccessManager)
                .put("entityCrud", entityCrud)
                .put("identityProviderFactory", identityProviderFactory)
                .put("protectedEntityTracker", protectedEntityTracker)
                .put("entityNameResolver", entityNameResolver)
                .put("identityAdmin", identityAdmin)
                .unmodifiableMap());
    }

    @Test
    public void findAllSecurityZones() throws Exception {
        when(securityZoneManager.findAll()).thenReturn(zones);
        final Collection<SecurityZone> found = admin.findAllSecurityZones();
        assertEquals(1, found.size());
        assertEquals(zone, found.iterator().next());
    }

    @Test
    public void findSecurityZoneByPrimaryKey() throws Exception {
        when(securityZoneManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        assertEquals(zone, admin.findSecurityZoneByPrimaryKey(ZONE_GOID));
    }

    @Test
    public void saveExistingSecurityZone() throws Exception {
        final Goid goid = admin.saveSecurityZone(zone);
        assertEquals(ZONE_GOID, goid);
        verify(securityZoneManager).update(zone);
        verify(securityZoneManager).updateRoles(zone);
    }

    @Test
    public void saveNewSecurityZone() throws Exception {
        final SecurityZone newZone = createSecurityZone(SecurityZone.DEFAULT_GOID);
        when(securityZoneManager.save(newZone)).thenReturn(ZONE_GOID);
        final Goid goid = admin.saveSecurityZone(newZone);
        assertEquals(ZONE_GOID, goid);
        verify(securityZoneManager).save(newZone);
        verify(securityZoneManager).createRoles(newZone);
    }

    @Test(expected = SaveException.class)
    public void saveExistingSecurityZoneUpdateError() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(securityZoneManager).update(zone);
        admin.saveSecurityZone(zone);
    }

    @Test
    public void deleteSecurityZone() throws Exception {
        admin.deleteSecurityZone(zone);
        verify(securityZoneManager).delete(zone);
    }

    @Test
    public void findRolesForUserAttachesEntities() throws Exception {
        final Role role = new Role();
        role.setName("Manage Test Service (#1234)");
        role.setEntityType(EntityType.SERVICE);
        role.setEntityGoid(GOID);
        final User user = new InternalUser("test");
        final PublishedService service = new PublishedService();
        when(roleManager.getAssignedRoles(user, true, false)).thenReturn(Arrays.asList(role));
//        Mockito.when(entityCrud.find(EntityType.SERVICE.getEntityClass(), GOID)).thenReturn(service);
        Mockito.when(entityCrud.find(EntityType.SERVICE.getEntityClass(), GOID)).thenAnswer(new Answer<PublishedService>() {
            @Override
            public PublishedService answer(InvocationOnMock invocationOnMock) throws Throwable {
                return service;
            }
        });

        final Collection<Role> assignedRoles = admin.findRolesForUser(user);
        assertEquals(1, assignedRoles.size());
        final Role assignedRole = assignedRoles.iterator().next();
        assertEquals(service, assignedRole.getCachedSpecificEntity());
    }

    @Test
    public void findRolesForUserAttachesGoidEntities() throws Exception {
        final Role role = new Role();
        role.setName("Manage Test Service (#1234)");
        role.setEntityType(EntityType.JDBC_CONNECTION);
        role.setEntityGoid(ZONE_GOID);
        final User user = new InternalUser("test");
        final JdbcConnection connection = new JdbcConnection();
        when(roleManager.getAssignedRoles(user, true, false)).thenReturn(Arrays.asList(role));
//        when(entityCrud.find(EntityType.JDBC_CONNECTION.getEntityClass(), ZONE_GOID)).thenReturn(connection);
        when(entityCrud.find(EntityType.JDBC_CONNECTION.getEntityClass(), ZONE_GOID)).thenAnswer(new Answer<JdbcConnection>() {
            @Override
            public JdbcConnection answer(InvocationOnMock invocationOnMock) throws Throwable {
                return connection;
            }
        });

        final Collection<Role> assignedRoles = admin.findRolesForUser(user);
        assertEquals(1, assignedRoles.size());
        final Role assignedRole = assignedRoles.iterator().next();
        assertEquals(connection, assignedRole.getCachedSpecificEntity());
    }

    @Test
    public void findRolesForGroup() throws Exception {
        final Role role = new Role();
        role.setName("Manage Test Service (#1234)");
        role.setEntityType(EntityType.JDBC_CONNECTION);
        role.setEntityGoid(GOID);
        final Group group = new InternalGroup("test");
        final JdbcConnection connection = new JdbcConnection();
        when(roleManager.getAssignedRoles(group)).thenReturn(Arrays.asList(role));
//        when(entityCrud.find(EntityType.JDBC_CONNECTION.getEntityClass(), GOID)).thenReturn(connection);
        when(entityCrud.find(EntityType.JDBC_CONNECTION.getEntityClass(), GOID)).thenAnswer(new Answer<JdbcConnection>() {
            @Override
            public JdbcConnection answer(InvocationOnMock invocationOnMock) throws Throwable {
                return connection;
            }
        });

        final Collection<Role> assignedRoles = admin.findRolesForGroup(group);
        assertEquals(1, assignedRoles.size());
        final Role assignedRole = assignedRoles.iterator().next();
        assertEquals(connection, assignedRole.getCachedSpecificEntity());
    }

    @Test
    public void findAllRoleHeaders() throws Exception {
        when(roleManager.findAllHeaders()).thenReturn(Collections.<EntityHeader>singletonList(new RoleEntityHeader(new Goid(0, 1), "name", "description", 1, true, new Goid(1, 2), EntityType.SERVICE)));
        final Collection<EntityHeader> headers = admin.findAllRoleHeaders();

        assertEquals(1, headers.size());
        assertTrue(headers.iterator().next() instanceof RoleEntityHeader);
        verify(roleManager).findAllHeaders();
    }
    @Test
    public void findSecurityZoneByTypeAndSecurityZoneGoid() throws Exception
    {
        Collection<EntityHeader> headers = new ArrayList<>();
        String entityName = "testZone";
        EntityHeader testHeader = new EntityHeader(ZONE_GOID, EntityType.SECURITY_ZONE, entityName, null );
        headers.add(testHeader);

        when(entityCrud.findByEntityTypeAndSecurityZoneGoid(EntityType.SECURITY_ZONE, ZONE_GOID))
                .thenReturn(headers);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenReturn(entityName);
        Collection<ResolvedEntityHeader> resolvedHeaders = admin.findSecurityZoneByTypeAndSecurityZoneGoid(EntityType.SECURITY_ZONE, ZONE_GOID);
        assertEquals(1, resolvedHeaders.size());
        ResolvedEntityHeader resolvedEntityHeader = resolvedHeaders.iterator().next();
        assertEquals(testHeader, resolvedEntityHeader.getEntityHeader());
        assertEquals(entityName, resolvedEntityHeader.getName());
    }
    @Test
    public void findSecurityZoneByTypeAndSecurityZoneGoidForWrongInput() throws Exception
    {
        Collection<EntityHeader> headers = new ArrayList<>();
        String entityName = "testZone";
        PolicyHeader testHeader = new PolicyHeader(ZONE_GOID, false, PolicyType.PRIVATE_SERVICE,
                entityName, null, null, null, null,
                0, 0, false, null);
        headers.add(testHeader);

        when(entityCrud.findByEntityTypeAndSecurityZoneGoid(EntityType.POLICY, ZONE_GOID))
                .thenReturn(headers);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenReturn(entityName);
        Collection<ResolvedEntityHeader> resolvedHeaders = admin.findSecurityZoneByTypeAndSecurityZoneGoid(EntityType.POLICY, ZONE_GOID);
        assertEquals(0, resolvedHeaders.size());
    }
    @Test(expected = FindException.class)
    public void findSecurityZoneByTypeAndSecurityZoneGoidForException() throws Exception
    {
        Collection<EntityHeader> headers = new ArrayList<>();
        String entityName = "testZone";
        EntityHeader testHeader = new EntityHeader(ZONE_GOID, EntityType.SECURITY_ZONE, entityName, null );
        headers.add(testHeader);

        when(entityCrud.findByEntityTypeAndSecurityZoneGoid(EntityType.SECURITY_ZONE, ZONE_GOID))
                .thenReturn(headers);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenThrow(new FindException());
        admin.findSecurityZoneByTypeAndSecurityZoneGoid(EntityType.SECURITY_ZONE, ZONE_GOID);
    }
    @Test
    public void findSecurityZoneByEntityHeaders() throws Exception
    {
        EntityHeaderSet<EntityHeader> headerSet = new EntityHeaderSet();
        String entityName = "testZone";
        EntityHeader testHeader = new EntityHeader(ZONE_GOID, EntityType.SECURITY_ZONE, entityName, null );
        headerSet.add(testHeader);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenReturn(entityName);
        Collection<ResolvedEntityHeader> resolvedHeaders = admin.findSecurityZoneByEntityHeaders(headerSet);
        assertEquals(1, resolvedHeaders.size());
        ResolvedEntityHeader resolvedEntityHeader = resolvedHeaders.iterator().next();
        assertEquals(testHeader, resolvedEntityHeader.getEntityHeader());
        assertEquals(entityName, resolvedEntityHeader.getName());
    }
    @Test
    public void findSecurityZoneByEntityHeadersForPrivatePolicy() throws Exception
    {
        EntityHeaderSet<EntityHeader> headerSet = new EntityHeaderSet();
        String entityName = "testZone";
        PolicyHeader testHeader = new PolicyHeader(ZONE_GOID, false, PolicyType.PRIVATE_SERVICE,
                entityName, null, null, null, null,
                0, 0, false, null);
        headerSet.add(testHeader);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenReturn(entityName);
        Collection<ResolvedEntityHeader> resolvedHeaders = admin.findSecurityZoneByEntityHeaders(headerSet);
        assertEquals(0, resolvedHeaders.size());
    }
    @Test
    public void findSecurityZoneByEntityHeadersForSharedPolicy() throws Exception
    {
        EntityHeaderSet<EntityHeader> headerSet = new EntityHeaderSet();
        String entityName = "com.l7tech.policy.assertion.EncapsulatedAssertion";
        EntityHeader testHeader = new EntityHeader(ZONE_GOID, EntityType.ASSERTION_ACCESS, entityName, null );
        headerSet.add(testHeader);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenReturn(entityName);
        Collection<ResolvedEntityHeader> resolvedHeaders = admin.findSecurityZoneByEntityHeaders(headerSet);
        assertEquals(0, resolvedHeaders.size());
    }
    @Test
    public void findSecurityZoneByEntityHeadersForAssertion() throws Exception
    {
        EntityHeaderSet<EntityHeader> headerSet = new EntityHeaderSet();
        String entityName = "testZone";
        PolicyHeader testHeader = new PolicyHeader(ZONE_GOID, false, PolicyType.SHARED_SERVICE,
                entityName, null, null, null, null,
                0, 0, false, null);
        headerSet.add(testHeader);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenReturn(entityName);
        Collection<ResolvedEntityHeader> resolvedHeaders = admin.findSecurityZoneByEntityHeaders(headerSet);
        assertEquals(0, resolvedHeaders.size());
    }
    @Test(expected = FindException.class)
    public void findSecurityZoneByEntityHeadersForException() throws Exception
    {
        EntityHeaderSet<EntityHeader> headerSet = new EntityHeaderSet();
        String entityName = "testZone";
        EntityHeader testHeader = new EntityHeader(ZONE_GOID, EntityType.SECURITY_ZONE, entityName, null );
        headerSet.add(testHeader);
        when(entityNameResolver.getNameForHeader(testHeader, false))
                .thenThrow(new FindException());
        admin.findSecurityZoneByEntityHeaders(headerSet);
    }

    @Test
    public void findNamesForEntityHeaders() throws Exception
    {
        EntityHeaderSet<EntityHeader> headerSet = new EntityHeaderSet();
        String entityName = "testZone";
        EntityHeader testHeader = new EntityHeader(ZONE_GOID, EntityType.SECURITY_ZONE, entityName, null );
        headerSet.add(testHeader);
        when(entityNameResolver.getNameForHeader(testHeader, true))
                .thenReturn(entityName);
        Map<EntityHeader, String> names = admin.findNamesForEntityHeaders(headerSet);
        assertEquals(1, names.size());
        String resolvedName = names.get(testHeader);
        assertEquals(entityName, resolvedName);
    }
    @Test
    public void findPermissionGroupScopeDescriptions() throws Exception
    {
        Collection<Pair<EntityType, Set<ScopePredicate>>> scopes = new ArrayList<>();
        String entityId = "testId";
        String entityId2 = "testId2";
        String entityName = "testName";
        String entityName2 = "testName2";
        ScopePredicate predicate = new ObjectIdentityPredicate(null, entityId);
        ScopePredicate predicate2 = new ObjectIdentityPredicate(null, entityId2);
        Set<ScopePredicate> predicateSet = new HashSet<ScopePredicate>();
        predicateSet.add(predicate);
        predicateSet.add(predicate2);
        Pair<EntityType, Set<ScopePredicate>> scopeItem = new Pair<EntityType, Set<ScopePredicate>>(EntityType.FOLDER, predicateSet);
        scopes.add(scopeItem);
        when( entityCrud.findHeader(EntityType.FOLDER, entityId))
                .thenReturn(null);
        when(entityNameResolver.getNameForEntity(predicate, true))
                .thenReturn(entityName);
        when(entityNameResolver.getNameForEntity(predicate2, true))
                .thenReturn(entityName2);
        Map<Pair<EntityType, Set<ScopePredicate>>, String> names = admin.findPermissionGroupScopeDescriptions(scopes);
        assertEquals(1, names.size());
        String description = names.get(scopeItem);
        assertEquals(entityName + ", " + entityName2, description);
    }
    @Test
    public void findPermissionGroupScopeDescriptionsForEmptyPredicate() throws Exception
    {
        Collection<Pair<EntityType, Set<ScopePredicate>>> scopes = new ArrayList<>();
        String entityId = "testId";
        Set<ScopePredicate> predicateSet = new HashSet<ScopePredicate>();
        Pair<EntityType, Set<ScopePredicate>> scopeItem = new Pair<EntityType, Set<ScopePredicate>>(EntityType.FOLDER, predicateSet);
        scopes.add(scopeItem);
        when( entityCrud.findHeader(EntityType.FOLDER, entityId))
                .thenReturn(null);

        Map<Pair<EntityType, Set<ScopePredicate>>, String> names = admin.findPermissionGroupScopeDescriptions(scopes);
        assertEquals(1, names.size());
        String description = names.get(scopeItem);
        assertEquals(admin.ALL, description);
    }
    @Test
    public void findPermissionGroupScopeDescriptionsForComplexPredicate() throws Exception
    {
        Collection<Pair<EntityType, Set<ScopePredicate>>> scopes = new ArrayList<>();
        String entityId = "testId";
        String entityId2 = "testId2";
        String entityId3 = "testId3";
        String entityName = "testName";
        String entityName2 = "testName2";
        ScopePredicate predicate = new ObjectIdentityPredicate(null, entityId);
        ScopePredicate predicate2 = new ObjectIdentityPredicate(null, entityId2);
        ScopePredicate predicate3 = new ObjectIdentityPredicate(null, entityId3);
        Set<ScopePredicate> predicateSet = new HashSet<ScopePredicate>();
        predicateSet.add(predicate);
        predicateSet.add(predicate2);
        predicateSet.add(predicate3);
        Pair<EntityType, Set<ScopePredicate>> scopeItem = new Pair<EntityType, Set<ScopePredicate>>(EntityType.FOLDER, predicateSet);
        scopes.add(scopeItem);
        when( entityCrud.findHeader(EntityType.FOLDER, entityId))
                .thenReturn(null);
        when(entityNameResolver.getNameForEntity(predicate, true))
                .thenReturn(entityName);
        when(entityNameResolver.getNameForEntity(predicate2, true))
                .thenReturn(entityName2);
        Map<Pair<EntityType, Set<ScopePredicate>>, String> names = admin.findPermissionGroupScopeDescriptions(scopes);
        assertEquals(1, names.size());
        String description = names.get(scopeItem);
        assertEquals("<complex scope>", description);
    }

    @Test
    public void findPermissionGroupScopeDescriptionsForAttributePredicate() throws Exception
    {
        Collection<Pair<EntityType, Set<ScopePredicate>>> scopes = new ArrayList<>();
        String testId = ZONE_GOID.toHexString();
        String testProvider = ZONE_GOID.toHexString();
        ScopePredicate predicate = new AttributePredicate(null, admin.ID, testId);
        ScopePredicate predicate2 = new AttributePredicate(null, admin.PROVIDER_ID, testProvider);
        Set<ScopePredicate> predicateSet = new HashSet<ScopePredicate>();
        predicateSet.add(predicate);
        predicateSet.add(predicate2);
        Pair<EntityType, Set<ScopePredicate>> scopeItem = new Pair<EntityType, Set<ScopePredicate>>(EntityType.GROUP, predicateSet);
        scopes.add(scopeItem);
        Group identity = new Group() {
            @Override
            public Goid getProviderId() {
                return Goid.parseGoid(testProvider);
            }

            @Override
            public boolean isEquivalentId(Object thatId) {
                return false;
            }

            @Override
            public String getId() {
                return testId;
            }
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }
        };
        when( identityAdmin.findGroupByID(Goid.parseGoid(testProvider), testId))
                .thenReturn(identity);
        when(entityNameResolver.getNameForEntity(new ObjectIdentityPredicate(null, identity.getId()), true))
                .thenReturn("testName");
        Map<Pair<EntityType, Set<ScopePredicate>>, String> names = admin.findPermissionGroupScopeDescriptions(scopes);
        assertEquals(1, names.size());
        String description = names.get(scopeItem);
        assertEquals("testName", description);
    }

    @Test
    public void findPermissionGroupScopeDescriptionsForServiceType() throws Exception
    {
        Collection<Pair<EntityType, Set<ScopePredicate>>> scopes = new ArrayList<>();
        String nodeId = ZONE_GOID.toHexString();
        String serviceId = ZONE_GOID.toHexString();
        ScopePredicate predicate = new AttributePredicate(null, admin.NODE_ID, nodeId);
        ScopePredicate predicate2 = new AttributePredicate(null, admin.SERVICE_ID, serviceId);
        Set<ScopePredicate> predicateSet = new HashSet<ScopePredicate>();
        predicateSet.add(predicate);
        predicateSet.add(predicate2);
        Pair<EntityType, Set<ScopePredicate>> scopeItem = new Pair<EntityType, Set<ScopePredicate>>(EntityType.SERVICE_USAGE, predicateSet);
        scopes.add(scopeItem);
        when(entityNameResolver.getNameForEntity(new ObjectIdentityPredicate(null, null), true))
                .thenReturn("testName");
        Map<Pair<EntityType, Set<ScopePredicate>>, String> names = admin.findPermissionGroupScopeDescriptions(scopes);
        assertEquals(1, names.size());
        String description = names.get(scopeItem);
        assertEquals("testName", description);
    }

    private SecurityZone createSecurityZone(final Goid goid) {
        final SecurityZone zone = new SecurityZone();
        zone.setGoid(goid);
        return zone;
    }
}
