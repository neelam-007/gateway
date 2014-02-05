package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Config;
import com.l7tech.util.FileUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.net.URL;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionsThirdPartyJarTest {

    // The Custom Assertion Jar used by the unit test. The filename must start with "com.l7tech.". Otherwise, it will
    // not be loaded by the unit test. See idea_project.xml file for list of valid unit test resource names.
    //
    // Content of com.l7tech.customAssertionTest.jar:
    //  custom_assertions.properties
    //        Test.Assertion.ui.allowed.packages=com.salesforce.jaxws
    //        Test.Assertion.ui.allowed.resources=test.icon.png, salesforce-partner-v26.wsdl
    // com
    //        Contains test Custom Assertion classes.
    // lib
    //  |_ ThirdPartyJar1.jar
    //       |_ com/salesforce/jaxws/*.class                          [In WhiteList]
    //       |_ salesforce-partner-v26-NotInWhiteList.wsdl            [Not In WhiteList]
    //       |_ salesforce-partner-v26.wsdl                           [In WhiteList]
    //  |_ ThirdPartyJar2.jar
    //       |_com/l7tech/custom/salesforce/assertion/partner/*.class [Not In WhiteList]
    //  |_ test.icon.NotInWhiteList.png                               [Not In WhiteList]
    //  |_ test.icon.png                                              [In WhiteList]
    //
    private static final String CUSTOM_ASSERTION_TEST_JAR_PATH = "com/l7tech/server/policy/custom/com.l7tech.customAssertionTest.jar";

    // The module temp directory name. This is where third-party jars in Custom Assertion jar are extracted to during
    // startup.
    //
    private static final String MODULE_TEMP_DIR_NAME = "l7tech-CustomAssertionsThirdPartyJarTest-";

    private static File moduleTmpDir;
    private static String modulesDirPath;
    private static String modulesTmpDirPath;

    @Mock
    private ServerAssertionRegistry serverAssertionRegistryMock;

    @Mock
    private Config configMock;

    @Mock
    private ExtensionInterfaceManager extensionInterfaceManagerMock;

    @Mock
    private SecurePasswordManager securePasswordManagerMock;

    @Mock
    private CustomKeyValueStoreManager customKeyValueStoreManagerMock;

    private CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl;

    @BeforeClass
    public static void init() throws Exception {
        // Create module temp directory.
        // In Windows environment, this directory does not get deleted on shutdown of this unit test because the third-party
        // jars were still being used by the current JVM.
        //
        moduleTmpDir = FileUtils.createTempDirectory(MODULE_TEMP_DIR_NAME, null, null, true);
        modulesTmpDirPath = moduleTmpDir.getPath();

        // Get the resource directory of this unit test. This is where Custom Assertions will be loaded from during
        // startup.
        //
        URL modulesDirUrl = CustomAssertionsThirdPartyJarTest.class.getClassLoader().getResource(CUSTOM_ASSERTION_TEST_JAR_PATH);
        modulesDirPath = modulesDirUrl.getPath();
        modulesDirPath = modulesDirPath.substring(0, modulesDirPath.lastIndexOf("/"));
    }

    @Before
    public void setup() throws Exception {
        when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        when(configMock.getProperty("custom.assertions.modules")).thenReturn(modulesDirPath);
        when(configMock.getProperty("custom.assertions.temp")).thenReturn(modulesTmpDirPath);

        // enable custom assertion scanner
        when(configMock.getBooleanProperty(eq("custom.assertions.rescan.enabled"), anyBoolean())).thenReturn(true);
        when(configMock.getLongProperty(eq("custom.assertions.rescan.millis"), anyLong())).thenReturn(10000L);

        customAssertionsRegistrarImpl =
            new CustomAssertionsRegistrarImpl(serverAssertionRegistryMock);
        customAssertionsRegistrarImpl.setServerConfig(configMock);
        customAssertionsRegistrarImpl.setExtensionInterfaceManager(extensionInterfaceManagerMock);
        customAssertionsRegistrarImpl.setSecurePasswordManager(securePasswordManagerMock);
        customAssertionsRegistrarImpl.setCustomKeyValueStoreManager(customKeyValueStoreManagerMock);
        // Load Custom Assertions
        customAssertionsRegistrarImpl.afterPropertiesSet();
    }

    @Test
    public void testGetResourceInCustomAssertionJarInWhiteList() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("test.icon.png");
        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    public void testGetResourceInCustomAssertionJarNotInWhiteList() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("test.icon.NotInWhiteList.png");
        assertNull(result);
    }

    @Test
    public void testGetResourceInThirdPartyJarInWhiteList() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("salesforce-partner-v26.wsdl");
        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    public void testGetResourceInThirdPartyJarNotInWhiteList() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("salesforce-partner-v26-NotInWhiteList.wsdl");
        assertNull(result);
    }

    @Test
    public void testGetResourceNotExist() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("Resource_File_Does_Not_Exist.png");
        assertNull(result);
    }

    @Test
    public void testGetClassInThirdPartyJarInWhiteList() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("com/salesforce/jaxws/SforceService.class");
        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    public void testGetClassInThirdPartyJarNotInWhiteList() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("com/l7tech/custom/salesforce/assertion/partner/SforceService.class");
        assertNull(result);
    }

    @Test
    public void testGetClassInThirdPartyJarNotExist() {
        CustomAssertionsRegistrar.AssertionResourceData result =
            customAssertionsRegistrarImpl.getAssertionResourceData("com/salesforce/jaxws/Class_File_Does_Not_Exist.class");
        assertNull(result);
    }
}