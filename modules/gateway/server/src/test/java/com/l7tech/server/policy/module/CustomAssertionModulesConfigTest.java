package com.l7tech.server.policy.module;

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
        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        Assert.assertEquals(
                "should be \"custom.assertions.file\"",
                modulesConfigSpy.getCustomAssertionPropertyFileName(),
                "custom_assertions.properties"
        );

        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn("blah-blah");
        Assert.assertEquals(
                "should be \"blah-blah\"",
                modulesConfigSpy.getCustomAssertionPropertyFileName(),
                "blah-blah"
        );
    }

    /**
     * {@link com.l7tech.server.policy.module.CustomAssertionModulesConfig#isScanningEnabled() isScanningEnabled}
     * depends on both <i>custom.assertions.file</i> and <i>custom.assertions.rescan.enabled</i> properties.<br/>
     * <i>custom.assertions.file</i> <b>must</b> return a non-<code>null</code> value
     * and <i>custom.assertions.rescan.enabled</i> must return <code>true</code>.
     */
    @Test
    public void testIsScanningEnabled() throws Exception {
        Assert.assertFalse("nothing enabled should be false", modulesConfigSpy.isScanningEnabled());

        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        Assert.assertFalse("custom.assertions.rescan.enabled is not set so false", modulesConfigSpy.isScanningEnabled());

        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn(null);
        Mockito.when(configMock.getBooleanProperty(Mockito.eq("custom.assertions.rescan.enabled"), Mockito.anyBoolean())).thenReturn(true);
        Assert.assertFalse("custom.assertions.file is not set, so false", modulesConfigSpy.isScanningEnabled());

        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        Mockito.when(configMock.getBooleanProperty(Mockito.eq("custom.assertions.rescan.enabled"), Mockito.anyBoolean())).thenReturn(false);
        Assert.assertFalse("custom.assertions.rescan.enabled is false so false", modulesConfigSpy.isScanningEnabled());

        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        Mockito.when(configMock.getBooleanProperty(Mockito.eq("custom.assertions.rescan.enabled"), Mockito.anyBoolean())).thenReturn(true);
        Assert.assertTrue("both are properly set, so true", modulesConfigSpy.isScanningEnabled());
    }

    @Test
    public void testGetModuleDir() throws Exception {
        Assert.assertNull("\"custom.assertions.modules\" not set, so null", modulesConfigSpy.getModuleDir());

        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn("/some/test/folder/path");
        Assert.assertNotNull("\"custom.assertions.modules\" is set, so non-null", modulesConfigSpy.getModuleDir());

        final File modDir = modulesConfigSpy.getModuleDir();
        Assert.assertEquals("/some/test/folder/path".replace('/', File.separatorChar), modDir.getPath());
    }

    @Test
    public void testGetModulesExt() throws Exception {
        Assert.assertEquals(modulesConfigSpy.getModulesExt(), Arrays.asList(".jar"));
    }

    @Test
    public void testGetModuleWorkDirectory() throws Exception {
        Assert.assertNull("\"custom.assertions.temp\" not set, so null", modulesConfigSpy.getModuleWorkDirectory());

        Mockito.when(configMock.getProperty("custom.assertions.temp")).thenReturn("/some/test/folder/path");
        Assert.assertEquals("\"custom.assertions.temp\" is set, so non-null", modulesConfigSpy.getModuleWorkDirectory(), "/some/test/folder/path");
    }

    @Test
    public void testGetRescanPeriodMillis() throws Exception {
        Mockito.when(configMock.getLongProperty(Mockito.eq("custom.assertions.rescan.millis"), Mockito.anyLong())).thenReturn(101L);
        Assert.assertEquals(modulesConfigSpy.getRescanPeriodMillis(), 101L);
    }

    @Test
    public void testIsSupportedLibrary() throws Exception {
        Assert.assertTrue("jar is supported", modulesConfigSpy.isSupportedLibrary(".jar"));
        Assert.assertTrue("zip is supported", modulesConfigSpy.isSupportedLibrary(".zip"));
        Assert.assertFalse("anything other then jar or zip is not supported", modulesConfigSpy.isSupportedLibrary(".war"));
        Assert.assertFalse("anything other then jar or zip is not supported", modulesConfigSpy.isSupportedLibrary(".aar"));
    }
}
