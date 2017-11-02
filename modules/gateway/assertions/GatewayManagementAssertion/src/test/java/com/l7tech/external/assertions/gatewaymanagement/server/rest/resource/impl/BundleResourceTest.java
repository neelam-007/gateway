package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleImporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.MappingBuilder;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.HexUtils;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * @author alee, 1/5/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class BundleResourceTest {
    private static final String BUNDLE_URI = "https://localhost:8443/restman/1.0/bundle";
    private static final String NEW_OR_EXISTING = "NewOrExisting";
    @Mock
    private RbacAccessService rbacAccessService;
    @Mock
    private UriInfo uriInfo;
    @Mock
    private BundleTransformer transformer;
    @Mock
    private ContainerRequest containerRequest;
    @Mock
    private BundleImporter bundleImporter;
    @Mock
    private BundleExporter bundleExporter;
    private BundleResource resource;
    private Bundle bundle;
    private Item<Bundle> itemBundle;
    private List<String> folderIds;
    private List<String> serviceIds;
    private List<String> policyIds;

    @Before
    public void setup() throws Exception {
        resource = new BundleResource(bundleImporter, bundleExporter, transformer, null, rbacAccessService, uriInfo, containerRequest);
        bundle = ManagedObjectFactory.createBundle();
        itemBundle = new ItemBuilder<Bundle>("Bundle", "BUNDLE").build();
        folderIds = new ArrayList<>();
        serviceIds = new ArrayList<>();
        policyIds = new ArrayList<>();
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedStringMap());
        when(uriInfo.getRequestUri()).thenReturn(new URI(BUNDLE_URI));
    }

    private Properties defaultExpectedProperties(){
        Properties expectedProperties = new Properties();
        expectedProperties.put(BundleExporter.IgnoreDependenciesOption, Collections.emptyList());
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "true");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "false");
        expectedProperties.setProperty(BundleExporter.IncludeSolutionKitsOption, "false");
        expectedProperties.setProperty(BundleExporter.EncassAsPolicyDependencyOption, "false");
        expectedProperties.setProperty(BundleExporter.IncludeOnlyServicePolicyOption, "false");
        expectedProperties.setProperty(BundleExporter.IncludeOnlyDependenciesOption, "false");
        expectedProperties.setProperty(BundleExporter.IncludeGatewayConfigurationOption, "false");
        return expectedProperties;
    }

    @Test
    public void exportEncryptPasswords() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, false, true, true, null, false, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test
    public void exportEncryptPasswordsWithPassphrase() throws Exception {
        final String encodedPassphrase = HexUtils.encodeBase64("customPassphrase".getBytes());
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, false, true, false, encodedPassphrase, false, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, encodedPassphrase, new EntityHeader[]{});
    }

    @Test
    public void exportDoNotEncryptPasswords() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, false, false, false, null, false, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, new EntityHeader[]{});
    }

    @Test
    public void exportDoNotEncryptPasswordsIgnoresPassphrase() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, false, false, false, "should be ignored", false, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, new EntityHeader[]{});
    }

    @Test
    public void exportEncryptPasswordsWithClusterPassphraseIgnoresCustomPassphrase() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, false, true, true, "should be ignored", false, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test(expected = InvalidArgumentException.class)
    public void exportEncryptPasswordsWithoutClusterOrCustomPassphrase() throws Exception {
        try {
            final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                    Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                    true, false, false, true, false, null, false, false, false, false);
            fail("Expected InvalidArgumentException");
        } catch (final InvalidArgumentException e) {
            verify(bundleExporter, never()).exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString());
            assertEquals("Passphrase is required for encryption", e.getMessage());
            throw e;
        }
    }

    @Test
    public void exportFolderServiceOrPolicyEncryptPasswords() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, true, true, null, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, header);
    }

    @Test
    public void exportFolderServiceOrPolicyEncryptPasswordsWithPassphrase() throws Exception {
        final String encodedPassphrase = HexUtils.encodeBase64("customPassphrase".getBytes());
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, true, false, encodedPassphrase, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, encodedPassphrase, header);
    }

    @Test
    public void exportFolderServiceOrPolicyDoNotEncryptPasswords() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, false, false, null, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, header);
    }

    @Test
    public void exportFolderServiceOrPolicyDoNotEncryptPasswordsIgnoresPassphrase() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, false, false, "should be ignored", false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, header);
    }

    @Test
    public void exportFolderServiceOrPolicyEncryptPasswordsWithClusterPassphraseIgnoresCustomPassphrase() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, true, true, "should be ignored", false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, header);
    }

    @Test(expected = InvalidArgumentException.class)
    public void exportFolderServiceOrPolicyEncryptPasswordsWithoutClusterOrCustomPassphrase() throws Exception {
        try {
            resource.exportFolderServiceOrPolicyBundle("policy", new Goid(0, 1), NEW_OR_EXISTING, "id", false, false, false, true, false, null, false, false);
            fail("Expected InvalidArgumentException");
        } catch (final InvalidArgumentException e) {
            verify(bundleExporter, never()).exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), any(EntityHeader.class));
            assertEquals("Passphrase is required for encryption", e.getMessage());
            throw e;
        }
    }

    @Test
    public void exportIncludeSolutionKits() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, true, true, true, "should be ignored", false, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        expectedProperties.setProperty(BundleExporter.IncludeSolutionKitsOption, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test
    public void exportEncassAsPolicyDependency() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, true, true, true, "should be ignored", true, false, false, false);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        expectedProperties.setProperty(BundleExporter.IncludeSolutionKitsOption, "true");
        expectedProperties.setProperty(BundleExporter.EncassAsPolicyDependencyOption, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test
    public void exportGatewayConfigurations() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), folderIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), policyIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), serviceIds, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(), Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList(),
                true, false, false, true, true, "should be ignored", false, false, false, true);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = defaultExpectedProperties();
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        expectedProperties.setProperty(BundleExporter.IncludeGatewayConfigurationOption, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test
    public void importBundle() throws Exception {
        final Mapping mapping1 = createMapping("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a343");
        final Mapping mapping2 = createMapping("POLICY", Mapping.Action.NewOrUpdate, "899eca6846c453e9a8e23ec887d6a344");
        final List<Mapping> mappingList = Arrays.asList(new Mapping[]{mapping1, mapping2});

        final List<List<Mapping>> mappingsResult = Arrays.asList(new List[]{mappingList});
        when(bundleImporter.importBundles(any(BundleList.class), anyBoolean(), anyBoolean(), anyString(), anyString())).thenReturn(mappingsResult);

        final Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setMappings(mappingList);

        // Call the method, BundleResource.importBundle for one bundle import
        final Response response = resource.importBundle(false, true, "test(v1.0)", "blabla", bundle);
        final Object entity = response.getEntity();

        // Verify the response entity type is Item (one item per bundle)
        assertTrue(entity instanceof Item);

        final Item<Mappings> item = (Item<Mappings>) entity;

        // Verify the item from the response should have the same mapping list as the original mapping list.
        assertTrue(item.getContent().getMappings() == mappingList);
    }

    @Test
    public void importBundles() throws Exception {
        // Prepare one mapping list for bundle #1
        final Mapping mapping1_1 = createMapping("FOLDER", Mapping.Action.NewOrUpdate, "599eca6846c453e9a8e23ec887d6a341");
        final Mapping mapping1_2 = createMapping("SERVICE", Mapping.Action.NewOrUpdate, "699eca6846c453e9a8e23ec887d6a342");
        final List<Mapping> mappingList1 = Arrays.asList(new Mapping[]{mapping1_1, mapping1_2});

        // Prepare one mapping list for bundle #2
        final Mapping mapping2_1 = createMapping("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a343");
        final Mapping mapping2_2 = createMapping("POLICY", Mapping.Action.NewOrUpdate, "899eca6846c453e9a8e23ec887d6a344");
        final List<Mapping> mappingList2 = Arrays.asList(new Mapping[]{mapping2_1, mapping2_2});


        final List<List<Mapping>> mappingsResult = Arrays.asList(new List[]{mappingList1, mappingList2});
        when(bundleImporter.importBundles(any(BundleList.class), anyBoolean(), anyBoolean(), anyString(), anyString())).thenReturn(mappingsResult);

        // Prepare a BundleList object using the above two mapping lists.
        final BundleList bundleList = ManagedObjectFactory.createBundleList();
        final Bundle bundle1 = ManagedObjectFactory.createBundle();
        final Bundle bundle2 = ManagedObjectFactory.createBundle();

        bundle1.setMappings(mappingList1);
        bundle2.setMappings(mappingList2);
        bundleList.setBundles(Arrays.asList(new Bundle[]{bundle1, bundle2}));

        // Call the method, BundleResource.importBundles for multi-bundle import
        final Response response = resource.importBundles(false, true, "test(v1.0)", "blabla", bundleList);
        final Object entity = response.getEntity();

        // Verify the response entity type is ItemsList (a list of Items for multiple bundles)
        assertTrue(entity instanceof ItemsList);

        final ItemsList<Mappings> itemsList = (ItemsList<Mappings>) entity;
        final List<Item<Mappings>> items = itemsList.getContent();

        // Verify the number of Items from the response is 2.
        assertTrue(items.size() == 2);

        // Verify each item from the response should have the same mapping list as the original mapping list.
        assertTrue(items.get(0).getContent().getMappings() == mappingList1);
        assertTrue(items.get(1).getContent().getMappings() == mappingList2);
    }

    @NotNull
    private Mapping createMapping(final String type, final Mapping.Action action, final String id) {
        return new MappingBuilder().withType(type).withAction(action).withSrcId(id).build();
    }
}
