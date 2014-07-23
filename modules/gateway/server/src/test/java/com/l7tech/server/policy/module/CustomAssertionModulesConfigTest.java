package com.l7tech.server.policy.module;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
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
public class CustomAssertionModulesConfigTest {
    @Mock
    private Config configMock;

    private CustomAssertionModulesConfig modulesConfigSpy;

    @Before
    public void setUp() throws Exception {
        modulesConfigSpy = new CustomAssertionModulesConfig(configMock);
    }

    @Test
    public void testIsFeatureEnabled() throws Exception {
        Assert.assertTrue("custom assertion feature is always enabled", modulesConfigSpy.isFeatureEnabled());
    }

    @Test
    public void testGetCustomAssertionPropertyFileName() throws Exception {
        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE)).thenReturn("custom_assertions.properties");
        Assert.assertEquals(
                "should be \"custom_assertions.properties\"",
                "custom_assertions.properties",
                modulesConfigSpy.getCustomAssertionPropertyFileName()
        );

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE)).thenReturn("blah-blah");
        Assert.assertEquals(
                "should be \"blah-blah\"",
                "blah-blah",
                modulesConfigSpy.getCustomAssertionPropertyFileName()
        );
    }

    /**
     * {@link com.l7tech.server.policy.module.CustomAssertionModulesConfig#isScanningEnabled() isScanningEnabled}
     * depends on both {@link ServerConfigParams#PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE}
     * and {@link ServerConfigParams#PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE} properties.<br/>
     * {@link ServerConfigParams#PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE} <b>must</b> return a non {@code null} value
     * and {@link ServerConfigParams#PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE} must return {@code true}.
     */
    @Test
    public void testIsScanningEnabled() throws Exception {
        Assert.assertFalse("nothing enabled should be false", modulesConfigSpy.isScanningEnabled());

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE)).thenReturn("custom_assertions.properties");
        Assert.assertEquals(
                "\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE + "\" is not set so default value is true",
                true,
                modulesConfigSpy.isScanningEnabled()
        );

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE)).thenReturn(null);
        Mockito.when(configMock.getBooleanProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE), Mockito.anyBoolean())).thenReturn(false);
        Assert.assertEquals(
                "\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE + "\" is not set, so false",
                false,
                modulesConfigSpy.isScanningEnabled()
        );

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE)).thenReturn("custom_assertions.properties");
        Mockito.when(configMock.getBooleanProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE), Mockito.anyBoolean())).thenReturn(true);
        Assert.assertEquals(
                "\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE + "\" is true, so false",
                false,
                modulesConfigSpy.isScanningEnabled()
        );

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE)).thenReturn("custom_assertions.properties");
        Mockito.when(configMock.getBooleanProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE), Mockito.anyBoolean())).thenReturn(false);
        Assert.assertEquals(
                "both are properly set, so true",
                true,
                modulesConfigSpy.isScanningEnabled()
        );
    }

    @Test
    public void testGetModuleDir() throws Exception {
        Assert.assertNull("\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY + "\" not set, so null", modulesConfigSpy.getModuleDir());

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY)).thenReturn("/some/test/folder/path");
        Assert.assertNotNull("\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY + "\" is set, so non-null", modulesConfigSpy.getModuleDir());

        final File modDir = modulesConfigSpy.getModuleDir();
        Assert.assertEquals("/some/test/folder/path".replace('/', File.separatorChar), modDir.getPath());
    }

    @Test
    public void testGetModulesExt() throws Exception {
        Assert.assertEquals(Arrays.asList(".jar"), modulesConfigSpy.getModulesExt());
    }

    @Test
    public void testGetModuleWorkDirectory() throws Exception {
        Assert.assertNull("\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY + "\" not set, so null", modulesConfigSpy.getModuleWorkDirectory());

        Mockito.when(configMock.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY)).thenReturn("/some/test/folder/path");
        Assert.assertEquals(
                "\"" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY + "\" is set, so non-null",
                "/some/test/folder/path",
                modulesConfigSpy.getModuleWorkDirectory()
        );
    }

    @Test
    public void testRescanEnabled() throws Exception {
        Mockito.when(configMock.getBooleanProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_RESCAN_ENABLE), Mockito.anyBoolean())).thenReturn(false);
        Assert.assertEquals(false, modulesConfigSpy.isHotSwapEnabled());

        Mockito.when(configMock.getBooleanProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_RESCAN_ENABLE), Mockito.anyBoolean())).thenReturn(true);
        Assert.assertEquals(true, modulesConfigSpy.isHotSwapEnabled());
    }

    @Test
    public void testGetRescanPeriodMillis() throws Exception {
        Mockito.when(configMock.getLongProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_RESCAN_MILLIS), Mockito.anyLong())).thenReturn(101L);
        Assert.assertEquals(101L, modulesConfigSpy.getRescanPeriodMillis());
    }

    @Test
    public void testIsSupportedLibrary() throws Exception {
        Assert.assertEquals(
                "jar is supported",
                true,
                modulesConfigSpy.isSupportedLibrary(".jar")
        );
        Assert.assertEquals(
                "zip is supported",
                true,
                modulesConfigSpy.isSupportedLibrary(".zip")
        );
        Assert.assertEquals(
                "anything other then jar or zip is not supported",
                false,
                modulesConfigSpy.isSupportedLibrary(".war")
        );
        Assert.assertEquals(
                "anything other then jar or zip is not supported",
                false,
                modulesConfigSpy.isSupportedLibrary(".aar")
        );
    }
}
