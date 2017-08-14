package com.l7tech.server.bundling;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleEntityHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.InternalUserBean;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.TestPasswordHasher;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.*;

import static com.l7tech.objectmodel.EntityType.USER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    @Mock
    private ServerModuleFileManager serverModuleFileManager;
    @Mock
    private ProtectedEntityTracker protectedEntityTracker;
    @Spy
    private PasswordHasher passwordHasher = new TestPasswordHasher();

    @Mock
    private PasswordEnforcerManager passwordEnforcerManager;

    @Mock
    private IdentityProvider identityProvider;

    @Mock
    private UserManager userManager;

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
                .put("serverModuleFileManager", serverModuleFileManager)
                .put("protectedEntityTracker", protectedEntityTracker)
                .put("passwordEnforcerManager", passwordEnforcerManager)
                .put("identityProvider", identityProvider)
                .put("userManager", userManager)
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

    @Test
    public void importBundleWithPasswordAttributesOnNewUser() throws Exception {
        final InternalUserBean internalUser = new InternalUserBean();
        internalUser.setLogin("foobar");
        internalUser.setName("foobar");
        internalUser.setUniqueIdentifier("96bfb8eb39656944856ae8a2579cfc97");

        final InternalUser iu = new InternalUser(internalUser.getLogin());
        iu.setLogin(internalUser.getLogin());
        iu.setName(internalUser.getName());
        iu.setGoid(Goid.parseGoid(internalUser.getId()));


        when(identityProviderFactory.getProvider(internalUser.getProviderId())).thenReturn(identityProvider);
        when(identityProvider.getUserManager()).thenReturn(userManager);
        when(userManager.findByPrimaryKey(internalUser.getId())).thenReturn(null);
        when(userManager.reify(internalUser)).thenReturn(iu);

        final List<EntityMappingInstructions> mappingInstructions = new ArrayList<>();
        mappingInstructions.add(new EntityMappingInstructions(createIndentityHeader(internalUser), null, EntityMappingInstructions.MappingAction.NewOrUpdate));
        final List<EntityContainer> entities = new ArrayList<>();
        entities.add(new EntityContainer(internalUser));

        Mockito.verify(passwordEnforcerManager, Mockito.never()).setUserPasswordPolicyAttributes(Mockito.any(InternalUser.class), Mockito.anyBoolean());
        importer.importBundle(new EntityBundle(entities, mappingInstructions, Collections.<DependencySearchResults>emptyList()), false, false, null);
        Mockito.verify(passwordEnforcerManager, Mockito.times(1)).setUserPasswordPolicyAttributes(iu, true);
    }

    @Test
    public void importBundleWithPasswordAttributesOnExistingUser() throws Exception {
        final InternalUserBean internalUser = new InternalUserBean();
        internalUser.setLogin("foobar");
        internalUser.setName("foobar");
        internalUser.setUniqueIdentifier("96bfb8eb39656944856ae8a2579cfc97");

        final InternalUser iu = new InternalUser(internalUser.getLogin());
        iu.setLogin(internalUser.getLogin());
        iu.setName(internalUser.getName());
        iu.setGoid(Goid.parseGoid(internalUser.getId()));

        when(identityProviderFactory.getProvider(internalUser.getProviderId())).thenReturn(identityProvider);
        when(identityProvider.getUserManager()).thenReturn(userManager);
        when(userManager.findByPrimaryKey(internalUser.getId())).thenReturn(iu);
        when(userManager.reify(internalUser)).thenReturn(iu);

        final List<EntityMappingInstructions> mappingInstructions = new ArrayList<>();
        mappingInstructions.add(new EntityMappingInstructions(createIndentityHeader(internalUser), null, EntityMappingInstructions.MappingAction.NewOrUpdate));
        final List<EntityContainer> entities = new ArrayList<>();
        entities.add(new EntityContainer(internalUser));

        Mockito.verify(passwordEnforcerManager, Mockito.never()).setUserPasswordPolicyAttributes(Mockito.any(InternalUser.class), Mockito.anyBoolean());
        importer.importBundle(new EntityBundle(entities, mappingInstructions, Collections.<DependencySearchResults>emptyList()), false, false, null);
        Mockito.verify(passwordEnforcerManager, Mockito.times(1)).setUserPasswordPolicyAttributes(iu, true);
    }

    @Test
    public void testMapByRoutingUri() throws FindException, SaveException, UpdateException {
        final Folder rootFolder = createRootFolder();
        final Policy policy = createTestingPolicy();
        final PublishedService service = createTestingPublishedService(policy, rootFolder);
        final EntityBundle entityBundle = createTestingEntityBundle(rootFolder, service);
        final EntityMappingInstructions serviceMappingInstructions= entityBundle.getMappingInstructions().get(1); // The second instruction is for published service.

        when(entityCrud.find(Folder.class, Folder.ROOT_FOLDER_ID.toString())).thenReturn(rootFolder);
        when(entityCrud.find(serviceMappingInstructions.getSourceEntityHeader())).thenReturn(service);
        when(policyVersionManager.findLatestRevisionForPolicy(policy.getGoid())).thenReturn(getPolicyVersion(policy));

        // Get mapping results:
        final List<EntityMappingResult> results = importer.importBundle(entityBundle, false, true, null);

        // Check whether a lookup for a published service by its routing uri is performed.  In this case, the routing uri is "/random_routing_uri".
        Mockito.verify(serviceManager, Mockito.times(1)).findByRoutingUri("/random_routing_uri");

        // Check whether the action of service manager creating or updating service is performed.
        Mockito.verify(serviceManager, Mockito.times(1)).save(service.getGoid(), service); // Creating a new service is performed.
        Mockito.verify(serviceManager, Mockito.times(0)).update(service);                  // No updating service is performed.

        // Check whether the service mapping result is correct, in terms of:
        // (1) There are two mapping results returned.
        // (2) No exceptions is included in the service mapping result.
        // (3) The service mapping action taken is CreateNew, not other types UpdatedExisting, etc.
        assertTrue("Two mapping results are returned.", results.size() == 2);
        final EntityMappingResult serviceMappingResult = results.get(1);  // The second mapping result is for service.
        assertNull("The service mapping result has no exception.", results.get(1).getException());
        assertTrue("Tne service mapping result action is CreatedNew.", serviceMappingResult.getMappingAction() == EntityMappingResult.MappingAction.CreatedNew);
    }

    private IdentityHeader createIndentityHeader(User user){
        return new IdentityHeader(user.getProviderId(), user.getId(), USER, user.getLogin(), null, user.getName(), null);
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

    private EntityBundle createTestingEntityBundle(@NotNull final Folder rootFolder, @NotNull final PublishedService publishedService) throws FindException {
        final List<EntityContainer> entities = new ArrayList<>();
        entities.add(new EntityContainer(publishedService));

        // Two mapping instructions for root folder and published service, respectively.
        final List<EntityMappingInstructions> mappingInstructions = createTestingMappingInstructions(rootFolder, publishedService);

        return new EntityBundle(entities, mappingInstructions, Collections.<DependencySearchResults>emptyList());
    }

    private List<EntityMappingInstructions> createTestingMappingInstructions(@NotNull final Folder rootFolder, @NotNull final PublishedService publishedService) throws FindException {
        final EntityMappingInstructions rootFolderMappingInstructions = new EntityMappingInstructions(
            createFolderEntityHeader(rootFolder), null, EntityMappingInstructions.MappingAction.NewOrExisting, true, false
        );

        final EntityMappingInstructions.TargetMapping serviceTargetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.ROUTING_URI, publishedService.getRoutingUri());
        final EntityMappingInstructions serviceMappingInstructions = new EntityMappingInstructions(
            createServiceEntityHeader(publishedService), serviceTargetMapping, EntityMappingInstructions.MappingAction.NewOrUpdate, false, false
        );

        final List<EntityMappingInstructions> mappingInstructions = new ArrayList<>();
        mappingInstructions.add(rootFolderMappingInstructions);
        mappingInstructions.add(serviceMappingInstructions);

        return mappingInstructions;
    }

    private PolicyVersion getPolicyVersion(@NotNull final Policy policy) {
        final PolicyVersion policyVersion = new PolicyVersion();
        policyVersion.setXml(policy.getXml());

        return policyVersion;
    }

    private Folder createRootFolder() {
        final Folder rootFolder = new Folder("Root", new Folder());
        rootFolder.setGoid(Folder.ROOT_FOLDER_ID);

        return rootFolder;
    }

    private Policy createTestingPolicy() {
        final Policy policy = new Policy(
            PolicyType.PRIVATE_SERVICE,
            "random_testing_policy",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            false
        );
        policy.setGuid(UUID.randomUUID().toString());

        return policy;
    }

    private PublishedService createTestingPublishedService(@NotNull final Policy policy, @NotNull final Folder parentFolder) {
        final PublishedService publishedService = new PublishedService();

        publishedService.setSoap(true);
        publishedService.setWsdlXml("");
        publishedService.setPolicy(policy);
        publishedService.setName("random_service_name");
        publishedService.setRoutingUri("/random_routing_uri");
        publishedService.setFolder(parentFolder);

        final byte[] bytes = new byte[16];
        new Random().nextBytes(bytes);
        final Goid random_goid = new Goid(bytes);
        publishedService.setGoid(random_goid);

        return publishedService;
    }

    private ServiceHeader createServiceEntityHeader(@NotNull final PublishedService publishedService) {
        return new ServiceHeader(
            publishedService.isSoap(),
            publishedService.isDisabled(),
            publishedService.displayName(),
            publishedService.getGoid(),
            publishedService.getName(), publishedService.getName(),
            publishedService.getFolder().getGoid(),
            null,
            0,
            1,
            publishedService.getRoutingUri(),
            false,
            false,
            null,
            publishedService.getPolicy().getGoid()
        );
    }

    private FolderHeader createFolderEntityHeader(@NotNull final Folder folder) {
        return new FolderHeader(
            folder.getGoid(),
            folder.getName(),
            folder.getFolder().getGoid(),
            folder.getVersion(),
            folder.getPath(),
            folder.getSecurityZone() != null? folder.getSecurityZone().getGoid() : null
        );
    }
}
