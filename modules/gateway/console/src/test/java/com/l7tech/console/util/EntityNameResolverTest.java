package com.l7tech.console.util;

import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderOid;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.ContentTypeAssertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntityNameResolverTest {
    private static final long OID = 1234L;
    private static final String NAME = "test";
    private EntityNameResolver resolver;
    @Mock
    private ServiceAdmin serviceAdmin;
    @Mock
    private PolicyAdmin policyAdmin;
    @Mock
    private TrustedCertAdmin trustedCertAdmin;
    @Mock
    private ResourceAdmin resourceAdmin;
    @Mock
    private FolderAdmin folderAdmin;
    @Mock
    private AssertionRegistry assertionRegistry;
    @Mock
    private PaletteFolderRegistry folderRegistry;

    @Before
    public void setup() {
        resolver = new EntityNameResolver(serviceAdmin, policyAdmin, trustedCertAdmin, resourceAdmin, folderAdmin, assertionRegistry, folderRegistry, "localhost");
    }

    @Test
    public void getNameForHeaderUsesNameOnHeader() throws Exception {
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader("", null, "test", null)));
    }

    @Test
    public void getNameForServiceAliasHeader() throws Exception {
        final PublishedService service = createService(OID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(folder);
        assertEquals("test alias [/routingUri] (/aliases/test alias)", resolver.getNameForHeader(new AliasHeader(alias)));
    }

    @Test
    public void getNameForServiceAliasHeaderNullRoutingUri() throws Exception {
        final PublishedService service = createService(OID, NAME, null);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(folder);
        assertEquals("test alias (/aliases/test alias)", resolver.getNameForHeader(new AliasHeader(alias)));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceAliasHeaderServiceNotFound() throws Exception {
        final PublishedService service = createService(OID, NAME, null);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findByAlias(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new AliasHeader(alias));
    }

    @Test
    public void getNameForPolicyAliasHeader() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PolicyAlias alias = createPolicyAlias(OID, policy, folder);
        when(policyAdmin.findByAlias(OID)).thenReturn(policy);
        assertEquals("test alias (/test alias)", resolver.getNameForHeader(new AliasHeader(alias)));
    }

    @Test(expected = FindException.class)
    public void getNameForPolicyAliasHeaderNotFound() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PolicyAlias alias = createPolicyAlias(OID, policy, folder);
        when(policyAdmin.findByAlias(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new AliasHeader(alias));
    }

    @Test
    public void getNameForKeyMetadataHeader() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(OID)).thenReturn(new SsgKeyMetadata(1L, NAME, null));
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(OID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForKeyMetadataHeaderNotFound() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(anyLong())).thenReturn(null);
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(OID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test
    public void getNameForResourceEntryHeader() throws Exception {
        final String uri = "http://localhost:8080";
        final ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setUri(uri);
        when(resourceAdmin.findResourceEntryByPrimaryKey(OID)).thenReturn(resourceEntry);
        assertEquals(uri, resolver.getNameForHeader(new EntityHeader(OID, EntityType.RESOURCE_ENTRY, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForResourceEntryHeaderNotFound() throws Exception {
        when(resourceAdmin.findResourceEntryByPrimaryKey(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.RESOURCE_ENTRY, null, null));
    }

    @Test
    public void getNameForHttpConfigurationHeader() throws Exception {
        final HttpConfiguration config = new HttpConfiguration();
        config.setProtocol(HttpConfiguration.Protocol.HTTP);
        config.setHost("localhost");
        config.setPort(8080);
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(OID)).thenReturn(config);
        assertEquals("HTTP localhost 8080", resolver.getNameForHeader(new EntityHeader(OID, EntityType.HTTP_CONFIGURATION, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForHttpConfigurationHeaderNotFound() throws Exception {
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.HTTP_CONFIGURATION, null, null));
    }

    @Test
    public void geNameForHeaderUnsupportedEntityType() throws Exception {
        assertTrue(resolver.getNameForHeader(new EntityHeader(OID, EntityType.ANY, null, null)).isEmpty());
    }

    /**
     * If the header has the entity's oid as its name - go get more info for the name.
     */
    @Test
    public void getNameForHeaderWithOidInName() throws Exception {
        final PublishedService service = createService(OID, NAME, null);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(folder);
        final AliasHeader header = new AliasHeader(alias);
        header.setName(String.valueOf(OID));
        assertEquals("test alias (/aliases/test alias)", resolver.getNameForHeader(header));
    }

    @Test
    public void getNameForHasFolderOidHeader() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final HasFolderOidStub header = new HasFolderOidStub(OID);
        header.setName(NAME);
        final String name = resolver.getNameForHeader(header);
        assertEquals("test (/folder1/folder2/test)", name);
    }

    @Test
    public void getNameForHasFolderOidHeaderNoName() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final HasFolderOidStub header = new HasFolderOidStub(OID);
        header.setName(null);
        final String name = resolver.getNameForHeader(header);
        assertEquals(" (/folder1/folder2/)", name);
    }

    @BugId("SSG-7239")
    @Test
    public void getNameForRootHasFolderOidHeader() throws Exception {
        final FolderHeader rootFolderHeader = new FolderHeader(createRootFolder());
        assertEquals("localhost (root)", resolver.getNameForHeader(rootFolderHeader));
    }

    @Test
    public void getPathNoFolder() throws Exception {
        assertEquals("/", resolver.getPath(new HasFolderStub(null)));
    }

    /**
     * Don't show the root node folder
     */
    @Test
    public void getPathSingleFolder() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("Root Node", null)));
        assertEquals("/", path);
    }

    @Test
    public void getPathLessThanMaxPathLength() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null)))));
        assertEquals("/folder1/folder2/", path);
    }

    @Test
    public void getPathMaxPathLength() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("folder3", new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))))));
        assertEquals("/folder1/folder2/folder3/", path);
    }

    @Test
    public void getPathGreaterThanMaxPathLength() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("folder4", new Folder("folder3", new Folder("folder2", new Folder("folder1", new Folder("Root Node", null)))))));
        assertEquals("/folder1/.../folder4/", path);
    }

    @BugId("SSG-7239")
    @Test
    public void getPathFromRootFolder() throws Exception {
        assertEquals("(root)", resolver.getPath(createRootFolder()));
    }

    @Test
    public void getPathFromFolderOid() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final String path = resolver.getPath(new HasFolderOidStub(OID));
        assertEquals("/folder1/folder2/", path);
    }

    @Test
    public void getPathFromNullFolderOid() throws Exception {
        final String path = resolver.getPath(new HasFolderOidStub(null));
        assertEquals("/", path);
        verify(folderAdmin, never()).findByPrimaryKey(anyLong());
    }

    @Test
    public void getPathFromFolderOidDoesNotExist() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(null);
        assertEquals("/", resolver.getPath(new HasFolderOidStub(OID)));
    }

    @Test(expected = FindException.class)
    public void getPathFromFolderOidFindException() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenThrow(new FindException("mocking exception"));
        resolver.getPath(new HasFolderOidStub(OID));
    }

    @BugId("SSG-7239")
    @Test
    public void getPathFromRootFolderHeader() throws Exception {
        assertEquals("(root)", resolver.getPath(new FolderHeader(createRootFolder())));
    }

    @Test
    public void getNameForServiceHeaderIncludesRoutingUri() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        service.setRoutingUri("/routingUri");
        assertEquals("test [/routingUri] (/test)", resolver.getNameForHeader(new ServiceHeader(service)));
    }

    @Test
    public void getNameForHeaderNullTypeAndName() throws Exception {
        assertTrue(resolver.getNameForHeader(new EntityHeader("", null, null, null)).isEmpty());
    }

    @Test
    public void getNameFromHeaderWithoutPath() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        service.setRoutingUri("/routingUri");
        assertEquals("test [/routingUri]", resolver.getNameForHeader(new ServiceHeader(service), false));
    }

    @BugId("SSG-7159")
    @Test
    public void getNameForAssertionAccessHeader() throws Exception {
        final String className = "com.l7tech.policy.assertion.composite.AllAssertion";
        final EntityHeader assertionHeader = new EntityHeader(OID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(new AllAssertion());
        assertEquals("All assertions must evaluate to true", resolver.getNameForHeader(assertionHeader));
    }

    @Test
    public void getNameForAssertionAccessHeaderNotFound() throws Exception {
        final String className = "com.l7tech.policy.assertion.composite.AllAssertion";
        final EntityHeader assertionHeader = new EntityHeader(OID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(null);
        assertEquals(className, resolver.getNameForHeader(assertionHeader));
    }

    @Test
    public void getNameForAssertionAccessHeaderMissingClassNameOnHeader() throws Exception {
        final EntityHeader assertionHeader = new EntityHeader(OID, EntityType.ASSERTION_ACCESS, null, null);
        assertTrue(resolver.getNameForHeader(assertionHeader).isEmpty());
    }

    @BugId("SSG-7267")
    @Test
    public void getNameForCustomAssertionAccessHeader() throws Exception {
        final String className = CustomAssertionHolder.class.getName();
        final EntityHeader assertionHeader = new EntityHeader(OID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(new CustomAssertionHolder());
        assertEquals(CustomAssertionHolder.CUSTOM_ASSERTION, resolver.getNameForHeader(assertionHeader));
    }

    @Test
    public void getPaletteFolderName() throws Exception {
        when(folderRegistry.getPaletteFolderName("threatProtection")).thenReturn("Threat Protection");
        when(folderRegistry.getPaletteFolderName("xml")).thenReturn("Message Validation/Transformation");
        assertEquals("Threat Protection,Message Validation/Transformation", resolver.getPaletteFolders(new ContentTypeAssertion()));
    }

    @Test
    public void getPaletteFolderNameCannotFindFolderName() throws Exception {
        when(folderRegistry.getPaletteFolderName("policyLogic")).thenReturn(null);
        assertEquals("unknown folder", resolver.getPaletteFolders(new AllAssertion()));
    }

    @Test
    public void getPaletteFolderNameNoFolders() throws Exception {
        assertTrue(resolver.getPaletteFolders(new RequireWssSaml()).isEmpty());
    }

    @BugId("SSM-4366")
    @Test
    public void getPaletteFolderNameForCustomAssertion() throws Exception {
        assertEquals("--", resolver.getPaletteFolders(new CustomAssertionHolder()));
    }

    @Test
    public void getNameForServiceAlias() throws Exception {
        final PublishedService service = createService(OID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findServiceByID(String.valueOf(OID))).thenReturn(service);
        assertEquals("test alias [/routingUri] (/aliases/test alias)", resolver.getNameForEntity(alias, true));
    }

    @Test
    public void getNameForServiceAliasWithoutPath() throws Exception {
        final PublishedService service = createService(OID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findServiceByID(String.valueOf(OID))).thenReturn(service);
        assertEquals("test alias [/routingUri]", resolver.getNameForEntity(alias, false));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceAliasCannotFindService() throws Exception {
        final PublishedService service = createService(OID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(OID, service, folder);
        when(serviceAdmin.findServiceByID(String.valueOf(OID))).thenReturn(null);
        resolver.getNameForEntity(alias, true);
    }

    @Test
    public void getNameForPolicyAlias() throws Exception {
        final Policy policy = createPolicy(OID, NAME);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PolicyAlias alias = createPolicyAlias(OID, policy, folder);
        when(policyAdmin.findPolicyByPrimaryKey(OID)).thenReturn(policy);
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(folder);
        assertEquals("test alias (/aliases/test alias)", resolver.getNameForEntity(alias, true));
    }

    @Test
    public void getNameForPolicyAliasWithoutPath() throws Exception {
        final Policy policy = createPolicy(OID, NAME);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PolicyAlias alias = createPolicyAlias(OID, policy, folder);
        when(policyAdmin.findPolicyByPrimaryKey(OID)).thenReturn(policy);
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(folder);
        assertEquals("test alias", resolver.getNameForEntity(alias, false));
    }

    @Test(expected = FindException.class)
    public void getNameForPolicyAliasCannotFindPolicy() throws Exception {
        final Policy policy = createPolicy(OID, NAME);
        final Folder folder = createFolderInRoot(OID, "aliases");
        final PolicyAlias alias = createPolicyAlias(OID, policy, folder);
        when(policyAdmin.findPolicyByPrimaryKey(OID)).thenReturn(null);
        resolver.getNameForEntity(alias, false);
    }

    @Test
    public void getNameForKeyMetadata() throws Exception {
        final SsgKeyMetadata metadata = new SsgKeyMetadata();
        metadata.setAlias(NAME);
        assertEquals(NAME, resolver.getNameForEntity(metadata, true));
    }

    @Test
    public void getNameForResourceEntry() throws Exception {
        final ResourceEntry resource = new ResourceEntry();
        resource.setUri("http://localhost");
        assertEquals("http://localhost", resolver.getNameForEntity(resource, true));
    }

    @Test
    public void getNameForHttpConfig() throws Exception {
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setProtocol(HttpConfiguration.Protocol.HTTP);
        httpConfig.setHost("localhost");
        httpConfig.setPort(8080);
        assertEquals("HTTP localhost 8080", resolver.getNameForEntity(httpConfig, true));
    }

    @Test
    public void getNameForAssertionAccess() throws Exception {
        final AssertionAccess assertionAccess = new AssertionAccess(AllAssertion.class.getName());
        when(assertionRegistry.findByClassName(AllAssertion.class.getName())).thenReturn(new AllAssertion());
        assertEquals("All assertions must evaluate to true", resolver.getNameForEntity(assertionAccess, true));
    }

    @Test
    public void getNameForAssertionAccessWithoutPath() throws Exception {
        final AssertionAccess assertionAccess = new AssertionAccess(AllAssertion.class.getName());
        when(assertionRegistry.findByClassName(AllAssertion.class.getName())).thenReturn(new AllAssertion());
        when(folderRegistry.getPaletteFolderName("policyLogic")).thenReturn("Policy Logic");
        assertEquals("All assertions must evaluate to true", resolver.getNameForEntity(assertionAccess, false));
    }

    @Test
    public void getNameForAssertionAccessCannotFindAssertion() throws Exception {
        final AssertionAccess assertionAccess = new AssertionAccess(AllAssertion.class.getName());
        when(assertionRegistry.findByClassName(AllAssertion.class.getName())).thenReturn(null);
        assertEquals(AllAssertion.class.getName(), resolver.getNameForEntity(assertionAccess, true));
    }

    @BugId("SSG-7267")
    @Test
    public void getNameForCustomAssertionAccess() throws Exception {
        final AssertionAccess assertionAccess = new AssertionAccess(CustomAssertionHolder.class.getName());
        when(assertionRegistry.findByClassName(CustomAssertionHolder.class.getName())).thenReturn(new CustomAssertionHolder());
        assertEquals(CustomAssertionHolder.CUSTOM_ASSERTION, resolver.getNameForEntity(assertionAccess, true));
    }

    @Test
    public void getNameForServiceRole() throws Exception {
        final PublishedService service = createService(OID, NAME, "/routingUri");
        service.setFolder(createFolderInRoot(OID, "services"));
        final Role role = createRole("Manage test Service (#1234)", service);
        assertEquals("Manage test [/routingUri] Service (/services/test)", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForServiceRoleNoPath() throws Exception {
        final PublishedService service = createService(OID, NAME, "/routingUri");
        service.setFolder(createFolderInRoot(OID, "services"));
        final Role role = createRole("Manage test Service (#1234)", service);
        assertEquals("Manage test [/routingUri] Service", resolver.getNameForEntity(role, false));
    }

    @Test
    public void getNameForPolicyRole() throws Exception {
        final Policy policy = createPolicy(OID, NAME);
        policy.setFolder(createFolderInRoot(OID, "policies"));
        final Role role = createRole("Manage test Policy (#1234)", policy);
        assertEquals("Manage test Policy (/policies/test)", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForPolicyRoleNoPath() throws Exception {
        final Policy policy = createPolicy(OID, NAME);
        policy.setFolder(createFolderInRoot(OID, "policies"));
        final Role role = createRole("Manage test Policy (#1234)", policy);
        assertEquals("Manage test Policy", resolver.getNameForEntity(role, false));
    }

    @Test
    public void getNameForFolderRole() throws Exception {
        final Folder folder = createFolderInRoot(OID, NAME);
        final Role role = createRole("Manage test Folder (#1234)", folder);
        assertEquals("Manage test Folder (/test)", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForFolderRoleNoPath() throws Exception {
        final Folder folder = createFolderInRoot(OID, NAME);
        final Role role = createRole("Manage test Folder (#1234)", folder);
        assertEquals("Manage test Folder", resolver.getNameForEntity(role, false));
    }

    @Test
    public void getNameForIdentityProviderRole() throws Exception {
        final IdentityProviderConfig provider = new IdentityProviderConfig();
        provider.setOid(OID);
        provider.setName(NAME);
        final Role role = createRole("Manage test Identity Provider (#1234)", provider);
        assertEquals("Manage test Identity Provider", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForSecurityZoneRole() throws Exception {
        final SecurityZone zone = new SecurityZone();
        zone.setName(NAME);
        final Role role = createRole("Manage test Zone (#1234)", zone);
        assertEquals("Manage test Zone", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForRoleNoEntity() throws Exception {
        final Role role = createRole(NAME, null);
        assertEquals(NAME, resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForNamedEntity() throws Exception {
        assertEquals(NAME, resolver.getNameForEntity(new NamedEntityImp() {
            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public int getVersion() {
                return 0;
            }
        }, true));
    }

    @Test
    public void getNameForNonNamedEntity() throws Exception {
        assertTrue(resolver.getNameForEntity(new Entity() {
            @Override
            public String getId() {
                return null;
            }
        }, true).isEmpty());
    }

    @BugId("SSG-7239")
    @Test
    public void getNameForRootFolder() throws Exception {
        assertEquals("localhost (root)", resolver.getNameForEntity(createRootFolder(), true));
    }

    @BugId("SSG-7239")
    @Test
    public void getNameForRootFolderWithoutPath() throws Exception {
        assertEquals("localhost", resolver.getNameForEntity(createRootFolder(), false));
    }

    private Folder createRootFolder() {
        final Folder rootFolder = new Folder("Root Node", null);
        rootFolder.setOid(RootNode.OID);
        return rootFolder;
    }

    private Role createRole(final String name, final Entity roleEntity) {
        final Role role = new Role();
        role.setName(name);
        role.setCachedSpecificEntity(roleEntity);
        return role;
    }

    private Policy createPolicy(final Long oid, final String name) {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, name, "xml", false);
        policy.setOid(oid);
        return policy;
    }

    private PublishedService createService(final Long oid, final String name, final String routingUri) {
        final PublishedService service = new PublishedService();
        service.setName(name);
        service.setRoutingUri(routingUri);
        service.setOid(oid);
        return service;
    }

    private Folder createFolderInRoot(final Long oid, final String name) {
        final Folder folder = new Folder(name, createRootFolder());
        folder.setOid(oid);
        return folder;
    }

    private PublishedServiceAlias createServiceAlias(final Long oid, final PublishedService service, final Folder folder) {
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, folder);
        alias.setOid(oid);
        return alias;
    }

    private PolicyAlias createPolicyAlias(final Long oid, final Policy policy, final Folder folder) {
        final PolicyAlias policyAlias = new PolicyAlias(policy, folder);
        policyAlias.setOid(oid);
        return policyAlias;
    }

    private class HasFolderStub implements HasFolder {
        private Folder folder;

        private HasFolderStub(final Folder folder) {
            this.folder = folder;
        }

        @Override
        public Folder getFolder() {
            return folder;
        }

        @Override
        public void setFolder(final Folder folder) {
            this.folder = folder;
        }
    }

    private class HasFolderOidStub extends EntityHeader implements HasFolderOid {
        private Long folderOid;

        private HasFolderOidStub(final Long folderOid) {
            this.folderOid = folderOid;
        }

        @Override
        public Long getFolderOid() {
            return folderOid;
        }

        @Override
        public void setFolderOid(final Long folderOid) {
            this.folderOid = folderOid;
        }
    }
}
