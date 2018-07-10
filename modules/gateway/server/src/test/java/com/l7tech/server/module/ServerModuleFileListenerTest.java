package com.l7tech.server.module;

import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.*;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.module.*;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.RunsOnWindows;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test ServerModuleFileListener
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFileListenerTest extends ServerModuleFileTestBase {

    // Modular Assertions deploy directory
    private static final String MODULAR_ASSERTIONS_MODULES_DIR_NAME = "l7tech-modular";
    // Custom Assertions deploy directory
    private static final String CUSTOM_ASSERTIONS_MODULES_DIR_NAME = "l7tech-custom";
    private static final String CUSTOM_ASSERTIONS_MODULES_WORK_TEMP_DIR_NAME = "l7tech-custom-Temp";
    // Staging directory
    private static final String STAGING_MODULES_DIR_NAME = "l7tech-staging";

    // Sample Custom assertion jar(s)
    private static final String NON_DYNAMIC_MODULES_EMPTY_DIR        = "com/l7tech/server/policy/module/custom/non_dynamic/dummy.png";
    private static final String DYNAMIC_MODULES_EMPTY_DIR            = "com/l7tech/server/policy/module/custom/dynamic/dummy.png";
    private static final String DYNAMIC_COPY_MODULES_EMPTY_DIR       = "com/l7tech/server/policy/module/custom/dynamic/copy/dummy.png";
    private static final String FAILED_ON_LOAD_MODULES_EMPTY_DIR     = "com/l7tech/server/policy/module/custom/failed/onLoad/dummy.png";
    private static final String FAILED_ON_UNLOAD_MODULES_EMPTY_DIR   = "com/l7tech/server/policy/module/custom/failed/onUnload/dummy.png";
    private static final String DUAL_MODULES_EMPTY_DIR               = "com/l7tech/server/policy/module/custom/dual/dummy.png";
    private static final String BROKEN_DESCRIPTOR_MODULES_EMPTY_DIR  = "com/l7tech/server/policy/module/custom/failed/descriptor/dummy.png";
    // Sample Modular assertion aar(s)
    private static final String MODULES_ROOT_EMPTY_DIR               = "com/l7tech/server/policy/module/modular/dummy.png";
    private static final String MODULES_COPY_ROOT_EMPTY_DIR          = "com/l7tech/server/policy/module/modular/copy/dummy.png";
    // folder vars
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String nonDynamicModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String dynamicModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String dynamicCopyModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String failedOnLoadModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String failedOnUnloadModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String dualModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String brokenDescriptorModulesEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String modulesRootEmptyDir;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"}) private static String modulesCopyRootEmptyDir;


    private static final String DISABLED_MODULES_SUFFIX = ".disabled";

    @Mock
    private ServerModuleFileManager serverModuleFileManager;

    private ServerModuleFileListener modulesListener;
    private CustomAssertionsScanner customAssertionsScanner;
    private ModularAssertionsScanner modularAssertionsScanner;

    // server module files initial repository
    private Map<Goid, ServerModuleFile> moduleFiles;
    // modular and custom assertions deploy folders as well as ServerModuleFile staging folder`
    private static File modularDeployFolder, customDeployFolder, customTempFolder, stagingFolder;

    // emulate this cluster node
    private String currentNodeId = "currentClusterNode";

    // untrusted signer and signer cert DN's
    private static TrustedSignerCertsManager untrustedCerts;
    private static final String[] untrustedSignerCertDns = new String[] {"cn=untrusted.signer1.ca.com", "cn=untrusted.signer1.ca.com"};

    @BeforeClass
    public static void setUpOnce() throws Exception {
        beforeClass();

        // On Windows platform jar files are locked by the JVM, therefore they cannot be cleaned up on exit.
        // On start, we will loop through all previously created temporary folders and delete them,
        // which means that at a worst case scenario we will only end up with files from a single run.
        cleanUpTemporaryFilesFromPreviousRuns(MODULAR_ASSERTIONS_MODULES_DIR_NAME, CUSTOM_ASSERTIONS_MODULES_DIR_NAME, CUSTOM_ASSERTIONS_MODULES_WORK_TEMP_DIR_NAME, STAGING_MODULES_DIR_NAME);

        Assert.assertFalse("NON_DYNAMIC_MODULES_EMPTY_DIR exists", StringUtils.isBlank(nonDynamicModulesEmptyDir = extractFolder(NON_DYNAMIC_MODULES_EMPTY_DIR)));
        Assert.assertFalse("DYNAMIC_MODULES_EMPTY_DIR exists", StringUtils.isBlank(dynamicModulesEmptyDir = extractFolder(DYNAMIC_MODULES_EMPTY_DIR)));
        Assert.assertFalse("DYNAMIC_COPY_MODULES_EMPTY_DIR exists", StringUtils.isBlank(dynamicCopyModulesEmptyDir = extractFolder(DYNAMIC_COPY_MODULES_EMPTY_DIR)));
        Assert.assertFalse("FAILED_ON_LOAD_MODULES_EMPTY_DIR exists", StringUtils.isBlank(failedOnLoadModulesEmptyDir = extractFolder(FAILED_ON_LOAD_MODULES_EMPTY_DIR)));
        Assert.assertFalse("FAILED_ON_UNLOAD_MODULES_EMPTY_DIR exists", StringUtils.isBlank(failedOnUnloadModulesEmptyDir = extractFolder(FAILED_ON_UNLOAD_MODULES_EMPTY_DIR)));
        Assert.assertFalse("DUAL_MODULES_EMPTY_DIR exists", StringUtils.isBlank(dualModulesEmptyDir = extractFolder(DUAL_MODULES_EMPTY_DIR)));
        Assert.assertFalse("BROKEN_DESCRIPTOR_MODULES_EMPTY_DIR exists", StringUtils.isBlank(brokenDescriptorModulesEmptyDir = extractFolder(BROKEN_DESCRIPTOR_MODULES_EMPTY_DIR)));
        Assert.assertFalse("MODULES_ROOT_EMPTY_DIR exists", StringUtils.isBlank(modulesRootEmptyDir = extractFolder(MODULES_ROOT_EMPTY_DIR)));
        Assert.assertFalse("MODULES_COPY_ROOT_EMPTY_DIR exists", StringUtils.isBlank(modulesCopyRootEmptyDir = extractFolder(MODULES_COPY_ROOT_EMPTY_DIR)));

        // combination of already trusted and untrusted DN's
        // everything signed with this signer will not be trusted
        untrustedCerts = SignatureTestUtils.createSignerManager(
                ArrayUtils.concat(SIGNER_CERT_DNS, untrustedSignerCertDns)
        );
    }

    @AfterClass
    public static void cleanUpOnce() throws Exception {
        afterClass();

        for (final File tmpDir : tmpFiles.values()) {
            FileUtils.deleteDir(tmpDir);
        }
        tmpFiles.clear();
    }

    @Before
    public void setUp() throws Exception {
        // generate dates array
        setupDates();

        // create a temporary modules folder for this test
        Assert.assertNotNull(modularDeployFolder = getTempFolder(MODULAR_ASSERTIONS_MODULES_DIR_NAME));
        Assert.assertNotNull(customDeployFolder = getTempFolder(CUSTOM_ASSERTIONS_MODULES_DIR_NAME));
        Assert.assertNotNull(customTempFolder = getTempFolder(CUSTOM_ASSERTIONS_MODULES_WORK_TEMP_DIR_NAME));
        Assert.assertNotNull(stagingFolder = getTempFolder(STAGING_MODULES_DIR_NAME));

        // set the modules folder property to the temporary folders
        final Config config = Mockito.mock(Config.class);
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY)).thenReturn(modularDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY), Mockito.anyString())).thenReturn(modularDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY)).thenReturn(customDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY), Mockito.anyString())).thenReturn(customDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY)).thenReturn(customTempFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY), Mockito.anyString())).thenReturn(customTempFolder.getCanonicalPath());
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_STAGING_FOLDER)).thenReturn(stagingFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_SERVER_MODULE_FILE_STAGING_FOLDER), Mockito.anyString())).thenReturn(stagingFolder.getCanonicalPath());

        // mock Custom Assertions Scanner
        final ScannerCallbacks.CustomAssertion customAssertionCallbacks = Mockito.mock(ScannerCallbacks.CustomAssertion.class);
        mockAssertionRegisterCallbacks(customAssertionCallbacks);
        final CustomAssertionModulesConfig customModulesConfig = Mockito.spy(new CustomAssertionModulesConfig(config));
        Mockito.doReturn(true).when(customModulesConfig).isFeatureEnabled();
        Mockito.doReturn(true).when(customModulesConfig).isScanningEnabled();
        Mockito.doReturn(true).when(customModulesConfig).isHotSwapEnabled();
        Mockito.doReturn(DISABLED_MODULES_SUFFIX).when(customModulesConfig).getDisabledSuffix();
        Mockito.doReturn("custom_assertions.properties").when(customModulesConfig).getCustomAssertionPropertyFileName();
        final CustomAssertionsRegistrar customAssertionRegistrar = Mockito.mock(CustomAssertionsRegistrar.class);
        customAssertionsScanner = Mockito.spy(new CustomAssertionsScanner(customModulesConfig, customAssertionCallbacks));
        mockServerModuleFileLoader(customAssertionRegistrar, customAssertionsScanner);

        // mock Modular Assertions Scanner
        final ScannerCallbacks.ModularAssertion modularAssertionCallbacks = Mockito.mock(ScannerCallbacks.ModularAssertion.class);
        mockAssertionRegisterCallbacks(modularAssertionCallbacks);
        final ModularAssertionModulesConfig modulesConfig = Mockito.mock(ModularAssertionModulesConfig.class);
        Mockito.when(modulesConfig.isFeatureEnabled()).thenReturn(true);
        Mockito.when(modulesConfig.isScanningEnabled()).thenReturn(true);
        Mockito.when(modulesConfig.getModulesExt()).thenReturn(Arrays.asList(Pattern.compile("\\s+").split(".jar .assertion .ass .assn .aar")));
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modularDeployFolder);
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);
        Mockito.when(modulesConfig.getManifestHdrAssertionList()).thenReturn("ModularAssertion-List");
        Mockito.when(modulesConfig.getManifestHdrPrivateLibraries()).thenReturn("ModularAssertion-Private-Libraries");
        final ServerAssertionRegistry modularAssertionRegistrar = Mockito.mock(ServerAssertionRegistry.class);
        modularAssertionsScanner = Mockito.spy(new ModularAssertionsScanner(modulesConfig, modularAssertionCallbacks));
        mockServerModuleFileLoader(modularAssertionRegistrar, modularAssertionsScanner);

        Assert.assertThat("modules signer is created", TRUSTED_SIGNER_CERTS, Matchers.notNullValue());
        // create modules listener spy
        modulesListener = Mockito.spy(
                new ServerModuleFileListener(
                        serverModuleFileManager,
                        null,
                        config,
                        modularAssertionRegistrar,
                        customAssertionRegistrar,
                        TRUSTED_SIGNER_CERTS
                )
        );

        unlicensedModules.clear();
    }

    @After
    public void tearDown() throws Exception {
        // remove any temporary folders used
        if (modularDeployFolder != null) {
            FileUtils.deleteDir(modularDeployFolder);
            tmpFiles.remove(modularDeployFolder.getAbsolutePath());
        }
        if (customDeployFolder != null) {
            FileUtils.deleteDir(customDeployFolder);
            tmpFiles.remove(customDeployFolder.getAbsolutePath());
        }
        if (customTempFolder != null) {
            FileUtils.deleteDir(customTempFolder);
            tmpFiles.remove(customTempFolder.getAbsolutePath());
        }
        if (stagingFolder != null) {
            FileUtils.deleteDir(stagingFolder);
            tmpFiles.remove(stagingFolder.getAbsolutePath());
        }
    }

    /**
     * Helper method for mocking {@link com.l7tech.server.policy.module.ScannerCallbacks.CustomAssertion}.
     */
    private static void mockAssertionRegisterCallbacks(ScannerCallbacks.CustomAssertion customAssertionCallbacks) {
        final ServiceFinder serviceFinder = Mockito.mock(ServiceFinder.class);
        Mockito.when(customAssertionCallbacks.getServiceFinder()).thenReturn(serviceFinder);
    }

    /**
     * Helper method for mocking {@link com.l7tech.server.policy.module.ScannerCallbacks.ModularAssertion}.
     */
    private static void mockAssertionRegisterCallbacks(ScannerCallbacks.ModularAssertion modularAssertionCallbacks) {
        // app context mock
        final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        // an improvised holder for registered Assertions
        final Set<String> regAssertions = new HashSet<>();
        // mock registerAssertion to store the assertion class-name into regAssertions
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertEquals("there is only one parameter for registerAssertion", 1, invocation.getArguments().length);

                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is Class", param1 instanceof Class);
                final Class assClassObject = (Class) param1;
                Assert.assertTrue("Assertion Class is of type Assertion", Assertion.class.isAssignableFrom(assClassObject));

                regAssertions.add(assClassObject.getName());
                Assert.assertTrue("Make sure the class name exists after addition", regAssertions.contains(assClassObject.getName()));

                return null;
            }
        }).when(modularAssertionCallbacks).registerAssertion(Mockito.<Class<? extends Assertion>>any());
        // mock unregisterAssertion to remove registered assertion class from regAssertions
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertEquals("there is only one parameter for unregisterAssertion", 1, invocation.getArguments().length);

                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is Assertion", param1 instanceof Assertion);
                final Assertion assertion = (Assertion) param1;

                Assert.assertTrue(regAssertions.remove(assertion.getClass().getName()));
                Assert.assertFalse("Make sure the class name doesn't exists after removal", regAssertions.contains(assertion.getClass().getName()));

                return null;
            }
        }).when(modularAssertionCallbacks).unregisterAssertion(Mockito.<Assertion>any());
        // mock isAssertionRegistered to check against regAssertions, whether the assertion class-name has been registered
        Mockito.doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertEquals("there is only one parameter for isAssertionRegistered", 1, invocation.getArguments().length);

                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is String", param1 instanceof String);
                final String newAssertionClass = (String) param1;

                return regAssertions.contains(newAssertionClass);
            }
        }).when(modularAssertionCallbacks).isAssertionRegistered(Mockito.anyString());
        // mock getApplicationContext and return our mocked applicationContext object
        Mockito.doAnswer(new Answer<ApplicationContext>() {
            @Override
            public ApplicationContext answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertEquals("no parameters for getApplicationContext", 0, invocation.getArguments().length);
                return applicationContext;
            }
        }).when(modularAssertionCallbacks).getApplicationContext();
    }

    /**
     * Utility method for mocking {@link ServerModuleFileLoader} methods.
     */
    private static void mockServerModuleFileLoader(final ServerModuleFileLoader loader, final ModulesScanner scanner) throws Exception {
        Assert.assertNotNull(loader);
        Assert.assertNotNull(scanner);

        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there are two parameter for loadModule", 2, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is File", param1 instanceof File);
                        final File stagedFile = (File) param1;
                        Assert.assertNotNull(stagedFile);
                        Assert.assertTrue(stagedFile.exists());
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertTrue("Param2 is ServerModuleFile", param2 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param2;
                        Assert.assertNotNull(moduleEntity);

                        scanner.loadServerModuleFile(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
                        return null;
                    }
                }
        ).when(loader).loadModule(Mockito.<File>any(), Mockito.<ServerModuleFile>any());

        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there are two parameter for updateModule", 2, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is File", param1 instanceof File);
                        final File stagedFile = (File) param1;
                        Assert.assertNotNull(stagedFile);
                        //Assert.assertTrue(stagedFile.exists());
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertTrue("Param2 is ServerModuleFile", param2 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param2;
                        Assert.assertNotNull(moduleEntity);

                        scanner.updateServerModuleFile(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
                        return null;
                    }
                }
        ).when(loader).updateModule(Mockito.<File>any(), Mockito.<ServerModuleFile>any());

        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there are two parameter for unloadModule", 2, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is File", param1 instanceof File);
                        final File stagedFile = (File) param1;
                        Assert.assertNotNull(stagedFile);
                        //Assert.assertTrue(stagedFile.exists());
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertTrue("Param2 is ServerModuleFile", param2 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param2;
                        Assert.assertNotNull(moduleEntity);

                        scanner.unloadServerModuleFile(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
                        return null;
                    }
                }
        ).when(loader).unloadModule(Mockito.<File>any(), Mockito.<ServerModuleFile>any());

        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there are two parameter for unloadModule", 2, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is File", param1 instanceof File);
                        final File stagedFile = (File) param1;
                        Assert.assertNotNull(stagedFile);
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertTrue("Param2 is ServerModuleFile", param2 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param2;
                        Assert.assertNotNull(moduleEntity);

                        scanner.isServerModuleFileLoaded(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
                        return null;
                    }
                }
        ).when(loader).isModuleLoaded(Mockito.<File>any(), Mockito.<ServerModuleFile>any());
    }

    /**
     * Creates sample modules with sample states for different cluster nodes:
     *
     *-----------------------------------------------------------------
     * module_0: Goid(GOID_HI_START, 0); test data 0; CUSTOM_ASSERTION
     * file: com.l7tech.NonDynamicCustomAssertionTest1.jar
     *-----------------------------------------------------------------
     *      currentCluster  => REJECTED
     *      node_1          => UPLOADED
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_1: Goid(GOID_HI_START, 1); test data 1; CUSTOM_ASSERTION
     * file: com.l7tech.DynamicCustomAssertionsTest1.jar
     *-----------------------------------------------------------------
     *      currentCluster  => UPLOADED
     *      node_1          => REJECTED
     *      node_2          => ERROR
     *      node_3          => UPLOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_2: Goid(GOID_HI_START, 2); test data 2; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest1.aar
     *-----------------------------------------------------------------
     *      currentCluster  => ERROR
     *      node_2          => UPLOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_3: Goid(GOID_HI_START, 3); test data 3; CUSTOM_ASSERTION
     * file: com.l7tech.DualAssertionsTest1.jar
     *-----------------------------------------------------------------
     *      currentCluster  => LOADED
     *      node_2          => REJECTED
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_4: Goid(GOID_HI_START, 4); test data 4; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest2.aar
     *-----------------------------------------------------------------
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_5: Goid(GOID_HI_START, 5); test data 5; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest3.aar
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
     * file: com.l7tech.NonDynamicCustomAssertionTest2.jar
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_7: Goid(GOID_HI_START, 7); test data 7; CUSTOM_ASSERTION
     * file: com.l7tech.BrokenDescriptorTest1.jar (fail)
     *-----------------------------------------------------------------
     *      currentCluster  => ACCEPTED
     *      node_2          => REJECTED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_8: Goid(GOID_HI_START, 8); test data 8; MODULAR_ASSERTION
     * file: com.l7tech.InvalidAssertionClassTest1.aar (fail)
     *-----------------------------------------------------------------
     *      currentCluster  => ACCEPTED
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_9: Goid(GOID_HI_START, 9); test data 9; MODULAR_ASSERTION
     * file: com.l7tech.NoAssertionsTest1.aar (fail)
     *-----------------------------------------------------------------
     *      currentCluster  => REJECTED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_10: Goid(GOID_HI_START, 10); test data 10; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest4.aar
     *-----------------------------------------------------------------
     *      currentCluster  => LOADED
     *      node_1          => LOADED
     *-----------------------------------------------------------------
     */
    private ModuleState[] initialStates;
    private void createUnsignedSampleModules() throws Exception {
        moduleFiles = CollectionUtils.<Goid, ServerModuleFile>mapBuilder()
                .put(
                        //-----------------------------------------------------------------
                        // module_0: Goid(GOID_HI_START, 0); test data 0; CUSTOM_ASSERTION
                        // file: com.l7tech.NonDynamicCustomAssertionTest1.jar
                        //-----------------------------------------------------------------
                        // currentCluster  => REJECTED
                        // node_1          => UPLOADED
                        // node_3          => LOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 0),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(0, ModuleType.CUSTOM_ASSERTION, new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest1.jar")))
                                .addState(currentNodeId, ModuleState.REJECTED)
                                .addState("node_1", ModuleState.UPLOADED)
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_1: Goid(GOID_HI_START, 1); test data 1; CUSTOM_ASSERTION
                        // file: com.l7tech.DynamicCustomAssertionsTest1.jar
                        //-----------------------------------------------------------------
                        // currentCluster  => UPLOADED
                        // node_1          => REJECTED
                        // node_2          => ERROR
                        // node_3          => UPLOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 1),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(1, ModuleType.CUSTOM_ASSERTION, new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest1.jar")))
                                .addState(currentNodeId, ModuleState.UPLOADED)
                                .addState("node_1", ModuleState.REJECTED)
                                .addStateError("node_2", "Error loading module!")
                                .addState("node_3", ModuleState.UPLOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_2: Goid(GOID_HI_START, 2); test data 2; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest1.aar
                        //-----------------------------------------------------------------
                        // currentCluster  => ERROR
                        // node_2          => UPLOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 2),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(2, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar")))
                                .addStateError(currentNodeId, "Error loading module")
                                .addState("node_2", ModuleState.UPLOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_3: Goid(GOID_HI_START, 3); test data 3; CUSTOM_ASSERTION
                        // file: com.l7tech.DualAssertionsTest1.jar
                        //-----------------------------------------------------------------
                        // currentCluster  => LOADED
                        // node_2          => REJECTED
                        // node_3          => LOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 3),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(3, ModuleType.CUSTOM_ASSERTION, new File(dualModulesEmptyDir, "com.l7tech.DualAssertionsTest1.jar")))
                                .addState(currentNodeId, ModuleState.LOADED)
                                .addState("node_2", ModuleState.REJECTED)
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_4: Goid(GOID_HI_START, 4); test data 4; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest2.aar
                        //-----------------------------------------------------------------
                        // node_3          => LOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 4),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(4, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar")))
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_5: Goid(GOID_HI_START, 5); test data 5; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest3.aar
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 5),
                        create_unsigned_test_module_without_states(5, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest3.aar"))
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
                        // file: com.l7tech.NonDynamicCustomAssertionTest2.jar
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 6),
                        create_unsigned_test_module_without_states(6, ModuleType.CUSTOM_ASSERTION, new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest2.jar"))
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_7: Goid(GOID_HI_START, 7); test data 7; CUSTOM_ASSERTION
                        // file: com.l7tech.BrokenDescriptorTest1.jar (fail)
                        // ----------------------------------------------------------------
                        // currentCluster  => ACCEPTED
                        // node_2          => REJECTED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 7),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(7, ModuleType.CUSTOM_ASSERTION, new File(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest1.jar")))
                                .addState(currentNodeId, ModuleState.ACCEPTED)
                                .addState("node_2", ModuleState.REJECTED)
                                .build()
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_8: Goid(GOID_HI_START, 8); test data 8; MODULAR_ASSERTION
                        // file: com.l7tech.InvalidAssertionClassTest1.aar (fail)
                        // ----------------------------------------------------------------
                        // currentCluster  => ACCEPTED
                        // node_3          => LOADED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 8),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(8, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.InvalidAssertionClassTest1.aar")))
                                .addState(currentNodeId, ModuleState.ACCEPTED)
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_9: Goid(GOID_HI_START, 9); test data 9; MODULAR_ASSERTION
                        // file: com.l7tech.NoAssertionsTest1.aar (fail)
                        // ----------------------------------------------------------------
                        // currentCluster  => REJECTED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 9),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(9, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.NoAssertionsTest1.aar")))
                                .addState(currentNodeId, ModuleState.REJECTED)
                                .build()
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_10: Goid(GOID_HI_START, 10); test data 10; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest4.aar
                        // ----------------------------------------------------------------
                        // currentCluster  => LOADED
                        // node_1          => LOADED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 10),
                        new ServerModuleFileBuilder(create_unsigned_test_module_without_states(10, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar")))
                                .addState(currentNodeId, ModuleState.LOADED)
                                .addState("node_1", ModuleState.LOADED)
                                .build()
                )
                .map();

        // modules in our repository with their state for current cluster node:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        initialStates = new ModuleState[]{
                ModuleState.REJECTED,
                ModuleState.UPLOADED,
                ModuleState.ERROR,
                ModuleState.LOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.ACCEPTED,
                ModuleState.ACCEPTED,
                ModuleState.REJECTED,
                ModuleState.LOADED,
        };
    }

    /**
     * Use the {@link #untrustedCerts} to sign this module
     */
    private MyServerModuleFile sign_with_untrusted_signer(
            final long ordinal,
            ModuleType moduleType,
            final File moduleBytes,
            final String signatureDn
    ) throws Exception {
        final String signatureProps = SignatureTestUtils.signAndGetSignature(untrustedCerts, moduleBytes, signatureDn);
        return create_test_module_without_states(
                ordinal,
                moduleType,
                moduleBytes,
                signatureProps
        );
    }

    /**
     * Simply use a different file for the module bytes and signature properties.
     * Optionally use the original digest if specified.
     */
    private MyServerModuleFile tamper_with_module_bytes(
            final long ordinal,
            final ModuleType moduleType,
            final File originalFile,
            final File tamperedWithFile,
            final String signatureDn,
            final boolean useOriginalFileDigest
    ) throws Exception {
        // some sanity check
        Assert.assertTrue(originalFile != null && originalFile.exists() && !originalFile.isDirectory());
        Assert.assertTrue(tamperedWithFile != null && tamperedWithFile.exists() && !tamperedWithFile.isDirectory());
        Assert.assertThat(signatureDn, Matchers.not(Matchers.isEmptyOrNullString()));
        // sign using the original file
        final String signatureProps = signAndGetSignature(originalFile, signatureDn);
        // create the module using the tamperedWithFile
        final MyServerModuleFile moduleFile = create_test_module_without_states(
                ordinal,
                moduleType,
                tamperedWithFile,
                signatureProps
        );
        if (useOriginalFileDigest) {
            try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(originalFile))) {
                final String newDigest = ModuleDigest.hexEncodedDigest(is);
                Assert.assertThat(newDigest, Matchers.not(Matchers.equalTo(moduleFile.getModuleSha256())));
                moduleFile.setModuleSha256(newDigest);
            }
        }
        return moduleFile;
    }

    /**
     * Creates sample signed modules with sample states for different cluster nodes:
     *
     *-----------------------------------------------------------------
     * module_0: Goid(GOID_HI_START, 0); test data 0; CUSTOM_ASSERTION
     * file: com.l7tech.NonDynamicCustomAssertionTest1.jar
     * SIGNED with SIGNER_CERT_DNS[0]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_1: Goid(GOID_HI_START, 1); test data 1; CUSTOM_ASSERTION
     * file: com.l7tech.DynamicCustomAssertionsTest1.jar
     * SIGNED with SIGNER_CERT_DNS[1]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_2: Goid(GOID_HI_START, 2); test data 2; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest1.aar
     * SIGNED with with SIGNER_CERT_DNS[2]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_3: Goid(GOID_HI_START, 3); test data 3; CUSTOM_ASSERTION
     * file: com.l7tech.DualAssertionsTest1.jar
     * SIGNED with SIGNER_CERT_DNS[0]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_4: Goid(GOID_HI_START, 4); test data 4; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest2.aar
     * SIGNED with SIGNER_CERT_DNS[0]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_5: Goid(GOID_HI_START, 5); test data 5; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest3.aar
     * SIGNATURE ERROR: DATA BYTES TAMPERED WITH
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
     * file: com.l7tech.NonDynamicCustomAssertionTest2.jar
     * UNSIGNED
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_7: Goid(GOID_HI_START, 7); test data 7; CUSTOM_ASSERTION
     * file: com.l7tech.BrokenDescriptorTest1.jar (fail)
     * SIGNED with SIGNER_CERT_DNS[3]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_8: Goid(GOID_HI_START, 8); test data 8; MODULAR_ASSERTION
     * file: com.l7tech.InvalidAssertionClassTest1.aar (fail)
     * SIGNED with SIGNER_CERT_DNS[3]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_9: Goid(GOID_HI_START, 9); test data 9; MODULAR_ASSERTION
     * file: com.l7tech.NoAssertionsTest1.aar (fail)
     * SIGNED with SIGNER_CERT_DNS[3]
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_10: Goid(GOID_HI_START, 10); test data 10; MODULAR_ASSERTION
     * file: com.l7tech.WorkingTest4.aar
     * SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_11: Goid(GOID_HI_START, 11); test data 11; CUSTOM_ASSERTION
     * file: com.l7tech.DynamicCustomAssertionsTest2.jar
     * SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_12: Goid(GOID_HI_START, 12); test data 12; CUSTOM_ASSERTION
     * file: com.l7tech.DynamicCustomAssertionsTest3.jar
     * SIGNATURE ERROR: DATA BYTES TAMPERED WITH
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     */
    private void createSignedSampleModules() throws Exception {
        moduleFiles = CollectionUtils.<Goid, ServerModuleFile>mapBuilder()
                .put(
                        //-----------------------------------------------------------------
                        // module_0: Goid(GOID_HI_START, 0); test data 0; CUSTOM_ASSERTION
                        // file: com.l7tech.NonDynamicCustomAssertionTest1.jar
                        // SIGNED with SIGNER_CERT_DNS[0]
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 0),
                        create_and_sign_test_module_without_states(
                                0,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest1.jar"),
                                SIGNER_CERT_DNS[0]
                        )
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_1: Goid(GOID_HI_START, 1); test data 1; CUSTOM_ASSERTION
                        // file: com.l7tech.DynamicCustomAssertionsTest1.jar
                        // SIGNED with SIGNER_CERT_DNS[1]
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 1),
                        create_and_sign_test_module_without_states(
                                1,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest1.jar"),
                                SIGNER_CERT_DNS[1]
                        )
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_2: Goid(GOID_HI_START, 2); test data 2; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest1.aar
                        // SIGNED with SIGNER_CERT_DNS[2]
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 2),
                        create_and_sign_test_module_without_states(
                                2,
                                ModuleType.MODULAR_ASSERTION,
                                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar"),
                                SIGNER_CERT_DNS[2]
                        )
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_3: Goid(GOID_HI_START, 3); test data 3; CUSTOM_ASSERTION
                        // file: com.l7tech.DualAssertionsTest1.jar
                        // SIGNED with SIGNER_CERT_DNS[0]
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 3),
                        create_and_sign_test_module_without_states(
                                3,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(dualModulesEmptyDir, "com.l7tech.DualAssertionsTest1.jar"),
                                SIGNER_CERT_DNS[0]
                        )
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_4: Goid(GOID_HI_START, 4); test data 4; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest2.aar
                        // SIGNED with SIGNER_CERT_DNS[0]
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 4),
                        create_and_sign_test_module_without_states(
                                4,
                                ModuleType.MODULAR_ASSERTION,
                                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar"),
                                SIGNER_CERT_DNS[0]
                        )
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_5: Goid(GOID_HI_START, 5); test data 5; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest3.aar
                        // SIGNATURE ERROR: DATA BYTES TAMPERED WITH
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 5),
                        tamper_with_module_bytes(
                                5,
                                ModuleType.MODULAR_ASSERTION,
                                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest3.aar"),
                                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar"),
                                SIGNER_CERT_DNS[1],
                                false // don't use the original file sha256
                        )
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
                        // file: com.l7tech.NonDynamicCustomAssertionTest2.jar
                        // UNSIGNED
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 6),
                        create_unsigned_test_module_without_states(
                                6,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest2.jar")
                        )
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_7: Goid(GOID_HI_START, 7); test data 7; CUSTOM_ASSERTION
                        // file: com.l7tech.BrokenDescriptorTest1.jar (fail)
                        // SIGNED with SIGNER_CERT_DNS[3]
                        // ----------------------------------------------------------------
                        // (empty)
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 7),
                        create_and_sign_test_module_without_states(
                                7,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest1.jar"),
                                SIGNER_CERT_DNS[3]
                        )
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_8: Goid(GOID_HI_START, 8); test data 8; MODULAR_ASSERTION
                        // file: com.l7tech.InvalidAssertionClassTest1.aar (fail)
                        // SIGNED with SIGNER_CERT_DNS[3]
                        // ----------------------------------------------------------------
                        // (empty)
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 8),
                        create_and_sign_test_module_without_states(
                                8,
                                ModuleType.MODULAR_ASSERTION,
                                new File(modulesRootEmptyDir, "com.l7tech.InvalidAssertionClassTest1.aar"),
                                SIGNER_CERT_DNS[3]
                        )
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_9: Goid(GOID_HI_START, 9); test data 9; MODULAR_ASSERTION
                        // file: com.l7tech.NoAssertionsTest1.aar (fail)
                        // SIGNED with SIGNER_CERT_DNS[3]
                        // ----------------------------------------------------------------
                        // (empty)
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 9),
                        create_and_sign_test_module_without_states(
                                9,
                                ModuleType.MODULAR_ASSERTION,
                                new File(modulesRootEmptyDir, "com.l7tech.NoAssertionsTest1.aar"),
                                SIGNER_CERT_DNS[3]
                        )
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_10: Goid(GOID_HI_START, 10); test data 10; MODULAR_ASSERTION
                        // file: com.l7tech.WorkingTest4.aar
                        // SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts
                        // ----------------------------------------------------------------
                        // (empty)
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 10),
                        sign_with_untrusted_signer(
                                10,
                                ModuleType.MODULAR_ASSERTION,
                                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar"),
                                SIGNER_CERT_DNS[1]
                        )
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_11: Goid(GOID_HI_START, 11); test data 11; CUSTOM_ASSERTION
                        // file: com.l7tech.DynamicCustomAssertionsTest2.jar
                        // SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts
                        // ----------------------------------------------------------------
                        // (empty)
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 11),
                        sign_with_untrusted_signer(
                                11,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest2.jar"),
                                untrustedSignerCertDns[0]
                        )
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_12: Goid(GOID_HI_START, 12); test data 12; CUSTOM_ASSERTION
                        // file: com.l7tech.DynamicCustomAssertionsTest3.jar
                        // SIGNATURE ERROR: DATA BYTES TAMPERED WITH
                        // ----------------------------------------------------------------
                        // (empty)
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 12),
                        tamper_with_module_bytes(
                                12,
                                ModuleType.CUSTOM_ASSERTION,
                                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest3.jar"),
                                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                                SIGNER_CERT_DNS[0],
                                true // use the original file sha256
                        )
                )
                .map();

        // modules in our repository with their state for current cluster node:
        // module_0  => <NONE> => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar      => SIGNED with SIGNER_CERT_DNS[0]
        // module_1  => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar        => SIGNED with SIGNER_CERT_DNS[1]
        // module_2  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                        => SIGNED with SIGNER_CERT_DNS[2]
        // module_3  => <NONE> => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                 => SIGNED with SIGNER_CERT_DNS[0]
        // module_4  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                        => SIGNED with SIGNER_CERT_DNS[0]
        // module_5  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                        => SIGNATURE ERROR: DATA BYTES TAMPERED WITH
        // module_6  => <NONE> => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar      => UNSIGNED
        // module_7  => <NONE> => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)        => SIGNED with SIGNER_CERT_DNS[3]
        // module_8  => <NONE> => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)   => SIGNED with SIGNER_CERT_DNS[3]
        // module_9  => <NONE> => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)            => SIGNED with SIGNER_CERT_DNS[3]
        // module_10 => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                        => SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts
        // module_11 => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest2.jar        => SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts
        // module_12 => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest3.jar        => SIGNATURE ERROR: DATA BYTES TAMPERED WITH
        initialStates = new ModuleState[]{
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
        };
    }
    // failed modules
    final Collection<Goid> failedModules = Collections.unmodifiableCollection(Arrays.asList(
            new Goid(GOID_HI_START, 7),   // module_7  => ACCEPTED => CUSTOM_ASSERTION;  com.l7tech.BrokenDescriptorTest1.jar (fail)
            new Goid(GOID_HI_START, 8),   // module_8  => ACCEPTED => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail)
            new Goid(GOID_HI_START, 9)    // module_9  => REJECTED => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)
    ));

    // rejected modules (applies only when using createSignedSampleModules and not createUnsignedSampleModules)
    final Collection<Goid> signedRejectedModules = Collections.unmodifiableCollection(Arrays.asList(
            new Goid(GOID_HI_START, 5),   // module_5  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                    => SIGNATURE ERROR: DATA BYTES TAMPERED WITH
            new Goid(GOID_HI_START, 6),   // module_6  => <NONE> => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar  => UNSIGNED
            new Goid(GOID_HI_START, 10),  // module_10 => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                    => SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts
            new Goid(GOID_HI_START, 11),  // module_11 => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest2.jar    => SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts
            new Goid(GOID_HI_START, 12)   // module_12 => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest3.jar    => SIGNATURE ERROR: DATA BYTES TAMPERED WITH
    ));

    // unlicensed modules (resets at each run i.e. at @Before)
    final Collection<Goid> unlicensedModules = new ArrayList<>();

    /**
     * Common mockups for {@link ServerModuleFileManager}:<br/>
     * <ul>
     *     <li>{@link com.l7tech.server.module.ServerModuleFileManager#isModuleUploadEnabled()} </li>
     *     <li>{@link ServerModuleFileManager#findStateForCurrentNode(com.l7tech.gateway.common.module.ServerModuleFile)} </li>
     *     <li>{@link ServerModuleFileManager#findByPrimaryKey(com.l7tech.objectmodel.Goid)} </li>
     *     <li>{@link com.l7tech.server.module.ServerModuleFileManager#findAll()} </li>
     *     <li>{@link ServerModuleFileManager#updateState(com.l7tech.objectmodel.Goid, com.l7tech.gateway.common.module.ModuleState)} </li>
     *     <li>{@link ServerModuleFileManager#updateState(com.l7tech.objectmodel.Goid, String)}  </li>
     * </ul>
     *
     * @param uploadEnabled    flag indicating what would {@link com.l7tech.server.module.ServerModuleFileManager#isModuleUploadEnabled()} return.
     */
    private void mockServerModuleFileManager(final boolean uploadEnabled) throws FindException, UpdateException {
        Assert.assertNotNull(serverModuleFileManager);
        Mockito.when(serverModuleFileManager.isModuleUploadEnabled()).thenReturn(uploadEnabled);
        Mockito.when(serverModuleFileManager.findStateForCurrentNode(Mockito.<ServerModuleFile>any()))
                .thenAnswer(
                        new Answer<ServerModuleFileState>() {
                            @Override
                            public ServerModuleFileState answer(final InvocationOnMock invocation) throws Throwable {
                                Assert.assertNotNull(invocation);
                                Assert.assertEquals("there is only one parameter for findStateForCurrentNode", 1, invocation.getArguments().length);
                                final Object param1 = invocation.getArguments()[0];
                                Assert.assertTrue("Param is Class", param1 instanceof ServerModuleFile);
                                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                                Assert.assertNotNull(moduleFile);
                                return moduleFile.getStateForNode(currentNodeId);
                            }
                        }
                );
        Mockito.when(serverModuleFileManager.findByPrimaryKey(Mockito.<Goid>any()))
                .thenAnswer(
                        new Answer<ServerModuleFile>() {
                            @Override
                            public ServerModuleFile answer(final InvocationOnMock invocation) throws Throwable {
                                Assert.assertNotNull(invocation);
                                Assert.assertEquals("there is only one parameter for findByPrimaryKey", 1, invocation.getArguments().length);
                                final Object param1 = invocation.getArguments()[0];
                                Assert.assertTrue("Param is Goid", param1 instanceof Goid);
                                final Goid goid = (Goid) param1;
                                Assert.assertNotNull(goid);
                                return moduleFiles.get(goid);
                            }
                        }
                );
        Mockito.when(serverModuleFileManager.findAll()).thenReturn(Collections.unmodifiableCollection(moduleFiles.values()));
        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there are two parameter for updateState", 2, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is Goid", param1 instanceof Goid);
                        final Goid goid = (Goid) param1;
                        Assert.assertNotNull(goid);
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertTrue("Param2 is String", param2 instanceof String);
                        final String errorMessage = (String) param2;

                        final ServerModuleFile moduleFile = moduleFiles.get(goid);
                        Assert.assertNotNull(moduleFile);
                        moduleFile.setStateErrorMessageForNode(currentNodeId, errorMessage);

                        return null;
                    }
                }
        ).when(serverModuleFileManager).updateState(Mockito.<Goid>any(), Mockito.anyString());
        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there are two parameter for updateState", 2, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is Goid", param1 instanceof Goid);
                        final Goid goid = (Goid) param1;
                        Assert.assertNotNull(goid);
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertTrue("Param2 is ModuleState", param2 instanceof ModuleState);
                        final ModuleState moduleState = (ModuleState) param2;

                        final ServerModuleFile moduleFile = moduleFiles.get(goid);
                        Assert.assertNotNull(moduleFile);
                        moduleFile.setStateForNode(currentNodeId, moduleState);

                        return null;
                    }
                }
        ).when(serverModuleFileManager).updateState(Mockito.<Goid>any(), Mockito.<ModuleState>any());
        Mockito.doAnswer(
                new Answer<Pair<InputStream, String>>() {
                    @Override
                    public Pair<InputStream, String> answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is only one parameter for getModuleBytesAsStream", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param is Goid", param1 instanceof Goid);
                        final Goid goid = (Goid) param1;
                        Assert.assertNotNull(goid);

                        final ServerModuleFile moduleFile = moduleFiles.get(goid);
                        if (moduleFile != null) {
                            Assert.assertTrue(moduleFile instanceof MyServerModuleFile);
                            Assert.assertNotNull(((MyServerModuleFile) moduleFile).getModuleContentStreamWithSignature());
                            return ((MyServerModuleFile) moduleFile).getModuleContentStreamWithSignature();
                        }
                        return null;
                    }
                }
        ).when(serverModuleFileManager).getModuleBytesAsStreamWithSignature(Mockito.<Goid>any());
    }

    /**
     * Gets the specified {@code future} result waiting for default two seconds to finish the task.
     *
     * @param future    the {@link Future} to wait for completion.  Optional and can be {@code null} in case when no task was executed.
     */
    private static Future waitForFuture(final Future future) throws ExecutionException, InterruptedException, TimeoutException {
        return waitForFuture(future, 5000);
    }

    /**
     * Gets the specified {@code future} result waiting for specified {@code timeout} in millis.
     *
     * @param future     the {@link Future} to wait for completion.
     * @param timeout    the timeout in millis to wait for {@code future} completion.  Value of {@code -1} will wait indefinitely.
     */
    private static Future waitForFuture(final Future future, final int timeout) throws ExecutionException, InterruptedException, TimeoutException {
        assertThat(
                "future timeout can either be -1 or greater then 0",
                timeout,
                either(equalTo(-1)).or(greaterThan(0))
        );
        if (future != null) {
            if (timeout != -1) {
                future.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                future.get();
            }
        }
        return future;
    }

    /**
     * Utility function for running modular and custom assertions initial scan.
     *
     * @param modularModulesLoaded    array of expected modular assertions module name(s) (file-names) loaded.  Required and cannot be {@code null}.
     * @param customModulesLoaded     array of expected custom assertions module name(s) (file-names) loaded.  Required and cannot be {@code null}.
     */
    private void do_scanner_run(final String[] modularModulesLoaded, final String[] customModulesLoaded) throws Exception {
        Assert.assertNotNull(modularModulesLoaded);
        Assert.assertNotNull(customModulesLoaded);

        // scan modular modules folder
        modularAssertionsScanner.scanModules();
        assertThat(modularAssertionsScanner.getModules().size(), equalTo(modularModulesLoaded.length));
        for (final ModularAssertionModule module : modularAssertionsScanner.getModules()) {
            Assert.assertNotNull(module.getName());
            assertThat(module.getName(), isOneOf(modularModulesLoaded));
        }

        // scan custom modules folder
        customAssertionsScanner.scanModules();
        assertThat(customAssertionsScanner.getModules().size(), equalTo(customModulesLoaded.length));
        for (final CustomAssertionModule module : customAssertionsScanner.getModules()) {
            Assert.assertNotNull(module.getName());
            assertThat(module.getName(), isOneOf(customModulesLoaded));
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void test_persistence_event_before_started() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders (simulate that deploy folders are empty)
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // new module with goid 100
        moduleFiles.put(
                new Goid(GOID_HI_START, 100),
                new ServerModuleFileBuilder(create_unsigned_test_module_without_states(100, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
                        .addState(currentNodeId, ModuleState.UPLOADED)
                        .build()
        );
        // send EntityInvalidationEvent, containing two events
        Assert.assertNull(
                "No events should be processed before Started is processed!",
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        new Goid[]{new Goid(GOID_HI_START, 0), new Goid(GOID_HI_START, 2), new Goid(GOID_HI_START, 100), new Goid(GOID_HI_START, 1)},
                                        new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.CREATE, EntityInvalidationEvent.DELETE}
                                )
                        )
                )
        );

        // EntityInvalidationEvent should be ignored until Started is executed
        Mockito.verify(modulesListener, Mockito.times(1)).handleEvent(Mockito.<ApplicationEvent>any());
        Mockito.verify(modulesListener, Mockito.never()).processGatewayStartedEvent();
        Mockito.verify(modulesListener, Mockito.never()).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any());
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Assert.assertNotNull(modulesListener.knownModuleFiles);
        assertThat(modulesListener.knownModuleFiles.values(), empty());
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());
    }

    @Test
    public void test_license_event_before_started() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders (simulate that deploy folders are empty)
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // send EntityInvalidationEvent, containing two events
        Assert.assertNull(
                "No events should be processed before Started is processed!",
                waitForFuture(
                        modulesListener.handleEvent(
                                new LicenseChangeEvent(
                                        this,
                                        Level.ALL,
                                        "license action",
                                        "license message"
                                )
                        )
                )
        );

        // EntityInvalidationEvent should be ignored until Started is executed
        Mockito.verify(modulesListener, Mockito.times(1)).handleEvent(Mockito.<ApplicationEvent>any());
        Mockito.verify(modulesListener, Mockito.never()).processGatewayStartedEvent();
        Mockito.verify(modulesListener, Mockito.never()).processLicenseChangeEvent();
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Assert.assertNotNull(modulesListener.knownModuleFiles);
        assertThat(modulesListener.knownModuleFiles.values(), empty());
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());
    }

    @Test
    public void test_upload_disabled() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(false);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        Assert.assertNotNull(modulesListener.knownModuleFiles);
        assertThat(modulesListener.knownModuleFiles.values(), empty());
        // send Started event
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(new Started(this, Component.GATEWAY, "Test"))
                )
        );
        // verify
        Mockito.verify(modulesListener, Mockito.times(1)).handleEvent(Mockito.<ApplicationEvent>any()); // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent(); // processGatewayStartedEvent should be called in order to populate knownModuleFiles
        Mockito.verify(modulesListener, Mockito.never()).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).processLicenseChangeEvent(); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());

        int knownSize = modulesListener.knownModuleFiles.size();
        // new module with goid 100
        moduleFiles.put(
                new Goid(GOID_HI_START, 100),
                new ServerModuleFileBuilder(create_unsigned_test_module_without_states(100, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
                        .addState(currentNodeId, ModuleState.UPLOADED)
                        .build()
        );
        // send EntityInvalidationEvent, containing two events
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        new Goid[]{new Goid(GOID_HI_START, 0), new Goid(GOID_HI_START, 100)},
                                        new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.CREATE}
                                )
                        )
                )
        );
        // verify
        Mockito.verify(modulesListener, Mockito.times(2)).handleEvent(Mockito.<ApplicationEvent>any()); // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent(); // still called only the first time
        Mockito.verify(modulesListener, Mockito.times(1)).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any()); // called once
        Mockito.verify(modulesListener, Mockito.never()).processLicenseChangeEvent(); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(knownSize + 1)); // make sure the new module_100 is populated
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());

        knownSize = modulesListener.knownModuleFiles.size();
        // remove module with goid 2
        assertThat(moduleFiles.remove(new Goid(GOID_HI_START, 2)), notNullValue());
        // send EntityInvalidationEvent, containing two events
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        new Goid[]{new Goid(GOID_HI_START, 2)},
                                        new char[]{EntityInvalidationEvent.DELETE}
                                )
                        )
                )
        );
        // verify
        Mockito.verify(modulesListener, Mockito.times(3)).handleEvent(Mockito.<ApplicationEvent>any()); // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent(); // still called only the first time
        Mockito.verify(modulesListener, Mockito.times(2)).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any()); // called twice now
        Mockito.verify(modulesListener, Mockito.never()).processLicenseChangeEvent(); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(knownSize - 1)); // make sure the new module_2 is removed
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());

        knownSize = modulesListener.knownModuleFiles.size();
        // send LicenseChangeEvent
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new LicenseChangeEvent(
                                        this,
                                        Level.ALL,
                                        "license action",
                                        "license message"
                                )
                        )
                )
        );
        // verify
        Mockito.verify(modulesListener, Mockito.times(4)).handleEvent(Mockito.<ApplicationEvent>any()); // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent(); // still called only the first time
        Mockito.verify(modulesListener, Mockito.times(2)).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any()); // called twice now
        Mockito.verify(modulesListener, Mockito.times(1)).processLicenseChangeEvent(); // called once
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(knownSize)); // no changes this time
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());

        knownSize = modulesListener.knownModuleFiles.size();
        // new module with goid 101
        moduleFiles.put(
                new Goid(GOID_HI_START, 101),
                new ServerModuleFileBuilder(create_unsigned_test_module_without_states(101, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
                        .addState(currentNodeId, ModuleState.UPLOADED)
                        .build()
        );
        // new module with goid 102
        moduleFiles.put(
                new Goid(GOID_HI_START, 102),
                new ServerModuleFileBuilder(create_unsigned_test_module_without_states(102, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
                        .addState(currentNodeId, ModuleState.UPLOADED)
                        .build()
        );
        // remove module with goid 4
        assertThat(moduleFiles.remove(new Goid(GOID_HI_START, 4)), notNullValue());
        // send LicenseChangeEvent
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new LicenseChangeEvent(
                                        this,
                                        Level.ALL,
                                        "license action",
                                        "license message"
                                )
                        )
                )
        );
        // verify
        Mockito.verify(modulesListener, Mockito.times(5)).handleEvent(Mockito.<ApplicationEvent>any()); // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent(); // still called only the first time
        Mockito.verify(modulesListener, Mockito.times(2)).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any()); // called twice now
        Mockito.verify(modulesListener, Mockito.times(2)).processLicenseChangeEvent(); // called once
        Mockito.verify(modulesListener, Mockito.never()).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(knownSize + 1)); // there should be one extra module (+2 -1)
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging folder is empty
        assertThat(stagingFolder.listFiles(), emptyArray());
    }

    /**
     * Utility function for checking that our modules (in the DB i.e. {@code moduleFiles}) have the expected states
     * @param expectedStates    array of module staes, ordert by module ordinal i.e. starting from module_0, module_1 etc.
     */
    private void verifyModulesState(final ModuleState[] expectedStates) {
        Assert.assertNotNull(expectedStates);
        for (int i = 0; i < expectedStates.length; ++i) {
            Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, i)));
            Assert.assertNotNull(modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, i))));
            assertThat("Unexpected state for module_" + i, modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, i))), equalTo(expectedStates[i]));
        }
    }

    /**
     * Will send out {@link Started} event, simulating SSG start, and will verify the initial state of the modules listener.
     *
     * @param expectedStates           the expected modules states (module ordinal order) after the modules listener initializes.
     * @param expectedLoadedModules    module files collection of expected modular assertions loaded modules.
     */
    private void publishInitialStartedEventAndVerifyResult(
            final ModuleState[] expectedStates,
            final Collection<ServerModuleFile> expectedLoadedModules
    ) throws Exception {
        // verify initial state
        Assert.assertNotNull(modulesListener.knownModuleFiles);
        assertThat(modulesListener.knownModuleFiles.values(), empty());
        assertThat(moduleFiles.values(), not(empty()));
        assertThat(stagingFolder.listFiles(), emptyArray());

        // verify initial states
        Assert.assertNotNull(initialStates);
        for (int i = 0; i < initialStates.length; ++i) {
            assertThat(modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, i))), equalTo(initialStates[i]));
        }

        // publish the event
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(new Started(this, Component.GATEWAY, "Test"))
                )
        );

        // verify loaded
        Assert.assertNotNull(expectedLoadedModules);
        for (final ServerModuleFile moduleFile : expectedLoadedModules) {
            Assert.assertNotNull(moduleFile);
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(ModuleState.LOADED));
        }

        // verify module states
        verifyModulesState(expectedStates);

        // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).handleEvent(Mockito.<ApplicationEvent>any());
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent();
        Mockito.verify(modulesListener, Mockito.never()).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any());  // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).processLicenseChangeEvent(); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.times(moduleFiles.values().size())).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // should be called for each module
        Mockito.verify(modulesListener, Mockito.never()).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        final File[] files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        assertThat(expectedLoadedModules.size(), lessThanOrEqualTo(files.length));
        for (final ServerModuleFile loadedModuleFile : expectedLoadedModules) {
            Assert.assertNotNull(loadedModuleFile.getGoid());
            final ServerModuleFileListener.StagedServerModuleFile stagedModuleFile = modulesListener.knownModuleFiles.get(loadedModuleFile.getGoid());
            Assert.assertNotNull(stagedModuleFile);
            Assert.assertNotNull(stagedModuleFile.getStagingFile());
            Assert.assertTrue(stagedModuleFile.getStagingFile().exists());
            assertThat(stagedModuleFile.getStagingFile().getParentFile().getCanonicalPath(), equalTo(stagingFolder.getCanonicalPath()));
            // verify module name
            final ModuleType moduleType = loadedModuleFile.getModuleType();
            Assert.assertNotNull(moduleType);
            final ModulesScanner modulesScanner = ModuleType.MODULAR_ASSERTION.equals(moduleType) ? modularAssertionsScanner : customAssertionsScanner;
            final BaseAssertionModule module = modulesScanner.getModule(stagedModuleFile.getStagingFile().getName());
            Assert.assertNotNull(module);
            Assert.assertThat(module.getEntityName(), equalTo(loadedModuleFile.getName()));
            if (ModuleType.CUSTOM_ASSERTION.equals(moduleType)) {
                Assert.assertTrue(module instanceof CustomAssertionModule);
                Assert.assertThat(((CustomAssertionModule) module).getDescriptors(), not(emptyCollectionOf(CustomAssertionDescriptor.class)));
                for (final CustomAssertionDescriptor descriptor : ((CustomAssertionModule)module).getDescriptors()) {
                    Assert.assertThat(descriptor.getModuleEntityName(), equalTo(loadedModuleFile.getName()));
                }
            }
        }
    }

    /**
     * Do the actual initial Started publish and verify the result.
     *
     * @param rejectedModules          Collection of module GOID(s) to be rejected.
     * @param signatureErrorModules    Collection of module GOID(s) to be fail signature verification.
     */
    private void do_test_started_event(
            final Collection<Goid> rejectedModules,
            final Collection<Goid> signatureErrorModules
    ) throws Exception {
        // simulate signature verification
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertNotNull(invocation);
                Assert.assertEquals("there are two parameters for verifySignature", 2, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                Assert.assertNotNull(moduleFile);

                // check if module should be rejected, fail to verify signature or be accepted
                if (rejectedModules.contains(moduleFile.getGoid())) {
                    throw new ServerModuleFileListener.ModuleRejectedException();
                } else if (signatureErrorModules.contains(moduleFile.getGoid())) {
                    throw new ServerModuleFileListener.ModuleSignatureException("signature error");
                }
                // the rest are accepted
                return null;
            }
        }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any(), Mockito.anyString());

        // calculate expected loaded based on rejectedModules and signatureErrorModules
        final Collection<ServerModuleFile> expectedLoaded = new ArrayList<>();
        for (final ServerModuleFile serverModuleFile : moduleFiles.values()) {
            Assert.assertNotNull(serverModuleFile.getGoid());
            if (!rejectedModules.contains(serverModuleFile.getGoid()) && !signatureErrorModules.contains(serverModuleFile.getGoid()) && !failedModules.contains(serverModuleFile.getGoid()) && !unlicensedModules.contains(serverModuleFile.getGoid())) {
                expectedLoaded.add(serverModuleFile);
            }
        }

        // we have:
        publishInitialStartedEventAndVerifyResult(
                new ModuleState[]{
                        rejectedModules.contains(new Goid(GOID_HI_START, 0))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 0)) || failedModules.contains(new Goid(GOID_HI_START, 0)) || unlicensedModules.contains(new Goid(GOID_HI_START, 0)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_0  => REJECTED => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest1.jar     => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 1))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 1)) || failedModules.contains(new Goid(GOID_HI_START, 1)) || unlicensedModules.contains(new Goid(GOID_HI_START, 1)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_1  => UPLOADED => CUSTOM_ASSERTION;  com.l7tech.DynamicCustomAssertionsTest1.jar       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 2))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 2)) || failedModules.contains(new Goid(GOID_HI_START, 2)) || unlicensedModules.contains(new Goid(GOID_HI_START, 2)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_2  => ERROR    => MODULAR_ASSERTION; com.l7tech.WorkingTest1.aar                       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 3))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 3)) || failedModules.contains(new Goid(GOID_HI_START, 3)) || unlicensedModules.contains(new Goid(GOID_HI_START, 3)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_3  => LOADED   => CUSTOM_ASSERTION;  com.l7tech.DualAssertionsTest1.jar                => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 4))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 4)) || failedModules.contains(new Goid(GOID_HI_START, 4)) || unlicensedModules.contains(new Goid(GOID_HI_START, 4)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_4  => <NONE>   => MODULAR_ASSERTION; com.l7tech.WorkingTest2.aar                       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 5))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 5)) || failedModules.contains(new Goid(GOID_HI_START, 5)) || unlicensedModules.contains(new Goid(GOID_HI_START, 5)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_5  => <NONE>   => MODULAR_ASSERTION; com.l7tech.WorkingTest3.aar                       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 6))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 6)) || failedModules.contains(new Goid(GOID_HI_START, 6)) || unlicensedModules.contains(new Goid(GOID_HI_START, 6)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_6  => <NONE>   => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest2.jar     => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 7))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 7)) || failedModules.contains(new Goid(GOID_HI_START, 7)) || unlicensedModules.contains(new Goid(GOID_HI_START, 7)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_7  => ACCEPTED => CUSTOM_ASSERTION;  com.l7tech.BrokenDescriptorTest1.jar (fail)       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 8))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 8)) || failedModules.contains(new Goid(GOID_HI_START, 8)) || unlicensedModules.contains(new Goid(GOID_HI_START, 8)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_8  => ACCEPTED => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail)  => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 9))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 9)) || failedModules.contains(new Goid(GOID_HI_START, 9)) || unlicensedModules.contains(new Goid(GOID_HI_START, 9)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_9  => REJECTED => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)           => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 10))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 10)) || failedModules.contains(new Goid(GOID_HI_START, 10)) || unlicensedModules.contains(new Goid(GOID_HI_START, 10)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED                     // module_10 => LOADED   => MODULAR_ASSERTION; com.l7tech.WorkingTest4.aar                       => REJECTED/ERROR/LOADED
                },
                expectedLoaded
        );
    }

    @Test
    public void test_started_event() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // test started event
        // initial state:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2),
                        new Goid(GOID_HI_START, 5),
                        new Goid(GOID_HI_START, 9)
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3),
                        new Goid(GOID_HI_START, 10)
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.REJECTED, // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => REJECTED
                        ModuleState.ERROR     // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => ERROR
                }
        );
    }

    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_started_event_without_write_permissions_to_staging() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        try {
            // set read-only to modular assertions deploy folder
            Assert.assertTrue(stagingFolder.setWritable(false));
            Assert.assertFalse(stagingFolder.canWrite());
            // do actual initial test
            // we have:
            publishInitialStartedEventAndVerifyResult(
                    new ModuleState[]{
                            ModuleState.ERROR,    // module_0  => REJECTED => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest1.jar    => ERROR
                            ModuleState.ERROR,    // module_1  => UPLOADED => CUSTOM_ASSERTION;  com.l7tech.DynamicCustomAssertionsTest1.jar      => ERROR
                            ModuleState.ERROR,    // module_2  => ERROR    => MODULAR_ASSERTION; com.l7tech.WorkingTest1.aar                      => ERROR
                            ModuleState.ERROR,    // module_3  => LOADED   => CUSTOM_ASSERTION;  com.l7tech.DualAssertionsTest1.jar               => ERROR
                            ModuleState.ERROR,    // module_4  => <NONE>   => MODULAR_ASSERTION; com.l7tech.WorkingTest2.aar                      => ERROR
                            ModuleState.ERROR,    // module_5  => <NONE>   => MODULAR_ASSERTION; com.l7tech.WorkingTest3.aar                      => ERROR
                            ModuleState.ERROR,    // module_6  => <NONE>   => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest2.jar    => ERROR
                            ModuleState.ERROR,    // module_7  => ACCEPTED => CUSTOM_ASSERTION;  com.l7tech.BrokenDescriptorTest1.jar (fail)      => ERROR
                            ModuleState.ERROR,    // module_8  => ACCEPTED => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail) => ERROR
                            ModuleState.ERROR,    // module_9  => REJECTED => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)          => ERROR
                            ModuleState.ERROR     // module_10 => LOADED   => MODULAR_ASSERTION; com.l7tech.WorkingTest4.aar                      => ERROR
                    },
                    Collections.<ServerModuleFile>emptyList()
            );
            // double-check that every state is in ERROR
            for (final ServerModuleFile moduleFile : moduleFiles.values()) {
                Assert.assertNotNull(modulesListener.getModuleState(moduleFile));
                Assert.assertNotNull(modulesListener.getModuleState(moduleFile).equals(ModuleState.ERROR));
            }
        } finally {
            // reset before exit the test
            Assert.assertTrue(stagingFolder.setWritable(true));
        }
    }

    /**
     * Convenient module builder callback for creating unsigned {@code ServerModuleFile}'s.
     */
    private final Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception> DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK = new Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception>() {
        @Override
        public ServerModuleFileBuilder call(final Long ordinal, final ModuleType moduleType, final File moduleContent) throws Exception {
            Assert.assertThat(ordinal, Matchers.notNullValue());
            return new ServerModuleFileBuilder(create_unsigned_test_module_without_states(ordinal, moduleType, moduleContent));
        }
    };

    /**
     * Convenient module builder callback for creating signed {@code ServerModuleFile}'s.<br/>
     * Default DN is SIGNER_CERT_DNS[0].
     */
    private final Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception> DEFAULT_SIGNED_MODULES_BUILDER_CALLBACK = new Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception>() {
        @Override
        public ServerModuleFileBuilder call(final Long ordinal, final ModuleType moduleType, final File moduleContent) throws Exception {
            Assert.assertThat(ordinal, Matchers.notNullValue());
            return new ServerModuleFileBuilder(create_and_sign_test_module_without_states(ordinal, moduleType, moduleContent, SIGNER_CERT_DNS[0]));
        }
    };

    /**
     * Convenient method for creating new ServerModuleFile entity, with the specified {@code ordinal},
     * then publishing a EntityInvalidationEvent(CREATE), and verifying the result.
     *
     * @param ordinal                           the ordinal of the server module file.
     * @param moduleType                        the module type.  Required and cannot be {@code null}.
     * @param initialState                      the module initial state upon creation, specify {@code null} for none.
     * @param moduleContent                     a {@code File} holding the content of the module. Required cannot be {@code null} and must exist.
     * @param expectedStateAfterLoad            expected state after module load. Required and cannot be {@code null}.
     * @param signatureVerificationCallback     a callback for verifying module signature.
     *                                          Throw {@link ServerModuleFileListener.ModuleRejectedException} to indicate module has been rejected.
     *                                          Throw {@link ServerModuleFileListener.ModuleSignatureException} to indicate an error while verifying module signature.
     *                                          Not throwing means the module is accepted.
     *                                          Specify {@code null} not to mock verifySignature
     * @param moduleCreateCallback              a callback for creating {@code ServerModuleFileBuilder}.
     */
    private void publishAndVerifyNewModuleFile(
            final long ordinal,
            final ModuleType moduleType,
            final ModuleState initialState,
            final File moduleContent,
            final ModuleState expectedStateAfterLoad,
            final Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException> signatureVerificationCallback,
            final Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception> moduleCreateCallback
    ) throws Exception {
        Assert.assertNotNull(moduleType);
        Assert.assertNotNull(moduleContent);
        Assert.assertTrue(moduleContent.exists());
        Assert.assertNotNull(expectedStateAfterLoad);
        Assert.assertNotNull(moduleCreateCallback);

        if (signatureVerificationCallback != null) {
            // simulate signature verification
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) throws Throwable {
                    Assert.assertNotNull(invocation);
                    Assert.assertEquals("there are two parameters for verifySignature", 2, invocation.getArguments().length);
                    final Object param1 = invocation.getArguments()[0];
                    Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                    final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                    Assert.assertNotNull(moduleFile);
                    // execute callback
                    signatureVerificationCallback.call(moduleFile);
                    // the rest are accepted
                    return null;
                }
            }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any(), Mockito.anyString());
        }

        // get the initial staged files count
        File[] files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        final int initialStagedFiles = files.length;
        final int initialModulesSize = moduleFiles.size();
        final int initialKnownModulesSize = modulesListener.knownModuleFiles.size();
        assertThat(initialModulesSize, equalTo(initialKnownModulesSize));

        final Goid goid = new Goid(GOID_HI_START, ordinal);
        Assert.assertNull(moduleFiles.get(goid)); // new module shouldn't be existing

        // create the new module file
        ServerModuleFile moduleFile = moduleCreateCallback.call(ordinal, moduleType, moduleContent)
                .addState(currentNodeId, initialState)
                .build();

        // create new module with the specified goid
        moduleFiles.put(goid, moduleFile);
        // send EntityInvalidationEvent, containing two events
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        new Goid[]{goid},
                                        new char[]{EntityInvalidationEvent.CREATE}
                                )
                        )
                )
        );

        // verify
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(initialKnownModulesSize + 1));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile module : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(module.getGoid()), notNullValue());
        }
        moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        assertThat(modulesListener.getModuleState(moduleFile), equalTo(expectedStateAfterLoad));
        // make sure module is staged (if applicable)
        files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        assertThat(files.length, ModuleState.LOADED.equals(expectedStateAfterLoad) ? equalTo(initialStagedFiles + 1) : Matchers.isOneOf(initialStagedFiles, initialStagedFiles + 1));
        if (ModuleState.LOADED.equals(expectedStateAfterLoad)) {
            final ServerModuleFileListener.StagedServerModuleFile stagedModuleFile = modulesListener.knownModuleFiles.get(goid);
            Assert.assertNotNull(stagedModuleFile);
            Assert.assertNotNull(stagedModuleFile.getStagingFile());
            Assert.assertTrue(stagedModuleFile.getStagingFile().exists());
            assertThat(stagedModuleFile.getStagingFile().getParentFile().getCanonicalPath(), equalTo(stagingFolder.getCanonicalPath()));
            // verify module name
            final ModulesScanner modulesScanner = ModuleType.MODULAR_ASSERTION.equals(moduleType) ? modularAssertionsScanner : customAssertionsScanner;
            final BaseAssertionModule module = modulesScanner.getModule(stagedModuleFile.getStagingFile().getName());
            Assert.assertNotNull(module);
            Assert.assertThat(module.getEntityName(), equalTo(moduleFile.getName()));
            if (ModuleType.CUSTOM_ASSERTION.equals(moduleType)) {
                Assert.assertTrue(module instanceof CustomAssertionModule);
                Assert.assertThat(((CustomAssertionModule) module).getDescriptors(), not(emptyCollectionOf(CustomAssertionDescriptor.class)));
                for (final CustomAssertionDescriptor descriptor : ((CustomAssertionModule)module).getDescriptors()) {
                    Assert.assertThat(descriptor.getModuleEntityName(), equalTo(moduleFile.getName()));
                }
            }
        }
    }

    /**
     * Convenient method for updating existing ServerModuleFile entity, with the specified {@code goid},
     * then publishing a EntityInvalidationEvent(UPDATE), and verifying the result.
     *
     * @param goid                              the module goid.
     * @param newName                           new entity name or {@code null} to ignore.
     * @param newModuleContent                  the updated module content or {@code null} to ignore.
     * @param expectedStateAfterLoad            expected ModuleState after load.  Required and cannot be {@code null}.
     * @param signatureVerificationCallback     in case when the module is not currently in loaded state, the module will be loaded; a callback for verifying module signature.
     *                                          Throw {@link ServerModuleFileListener.ModuleRejectedException} to indicate module has been rejected.
     *                                          Throw {@link ServerModuleFileListener.ModuleSignatureException} to indicate an error while verifying module signature.
     *                                          Not throwing means the module is accepted.
     * @param moduleCreateCallback              a callback for creating {@code ServerModuleFileBuilder}.
     * @throws Exception
     */
    private void publishAndVerifyUpdateModuleFile(
            final Goid goid,
            final String newName,
            final File newModuleContent,
            final ModuleState expectedStateAfterLoad,
            final Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException> signatureVerificationCallback,
            final Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception> moduleCreateCallback
    ) throws Exception {
        Assert.assertNotNull(goid);
        Assert.assertNotNull(expectedStateAfterLoad);
        Assert.assertNotNull(signatureVerificationCallback);
        Assert.assertTrue(newModuleContent == null || newModuleContent.exists());

        // simulate signature verification
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertNotNull(invocation);
                Assert.assertEquals("there are two parameters for verifySignature", 2, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                Assert.assertNotNull(moduleFile);
                // execute callback
                signatureVerificationCallback.call(moduleFile);
                // the rest are accepted
                return null;
            }
        }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any(), Mockito.anyString());

        // get current module state
        ServerModuleFile moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        final ModuleType moduleType = moduleFile.getModuleType();
        Assert.assertNotNull(moduleType);
        final ModuleState moduleState = modulesListener.getModuleState(moduleFile);
        Assert.assertNotNull(moduleState);
        if (newName != null) {
            moduleFile.setName(newName);
        }
        if (newModuleContent != null && moduleCreateCallback != null) {
            // create the new module file
            final ServerModuleFile newModuleFile = moduleCreateCallback.call(0L, moduleType, newModuleContent).build();
            newModuleFile.copyFrom(moduleFile, false, false, true);
            // replace the existing one
            final ServerModuleFile removed = moduleFiles.put(newModuleFile.getGoid(), newModuleFile);
            Assert.assertThat(removed, Matchers.sameInstance(moduleFile));
        }

        // get initial counts
        File[] files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        final int initialStagedFiles = files.length;
        final int initialModulesSize = moduleFiles.size();
        final int initialKnownModulesSize = modulesListener.knownModuleFiles.size();
        assertThat(initialModulesSize, equalTo(initialKnownModulesSize));

        // send EntityInvalidationEvent, containing two events
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        new Goid[]{goid},
                                        new char[]{EntityInvalidationEvent.UPDATE}
                                )
                        )
                )
        );

        // verify
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(initialKnownModulesSize));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile module : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(module.getGoid()), notNullValue());
        }
        moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        assertThat(modulesListener.getModuleState(moduleFile), equalTo(expectedStateAfterLoad));

        // make sure module is staged (if applicable)
        files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        if (newModuleContent == null && ModuleState.LOADED.equals(moduleState)) {
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(moduleState));
            assertThat(files.length, equalTo(initialStagedFiles));
            // check if name was successfully modified
            if (newName != null) {
                Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid));
                Assert.assertThat(modulesListener.knownModuleFiles.get(goid).getName(), equalTo(newName));
                // verify module name
                final ModulesScanner modulesScanner = ModuleType.MODULAR_ASSERTION.equals(moduleType) ? modularAssertionsScanner : customAssertionsScanner;
                final ServerModuleFileListener.StagedServerModuleFile stagedModuleFile = modulesListener.knownModuleFiles.get(goid);
                Assert.assertNotNull(stagedModuleFile);
                Assert.assertNotNull(stagedModuleFile.getStagingFile());
                final BaseAssertionModule module = modulesScanner.getModule(stagedModuleFile.getStagingFile().getName());
                Assert.assertNotNull(module);
                Assert.assertThat(module.getEntityName(), equalTo(newName));
                if (ModuleType.CUSTOM_ASSERTION.equals(moduleType)) {
                    Assert.assertTrue(module instanceof CustomAssertionModule);
                    Assert.assertThat(((CustomAssertionModule) module).getDescriptors(), not(emptyCollectionOf(CustomAssertionDescriptor.class)));
                    for (final CustomAssertionDescriptor descriptor : ((CustomAssertionModule)module).getDescriptors()) {
                        Assert.assertThat(descriptor.getModuleEntityName(), equalTo(moduleFile.getName()));
                    }
                }
            }
        } else {
            assertThat(files.length, ModuleState.LOADED.equals(expectedStateAfterLoad) ? Matchers.isOneOf(initialStagedFiles, initialStagedFiles + 1) : Matchers.isOneOf(initialStagedFiles, initialStagedFiles - 1));
            if (ModuleState.LOADED.equals(expectedStateAfterLoad)) {
                final ServerModuleFileListener.StagedServerModuleFile stagedModuleFile = modulesListener.knownModuleFiles.get(goid);
                Assert.assertNotNull(stagedModuleFile);
                Assert.assertNotNull(stagedModuleFile.getStagingFile());
                Assert.assertTrue(stagedModuleFile.getStagingFile().exists());
                assertThat(stagedModuleFile.getStagingFile().getParentFile().getCanonicalPath(), equalTo(stagingFolder.getCanonicalPath()));
                // verify module name
                final ModulesScanner modulesScanner = ModuleType.MODULAR_ASSERTION.equals(moduleType) ? modularAssertionsScanner : customAssertionsScanner;
                final BaseAssertionModule module = modulesScanner.getModule(stagedModuleFile.getStagingFile().getName());
                Assert.assertNotNull(module);
                Assert.assertThat(module.getEntityName(), equalTo(moduleFile.getName()));
                if (ModuleType.CUSTOM_ASSERTION.equals(moduleType)) {
                    Assert.assertTrue(module instanceof CustomAssertionModule);
                    Assert.assertThat(((CustomAssertionModule) module).getDescriptors(), not(emptyCollectionOf(CustomAssertionDescriptor.class)));
                    for (final CustomAssertionDescriptor descriptor : ((CustomAssertionModule)module).getDescriptors()) {
                        Assert.assertThat(descriptor.getModuleEntityName(), equalTo(moduleFile.getName()));
                    }
                }
                if (newModuleContent != null) {
                    Assert.assertNotNull(moduleFiles.get(goid));
                    final Pair<InputStream, String> bytesAndSignature = serverModuleFileManager.getModuleBytesAsStreamWithSignature(goid);
                    Assert.assertNotNull(bytesAndSignature);
                    Assert.assertNotNull(bytesAndSignature.left);
                    final byte[] bytes = IOUtils.slurpStream(bytesAndSignature.left);
                    Assert.assertTrue(bytes != null && bytes .length > 0);
                    Assert.assertTrue(Arrays.equals(bytes, IOUtils.slurpFile(newModuleContent)));
                }
            }
        }
    }

    /**
     * Convenient method for removing ServerModuleFile entity, with the specified {@code goid},
     * then publishing a EntityInvalidationEvent(DELETE), and verifying the result.
     *
     * @param goid   the module file goid to delete.
     */
    private void publishAndVerifyDeletedModuleFile(final Goid goid) throws Exception {
        Assert.assertNotNull(goid);

        final ServerModuleFile moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        final ModuleType moduleType = moduleFile.getModuleType();
        Assert.assertNotNull(moduleType);
        final ModuleState moduleState = modulesListener.getModuleState(moduleFile);
        Assert.assertNotNull(moduleState);

        // get initial counts
        File[] files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        final int initialStagedFiles = files.length;
        final int initialModulesSize = moduleFiles.size();
        final int initialKnownModulesSize = modulesListener.knownModuleFiles.size();
        assertThat(initialModulesSize, equalTo(initialKnownModulesSize));

        // get module name
        String stagedFileName = null;
        ModulesScanner modulesScanner = null;
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid));
        if (ModuleState.LOADED.equals(moduleState)) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid));
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid).getStagingFile());
            //noinspection ConstantConditions
            Assert.assertTrue(modulesListener.knownModuleFiles.get(goid).getStagingFile().exists());
            //noinspection ConstantConditions
            stagedFileName = modulesListener.knownModuleFiles.get(goid).getStagingFile().getName();
            modulesScanner = ModuleType.MODULAR_ASSERTION.equals(moduleType) ? modularAssertionsScanner : customAssertionsScanner;
            Assert.assertNotNull(modulesScanner.getModule(stagedFileName));
        }

        // remove the module with the specified goid
        Assert.assertNotNull("module to be deleted should exist", moduleFiles.remove(goid));
        // send EntityInvalidationEvent, containing two events
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        new Goid[]{goid},
                                        new char[]{EntityInvalidationEvent.DELETE}
                                )
                        )
                )
        );

        // verify
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(initialKnownModulesSize - 1));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        Assert.assertNull(modulesListener.knownModuleFiles.get(goid));
        for (final ServerModuleFile module : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(module.getGoid()), notNullValue());
        }
        assertThat(modulesListener.getModuleState(moduleFile), equalTo(moduleState)); // state not changed

        // verify the file has been removed everywhere
        files = stagingFolder.listFiles();
        Assert.assertNotNull(files);
        assertThat(files.length, Matchers.isOneOf(initialStagedFiles, initialStagedFiles - 1));
        // make sure module was deleted from the scanner as well
        //noinspection ConstantConditions
        if (modulesScanner != null && stagedFileName != null) {
            Assert.assertNull(modulesScanner.getModule(stagedFileName));
        }
    }

    /**
     * Convenient method for sending {@code LicenseChangeEvent} event and verifying the result.
     *
     * @param expectedStates    a list of expected states after the event is published.  Required and cannot be {@code null}.
     */
    private void publishAndVerifyLicenseChange(final ModuleState[] expectedStates) throws Exception {
        Assert.assertNotNull(expectedStates);

        // simulate signature verification
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertNotNull(invocation);
                Assert.assertEquals("there are two parameters for verifySignature", 2, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                Assert.assertNotNull(moduleFile);
                // the rest are accepted
                return null;
            }
        }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any(), Mockito.anyString());

        // make sure they are the same size before
        assertThat(moduleFiles.size(), equalTo(modulesListener.knownModuleFiles.size()));

        // send license change
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new LicenseChangeEvent(this, Level.ALL, "license action", "license message")
                        )
                )
        );

        // verify
        assertThat(moduleFiles.size(), equalTo(modulesListener.knownModuleFiles.size()));
        verifyModulesState(expectedStates);
    }

    @Test
    public void test_entity_crud() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2), // file: com.l7tech.WorkingTest1.aar
                        new Goid(GOID_HI_START, 5)  // file: com.l7tech.WorkingTest3.aar
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3)  // file: com.l7tech.DualAssertionsTest1.jar
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accept nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // update module_5; com.l7tech.WorkingTest3.aar; was REJECTED, should be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 5),
                null,
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 5)));
                        // accept; nothing to do
                    }
                },
                null
        );

        // update module_2; com.l7tech.WorkingTest1.aar; was REJECTED, should be loaded again => ERROR (as signature will fail i.e. throw ModuleSignatureException)
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 2),
                null,
                null,
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 2)));
                        throw new ServerModuleFileListener.ModuleSignatureException("signature error");
                    }
                },
                null
        );

        // update module_3; com.l7tech.DualAssertionsTest1.jar; was ERROR, should be loaded again => REJECTED (as signature was rejected i.e. throw ModuleRejectedException)
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 3),
                null,
                null,
                ModuleState.REJECTED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 3)));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                },
                null
        );

        // update module_100; com.l7tech.DynamicCustomAssertionsTest5.jar; was LOADED, should not be loaded again => remains LOADED (name will change though)
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 100),
                "new name for 100",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accept; nothing to do
                    }
                },
                null
        );

        // remove module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 100));
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));

        // verify states
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.ERROR,    // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => ERROR
                        ModuleState.REJECTED, // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => REJECTED
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.LOADED,   // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // do other crud work
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest2.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                ModuleState.UPLOADED,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest2.jar"),
                ModuleState.REJECTED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_101; com.l7tech.WorkingTest1NewVersion.aar; (new version of module_2; com.l7tech.WorkingTest1.aar)
        // LOADED as module_2 is not currently loaded i.e. its ERROR
        publishAndVerifyNewModuleFile(
                101,
                ModuleType.MODULAR_ASSERTION,
                null,
                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest1NewVersion.aar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 101)));
                        // accept; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );


        // remove module_100; com.l7tech.DynamicCustomAssertionsTest2.jar; REJECTED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 100));
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));

        // remove module_101; com.l7tech.WorkingTest1NewVersion.aar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 101));
        // double-check module_101 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));

        // remove module_1; com.l7tech.DynamicCustomAssertionsTest1.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 1));
        // double-check module_1 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)));

        // remove module_2; com.l7tech.WorkingTest1.aar; ERROR
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 2));
        // double-check module_2 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 2)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 2)));

        // remove module_0; com.l7tech.NonDynamicCustomAssertionTest1.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 0));
        // double-check module_0 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 0)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)));

        // remove module_9; com.l7tech.NoAssertionsTest1.aar (fail); ERROR
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 9));
        // double-check module_9 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 9)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 9)));

        // remove module_3; com.l7tech.DualAssertionsTest1.jar; REJECTED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 3));
        // double-check module_3 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 3)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 3)));

        // remove module_10; com.l7tech.WorkingTest4.aar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 10));
        // double-check module_10 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 10)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)));

        // remaining modules
        // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
        // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
        // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
        // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
        // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final Goid goid : moduleFiles.keySet()) {
            assertThat(
                    "Only modules (4, 5, 6, 7 and 8) should be remaining in the repository",
                    goid,
                    isOneOf(
                            new Goid(GOID_HI_START, 4),
                            new Goid(GOID_HI_START, 5),
                            new Goid(GOID_HI_START, 6),
                            new Goid(GOID_HI_START, 7),
                            new Goid(GOID_HI_START, 8)
                    )
            );
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid));
        }


        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest4.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                ModuleState.UPLOADED,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                ModuleState.REJECTED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        final Goid goid100 = new Goid(GOID_HI_START, 100);
        assertThat("module_100", equalTo(moduleFiles.get(goid100).getName()));

        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was REJECTED, should be loaded again => ERROR (as signature will fail i.e. throw ModuleSignatureException)
        publishAndVerifyUpdateModuleFile(
                goid100,
                "load with new name for module 100",
                null,
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        throw new ServerModuleFileListener.ModuleSignatureException("signature error");
                    }
                },
                null
        );
        assertThat("load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));

        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was ERROR, should be loaded again => REJECTED (as signature will fail i.e. throw ModuleRejectedException)
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new load with new name for module 100",
                null,
                ModuleState.REJECTED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                },
                null
        );
        assertThat("new load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));


        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was REJECTED, should be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new new load with new name for module 100",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        // accept; nothing to do
                    }
                },
                null
        );
        assertThat("new new load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));

        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new new new load with new name for module 100",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                },
                null
        );
        assertThat("new new new load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));


        // remove module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 100));
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));


        // add new entity module_200; com.l7tech.WorkingTest5.aar;
        publishAndVerifyNewModuleFile(
                200,
                ModuleType.MODULAR_ASSERTION,
                ModuleState.UPLOADED,
                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );
        assertThat("module_200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));

        // update module_200; com.l7tech.WorkingTest5.aar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 200),
                "new name for module 200",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                },
                null
        );
        assertThat("new name for module 200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));

        // update module_200; com.l7tech.WorkingTest5.aar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 200),
                "new new name for module 200",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        throw new ServerModuleFileListener.ModuleSignatureException("signature error");
                    }
                },
                null
        );
        assertThat("new new name for module 200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));

        // update module_200; com.l7tech.WorkingTest5.aar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 200),
                "new new new name for module 200",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        // accepted; nothing to do
                    }
                },
                null
        );
        assertThat("new new new name for module 200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));


        // remove module_200; com.l7tech.WorkingTest5.aar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 200));
        // double-check module_200 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 200)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 200)));


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // remove the remaining modules:
        // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
        // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
        // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
        // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
        // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // remove module_4; com.l7tech.WorkingTest2.aar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 4));
        // double-check module_4 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 4)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)));

        // remove module_5; com.l7tech.WorkingTest3.aar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 5));
        // double-check module_5 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 5)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 5)));

        // remove module_6; com.l7tech.NonDynamicCustomAssertionTest2.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 6));
        // double-check module_6 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 6)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 6)));

        // remove module_7; com.l7tech.BrokenDescriptorTest1.jar (fail); ERROR
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 7));
        // double-check module_7 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 7)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 7)));

        // remove module_8; com.l7tech.InvalidAssertionClassTest1.aar (fail); ERROR
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 8));
        // double-check module_8 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 8)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 8)));

        // make sure there are no more modules in the repository
        assertThat(modulesListener.knownModuleFiles.values(), emptyCollectionOf(ServerModuleFileListener.StagedServerModuleFile.class));
        assertThat(moduleFiles.values(), emptyCollectionOf(ServerModuleFile.class));
        assertThat(modularAssertionsScanner.getModules(), emptyCollectionOf(ModularAssertionModule.class));
        assertThat(customAssertionsScanner.getModules(), emptyCollectionOf(CustomAssertionModule.class));
    }

    @Test
    public void test_duplicate_modules() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // copy two non-dynamic custom assertions
        copy_all_files(
                customDeployFolder,
                1,
                new CopyData[]{
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest4.jar"),
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest5.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar")
                }
        );

        // initial scan for empty deploy folders
        do_scanner_run(
                ArrayUtils.EMPTY_STRING_ARRAY,
                new String[]{
                        "com.l7tech.NonDynamicCustomAssertionTest4.jar",
                        "com.l7tech.NonDynamicCustomAssertionTest5.jar",
                        "com.l7tech.DynamicCustomAssertionsTest4.jar",
                        "com.l7tech.DynamicCustomAssertionsTest5.jar"
                }
        );

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2), // file: com.l7tech.WorkingTest1.aar
                        new Goid(GOID_HI_START, 5)  // file: com.l7tech.WorkingTest3.aar
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3)  // file: com.l7tech.DualAssertionsTest1.jar
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest4.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_101; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyNewModuleFile(
                101,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 101)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_102; com.l7tech.NonDynamicCustomAssertionTest4.jar;
        publishAndVerifyNewModuleFile(
                102,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest4.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 102)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_103; com.l7tech.NonDynamicCustomAssertionTest5.jar;
        publishAndVerifyNewModuleFile(
                103,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 103)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_104; com.l7tech.NonDynamicCustomAssertionTest1.jar;
        publishAndVerifyNewModuleFile(
                104,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest1.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 104)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_105; com.l7tech.DynamicCustomAssertionsTest1.jar;
        publishAndVerifyNewModuleFile(
                105,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest1.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 105)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_106; com.l7tech.WorkingTest2.aar;
        publishAndVerifyNewModuleFile(
                106,
                ModuleType.MODULAR_ASSERTION,
                null,
                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 106)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_107; com.l7tech.NonDynamicCustomAssertionTest2.jar
        publishAndVerifyNewModuleFile(
                107,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest2.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 107)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // add new entity module_108; com.l7tech.WorkingTest4.aar
        publishAndVerifyNewModuleFile(
                108,
                ModuleType.MODULAR_ASSERTION,
                null,
                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 108)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );
    }

    @BugId("SSG-11132")
    @Test
    public void test_ensure_non_dynamic_custom_assertion_modules_are_loaded_only_once() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // copy two non-dynamic custom assertions
        copy_all_files(
                customDeployFolder,
                1,
                new CopyData[]{
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest4.jar"),
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest5.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar")
                }
        );

        // initial scan for empty deploy folders
        do_scanner_run(
                ArrayUtils.EMPTY_STRING_ARRAY,
                new String[]{
                        "com.l7tech.NonDynamicCustomAssertionTest4.jar",
                        "com.l7tech.NonDynamicCustomAssertionTest5.jar",
                        "com.l7tech.DynamicCustomAssertionsTest4.jar",
                        "com.l7tech.DynamicCustomAssertionsTest5.jar"
                }
        );

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2), // file: com.l7tech.WorkingTest1.aar
                        new Goid(GOID_HI_START, 5)  // file: com.l7tech.WorkingTest3.aar
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3)  // file: com.l7tech.DualAssertionsTest1.jar
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test with non-dynamic loadable => should not be able to reload the module
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // remove module_6; com.l7tech.NonDynamicCustomAssertionTest2.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 6));
        // double-check module_6 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 6)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 6)));

        // add new entity module_100; with the deleted non-dynamic custom assertion; com.l7tech.NonDynamicCustomAssertionTest2.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest2.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test again with dynamic loadable => should be able to reload the module
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // remove module_1; com.l7tech.DynamicCustomAssertionsTest1.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 1));
        // double-check module_1 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)));

        // add new entity module_101; with the deleted dynamic custom assertion; com.l7tech.DynamicCustomAssertionsTest1.jar;
        publishAndVerifyNewModuleFile(
                101,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest1.jar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 101)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // loading com.l7tech.DynamicCustomAssertionsTest1.jar again should fail
        publishAndVerifyNewModuleFile(
                102,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest1.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 102)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test with non-dynamic modules already deployed
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // loading com.l7tech.NonDynamicCustomAssertionTest4.jar should fail since its already deployed
        publishAndVerifyNewModuleFile(
                103,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest4.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 103)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // loading com.l7tech.NonDynamicCustomAssertionTest5.jar should fail since its already deployed
        publishAndVerifyNewModuleFile(
                104,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 104)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // unload non-dynamic modules using the workaround (working on both windows and linux)
        copy_all_files(
                customDeployFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                nonDynamicModulesEmptyDir,
                                "com.l7tech.NonDynamicCustomAssertionTest4.jar",
                                "com.l7tech.NonDynamicCustomAssertionTest4.jar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                nonDynamicModulesEmptyDir,
                                "com.l7tech.NonDynamicCustomAssertionTest5.jar",
                                "com.l7tech.NonDynamicCustomAssertionTest5.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );
        // server module files loaded
        // module_0    => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
        // module_101  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
        // module_4    => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
        // module_10   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
        // do scan modules
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile().exists());
        //noinspection ConstantConditions
        do_scanner_run(
                new String[] {
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile().getName(),
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile().getName()
                },
                new String[]{
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile().getName(),
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile().getName(),
                        "com.l7tech.DynamicCustomAssertionsTest4.jar",
                        "com.l7tech.DynamicCustomAssertionsTest5.jar"
                }
        );

        // loading com.l7tech.NonDynamicCustomAssertionTest4.jar should fail since its already deployed
        publishAndVerifyNewModuleFile(
                105,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest4.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 105)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // loading com.l7tech.NonDynamicCustomAssertionTest5.jar should fail since its already deployed
        publishAndVerifyNewModuleFile(
                106,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 106)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test with dynamic modules already deployed
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // loading com.l7tech.DynamicCustomAssertionsTest4.jar should fail since its already deployed
        publishAndVerifyNewModuleFile(
                107,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 107)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // loading com.l7tech.DynamicCustomAssertionsTest5.jar should fail since its already deployed
        publishAndVerifyNewModuleFile(
                108,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 108)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // unload non-dynamic modules using the workaround (working on both windows and linux)
        copy_all_files(
                customDeployFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                dynamicModulesEmptyDir,
                                "com.l7tech.DynamicCustomAssertionsTest4.jar",
                                "com.l7tech.DynamicCustomAssertionsTest4.jar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                dynamicModulesEmptyDir,
                                "com.l7tech.DynamicCustomAssertionsTest5.jar",
                                "com.l7tech.DynamicCustomAssertionsTest5.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );
        // do scan modules
        // server module files loaded
        // module_0    => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
        // module_101  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
        // module_4    => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
        // module_10   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile().exists());
        // do scan modules
        //noinspection ConstantConditions
        do_scanner_run(
                new String[] {
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile().getName(),
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile().getName()
                },
                new String[]{
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile().getName(),
                        modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile().getName(),
                }
        );

        // loading com.l7tech.DynamicCustomAssertionsTest4.jar should succeed since its was removed from deploy folder
        publishAndVerifyNewModuleFile(
                109,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 109)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

        // loading com.l7tech.DynamicCustomAssertionsTest5.jar should succeed since its was removed from deploy folder
        publishAndVerifyNewModuleFile(
                110,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 110)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );

    }

    @BugId("SSG-11149")
    @Test
    public void test_runtime_exception_while_loading_module() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2), // file: com.l7tech.WorkingTest1.aar
                        new Goid(GOID_HI_START, 5)  // file: com.l7tech.WorkingTest3.aar
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3)  // file: com.l7tech.DualAssertionsTest1.jar
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        throw new RuntimeException("test runtime");
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
         );
    }

    @BugId("SSG-11161")
    @Test
    public void test_delete_after_update() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2), // file: com.l7tech.WorkingTest1.aar
                        new Goid(GOID_HI_START, 5)  // file: com.l7tech.WorkingTest3.aar
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3)  // file: com.l7tech.DualAssertionsTest1.jar
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        // update module_1; CUSTOM_ASSERTION; com.l7tech.DynamicCustomAssertionsTest1.jar; LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 1),
                "new name for module 1",
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accepted; nothing to do
                    }
                },
                null
        );

        // verify the module still exists in both listener and scanner
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)).getStagingFile());
        //noinspection ConstantConditions
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)).getStagingFile().exists());
        //noinspection ConstantConditions
        final String stagedFileName = modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)).getStagingFile().getName();
        Assert.assertNotNull(customAssertionsScanner.getModule(stagedFileName));

        // remove module_1; com.l7tech.DynamicCustomAssertionsTest1.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 1));
        // double-check module_1 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNull(customAssertionsScanner.getModule(stagedFileName));
    }

    @BugId("SSG-11352")
    @Test
    public void test_failed_to_download_from_db() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(
                        new Goid(GOID_HI_START, 2), // file: com.l7tech.WorkingTest1.aar
                        new Goid(GOID_HI_START, 5)  // file: com.l7tech.WorkingTest3.aar
                ),
                Arrays.asList(
                        new Goid(GOID_HI_START, 3)  // file: com.l7tech.DualAssertionsTest1.jar
                )
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.REJECTED, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => REJECTED
                        ModuleState.ERROR,    // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => REJECTED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );

        // mock to return a stream throwing java.io.IOException
        Mockito.doAnswer(
                new Answer<Pair<InputStream, String>>() {
                    @Override
                    public Pair<InputStream, String> answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is only one parameter for getModuleBytesAsStream", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param is Goid", param1 instanceof Goid);
                        final Goid goid = (Goid) param1;
                        Assert.assertNotNull(goid);
                        Assert.assertThat(goid, Matchers.is(new Goid(GOID_HI_START, 100)));

                        final ServerModuleFile moduleFile = moduleFiles.get(goid);
                        if (moduleFile != null) {
                            return Pair.<InputStream, String>pair(new IOExceptionThrowingInputStream(new IOException("simulated database I/O error")), null);
                        }
                        return null;
                    }
                }
        ).when(serverModuleFileManager).getModuleBytesAsStreamWithSignature(Mockito.<Goid>any());

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accept nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));
        // remove module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 100));
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));

        // mock to return null
        Mockito.doAnswer(
                new Answer<Pair<InputStream, String>>() {
                    @Override
                    public Pair<InputStream, String> answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is only one parameter for getModuleBytesAsStream", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param is Goid", param1 instanceof Goid);
                        final Goid goid = (Goid) param1;
                        Assert.assertNotNull(goid);
                        Assert.assertThat(goid, Matchers.is(new Goid(GOID_HI_START, 200)));
                        Assert.assertNotNull(moduleFiles.get(goid));
                        return null;
                    }
                }
        ).when(serverModuleFileManager).getModuleBytesAsStreamWithSignature(Mockito.<Goid>any());

        // add new entity module_200; com.l7tech.WorkingTest5.aar;
        publishAndVerifyNewModuleFile(
                200,
                ModuleType.MODULAR_ASSERTION,
                null,
                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        // accepted; nothing to do
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 200)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 200)));
        // remove module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 200));
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 200)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 200)));
    }

    @Test
    public void test_modules_signature() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // modules initial states:
        // module_0  => <NONE> => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar      => SIGNED with SIGNER_CERT_DNS[0]
        // module_1  => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar        => SIGNED with SIGNER_CERT_DNS[1]
        // module_2  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                        => SIGNED with SIGNER_CERT_DNS[2]
        // module_3  => <NONE> => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                 => SIGNED with SIGNER_CERT_DNS[0]
        // module_4  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                        => SIGNED with SIGNER_CERT_DNS[0]
        // module_5  => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                        => SIGNATURE ERROR: DATA BYTES TAMPERED WITH
        // module_6  => <NONE> => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar      => UNSIGNED
        // module_7  => <NONE> => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)        => SIGNED with SIGNER_CERT_DNS[3]
        // module_8  => <NONE> => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)   => SIGNED with SIGNER_CERT_DNS[3]
        // module_9  => <NONE> => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)            => SIGNED with SIGNER_CERT_DNS[3]
        // module_10 => <NONE> => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                        => SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts
        // module_11 => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest2.jar        => SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts
        // module_12 => <NONE> => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest3.jar        => SIGNATURE ERROR: DATA BYTES TAMPERED WITH

        // calculate expected loaded based on rejectedModules and signatureErrorModules
        final Collection<ServerModuleFile> expectedLoaded = new ArrayList<>();
        for (final ServerModuleFile serverModuleFile : moduleFiles.values()) {
            Assert.assertNotNull(serverModuleFile.getGoid());
            if (!signedRejectedModules.contains(serverModuleFile.getGoid()) && !failedModules.contains(serverModuleFile.getGoid()) && !unlicensedModules.contains(serverModuleFile.getGoid())) {
                expectedLoaded.add(serverModuleFile);
            }
        }

        // we have:
        publishInitialStartedEventAndVerifyResult(
                new ModuleState[]{
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 0))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 0)) || unlicensedModules.contains(new Goid(GOID_HI_START, 0)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_0  => <NONE> => CUSTOM_ASSERTION; com.l7tech.NonDynamicCustomAssertionTest1.jar      => SIGNED with SIGNER_CERT_DNS[0]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 1))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 1)) || unlicensedModules.contains(new Goid(GOID_HI_START, 1)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_1  => <NONE> => CUSTOM_ASSERTION; com.l7tech.DynamicCustomAssertionsTest1.jar        => SIGNED with SIGNER_CERT_DNS[1]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 2))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 2)) || unlicensedModules.contains(new Goid(GOID_HI_START, 2)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_2  => <NONE> => MODULAR_ASSERTION; com.l7tech.WorkingTest1.aar                       => SIGNED with SIGNER_CERT_DNS[2]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 3))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 3)) || unlicensedModules.contains(new Goid(GOID_HI_START, 3)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  //module_3  => <NONE> => CUSTOM_ASSERTION; com.l7tech.DualAssertionsTest1.jar                  => SIGNED with SIGNER_CERT_DNS[0]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 4))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 4)) || unlicensedModules.contains(new Goid(GOID_HI_START, 4)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_4  => <NONE> => MODULAR_ASSERTION; com.l7tech.WorkingTest2.aar                       => SIGNED with SIGNER_CERT_DNS[0]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 5))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 5)) || unlicensedModules.contains(new Goid(GOID_HI_START, 5)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_5  => <NONE> => MODULAR_ASSERTION; com.l7tech.WorkingTest3.aar                       => SIGNATURE ERROR: DATA BYTES TAMPERED WITH  => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 6))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 6)) || unlicensedModules.contains(new Goid(GOID_HI_START, 6)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_6  => <NONE> => CUSTOM_ASSERTION; com.l7tech.NonDynamicCustomAssertionTest2.jar      => UNSIGNED                                   => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 7))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 7)) || unlicensedModules.contains(new Goid(GOID_HI_START, 7)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_7  => <NONE> => CUSTOM_ASSERTION; com.l7tech.BrokenDescriptorTest1.jar (fail)        => SIGNED with SIGNER_CERT_DNS[3]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 8))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 8)) || unlicensedModules.contains(new Goid(GOID_HI_START, 8)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_8  => <NONE> => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail)  => SIGNED with SIGNER_CERT_DNS[3]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 9))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 9)) || unlicensedModules.contains(new Goid(GOID_HI_START, 9)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_9  => <NONE> => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)           => SIGNED with SIGNER_CERT_DNS[3]             => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 10))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 10)) || unlicensedModules.contains(new Goid(GOID_HI_START, 10)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_10 => <NONE> => MODULAR_ASSERTION; com.l7tech.WorkingTest4.aar                       => SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts    => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 11))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 11)) || unlicensedModules.contains(new Goid(GOID_HI_START, 11)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED,  // module_11 => <NONE> => CUSTOM_ASSERTION; com.l7tech.DynamicCustomAssertionsTest2.jar        => SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts    => REJECTED/ERROR/LOADED
                        signedRejectedModules.contains(new Goid(GOID_HI_START, 12))
                                ? ModuleState.REJECTED
                                : (failedModules.contains(new Goid(GOID_HI_START, 12)) || unlicensedModules.contains(new Goid(GOID_HI_START, 12)))
                                ? ModuleState.ERROR
                                : ModuleState.LOADED   // module_12 => <NONE> => CUSTOM_ASSERTION; com.l7tech.DynamicCustomAssertionsTest3.jar        => SIGNATURE ERROR: DATA BYTES TAMPERED WITH   => REJECTED/ERROR/LOADED
                },
                expectedLoaded
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest1.jar    SIGNED with SIGNER_CERT_DNS[0]            => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION;  com.l7tech.DynamicCustomAssertionsTest1.jar      SIGNED with SIGNER_CERT_DNS[1]            => LOADED
                        ModuleState.LOADED,   // module_2  => MODULAR_ASSERTION; com.l7tech.WorkingTest1.aar                      SIGNED with SIGNER_CERT_DNS[2]            => LOADED
                        ModuleState.LOADED,   // module_3  => CUSTOM_ASSERTION;  com.l7tech.DualAssertionsTest1.jar               SIGNED with SIGNER_CERT_DNS[0]            => LOADED
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION; com.l7tech.WorkingTest2.aar                      SIGNED with SIGNER_CERT_DNS[0]            => LOADED
                        ModuleState.REJECTED, // module_5  => MODULAR_ASSERTION; com.l7tech.WorkingTest3.aar                      SIGNATURE ERROR: DATA BYTES TAMPERED WITH => REJECTED
                        ModuleState.REJECTED, // module_6  => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest2.jar    UNSIGNED                                  => REJECTED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION;  com.l7tech.BrokenDescriptorTest1.jar (fail)      SIGNED with SIGNER_CERT_DNS[3]            => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail) SIGNED with SIGNER_CERT_DNS[3]            => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)          SIGNED with SIGNER_CERT_DNS[3]            => ERROR
                        ModuleState.REJECTED, // module_10 => MODULAR_ASSERTION; com.l7tech.WorkingTest4.aar                      SIGNATURE ERROR: UNTRUSTED SIGNER: SIGNER_CERT_DNS[1] from untrustedCerts          => REJECTED
                        ModuleState.REJECTED, // module_11 => CUSTOM_ASSERTION;  com.l7tech.DynamicCustomAssertionsTest2.aar      SIGNATURE ERROR: UNTRUSTED SIGNER: untrustedSignerCertDns[0] from untrustedCerts   => REJECTED
                        ModuleState.REJECTED  // module_12 => CUSTOM_ASSERTION;  com.l7tech.DynamicCustomAssertionsTest3.aar      SIGNATURE ERROR: DATA BYTES TAMPERED WITH => REJECTED
                }
        );

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;  SIGNED with SIGNER_CERT_DNS[0] => LOADED
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.LOADED,
                null,
                DEFAULT_SIGNED_MODULES_BUILDER_CALLBACK // SIGNED with SIGNER_CERT_DNS[0]
        );
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));
        // remove module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 100));
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 100)));

        // add new entity module_101; com.l7tech.WorkingTest5.aar;  TAMPER WITH BYTES => REJECTED
        publishAndVerifyNewModuleFile(
                101,
                ModuleType.MODULAR_ASSERTION,
                null,
                new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar"),
                ModuleState.REJECTED,
                null,
                new Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception>() {
                    @Override
                    public ServerModuleFileBuilder call(final Long ordinal, final ModuleType moduleType, final File moduleContent) throws Exception {
                        Assert.assertThat(ordinal, Matchers.notNullValue());
                        return new ServerModuleFileBuilder(
                                tamper_with_module_bytes(
                                        ordinal,
                                        moduleType,
                                        moduleContent,
                                        new File(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar"),
                                        SIGNER_CERT_DNS[2],
                                        false
                                )
                        );
                    }
                }
        );
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));
        // remove module_101; com.l7tech.WorkingTest5.aar;
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 101));
        // double-check module_101 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));


        // add new entity module_102; com.l7tech.NonDynamicCustomAssertionTest5.jar;  UNTRUSTED SIGNER => REJECTED
        publishAndVerifyNewModuleFile(
                102,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest5.jar"),
                ModuleState.REJECTED,
                null,
                new Functions.TernaryThrows<ServerModuleFileBuilder, Long, ModuleType, File, Exception>() {
                    @Override
                    public ServerModuleFileBuilder call(final Long ordinal, final ModuleType moduleType, final File moduleContent) throws Exception {
                        Assert.assertThat(ordinal, Matchers.notNullValue());
                        return new ServerModuleFileBuilder(
                                sign_with_untrusted_signer(
                                        ordinal,
                                        moduleType,
                                        moduleContent,
                                        SIGNER_CERT_DNS[1]
                                )
                        );
                    }
                }
        );
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 102)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 102)));
        // remove module_102; com.l7tech.NonDynamicCustomAssertionTest5.jar;
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 102));
        // double-check module_102 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 102)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 102)));
    }

    @Test
    public void test_license_change() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // callback for loadServerModuleFile for both modular and custom assertions
        // simply throw ModuleLoadingException if module is in the unlicensedModules list
        final Functions.UnaryVoidThrows<InvocationOnMock, ModuleLoadingException> loadServerModuleFileCallback = new Functions.UnaryVoidThrows<InvocationOnMock, ModuleLoadingException>() {
            @Override
            public void call(final InvocationOnMock invocation) throws ModuleLoadingException {
                Assert.assertEquals("three parameters for loadServerModuleFile", 3, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param1 is File", param1 instanceof File);
                final Object param2 = invocation.getArguments()[2];
                Assert.assertTrue("Param2 is String", param2 instanceof String);
                final Object param3 = invocation.getArguments()[2];
                Assert.assertTrue("Param3 is String", param3 instanceof String);
                final String entityName = (String) param3;
                Assert.assertThat(entityName, Matchers.not(Matchers.isEmptyOrNullString()));

                // locate module with the specified entity name
                boolean found = false;
                for (final ServerModuleFile moduleFile : moduleFiles.values()) {
                    if (entityName.equals(moduleFile.getName())) {
                        found = true;
                        if (unlicensedModules.contains(moduleFile.getGoid())) {
                            throw new ModuleLoadingException("module [" + entityName + "] is not licensed.");
                        }
                    }
                }
                Assert.assertTrue(found);

                // if not thrown call real method
                try {
                    invocation.callRealMethod();
                } catch (ModuleLoadingException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // mock modular assertions loadServerModuleFile
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                loadServerModuleFileCallback.call(invocation);
                return null;
            }
        }).when(modularAssertionsScanner).loadServerModuleFile(Mockito.<File>any(), Mockito.<String>any(), Mockito.<String>any());
        // mock custom assertions loadServerModuleFile
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                loadServerModuleFileCallback.call(invocation);
                return null;
            }
        }).when(customAssertionsScanner).loadServerModuleFile(Mockito.<File>any(), Mockito.<String>any(), Mockito.<String>any());

        // make all modules unlicensed
        unlicensedModules.addAll(moduleFiles.keySet());
        Assert.assertThat(unlicensedModules.size(), Matchers.equalTo(moduleFiles.size()));

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(Collections.<Goid>emptyList(), Collections.<Goid>emptyList());

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.ERROR, // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
                        ModuleState.ERROR, // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => ERROR
                        ModuleState.ERROR, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => ERROR
                        ModuleState.ERROR, // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.ERROR, // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => ERROR
                        ModuleState.ERROR, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => ERROR
                        ModuleState.ERROR, // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => ERROR
                        ModuleState.ERROR, // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR, // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR, // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.ERROR  // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => ERROR
                }
        );

        // send license change
        publishAndVerifyLicenseChange(
                new ModuleState[]{
                        ModuleState.ERROR, // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
                        ModuleState.ERROR, // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => ERROR
                        ModuleState.ERROR, // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => ERROR
                        ModuleState.ERROR, // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.ERROR, // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => ERROR
                        ModuleState.ERROR, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => ERROR
                        ModuleState.ERROR, // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => ERROR
                        ModuleState.ERROR, // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR, // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR, // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.ERROR  // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => ERROR
                }
        );

        // remove module_1 from unlicensed
        unlicensedModules.remove(new Goid(GOID_HI_START, 1));
        unlicensedModules.remove(new Goid(GOID_HI_START, 5));
        unlicensedModules.remove(new Goid(GOID_HI_START, 8));

        // send license change
        // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
        // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
        // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => ERROR
        // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
        // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => ERROR
        // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
        // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => ERROR
        // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
        // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
        // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
        // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => ERROR
        publishAndVerifyLicenseChange(
                new ModuleState[]{
                        ModuleState.ERROR,  // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
                        ModuleState.LOADED, // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.ERROR,  // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => ERROR
                        ModuleState.ERROR,  // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => ERROR
                        ModuleState.ERROR,  // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => ERROR
                        ModuleState.LOADED, // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
                        ModuleState.ERROR,  // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => ERROR
                        ModuleState.ERROR,  // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,  // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,  // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.ERROR   // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => ERROR
                }
        );

        final Goid goid100 = new Goid(GOID_HI_START, 100);

        // add new entity module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyNewModuleFile(
                100,
                ModuleType.CUSTOM_ASSERTION,
                null,
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        // accepted
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );
        Assert.assertNotNull(moduleFiles.get(goid100));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid100));

        // next try to update the module name and content
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new module 100",
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest4.jar"),
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        // accepted
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );
        Assert.assertNotNull(moduleFiles.get(goid100));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid100));

        // mark module_100 as unlicensed
        unlicensedModules.add(goid100);

        // try to update the module name and content again, now with module_100 being unlicensed
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new new module 100",
                new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest5.jar"),
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        // accepted
                    }
                },
                DEFAULT_UNSIGNED_MODULES_BUILDER_CALLBACK
        );
        Assert.assertNotNull(moduleFiles.get(goid100));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid100));

        // remove module_100; com.l7tech.DynamicCustomAssertionsTest5.jar;
        publishAndVerifyDeletedModuleFile(goid100);
        // double-check module_100 was deleted
        Assert.assertNull(moduleFiles.get(goid100));
        Assert.assertNull(modulesListener.knownModuleFiles.get(goid100));

        // clear any unlicensed modules
        unlicensedModules.clear();

        // send license change
        publishAndVerifyLicenseChange(
                new ModuleState[]{
                        ModuleState.LOADED,   // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => LOADED
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.LOADED,   // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => LOADED
                        ModuleState.LOADED,   // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => LOADED
                        ModuleState.LOADED,   // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => LOADED
                        ModuleState.LOADED,   // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );
    }

    @Test
    public void test_process_entity_invalidation_events() throws Exception {
        Assert.assertNotNull(modulesListener);
        createUnsignedSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // do initial test
        // modules initial states:
        // module_0  => REJECTED => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar
        // module_1  => UPLOADED => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar
        // module_2  => ERROR    => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar
        // module_3  => LOADED   => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar
        // module_4  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar
        // module_5  => <NONE>   => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar
        // module_6  => <NONE>   => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar
        // module_7  => ACCEPTED => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)
        // module_8  => ACCEPTED => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)
        // module_9  => REJECTED => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)
        // module_10 => LOADED   => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar
        do_test_started_event(
                Arrays.asList(new Goid(GOID_HI_START, 4)), // module_4 REJECTED
                Arrays.asList(new Goid(GOID_HI_START, 0))  // module_0 ERROR
        );

        // expected states after the started event is processed:
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.ERROR,    // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.LOADED,   // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => LOADED
                        ModuleState.LOADED,   // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => LOADED
                        ModuleState.REJECTED, // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => REJECTED
                        ModuleState.LOADED,   // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test EntityInvalidationEvent
        // send UPDATE for all LOADED modules (1, 2, 3, 5, 6, 10)
        // since no changes were actually done, i.e. moduleFiles (mimicking our DB) is unchanged, callbacks shouldn't be called
        final AtomicInteger countLoad = new AtomicInteger(0), countUnload = new AtomicInteger(0), countUpdate = new AtomicInteger(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.fail("there shouldn't be any load");
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.fail("there shouldn't be any unload");
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.fail("there shouldn't be any update");
                    }
                },
                new Goid[]{new Goid(GOID_HI_START, 1), new Goid(GOID_HI_START, 2), new Goid(GOID_HI_START, 3), new Goid(GOID_HI_START, 5), new Goid(GOID_HI_START, 6), new Goid(GOID_HI_START, 10)},
                new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.CREATE, EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(0));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(0));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(0));
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test EntityInvalidationEvent
        // send UPDATE for all not loaded (REJECTED, ERROR etc) modules (0, 4, 7, 8, 9)
        // since these modules failed to load and UPDATE is send for those modules they'll be reloaded
        countLoad.set(0); countUnload.set(0); countUpdate.set(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // make sure these are our deleted modules (100, 3 and 8)
                        Assert.assertThat(
                                module.getGoid(),
                                Matchers.anyOf(
                                        Matchers.equalTo(new Goid(GOID_HI_START, 0)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 4)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 7)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 8)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 9))
                                )
                        );
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.fail("there shouldn't be any unload");
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.fail("there shouldn't be any update");
                    }
                },
                new Goid[]{new Goid(GOID_HI_START, 0), new Goid(GOID_HI_START, 4), new Goid(GOID_HI_START, 7), new Goid(GOID_HI_START, 8), new Goid(GOID_HI_START, 9)},
                new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(5));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(0));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(0));
        // make sure nothing changed
        verifyModulesState(
                new ModuleState[]{
                        ModuleState.ERROR,    // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
                        ModuleState.LOADED,   // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
                        ModuleState.LOADED,   // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => LOADED
                        ModuleState.LOADED,   // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => LOADED
                        ModuleState.REJECTED, // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => REJECTED
                        ModuleState.LOADED,   // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
                        ModuleState.LOADED,   // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
                        ModuleState.ERROR,    // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
                        ModuleState.ERROR,    // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR
                        ModuleState.ERROR,    // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
                        ModuleState.LOADED    // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
                }
        );
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // add new module with goid 100
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        moduleFiles.put(
                new Goid(GOID_HI_START, 100),
                new ServerModuleFileBuilder(create_unsigned_test_module_without_states(100, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
                        .addState(currentNodeId, ModuleState.UPLOADED)
                        .build()
        );
        // test EntityInvalidationEvent
        // send three sample UPDATE's (modules 1, 2, 3 are already LOADED so they'll be ignored)
        // since only one module (module_100) is created, then only the load callback for module_100 should be called
        countLoad.set(0); countUnload.set(0); countUpdate.set(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // make sure this is our newly added module_100
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 100)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.fail("there shouldn't be any unload");
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.fail("there shouldn't be any update");
                    }
                },
                // for new modules, the logic acts on a EntityInvalidationEvent as a trigger, so it doesn't matter that CREATE is not sending
                new Goid[]{new Goid(GOID_HI_START, 1), new Goid(GOID_HI_START, 2), new Goid(GOID_HI_START, 3)},
                new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(1));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(0));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(0));
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // remove modules with goids 100, 3 and 8
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        moduleFiles.remove(new Goid(GOID_HI_START, 100));
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 3)));
        moduleFiles.remove(new Goid(GOID_HI_START, 3));
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 8)));
        moduleFiles.remove(new Goid(GOID_HI_START, 8));

        // test EntityInvalidationEvent
        // send three sample UPDATE's (modules 1, 2, 3 are already LOADED so they'll be ignored)
        // we have 3 deletes so make sure only unload callback is called with exact goids (100, 3 and 8)
        countLoad.set(0); countUnload.set(0); countUpdate.set(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.fail("there shouldn't be any load");
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // make sure these are our deleted modules (100, 3 and 8)
                        Assert.assertThat(
                                module.getGoid(),
                                Matchers.anyOf(
                                        Matchers.equalTo(new Goid(GOID_HI_START, 100)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 3)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 8))
                                )
                        );
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.fail("there shouldn't be any update");
                    }
                },
                // for deleted modules, the logic acts on a EntityInvalidationEvent as a trigger, so it doesn't matter that CREATE is not sending
                new Goid[]{new Goid(GOID_HI_START, 1), new Goid(GOID_HI_START, 2), new Goid(GOID_HI_START, 3)},
                new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(0));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(3));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(0));
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // this is the current modules states:
        // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
        // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
        // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => LOADED
        // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => LOADED  (deleted)
        // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => REJECTED
        // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
        // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
        // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
        // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR   (deleted)
        // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
        // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
        // 1). make sure module_0 has an error so that we can test reload
        Goid goid = new Goid(GOID_HI_START, 0);
        ServerModuleFile module = moduleFiles.get(goid);
        Assert.assertNotNull(module);
        Assert.assertThat(modulesListener.getModuleState(module), Matchers.equalTo(ModuleState.ERROR));
        Assert.assertThat(module.getName(), Matchers.not(Matchers.equalTo("new module_0")));
        module.setName("new module_0");
        Assert.assertThat(module.getName(), Matchers.equalTo("new module_0"));
        // 2). make sure module_1 is loaded so that we can test just name update
        goid = new Goid(GOID_HI_START, 1);
        module = moduleFiles.get(goid);
        Assert.assertNotNull(module);
        Assert.assertThat(modulesListener.getModuleState(module), Matchers.equalTo(ModuleState.LOADED));
        Assert.assertThat(module.getName(), Matchers.not(Matchers.equalTo("new module_1")));
        module.setName("new module_1");
        Assert.assertThat(module.getName(), Matchers.equalTo("new module_1"));
        // 3). make sure module_2 is loaded so that we can test reload (due to content change)
        goid = new Goid(GOID_HI_START, 2);
        module = moduleFiles.get(goid);
        Assert.assertNotNull(module);
        Assert.assertThat(modulesListener.getModuleState(module), Matchers.equalTo(ModuleState.LOADED));
        Assert.assertThat(module.getName(), Matchers.not(Matchers.equalTo("new module_2")));
        module.setName("new module_2");
        Assert.assertThat(module.getName(), Matchers.equalTo("new module_2"));
        Pair<InputStream, String> bytesAndSig = serverModuleFileManager.getModuleBytesAsStreamWithSignature(goid);
        Assert.assertNotNull(bytesAndSig);
        Assert.assertNotNull(bytesAndSig.left); // only bytes are of interest
        byte[] moduleBytes = IOUtils.slurpStream(bytesAndSig.left);
        Assert.assertTrue(moduleBytes != null && moduleBytes.length > 0);
        String moduleDigest = module.getModuleSha256();
        ServerModuleFile newModule = new ServerModuleFileBuilder(create_unsigned_test_module_without_states(2, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar"))).build();
        newModule.copyFrom(module, false, false, true);
        Assert.assertThat(newModule.getGoid(), Matchers.equalTo(goid));
        ServerModuleFile removed = moduleFiles.put(newModule.getGoid(), newModule);
        Assert.assertThat(removed, Matchers.sameInstance(module));
        module = moduleFiles.get(goid);
        Assert.assertThat(module, Matchers.sameInstance(newModule));
        Assert.assertThat(moduleDigest, Matchers.not(Matchers.equalTo(module.getModuleSha256())));
        Pair<InputStream, String> newBytesAndSig = serverModuleFileManager.getModuleBytesAsStreamWithSignature(goid);
        Assert.assertNotNull(newBytesAndSig);
        Assert.assertNotNull(newBytesAndSig.left);
        byte[] newModuleBytes = IOUtils.slurpStream(newBytesAndSig.left);
        Assert.assertFalse(Arrays.equals(moduleBytes, newModuleBytes));

        // test EntityInvalidationEvent
        // for UPDATE send either UPDATE or CREATE with the specified goid's
        // this time we are not going to send the correct goids, therefore 1). will not be executed, however 2). and 3). will be executed
        countLoad.set(0); countUnload.set(0); countUpdate.set(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // then module_2 will be loaded
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 2)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // module_2 will first be unloaded
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 2)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // all modules (i.e. module_0, module_1, module_2) names are changed
                        Assert.assertThat(
                                module.getGoid(),
                                Matchers.anyOf(
                                        Matchers.equalTo(new Goid(GOID_HI_START, 0)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 1)),
                                        Matchers.equalTo(new Goid(GOID_HI_START, 2))
                                )
                        );
                    }
                },
                // for deleted modules, the logic acts on a EntityInvalidationEvent as a trigger, so it doesn't matter that CREATE is not sending
                new Goid[]{new Goid(GOID_HI_START, 101), new Goid(GOID_HI_START, 102)},
                new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(1));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(1));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(3));
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 2)));
        Assert.assertThat(modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, 2))), Matchers.equalTo(ModuleState.LOADED));

        // test EntityInvalidationEvent
        // for UPDATE send either UPDATE or CREATE with the specified goid's
        // finally send update for 1).
        countLoad.set(0); countUnload.set(0); countUpdate.set(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // then module_0 will be loaded
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 0)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // module_0 will first be unloaded
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 0)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.fail("there shouldn't be any update");
                    }
                },
                // for deleted modules, the logic acts on a EntityInvalidationEvent as a trigger, so it doesn't matter that CREATE is not sending
                new Goid[]{new Goid(GOID_HI_START, 0)},
                new char[]{EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(1));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(0));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(0));
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 0)));
        Assert.assertThat(modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, 0))), Matchers.equalTo(ModuleState.ERROR));
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // this is the current modules states:
        // module_0  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest1.jar     => ERROR
        // module_1  => CUSTOM_ASSERTION    com.l7tech.DynamicCustomAssertionsTest1.jar       => LOADED
        // module_2  => MODULAR_ASSERTION   com.l7tech.WorkingTest1.aar                       => LOADED
        // module_3  => CUSTOM_ASSERTION    com.l7tech.DualAssertionsTest1.jar                => LOADED  (deleted)
        // module_4  => MODULAR_ASSERTION   com.l7tech.WorkingTest2.aar                       => REJECTED
        // module_5  => MODULAR_ASSERTION   com.l7tech.WorkingTest3.aar                       => LOADED
        // module_6  => CUSTOM_ASSERTION    com.l7tech.NonDynamicCustomAssertionTest2.jar     => LOADED
        // module_7  => CUSTOM_ASSERTION    com.l7tech.BrokenDescriptorTest1.jar (fail)       => ERROR
        // module_8  => MODULAR_ASSERTION   com.l7tech.InvalidAssertionClassTest1.aar (fail)  => ERROR   (deleted)
        // module_9  => MODULAR_ASSERTION   com.l7tech.NoAssertionsTest1.aar (fail)           => ERROR
        // module_10 => MODULAR_ASSERTION   com.l7tech.WorkingTest4.aar                       => LOADED
        goid = new Goid(GOID_HI_START, 2);
        module = moduleFiles.get(goid);
        Assert.assertNotNull(module);
        Assert.assertThat(modulesListener.getModuleState(module), Matchers.equalTo(ModuleState.LOADED));
        Assert.assertThat(module.getName(), Matchers.equalTo("new module_2"));
        bytesAndSig = serverModuleFileManager.getModuleBytesAsStreamWithSignature(goid);
        Assert.assertNotNull(bytesAndSig);
        Assert.assertNotNull(bytesAndSig.left); // only bytes are of interest
        moduleBytes = IOUtils.slurpStream(bytesAndSig.left);
        Assert.assertTrue(moduleBytes != null && moduleBytes.length > 0);
        moduleDigest = module.getModuleSha256();
        newModule = new ServerModuleFileBuilder(create_unsigned_test_module_without_states(2, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.MissingAssertionClassTest1.aar"))).build();
        newModule.copyFrom(module, false, false, true);
        Assert.assertThat(newModule.getGoid(), Matchers.equalTo(goid));
        removed = moduleFiles.put(newModule.getGoid(), newModule);
        Assert.assertThat(removed, Matchers.sameInstance(module));
        module = moduleFiles.get(goid);
        Assert.assertThat(module, Matchers.sameInstance(newModule));
        Assert.assertThat(moduleDigest, Matchers.not(Matchers.equalTo(module.getModuleSha256())));
        newBytesAndSig = serverModuleFileManager.getModuleBytesAsStreamWithSignature(goid);
        Assert.assertNotNull(newBytesAndSig);
        Assert.assertNotNull(newBytesAndSig.left);
        newModuleBytes = IOUtils.slurpStream(newBytesAndSig.left);
        Assert.assertFalse(Arrays.equals(moduleBytes, newModuleBytes));

        // test EntityInvalidationEvent
        // for UPDATE send either UPDATE or CREATE with the specified goid's
        // this time we are not going to send the correct goids, therefore 1). will not be executed, however 2). and 3). will be executed
        countLoad.set(0); countUnload.set(0); countUpdate.set(0);
        do_test_process_module_events(
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countLoad.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // then module_2 will be loaded
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 2)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUnload.getAndIncrement();
                        Assert.assertNotNull(module);
                        Assert.assertNotNull(module.getGoid());
                        // module_2 will first be unloaded
                        Assert.assertThat(module.getGoid(), Matchers.equalTo(new Goid(GOID_HI_START, 2)));
                    }
                },
                new Functions.UnaryVoid<ServerModuleFile>() {
                    @Override
                    public void call(final ServerModuleFile module) {
                        countUpdate.getAndIncrement();
                        Assert.fail("there shouldn't be any update");
                    }
                },
                // for deleted modules, the logic acts on a EntityInvalidationEvent as a trigger, so it doesn't matter that CREATE is not sending
                new Goid[]{new Goid(GOID_HI_START, 101), new Goid(GOID_HI_START, 102)},
                new char[]{EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.UPDATE}
        );
        Assert.assertThat(countLoad.get(), Matchers.equalTo(1));
        Assert.assertThat(countUnload.get(), Matchers.equalTo(1));
        Assert.assertThat(countUpdate.get(), Matchers.equalTo(0));
        // make sure all modules are populate into knownModuleFiles
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(moduleFile.getGoid()));
        }
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 2)));
        Assert.assertThat(modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, 2))), Matchers.equalTo(ModuleState.ERROR));
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private void do_test_process_module_events(
            final Functions.UnaryVoid<ServerModuleFile> loadCallback,
            final Functions.UnaryVoid<ServerModuleFile> unloadCallback,
            final Functions.UnaryVoid<ServerModuleFile> updateCallback,
            final Goid[] goids,
            final char[] ops
    ) throws Exception {
        // mock load/unload/update module methods
        Assert.assertNotNull(loadCallback);
        Assert.assertNotNull(unloadCallback);
        Assert.assertNotNull(updateCallback);
        Assert.assertNotNull(goids);
        Assert.assertNotNull(ops);
        Assert.assertThat(goids.length, Matchers.equalTo(ops.length));
        // mock loadModule
        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is one parameter for updateModule", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is ServerModuleFile", param1 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param1;
                        Assert.assertNotNull(moduleEntity);
                        // execute callback
                        loadCallback.call(moduleEntity);
                        invocation.callRealMethod();
                        return null;
                    }
                }
        ).when(modulesListener).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        // mock unloadModule
        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is one parameter for updateModule", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is ServerModuleFile", param1 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param1;
                        Assert.assertNotNull(moduleEntity);
                        // execute callback
                        unloadCallback.call(moduleEntity);
                        invocation.callRealMethod();
                        return null;
                    }
                }
        ).when(modulesListener).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        // mock updateModule
        Mockito.doAnswer(
                new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is one parameter for updateModule", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param1 is ServerModuleFile", param1 instanceof ServerModuleFile);
                        final ServerModuleFile moduleEntity = (ServerModuleFile) param1;
                        Assert.assertNotNull(moduleEntity);
                        // execute callback
                        updateCallback.call(moduleEntity);
                        invocation.callRealMethod();
                        return null;
                    }
                }
        ).when(modulesListener).updateModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());

        // send EntityInvalidationEvent, containing two events
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(
                                new EntityInvalidationEvent(
                                        this,
                                        ServerModuleFile.class,
                                        goids,
                                        ops
                                )
                        )
                )
        );
    }
}