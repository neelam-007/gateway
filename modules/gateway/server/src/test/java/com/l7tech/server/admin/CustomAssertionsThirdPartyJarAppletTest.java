package com.l7tech.server.admin;

import com.l7tech.server.audit.Auditor;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.custom.CustomAssertionsRegistrarImpl;
import com.l7tech.util.Config;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionsThirdPartyJarAppletTest {

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
    private static final String MODULE_TEMP_DIR_NAME = "l7tech-CustomAssertionsThirdPartyJarAppletTest";

    private static String modulesDirPath;
    private static String modulesTmpDirPath;

    @Mock
    private ServerAssertionRegistry serverAssertionRegistryMock;

    @Mock
    private Config configMock;

    @Mock
    private ExtensionInterfaceManager extensionInterfaceManagerMock;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private HttpServletRequest hreq;

    @Mock
    private HttpServletResponse hresp;

    @Mock
    private ServletContext servletContext;

    @Mock
    private WebApplicationContext webApplicationContext;

    @Mock
    private ServletOutputStream servletOutputStream;

    @Mock
    private Auditor auditor;

    @Mock
    private Object dummyBean;

    private CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl;

    private ManagerAppletFilter managerAppletFilter;

    @BeforeClass
    public static void init() {
        // Create module temp directory.
        // If directory already exists, delete it, and create a new one.
        // This directory cannot be deleted on shutdown of this unit test (ie. @AfterClass) because the third-party
        // jars were still being used by the current JVM.
        //
        String tmpDirPath = SyspropUtil.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirPath);
        File moduleTmpDir = new File(tmpDir, MODULE_TEMP_DIR_NAME);
        if (moduleTmpDir.exists()) {
            FileUtils.deleteDir(moduleTmpDir);
        }

        moduleTmpDir.mkdir();
        modulesTmpDirPath = moduleTmpDir.getPath();

        // Get the resource directory of this unit test. This is where Custom Assertions will be loaded from during
        // startup.
        //
        URL modulesDirUrl = CustomAssertionsThirdPartyJarAppletTest.class.getClassLoader().getResource(CUSTOM_ASSERTION_TEST_JAR_PATH);
        modulesDirPath = modulesDirUrl.getPath();
        modulesDirPath = modulesDirPath.substring(0, modulesDirPath.lastIndexOf("/"));
    }

    @Before
    public void setup() throws Exception {
        when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        when(configMock.getProperty("custom.assertions.modules")).thenReturn(modulesDirPath);
        when(configMock.getProperty("custom.assertions.temp")).thenReturn(modulesTmpDirPath);

        when(servletContext.getAttribute(anyString())).thenReturn(webApplicationContext);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(hreq.getContextPath()).thenReturn("");
        when(hresp.getOutputStream()).thenReturn(servletOutputStream);

        customAssertionsRegistrarImpl =
            new CustomAssertionsRegistrarImpl(serverAssertionRegistryMock, extensionInterfaceManagerMock);
        customAssertionsRegistrarImpl.setServerConfig(configMock);
        // Load Custom Assertions
        customAssertionsRegistrarImpl.afterPropertiesSet();

        when(webApplicationContext.getBean(Matchers.anyString(), Matchers.<Class<?>>any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (invocationOnMock.getArguments()[0].equals("customAssertionRegistrar")) {
                    return customAssertionsRegistrarImpl;
                } else {
                    Class clazz = (Class) invocationOnMock.getArguments()[1];
                    return mock(clazz);
                }
            }
        });

        managerAppletFilter = new ManagerAppletFilter();
        managerAppletFilter.init(filterConfig);
    }

    @Test
    public void testGetResourceInCustomAssertionJarInWhiteList() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/test.icon.png");
        boolean handled = managerAppletFilter.handleCustomAssertionResourceRequest(hreq, hresp);
        assertTrue(handled);
    }

    @Test
    public void testGetResourceInCustomAssertionJarNotInWhiteList() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/test.icon.NotInWhiteList.png");
        boolean handled = managerAppletFilter.handleCustomAssertionResourceRequest(hreq, hresp);
        assertFalse(handled);
    }

    @Test
    public void testGetResourceInThirdPartyJarInWhiteList() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/salesforce-partner-v26.wsdl");
        boolean handled = managerAppletFilter.handleCustomAssertionResourceRequest(hreq, hresp);
        assertTrue(handled);
    }

    @Test
    public void testGetResourceInThirdPartyJarNotInWhiteList() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/salesforce-partner-v26-NotInWhiteList.wsdl");
        boolean handled = managerAppletFilter.handleCustomAssertionResourceRequest(hreq, hresp);
        assertFalse(handled);
    }

    @Test
    public void testGetResourceNotExist() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/Resource_File_Does_Not_Exist.png");
        boolean handled = managerAppletFilter.handleCustomAssertionResourceRequest(hreq, hresp);
        assertFalse(handled);
    }

    @Test
    public void testGetClassInThirdPartyJarInWhiteList() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/com/salesforce/jaxws/SforceService.class");
        boolean handled = managerAppletFilter.handleCustomAssertionClassRequest(hreq, hresp, auditor);
        assertTrue(handled);
    }

    @Test
    public void testGetClassInThirdPartyJarNotInWhiteList() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/com/l7tech/custom/salesforce/assertion/partner/SforceService.class");
        boolean handled = managerAppletFilter.handleCustomAssertionClassRequest(hreq, hresp, auditor);
        assertFalse(handled);
    }

    @Test
    public void testGetClassInThirdPartyJarNotExist() throws IOException {
        when(hreq.getRequestURI()).thenReturn("/ssg/webadmin/applet/com/salesforce/jaxws/Class_File_Does_Not_Exist.class");
        boolean handled = managerAppletFilter.handleCustomAssertionClassRequest(hreq, hresp, auditor);
        assertFalse(handled);
    }
}