package com.l7tech.console.util;

import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfoHeader;
import com.l7tech.gateway.common.uddi.UDDIServiceControlHeader;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.ContentTypeAssertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntityNameResolverTest {
    private static final Goid GOID = new Goid(0, 1234L);
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
    private ClusterStatusAdmin clusterStatusAdmin;
    @Mock
    private AssertionRegistry assertionRegistry;
    @Mock
    private PaletteFolderRegistry folderRegistry;

    @Before
    public void setup() {
        resolver = new EntityNameResolver(serviceAdmin, policyAdmin, trustedCertAdmin, resourceAdmin, folderAdmin, clusterStatusAdmin, assertionRegistry, folderRegistry, "localhost");
    }

    @Test
    public void getNameForHeaderUsesNameOnHeader() throws Exception {
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader("", null, "test", null)));
    }

    @Test
    public void getNameForServiceAliasHeader() throws Exception {
        final PublishedService service = createService(GOID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(GOID, service, folder);
        when(serviceAdmin.findByAlias(GOID)).thenReturn(service);
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(folder);
        assertEquals("test alias [/routingUri] (/aliases/test alias)", resolver.getNameForHeader(new AliasHeader(alias)));
    }

    @Test
    public void getNameForServiceAliasHeaderNullRoutingUri() throws Exception {
        final PublishedService service = createService(GOID, NAME, null);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(GOID, service, folder);
        when(serviceAdmin.findByAlias(GOID)).thenReturn(service);
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(folder);
        assertEquals("test alias (/aliases/test alias)", resolver.getNameForHeader(new AliasHeader(alias)));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceAliasHeaderServiceNotFound() throws Exception {
        final PublishedService service = createService(GOID, NAME, null);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(GOID, service, folder);
        when(serviceAdmin.findByAlias(any(Goid.class))).thenReturn(null);
        resolver.getNameForHeader(new AliasHeader(alias));
    }

    @Test
    public void getNameForPolicyAliasHeader() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PolicyAlias alias = createPolicyAlias(GOID, policy, folder);
        when(policyAdmin.findByAlias(GOID)).thenReturn(policy);
        assertEquals("test alias (/test alias)", resolver.getNameForHeader(new AliasHeader(alias)));
    }

    @Test(expected = FindException.class)
    public void getNameForPolicyAliasHeaderNotFound() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PolicyAlias alias = createPolicyAlias(GOID, policy, folder);
        when(policyAdmin.findByAlias(any(Goid.class))).thenReturn(null);
        resolver.getNameForHeader(new AliasHeader(alias));
    }

    @Test
    public void getNameForKeyMetadataHeader() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(GOID)).thenReturn(new SsgKeyMetadata(new Goid(0, 1), NAME, null));
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(GOID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForKeyMetadataHeaderNotFound() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(any(Goid.class))).thenReturn(null);
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(GOID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @BugId("SSG-7314")
    @Test
    public void getNameForKeyMetadataHeaderNotYetPersisted() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(any(Goid.class))).thenReturn(null);
        assertEquals(NAME, resolver.getNameForHeader(new KeyMetadataHeaderWrapper(new SsgKeyMetadata(new Goid(0, 1), NAME, null))));
    }

    @Test
    public void getNameForResourceEntryHeader() throws Exception {
        final String uri = "http://localhost:8080";
        final ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setUri(uri);
        when(resourceAdmin.findResourceEntryByPrimaryKey(GOID)).thenReturn(resourceEntry);
        assertEquals(uri, resolver.getNameForHeader(new EntityHeader(GOID, EntityType.RESOURCE_ENTRY, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForResourceEntryHeaderNotFound() throws Exception {
        when(resourceAdmin.findResourceEntryByPrimaryKey(any(Goid.class))).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(GOID, EntityType.RESOURCE_ENTRY, null, null));
    }

    @Test
    public void getNameForHttpConfigurationHeader() throws Exception {
        final HttpConfiguration config = new HttpConfiguration();
        config.setProtocol(HttpConfiguration.Protocol.HTTP);
        config.setHost("localhost");
        config.setPort(8080);
        config.setPath("somePath");
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(GOID)).thenReturn(config);
        assertEquals("HTTP localhost 8080 somePath", resolver.getNameForHeader(new EntityHeader(GOID, EntityType.HTTP_CONFIGURATION, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForHttpConfigurationHeaderNotFound() throws Exception {
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(Matchers.<Goid>anyObject())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(GOID, EntityType.HTTP_CONFIGURATION, null, null));
    }

    @Test
    public void geNameForHeaderUnsupportedEntityType() throws Exception {
        assertTrue(resolver.getNameForHeader(new EntityHeader(GOID, EntityType.ANY, null, null)).isEmpty());
    }

    @Test
    public void getNameForHasFolderOidHeader() throws Exception {
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final HasFolderOidStub header = new HasFolderOidStub(GOID);
        header.setName(NAME);
        final String name = resolver.getNameForHeader(header);
        assertEquals("test (/folder1/folder2/test)", name);
    }

    @Test
    public void getNameForHasFolderOidHeaderNoName() throws Exception {
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final HasFolderOidStub header = new HasFolderOidStub(GOID);
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
    public void getNameForServiceUsageHeader() throws Exception {
        final Goid serviceGoid = new Goid(0, 1);
        final ServiceUsageHeader usageHeader = new ServiceUsageHeader(serviceGoid, "abc123");
        final PublishedService service = new PublishedService();
        service.setName("test");
        when(serviceAdmin.findServiceByID(serviceGoid.toHexString())).thenReturn(service);

        assertEquals("test (/test) on node abc123", resolver.getNameForHeader(usageHeader, true));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceUsageHeaderServiceDoesNotExist() throws Exception {
        final Goid serviceGoid = new Goid(0, 1);
        final ServiceUsageHeader usageHeader = new ServiceUsageHeader(serviceGoid, "abc123");
        when(serviceAdmin.findServiceByID(serviceGoid.toHexString())).thenReturn(null);
        resolver.getNameForHeader(usageHeader, true);
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
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final String path = resolver.getPath(new HasFolderOidStub(GOID));
        assertEquals("/folder1/folder2/", path);
    }

    @Test
    public void getPathFromNullFolderOid() throws Exception {
        final String path = resolver.getPath(new HasFolderOidStub(null));
        assertEquals("/", path);
        verify(folderAdmin, never()).findByPrimaryKey(any(Goid.class));
    }

    @Test
    public void getPathFromFolderOidDoesNotExist() throws Exception {
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(null);
        assertEquals("/", resolver.getPath(new HasFolderOidStub(GOID)));
    }

    @Test(expected = FindException.class)
    public void getPathFromFolderOidFindException() throws Exception {
        when(folderAdmin.findByPrimaryKey(GOID)).thenThrow(new FindException("mocking exception"));
        resolver.getPath(new HasFolderOidStub(GOID));
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
        final EntityHeader assertionHeader = new EntityHeader(GOID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(new AllAssertion());
        assertEquals("All assertions must evaluate to true", resolver.getNameForHeader(assertionHeader, false));
    }

    @BugId("SSG-7298")
    @Test
    public void getNameForAssertionAccessHeaderUsesPaletteName() throws Exception {
        final String className = "com.l7tech.policy.assertion.xmlsec.AddWssTimestamp";
        final EntityHeader assertionHeader = new EntityHeader(GOID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(new AddWssTimestamp());
        assertEquals("Add Timestamp", resolver.getNameForHeader(assertionHeader, false));
    }

    @Test
    public void getNameForAssertionAccessHeaderNotFound() throws Exception {
        final String className = "com.l7tech.policy.assertion.composite.AllAssertion";
        final EntityHeader assertionHeader = new EntityHeader(GOID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(null);
        assertEquals(className, resolver.getNameForHeader(assertionHeader));
    }

    @Test
    public void getNameForAssertionAccessHeaderMissingClassNameOnHeader() throws Exception {
        final EntityHeader assertionHeader = new EntityHeader(GOID, EntityType.ASSERTION_ACCESS, null, null);
        assertTrue(resolver.getNameForHeader(assertionHeader).isEmpty());
    }

    @BugId("SSG-7267")
    @Test
    public void getNameForCustomAssertionAccessHeader() throws Exception {
        final String className = CustomAssertionHolder.class.getName();
        final EntityHeader assertionHeader = new EntityHeader(GOID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(new CustomAssertionHolder());
        assertEquals(CustomAssertionHolder.CUSTOM_ASSERTION, resolver.getNameForHeader(assertionHeader, false));
    }

    @Test
    public void getNameForAssertionAccessHeaderWithPath() throws Exception {
        final String className = "com.l7tech.policy.assertion.composite.AllAssertion";
        final EntityHeader assertionHeader = new EntityHeader(GOID, EntityType.ASSERTION_ACCESS, className, null);
        when(assertionRegistry.findByClassName(className)).thenReturn(new AllAssertion());
        when(folderRegistry.getPaletteFolderName("policyLogic")).thenReturn("Policy Logic");
        assertEquals("All assertions must evaluate to true (Policy Logic)", resolver.getNameForHeader(assertionHeader, true));
    }

    @BugId("SSG-6981")
    @Test
    public void getNameForUDDIServiceControlHeader() throws Exception {
        final Goid publishedServiceGoid = new Goid(1, 2);
        final UDDIServiceControlHeader header = new UDDIServiceControlHeader(new Goid(0, 1), null, null, 0, null, publishedServiceGoid);
        final PublishedService publishedService = new PublishedService();
        publishedService.setName("test");
        when(serviceAdmin.findServiceByID(publishedServiceGoid.toHexString())).thenReturn(publishedService);
        assertEquals("test (/test)", resolver.getNameForHeader(header, true));
    }

    @BugId("SSG-6981")
    @Test
    public void getNameForUDDIProxiedServiceInfoHeader() throws Exception {
        final Goid publishedServiceGoid = new Goid(1, 2);
        final UDDIProxiedServiceInfoHeader header = new UDDIProxiedServiceInfoHeader(new Goid(0, 1), null, null, 0, null, publishedServiceGoid);
        final PublishedService publishedService = new PublishedService();
        publishedService.setName("test");
        when(serviceAdmin.findServiceByID(publishedServiceGoid.toHexString())).thenReturn(publishedService);
        assertEquals("test (/test)", resolver.getNameForHeader(header, true));
    }

    @BugId("SSM-4491")
    @Test
    public void getNameForTrustedEsmHeader() throws Exception {
        final TrustedCert trustedCert = new TrustedCert();
        trustedCert.setSubjectDn("CN=test");
        final Goid trustedEsmGoid = new Goid(1, 2);
        final TrustedEsm trustedEsm = new TrustedEsm();
        trustedEsm.setGoid(trustedEsmGoid);
        trustedEsm.setTrustedCert(trustedCert);
        final EntityHeader header = new EntityHeader(trustedEsmGoid, EntityType.TRUSTED_ESM, "thisnameshouldbeignored", null, null);
        when(clusterStatusAdmin.findTrustedEsm(trustedEsmGoid)).thenReturn(trustedEsm);
        assertEquals("CN=test", resolver.getNameForHeader(header, true));
    }

    @Test(expected = FindException.class)
    public void getNameForTrustedEsmHeaderNotFound() throws Exception {
        final Goid trustedEsmGoid = new Goid(1, 2);
        final EntityHeader header = new EntityHeader(trustedEsmGoid, EntityType.TRUSTED_ESM, "thisnameshouldbeignored", null, null);
        when(clusterStatusAdmin.findTrustedEsm(trustedEsmGoid)).thenReturn(null);
        resolver.getNameForHeader(header);
    }

    @Test
    public void getNameForTrustedEsmHeaderNoTrustedCert() throws Exception {
        final Goid trustedEsmGoid = new Goid(1, 2);
        final TrustedEsm trustedEsm = new TrustedEsm();
        trustedEsm.setName("backupname");
        trustedEsm.setGoid(trustedEsmGoid);
        trustedEsm.setTrustedCert(null);
        final EntityHeader header = new EntityHeader(trustedEsmGoid, EntityType.TRUSTED_ESM, "thisnameshouldbeignored", null, null);
        when(clusterStatusAdmin.findTrustedEsm(trustedEsmGoid)).thenReturn(trustedEsm);
        assertEquals("backupname", resolver.getNameForHeader(header, true));
    }

    @BugId("SSM-4491")
    @Test
    public void getNameForTrustedEsmUserHeader() throws Exception {
        final Goid trustedEsmUserGoid = new Goid(1, 2);
        final TrustedEsmUser user = new TrustedEsmUser();
        user.setGoid(trustedEsmUserGoid);
        user.setEsmUserDisplayName("test");
        final EntityHeader header = new EntityHeader(trustedEsmUserGoid, EntityType.TRUSTED_ESM_USER, null, null);
        when(clusterStatusAdmin.findTrustedEsmUser(trustedEsmUserGoid)).thenReturn(user);
        assertEquals("test", resolver.getNameForHeader(header, true));
    }

    @Test
    public void getNameForTrustedEsmUserHeaderNoDisplayName() throws Exception {
        final Goid trustedEsmUserGoid = new Goid(1, 2);
        final TrustedEsmUser user = new TrustedEsmUser();
        user.setGoid(trustedEsmUserGoid);
        user.setEsmUserDisplayName(null);
        user.setSsgUserId("userId");
        final EntityHeader header = new EntityHeader(trustedEsmUserGoid, EntityType.TRUSTED_ESM_USER, null, null);
        when(clusterStatusAdmin.findTrustedEsmUser(trustedEsmUserGoid)).thenReturn(user);
        assertEquals("userId", resolver.getNameForHeader(header, true));
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
        assertEquals("<multiple>", resolver.getPaletteFolders(new CustomAssertionHolder()));
    }

    @Test
    public void getPaletteFolderNameForEncapsulatedAssertion() throws Exception {
        assertEquals("<multiple>", resolver.getPaletteFolders(new EncapsulatedAssertion()));
    }

    @Test
    public void getNameForServiceAlias() throws Exception {
        final PublishedService service = createService(GOID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(GOID, service, folder);
        when(serviceAdmin.findServiceByID(String.valueOf(GOID))).thenReturn(service);
        assertEquals("test alias [/routingUri] (/aliases/test alias)", resolver.getNameForEntity(alias, true));
    }

    @Test
    public void getNameForServiceAliasWithoutPath() throws Exception {
        final PublishedService service = createService(GOID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(GOID, service, folder);
        when(serviceAdmin.findServiceByID(String.valueOf(GOID))).thenReturn(service);
        assertEquals("test alias [/routingUri]", resolver.getNameForEntity(alias, false));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceAliasCannotFindService() throws Exception {
        final PublishedService service = createService(GOID, NAME, "/routingUri");
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PublishedServiceAlias alias = createServiceAlias(GOID, service, folder);
        when(serviceAdmin.findServiceByID(String.valueOf(GOID))).thenReturn(null);
        resolver.getNameForEntity(alias, true);
    }

    @Test
    public void getNameForPolicyAlias() throws Exception {
        final Policy policy = createPolicy(GOID, NAME);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PolicyAlias alias = createPolicyAlias(GOID, policy, folder);
        when(policyAdmin.findPolicyByPrimaryKey(GOID)).thenReturn(policy);
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(folder);
        assertEquals("test alias (/aliases/test alias)", resolver.getNameForEntity(alias, true));
    }

    @Test
    public void getNameForPolicyAliasWithoutPath() throws Exception {
        final Policy policy = createPolicy(GOID, NAME);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PolicyAlias alias = createPolicyAlias(GOID, policy, folder);
        when(policyAdmin.findPolicyByPrimaryKey(GOID)).thenReturn(policy);
        when(folderAdmin.findByPrimaryKey(GOID)).thenReturn(folder);
        assertEquals("test alias", resolver.getNameForEntity(alias, false));
    }

    @Test(expected = FindException.class)
    public void getNameForPolicyAliasCannotFindPolicy() throws Exception {
        final Policy policy = createPolicy(GOID, NAME);
        final Folder folder = createFolderInRoot(GOID, "aliases");
        final PolicyAlias alias = createPolicyAlias(GOID, policy, folder);
        when(policyAdmin.findPolicyByPrimaryKey(GOID)).thenReturn(null);
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
        httpConfig.setPath("somePath");
        assertEquals("HTTP localhost 8080 somePath", resolver.getNameForEntity(httpConfig, true));
    }

    @Test
    public void getNameForHttpConfigNoProtocolOrPathAndDefaultPort() throws Exception {
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setProtocol(null);
        httpConfig.setHost("localhost");
        httpConfig.setPath(null);
        assertEquals("<no protocol> localhost <no port> <no path>", resolver.getNameForEntity(httpConfig, true));
    }

    @Test
    public void getNameForAssertionAccess() throws Exception {
        final AssertionAccess assertionAccess = new AssertionAccess(AllAssertion.class.getName());
        when(assertionRegistry.findByClassName(AllAssertion.class.getName())).thenReturn(new AllAssertion());
        when(folderRegistry.getPaletteFolderName("policyLogic")).thenReturn("Policy Logic");
        assertEquals("All assertions must evaluate to true (Policy Logic)", resolver.getNameForEntity(assertionAccess, true));
    }

    @Test
    public void getNameForAssertionAccessWithoutPath() throws Exception {
        final AssertionAccess assertionAccess = new AssertionAccess(AllAssertion.class.getName());
        when(assertionRegistry.findByClassName(AllAssertion.class.getName())).thenReturn(new AllAssertion());
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
        assertEquals(CustomAssertionHolder.CUSTOM_ASSERTION, resolver.getNameForEntity(assertionAccess, false));
    }

    @Test
    public void getNameForServiceRole() throws Exception {
        final PublishedService service = createService(GOID, NAME, "/routingUri");
        service.setFolder(createFolderInRoot(GOID, "services"));
        final Role role = createRole("Manage test Service (#1234)", service);
        assertEquals("Manage test [/routingUri] Service (/services/test)", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForServiceRoleNoPath() throws Exception {
        final PublishedService service = createService(GOID, NAME, "/routingUri");
        service.setFolder(createFolderInRoot(GOID, "services"));
        final Role role = createRole("Manage test Service (#1234)", service);
        assertEquals("Manage test [/routingUri] Service", resolver.getNameForEntity(role, false));
    }

    @Test
    public void getNameForPolicyRole() throws Exception {
        final Policy policy = createPolicy(GOID, NAME);
        policy.setFolder(createFolderInRoot(GOID, "policies"));
        final Role role = createRole("Manage test Policy (#1234)", policy);
        assertEquals("Manage test Policy (/policies/test)", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForPolicyRoleNoPath() throws Exception {
        final Policy policy = createPolicy(GOID, NAME);
        policy.setFolder(createFolderInRoot(GOID, "policies"));
        final Role role = createRole("Manage test Policy (#1234)", policy);
        assertEquals("Manage test Policy", resolver.getNameForEntity(role, false));
    }

    @Test
    public void getNameForFolderRole() throws Exception {
        final Folder folder = createFolderInRoot(GOID, NAME);
        final Role role = createRole("Manage test Folder (#1234)", folder);
        assertEquals("Manage test Folder (/test)", resolver.getNameForEntity(role, true));
    }

    @Test
    public void getNameForFolderRoleNoPath() throws Exception {
        final Folder folder = createFolderInRoot(GOID, NAME);
        final Role role = createRole("Manage test Folder (#1234)", folder);
        assertEquals("Manage test Folder", resolver.getNameForEntity(role, false));
    }

    @Test
    public void getNameForIdentityProviderRole() throws Exception {
        final IdentityProviderConfig provider = new IdentityProviderConfig();
        provider.setGoid(GOID);
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

    @Test
    public void getNameForObjectIdentityPredicate() throws Exception {
        final ObjectIdentityPredicate predicate = new ObjectIdentityPredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "1234");
        predicate.setHeader(new EntityHeader("1234", EntityType.POLICY, "test", null));
        assertEquals("Policy \"test\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForObjectIdentityPredicateNoHeader() throws Exception {
        final ObjectIdentityPredicate predicate = new ObjectIdentityPredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "1234");
        predicate.setHeader(null);
        assertEquals("Policy 1234", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForObjectIdentityPredicateNoHeaderOrPermission() throws Exception {
        final ObjectIdentityPredicate predicate = new ObjectIdentityPredicate(null, "1234");
        predicate.setHeader(null);
        assertEquals("1234", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateStartsWith() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "name", "test");
        predicate.setMode("eq");
        assertEquals("name equals test", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateNullMode() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "name", "test");
        predicate.setMode(null);
        assertEquals("name equals test", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateEquals() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "name", "test");
        predicate.setMode("sw");
        assertEquals("name starts with test", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateUnknownMode() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "name", "test");
        predicate.setMode("unknown");
        assertEquals("attribute=name mode=unknown value=test", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateCaseInsensitiveMode() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "name", "test");
        predicate.setMode("EQ");
        assertEquals("name equals test", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateAssertionAccessNameAttribute() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.ASSERTION_ACCESS), "name", AllAssertion.class.getName());
        when(assertionRegistry.findByClassName(AllAssertion.class.getName())).thenReturn(new AllAssertion());
        assertEquals("name equals All assertions must evaluate to true", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateAssertionAccessNonNameAttribute() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.ASSERTION_ACCESS), "id", "1234");
        assertEquals("id equals 1234", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateAssertionAccessNameAttributeStartsWith() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.ASSERTION_ACCESS), "name", "com.l7tech");
        predicate.setMode("sw");
        assertEquals("name starts with com.l7tech", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateAssertionAccessNameAttributeUnrecognized() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.ASSERTION_ACCESS), "name", "notvalid");
        assertEquals("name equals notvalid", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForAttributePredicateGetsDisplayNameForAttribute() throws Exception {
        final AttributePredicate predicate = new AttributePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), "soap", "true");
        predicate.setMode("eq");
        assertEquals("is SOAP equals true", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForSecurityZonePredicate() throws Exception {
        final SecurityZone zone = new SecurityZone();
        zone.setName("test");
        final SecurityZonePredicate predicate = new SecurityZonePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), zone);
        assertEquals("in security zone \"test\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForSecurityZonePredicateNullZone() throws Exception {
        final SecurityZonePredicate predicate = new SecurityZonePredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), null);
        assertEquals("without a security zone", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderPredicateTransitive() throws Exception {
        final FolderPredicate predicate = new FolderPredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), new Folder("test", null), true);
        assertEquals("in folder \"test\" and subfolders", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderPredicateNotTransitive() throws Exception {
        final FolderPredicate predicate = new FolderPredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), new Folder("test", null), false);
        assertEquals("in folder \"test\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderAncestryPredicatePolicy() throws Exception {
        when(policyAdmin.findPolicyByPrimaryKey(new Goid(0, 1234L))).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false));
        final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(new Permission(new Role(), OperationType.READ, EntityType.FOLDER), EntityType.POLICY, new Goid(0, 1234));
        assertEquals("ancestors of policy \"test\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderAncestryPredicateService() throws Exception {
        final PublishedService publishedService = new PublishedService();
        publishedService.setName("test");
        when(serviceAdmin.findServiceByID("1234")).thenReturn(publishedService);
        final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(new Permission(new Role(), OperationType.READ, EntityType.FOLDER), EntityType.SERVICE, "1234");
        assertEquals("ancestors of published service \"test\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderAncestryPredicateFolder() throws Exception {
        when(folderAdmin.findByPrimaryKey(new Goid(0, 1234L))).thenReturn(new Folder("test", null));
        final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(new Permission(new Role(), OperationType.READ, EntityType.FOLDER), EntityType.FOLDER, new Goid(0, 1234));
        assertEquals("ancestors of folder \"test\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderAncestryPredicateServiceAlias() throws Exception {
        final PublishedService publishedService = new PublishedService();
        publishedService.setName("test");
        when(serviceAdmin.findByAlias(new Goid(0, 1234L))).thenReturn(publishedService);
        final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(new Permission(new Role(), OperationType.READ, EntityType.FOLDER), EntityType.SERVICE_ALIAS, new Goid(0, 1234));
        assertEquals("ancestors of published service alias \"test alias\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderAncestryPredicatePolicyAlias() throws Exception {
        when(policyAdmin.findByAlias(new Goid(0, 1234L))).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false));
        final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(new Permission(new Role(), OperationType.READ, EntityType.FOLDER), EntityType.POLICY_ALIAS, new Goid(0, 1234));
        assertEquals("ancestors of policy alias \"test alias\"", resolver.getNameForEntity(predicate, false));
    }

    @Test
    public void getNameForFolderAncestryPredicateNullEntityType() throws Exception {
        final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(new Permission(new Role(), OperationType.READ, EntityType.POLICY), null, new Goid(0, 1234));
        assertTrue(resolver.getNameForEntity(predicate, false).isEmpty());
    }

    @Test
    public void getNameForTrustedEsm() throws Exception {
        final TrustedCert trustedCert = new TrustedCert();
        trustedCert.setSubjectDn("CN=test");
        final TrustedEsm trustedEsm = new TrustedEsm();
        trustedEsm.setTrustedCert(trustedCert);
        assertEquals("CN=test", resolver.getNameForEntity(trustedEsm, true));
    }

    @Test
    public void getNameForTrustedEsmNoTrustedCert() throws Exception {
        final TrustedEsm trustedEsm = new TrustedEsm();
        trustedEsm.setName("backupname");
        trustedEsm.setTrustedCert(null);
        assertEquals("backupname", resolver.getNameForEntity(trustedEsm, true));
    }

    @Test
    public void getNameForTrustedEsmUser() throws Exception {
        final TrustedEsmUser trustedEsmUser = new TrustedEsmUser();
        trustedEsmUser.setEsmUserDisplayName("test");
        assertEquals("test", resolver.getNameForEntity(trustedEsmUser, true));
    }

    @Test
    public void getNameForTrustedEsmUserNoDisplayName() throws Exception {
        final TrustedEsmUser trustedEsmUser = new TrustedEsmUser();
        trustedEsmUser.setEsmUserDisplayName(null);
        trustedEsmUser.setSsgUserId("userId");
        assertEquals("userId", resolver.getNameForEntity(trustedEsmUser, true));
    }

    private Folder createRootFolder() {
        final Folder rootFolder = new Folder("Root Node", null);
        rootFolder.setGoid(Folder.ROOT_FOLDER_ID);
        return rootFolder;
    }

    private Role createRole(final String name, final Entity roleEntity) {
        final Role role = new Role();
        role.setName(name);
        role.setCachedSpecificEntity(roleEntity);
        return role;
    }

    private Policy createPolicy(final Goid goid, final String name) {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, name, "xml", false);
        policy.setGoid(goid);
        return policy;
    }

    private PublishedService createService(final Goid goid, final String name, final String routingUri) {
        final PublishedService service = new PublishedService();
        service.setName(name);
        service.setRoutingUri(routingUri);
        service.setGoid(goid);
        return service;
    }

    private Folder createFolderInRoot(final Goid goid, final String name) {
        final Folder folder = new Folder(name, createRootFolder());
        folder.setGoid(goid);
        return folder;
    }

    private PublishedServiceAlias createServiceAlias(final Goid goid, final PublishedService service, final Folder folder) {
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, folder);
        alias.setGoid(goid);
        return alias;
    }

    private PolicyAlias createPolicyAlias(final Goid goid, final Policy policy, final Folder folder) {
        final PolicyAlias policyAlias = new PolicyAlias(policy, folder);
        policyAlias.setGoid(goid);
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

    private class HasFolderOidStub extends EntityHeader implements HasFolderId {
        private Goid folderGoid;

        private HasFolderOidStub(final Goid folderGoid) {
            this.folderGoid = folderGoid;
        }

        @Override
        public Goid getFolderId() {
            return folderGoid;
        }

        @Override
        public void setFolderId(final Goid folderId) {
            this.folderGoid = folderId;
        }
    }
}
