package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.HexUtils;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
    private BundleExporter bundleExporter;
    private BundleResource resource;
    private Bundle bundle;
    private Item<Bundle> itemBundle;
    private List<String> folderIds;
    private List<String> serviceIds;
    private List<String> policyIds;

    @Before
    public void setup() throws Exception {
        resource = new BundleResource(null, bundleExporter, transformer, null, rbacAccessService, uriInfo, containerRequest);
        bundle = ManagedObjectFactory.createBundle();
        itemBundle = new ItemBuilder<Bundle>("Bundle", "BUNDLE").build();
        folderIds = new ArrayList<>();
        serviceIds = new ArrayList<>();
        policyIds = new ArrayList<>();
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedStringMap());
        when(uriInfo.getRequestUri()).thenReturn(new URI(BUNDLE_URI));
    }

    @Test
    public void exportEncryptPasswords() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, folderIds, serviceIds, policyIds, true, false, true, true, null);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "true");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test
    public void exportEncryptPasswordsWithPassphrase() throws Exception {
        final String encodedPassphrase = HexUtils.encodeBase64("customPassphrase".getBytes());
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, folderIds, serviceIds, policyIds, true, false, true, false, encodedPassphrase);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "true");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, encodedPassphrase, new EntityHeader[]{});
    }

    @Test
    public void exportDoNotEncryptPasswords() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, folderIds, serviceIds, policyIds, true, false, false, false, null);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "true");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "false");
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, new EntityHeader[]{});
    }

    @Test
    public void exportDoNotEncryptPasswordsIgnoresPassphrase() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, folderIds, serviceIds, policyIds, true, false, false, false, "should be ignored");
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "true");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "false");
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, new EntityHeader[]{});
    }

    @Test
    public void exportEncryptPasswordsWithClusterPassphraseIgnoresCustomPassphrase() throws Exception {
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString())).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportBundle(NEW_OR_EXISTING, false, folderIds, serviceIds, policyIds, true, false, true, true, "should be ignored");
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "true");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, new EntityHeader[]{});
    }

    @Test(expected = InvalidArgumentException.class)
    public void exportEncryptPasswordsWithoutClusterOrCustomPassphrase() throws Exception {
        try {
            resource.exportBundle(NEW_OR_EXISTING, false, folderIds, serviceIds, policyIds, true, false, true, false, null);
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
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, true, true, null);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
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
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, true, false, encodedPassphrase);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, encodedPassphrase, header);
    }

    @Test
    public void exportFolderServiceOrPolicyDoNotEncryptPasswords() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, false, false, null);
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "false");
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, header);
    }

    @Test
    public void exportFolderServiceOrPolicyDoNotEncryptPasswordsIgnoresPassphrase() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, false, false, "should be ignored");
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "false");
        verify(bundleExporter).exportBundle(expectedProperties, false, false, null, header);
    }

    @Test
    public void exportFolderServiceOrPolicyEncryptPasswordsWithClusterPassphraseIgnoresCustomPassphrase() throws Exception {
        final Goid id = new Goid(0, 1);
        final EntityHeader header = new EntityHeader(id, EntityType.POLICY, null, null);
        when(bundleExporter.exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), eq(header))).thenReturn(bundle);
        when(transformer.convertToItem(bundle)).thenReturn(itemBundle);
        final Item<Bundle> resultBundle = resource.exportFolderServiceOrPolicyBundle("policy", id, NEW_OR_EXISTING, "id", false, false, false, true, true, "should be ignored");
        assertEquals(itemBundle, resultBundle);

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty(BundleExporter.IncludeRequestFolderOption, "false");
        expectedProperties.setProperty(BundleExporter.DefaultMappingActionOption, NEW_OR_EXISTING);
        expectedProperties.setProperty(BundleExporter.DefaultMapByOption, "id");
        expectedProperties.setProperty(BundleExporter.EncryptSecrets, "true");
        verify(bundleExporter).exportBundle(expectedProperties, false, true, null, header);
    }

    @Test(expected = InvalidArgumentException.class)
    public void exportFolderServiceOrPolicyEncryptPasswordsWithoutClusterOrCustomPassphrase() throws Exception {
        try {
            resource.exportFolderServiceOrPolicyBundle("policy", new Goid(0, 1), NEW_OR_EXISTING, "id", false, false, false, true, false, null);
            fail("Expected InvalidArgumentException");
        } catch (final InvalidArgumentException e) {
            verify(bundleExporter, never()).exportBundle(any(Properties.class), anyBoolean(), anyBoolean(), anyString(), any(EntityHeader.class));
            assertEquals("Passphrase is required for encryption", e.getMessage());
            throw e;
        }
    }
}
