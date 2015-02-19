package com.l7tech.server.bundling;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleEntityHeader;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.TestPasswordHasher;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author alee, 1/23/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityBundleImporterImplTest {
    private static final Goid GOID = new Goid(0, 1);
    private EntityBundleImporterImpl importer;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private DependencyAnalyzer dependencyAnalyzer;
    @Mock
    private EntityCrud entityCrud;
    @Mock
    private IdentityProviderFactory identityProviderFactory;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private ServiceManager serviceManager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private PolicyVersionManager policyVersionManager;
    @Mock
    private AuditContextFactory auditContextFactory;
    @Mock
    private SsgKeyStoreManager keyStoreManager;
    @Mock
    private SsgKeyStoreManager ssgKeyStoreManager;
    @Mock
    private ServiceAliasManager serviceAliasManager;
    @Mock
    private PolicyAliasManager policyAliasManager;
    @Mock
    private ClusterPropertyManager clusterPropertyManager;
    @Mock
    private ClientCertManager clientCertManager;
    @Mock
    private PolicyCache policyCache;
    @Spy
    private PasswordHasher passwordHasher = new TestPasswordHasher();

    @Before
    public void steup() {
        importer = new EntityBundleImporterImpl();
        ApplicationContexts.inject(importer, CollectionUtils.<String, Object>mapBuilder()
                .put("transactionManager", transactionManager)
                .put("dependencyAnalyzer", dependencyAnalyzer)
                .put("entityCrud", entityCrud)
                .put("identityProviderFactory", identityProviderFactory)
                .put("policyManager", policyManager)
                .put("serviceManager", serviceManager)
                .put("roleManager", roleManager)
                .put("policyVersionManager", policyVersionManager)
                .put("auditContextFactory", auditContextFactory)
                .put("keyStoreManager", keyStoreManager)
                .put("ssgKeyStoreManager", ssgKeyStoreManager)
                .put("serviceAliasManager", serviceAliasManager)
                .put("policyAliasManager", policyAliasManager)
                .put("clusterPropertyManager", clusterPropertyManager)
                .put("clientCertManager", clientCertManager)
                .put("passwordHasher", passwordHasher)
                .put("policyCache", policyCache)
                .map(), false);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new DefaultTransactionStatus(null, false, false, false, false, null));
    }

    @Test
    public void importBundleCreateNewRoleSetsUserCreatedTrue() throws Exception {
        // role is userCreated=false, likely in error as only the SSG can create system roles
        final Role role = createRole(false);
        final List<EntityMappingInstructions> mappingInstructions = new ArrayList<>();
        mappingInstructions.add(new EntityMappingInstructions(createRoleEntityHeader(role), null));
        final List<EntityContainer> entities = new ArrayList<>();
        entities.add(new EntityContainer(role));
        importer.importBundle(new EntityBundle(entities, mappingInstructions, Collections.<DependencySearchResults>emptyList()), false, false, null);

        final ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(entityCrud).save(eq(GOID), captor.capture());
        // role should be set to user created
        assertTrue(captor.getValue().isUserCreated());
    }

    @Test
    public void importBundleUpdateRoleDoesNotModifyUserCreated() throws Exception {
        final Role role = createRole(false);
        final List<EntityMappingInstructions> mappingInstructions = new ArrayList<>();
        mappingInstructions.add(new EntityMappingInstructions(createRoleEntityHeader(role), null, EntityMappingInstructions.MappingAction.NewOrUpdate));
        final List<EntityContainer> entities = new ArrayList<>();
        entities.add(new EntityContainer(role));
        when(entityCrud.find(Role.class, GOID.toString())).thenReturn(role);
        importer.importBundle(new EntityBundle(entities, mappingInstructions, Collections.<DependencySearchResults>emptyList()), false, false, null);

        final ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(entityCrud).update(captor.capture());
        // user created should not be modified
        assertFalse(captor.getValue().isUserCreated());
    }

    private RoleEntityHeader createRoleEntityHeader(final Role role) {
        return new RoleEntityHeader(role.getGoid(), role.getName(), role.getDescription(), role.getVersion(), role.isUserCreated(), role.getEntityGoid(), role.getEntityType());
    }

    private Role createRole(final boolean userCreated) {
        final Role role = new Role();
        role.setGoid(GOID);
        role.setName("Test Role");
        role.setUserCreated(userCreated);
        return role;
    }
}
