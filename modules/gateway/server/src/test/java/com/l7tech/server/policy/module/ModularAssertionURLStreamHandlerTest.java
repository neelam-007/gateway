package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.GatewayURLStreamHandlerFactory;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.ServerAssertionRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ModularAssertionURLStreamHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class ModularAssertionURLStreamHandlerTest {

    private static final String MODULE_NAME = ModularAssertionURLStreamHandlerTest.class.getSimpleName() + ".aar";
    private static final String CUSTOM_ASSNMOD_PROTOCOL = "assnmodcustom"; // using a custom protocol to ensure this test does not interfere with any other.

    @Mock
    private ServerAssertionRegistry registry;
    @Mock
    private LicenseManager licenseManager;
    @Mock
    private ScannerCallbacks.ModularAssertion modularAssertionCallbacks;
    @Mock
    private ServerConfig serverConfig;

    @BeforeClass
    public static void installFactory() throws LifecycleException {
        GatewayURLStreamHandlerFactory.install();
    }

    @Before
    public void setup() throws Exception {
        ModularAssertionURLStreamHandler handler = new ModularAssertionURLStreamHandler(this.registry);
        GatewayURLStreamHandlerFactory.registerHandlerFactory(CUSTOM_ASSNMOD_PROTOCOL, protocol -> handler);

        File file = new File(this.getClass().getClassLoader().getResource("com/l7tech/server/policy/module/" + MODULE_NAME).toURI());
        byte[] bytes = FileUtils.readFileToByteArray(file);

        ModularAssertionModule module = new TestModularAssertionsScanner(new ModularAssertionModulesConfig(this.serverConfig, this.licenseManager), this.modularAssertionCallbacks).onModuleLoad(new ModuleData() {
            @Override
            public @NotNull File getFile() {
                return file;
            }

            @Override
            public @NotNull String getDigest() {
                return DigestUtils.md5Hex(bytes);
            }

            @Override
            public long getLastModified() {
                return System.currentTimeMillis();
            }

            @Override
            public String getName() {
                return MODULE_NAME;
            }
        }).getLoadedModule();

        when(this.registry.getModuleByName(eq(MODULE_NAME))).thenReturn(module);
    }

    @Test
    public void testLoadFileFromAssertion() throws Exception {
        testURLLoading(new URL(CUSTOM_ASSNMOD_PROTOCOL + ":" + MODULE_NAME + "!AAR-INF/assertion.index"), "com/l7tech/external/assertions/modulartest1/ModularTest1Assertion.class");
    }

    @Test
    public void testLoadFileFromAssertionNestedJar() throws Exception {
        testURLLoading(new URL(CUSTOM_ASSNMOD_PROTOCOL + ":" + MODULE_NAME + "!AAR-INF/lib/nested.jar!TestFile.properties"), "test");
    }

    @Test(expected = IOException.class)
    public void testTryLoadingMissingFile() throws Exception {
        testURLLoading(new URL(CUSTOM_ASSNMOD_PROTOCOL + ":" + MODULE_NAME + "!AAR-INF/filethatdoesnotexist.properties"), null);
    }

    @Test(expected = IOException.class)
    public void testTryLoadingMissingNestedJar() throws Exception {
        testURLLoading(new URL(CUSTOM_ASSNMOD_PROTOCOL + ":" + MODULE_NAME + "!AAR-INF/lib/jarthatdoesnotexist.jar!file.properties"), null);
    }

    @Test(expected = IOException.class)
    public void testTryLoadingMissingFileExistingNestedJar() throws Exception {
        testURLLoading(new URL(CUSTOM_ASSNMOD_PROTOCOL + ":" + MODULE_NAME + "!AAR-INF/lib/nested.jar!missingfile.properties"), null);
    }

    private static void testURLLoading(URL url, String expectedFileContent) throws IOException {
        assertNotNull(url);

        URLConnection connection = url.openConnection();
        assertNotNull(connection);

        InputStream stream = connection.getInputStream();
        assertNotNull(stream);

        byte[] bytes = toByteArray(stream);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = new String(bytes);
        assertEquals(expectedFileContent, content.trim());
    }

    private static class TestModularAssertionsScanner extends ModularAssertionsScanner {

        private TestModularAssertionsScanner(@NotNull ModularAssertionModulesConfig modulesConfig, @NotNull ScannerCallbacks.ModularAssertion callbacks) {
            super(modulesConfig, callbacks);
        }

        @Override
        public @NotNull ModuleLoadStatus<ModularAssertionModule> onModuleLoad(ModuleData moduleData) throws ModuleException {
            return super.onModuleLoad(moduleData);
        }
    }
}
