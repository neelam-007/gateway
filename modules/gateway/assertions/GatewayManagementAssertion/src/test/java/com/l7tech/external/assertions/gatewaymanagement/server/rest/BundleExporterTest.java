package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityBundleExporter;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author alee, 1/5/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class BundleExporterTest {
    private static final String ENCODED_PASSPHRASE = HexUtils.encodeBase64("customPassphrase".getBytes());
    private static final String CONFIG_PATH = "com.l7tech.config.path";
    @Mock
    private EntityBundleExporter entityBundleExporter;
    @Mock
    private BundleTransformer bundleTransformer;
    @Mock
    private DependencyTransformer dependencyTransformer;
    private BundleExporter exporter;
    private Properties properties;
    private EntityBundle entityBundle;
    private Bundle bundle;
    private URL conf = BundleExporterTest.class.getResource("conf");

    @Before
    public void setup() {
        SyspropUtil.setProperty(CONFIG_PATH, conf.getPath());
        exporter = new BundleExporter(entityBundleExporter, bundleTransformer, dependencyTransformer);
        properties = new Properties();
        entityBundle = new EntityBundle(new ArrayList<EntityContainer>(),
                new ArrayList<EntityMappingInstructions>(),
                new ArrayList<DependencySearchResults>());
        bundle = ManagedObjectFactory.createBundle();
    }

    @After
    public void teardown() {
        SyspropUtil.clearProperty(CONFIG_PATH);
    }

    @Test
    public void exportBundleEncryptPasswords() throws Exception {
        when(entityBundleExporter.exportBundle(properties)).thenReturn(
                entityBundle);
        when(bundleTransformer.convertToMO(eq(entityBundle), any(SecretsEncryptor.class))).thenReturn(bundle);

        assertEquals(bundle, exporter.exportBundle(properties, false, true, null));
        verify(bundleTransformer).convertToMO(eq(entityBundle), any(SecretsEncryptor.class));
    }

    @Test(expected = FileNotFoundException.class)
    public void exportBundleEncryptPasswordsOmpDatNotFound() throws Exception {
        SyspropUtil.setProperty(CONFIG_PATH, "doesnotexist");
        exporter.exportBundle(properties, false, true, null);
    }

    @Test
    public void exportBundleEncryptPasswordsWithPassphrase() throws Exception {
        when(entityBundleExporter.exportBundle(properties)).thenReturn(
                entityBundle);
        when(bundleTransformer.convertToMO(eq(entityBundle), any(SecretsEncryptor.class))).thenReturn(bundle);

        assertEquals(bundle, exporter.exportBundle(properties, false, true, ENCODED_PASSPHRASE));
        verify(bundleTransformer).convertToMO(eq(entityBundle), any(SecretsEncryptor.class));
    }

    @Test
    public void exportBundleDoNotEncryptPasswords() throws Exception {
        when(entityBundleExporter.exportBundle(properties)).thenReturn(
                entityBundle);
        when(bundleTransformer.convertToMO(entityBundle, null)).thenReturn(bundle);

        assertEquals(bundle, exporter.exportBundle(properties, false, false, null));
        verify(bundleTransformer).convertToMO(entityBundle, null);
    }

    @Test
    public void exportBundleDoNotEncryptPasswordsIgnoresPassphrase() throws Exception {
        when(entityBundleExporter.exportBundle(properties)).thenReturn(
                entityBundle);
        when(bundleTransformer.convertToMO(entityBundle, null)).thenReturn(bundle);

        assertEquals(bundle, exporter.exportBundle(properties, false, false, ENCODED_PASSPHRASE));
        verify(bundleTransformer).convertToMO(entityBundle, null);
    }
}
