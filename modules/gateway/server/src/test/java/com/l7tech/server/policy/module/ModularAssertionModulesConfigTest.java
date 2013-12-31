package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.ServerConfigStub;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class ModularAssertionModulesConfigTest {
    @Mock
    private LicenseManager licenseManager;

    private ServerConfig configMock = new ServerConfigStub(); //ServerConfig.getInstance();
    private ModularAssertionModulesConfig modulesConfigSpy;

    @Before
    public void setUp() throws Exception {
        modulesConfigSpy = new ModularAssertionModulesConfig(configMock, licenseManager);
    }

    @Test
    public void testIsFeatureEnabled() throws Exception {
        Mockito.when(licenseManager.isFeatureEnabled(GatewayFeatureSets.SERVICE_MODULELOADER)).thenReturn(false);
        Assert.assertFalse("modular assertions feature is disabled", modulesConfigSpy.isFeatureEnabled());

        Mockito.when(licenseManager.isFeatureEnabled(GatewayFeatureSets.SERVICE_MODULELOADER)).thenReturn(true);
        Assert.assertTrue("modular assertions feature is disabled", modulesConfigSpy.isFeatureEnabled());
    }

    @Test
    public void testIsScanningEnabled() throws Exception {
        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, ".jar .assertion .ass .assn .aar");
        Assert.assertTrue(modulesConfigSpy.isScanningEnabled());

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, ".jar");
        Assert.assertTrue(modulesConfigSpy.isScanningEnabled());

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, ".aar");
        Assert.assertTrue(modulesConfigSpy.isScanningEnabled());

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, "blah-blah");
        Assert.assertTrue(modulesConfigSpy.isScanningEnabled());

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, "-");
        Assert.assertFalse(modulesConfigSpy.isScanningEnabled());
    }

    @Test
    public void testGetModulesExt() throws Exception {
        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, ".jar .assertion .ass .assn .aar");
        Assert.assertEquals(modulesConfigSpy.getModulesExt(), Arrays.asList(".jar", ".assertion", ".ass", ".assn", ".aar"));

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, ".jar");
        Assert.assertEquals(modulesConfigSpy.getModulesExt(), Arrays.asList(".jar"));

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, ".aar");
        Assert.assertEquals(modulesConfigSpy.getModulesExt(), Arrays.asList(".aar"));

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, "blah-blah");
        Assert.assertEquals(modulesConfigSpy.getModulesExt(), Arrays.asList("blah-blah"));

        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, "-");
        Assert.assertEquals(modulesConfigSpy.getModulesExt(), Arrays.asList("-"));
    }

    @Test
    public void testGetRescanPeriodMillis() throws Exception {
        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_RESCAN_MILLIS, "1001");
        Assert.assertEquals(modulesConfigSpy.getRescanPeriodMillis(), 1001L);
    }

    @Test
    public void testGetModuleDir() throws Exception {
        configMock.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY, "./");
        final File modDir = modulesConfigSpy.getModuleDir();
        Assert.assertEquals(new File("./").getAbsoluteFile(), modDir.getAbsoluteFile());
    }
}
