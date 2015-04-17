package com.l7tech.server.module;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.module.*;
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
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// TODO : Add signature verification tests

/**
 * Test ServerModuleFileListener
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFileListenerTest extends ModulesScannerTestBase {

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

    // used to generate random GOID
    private static final Random rnd = new Random();
    private static final int typeLength = ModuleType.values().length;
    private static final long GOID_HI_START = Long.MAX_VALUE - 1;

    @Mock
    private ServerModuleFileManager serverModuleFileManager;

    private ServerModuleFileListener modulesListener;
    private CustomAssertionsScanner customAssertionsScanner;
    private ModularAssertionsScanner modularAssertionsScanner;

    // server module files initial repository
    private Map<Goid, ServerModuleFile> moduleFiles;
    // modular and custom assertions deploy folders as well as ServerModuleFile staging folder
    private static File modularDeployFolder, customDeployFolder, customTempFolder, stagingFolder;

    // emulate this cluster node
    private String currentNodeId = "currentClusterNode";

    @BeforeClass
    public static void setUpOnce() throws Exception {
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
    }

    @AfterClass
    public static void cleanUpOnce() {
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

        // create modules listener spy
        modulesListener = Mockito.spy(
                new ServerModuleFileListener(
                        serverModuleFileManager,
                        null,
                        config,
                        modularAssertionRegistrar,
                        customAssertionRegistrar
                )
        );
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
                        Assert.assertEquals("there are two parameter for loadModule", 2, invocation.getArguments().length);
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
    }

    /**
     * Utility class for storing the module bytes as a File.
     */
    class MyServerModuleFile extends ServerModuleFile {
        private File moduleFile;
        File getModuleFile() { return moduleFile; }
        void setModuleFile(File moduleFile) { this.moduleFile = moduleFile; }
    }

    /**
     * {@link MyServerModuleFile} builder class.
     */
    @SuppressWarnings("UnusedDeclaration")
    class ServerModuleFileBuilder {
        private Goid goid;
        private String name;
        private Integer version;
        private ModuleType moduleType;
        private File moduleContent;
        private String checkSum;

        /**
         * Pre-attached {@link MyServerModuleFile}.
         * The builder will append new properties or override existing ones.
         */
        private MyServerModuleFile moduleFile;

        /**
         * Default constructor
         */
        public ServerModuleFileBuilder() {
            this(null);
        }

        /**
         * Initialize the builder with preexisting module file.
         * This way the builder will append new properties or override existing ones.
         *
         * @param moduleFile    the {@link MyServerModuleFile} to attach to.
         */
        public ServerModuleFileBuilder(final MyServerModuleFile moduleFile) {
            this.moduleFile = moduleFile;
        }

        /**
         * @return either the pre-attached {@link #moduleFile} or a new {@link MyServerModuleFile} instance.
         */
        private MyServerModuleFile getModuleFile() {
            return this.moduleFile == null ? new MyServerModuleFile() : this.moduleFile;
        }

        private final Collection<Triple<String, ModuleState, String>> states = new ArrayList<>();
        private final Map<String, String> properties = new HashMap<>();

        public ServerModuleFileBuilder goid(final Goid goid) {
            this.goid = goid;
            return this;
        }

        public ServerModuleFileBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public ServerModuleFileBuilder version(final Integer version) {
            this.version = version;
            return this;
        }

        public ServerModuleFileBuilder moduleType(final ModuleType moduleType) {
            this.moduleType = moduleType;
            return this;
        }

        public ServerModuleFileBuilder content(final File file) {
            this.moduleContent = file;
            return this;
        }

        public ServerModuleFileBuilder checkSum(final String checkSum) {
            this.checkSum = checkSum;
            return this;
        }

        public ServerModuleFileBuilder addState(final String node, final ModuleState state) {
            if (state == null) // do not add if null
                return this;
            return addState(node, state, null);
        }

        public ServerModuleFileBuilder addStateError(final String node, final String error) {
            if (error == null) // do not add if null
                return this;
            return addState(node, null, error);

        }

        private ServerModuleFileBuilder addState(final String node, final ModuleState state, final String error) {
            this.states.add(Triple.triple(node, state, error));
            return this;
        }

        public ServerModuleFileBuilder addProperty(final String name, final String value) {
            this.properties.put(name, value);
            return this;
        }

        public MyServerModuleFile build() {
            final MyServerModuleFile moduleFile = getModuleFile();
            if (goid != null) {
                moduleFile.setGoid(goid);
            }
            if (name != null) {
                moduleFile.setName(name);
            }
            if (version != null) {
                moduleFile.setVersion(version);
            }
            if (moduleType != null) {
                moduleFile.setModuleType(moduleType);
            }
            if (moduleContent != null) {
                moduleFile.setModuleFile(moduleContent);
                try {
                    moduleFile.setModuleSha256(ModuleDigest.digest(moduleContent));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (checkSum != null) {
                moduleFile.setModuleSha256(checkSum); // override check-sum
            }
            for (final Map.Entry<String, String> property : properties.entrySet()) {
                if (MyServerModuleFile.PROP_SIZE.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_SIZE, property.getValue());
                } else if (MyServerModuleFile.PROP_ASSERTIONS.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_ASSERTIONS, property.getValue());
                } else {
                    Assert.fail("Unsupported property: " + property.getKey());
                }
            }
            for (final Triple<String, ModuleState, String> state : states) {
                if (StringUtils.isNotBlank(state.left)) {
                    if (state.middle != null) {
                        moduleFile.setStateForNode(state.left, state.middle);
                    } else {
                        moduleFile.setStateErrorMessageForNode(state.left, state.right);
                    }
                }
            }

            return moduleFile;
        }
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
    private void createSampleModules() {
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
                        new ServerModuleFileBuilder(create_test_module_without_states(0, ModuleType.CUSTOM_ASSERTION, new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest1.jar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(1, ModuleType.CUSTOM_ASSERTION, new File(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest1.jar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(2, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(3, ModuleType.CUSTOM_ASSERTION, new File(dualModulesEmptyDir, "com.l7tech.DualAssertionsTest1.jar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(4, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar")))
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
                        create_test_module_without_states(5, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest3.aar"))
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
                        // file: com.l7tech.NonDynamicCustomAssertionTest2.jar
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 6),
                        create_test_module_without_states(6, ModuleType.CUSTOM_ASSERTION, new File(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest2.jar"))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(7, ModuleType.CUSTOM_ASSERTION, new File(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest1.jar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(8, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.InvalidAssertionClassTest1.aar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(9, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.NoAssertionsTest1.aar")))
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
                        new ServerModuleFileBuilder(create_test_module_without_states(10, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar")))
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
    // failed modules
    final Collection<Goid> failedModules = Arrays.asList(
            new Goid(GOID_HI_START, 7),   // module_7  => ACCEPTED => CUSTOM_ASSERTION;  com.l7tech.BrokenDescriptorTest1.jar (fail)
            new Goid(GOID_HI_START, 8),   // module_8  => ACCEPTED => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail)
            new Goid(GOID_HI_START, 9)    // module_9  => REJECTED => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)
    );

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
                new Answer<InputStream>() {
                    @Override
                    public InputStream answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertNotNull(invocation);
                        Assert.assertEquals("there is only one parameter for getModuleBytesAsStream", 1, invocation.getArguments().length);
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertTrue("Param is Goid", param1 instanceof Goid);
                        final Goid goid = (Goid) param1;
                        Assert.assertNotNull(goid);

                        final ServerModuleFile moduleFile = moduleFiles.get(goid);
                        if (moduleFile != null) {
                            Assert.assertTrue(moduleFile instanceof MyServerModuleFile);
                            Assert.assertNotNull(((MyServerModuleFile) moduleFile).getModuleFile());
                            Assert.assertTrue(((MyServerModuleFile) moduleFile).getModuleFile().exists());
                            return new BufferedInputStream(new FileInputStream(((MyServerModuleFile)moduleFile).getModuleFile()));
                        }
                        return null;
                    }
                }
        ).when(serverModuleFileManager).getModuleBytesAsStream(Mockito.<Goid>any());
    }

    /**
     * Convenient method for creating a test sample of {@link MyServerModuleFile} without any states, having the following attributes:
     * <ul>
     *     <li>goid: {@code Goid(GOID_HI_START, ordinal)}</li>
     *     <li>name: {@code module_[ordinal]}</li>
     *     <li>version: {@code 0}</li>
     *     <li>specified {@code moduleType} or a random type if {@code null}</li>
     *     <li>bytes: {@code test data _[ordinal]}</li>
     *     <li>file-name: {@code module_[ordinal].[jar or aar, depending whether the type is modular or custom assertions]}</li>
     *     <li>size: {@code length of the bytes array}</li>
     *     <li>assertions: {@code assertion_[ordinal]}</li>
     * </ul>
     * @param ordinal       the ordinal of this test sample
     * @param moduleType    the module type either {@link ModuleType#MODULAR_ASSERTION} or {@link ModuleType#CUSTOM_ASSERTION}
     * @param moduleBytes   the module file containing the bytes, instead of loading the bytes in memory. Required and cannot be {@code null}.
     */
    private MyServerModuleFile create_test_module_without_states(final long ordinal, ModuleType moduleType, final File moduleBytes) {
        Assert.assertNotNull(moduleBytes);
        Assert.assertTrue(moduleBytes.exists());
        moduleType = moduleType != null ? moduleType : ModuleType.values()[rnd.nextInt(typeLength)];
        final byte[] bytes = String.valueOf("test data " + ordinal).getBytes(Charsets.UTF8);
        return new ServerModuleFileBuilder()
                .goid(new Goid(GOID_HI_START, ordinal))
                .name("module_" + ordinal)
                .version(0)
                .moduleType(moduleType)
                .content(moduleBytes)
                .addProperty(ServerModuleFile.PROP_ASSERTIONS, "assertion_" + ordinal)
                .addProperty(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                .build();
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
        createSampleModules();
        mockServerModuleFileManager(true);

        // initial scan for empty deploy folders
        do_scanner_run(ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);

        // new module with goid 100
        moduleFiles.put(
                new Goid(GOID_HI_START, 100),
                new ServerModuleFileBuilder(create_test_module_without_states(100, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
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
    public void test_upload_disabled() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
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
                new ServerModuleFileBuilder(create_test_module_without_states(100, ModuleType.MODULAR_ASSERTION, new File(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")))
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
        moduleFiles.remove(new Goid(GOID_HI_START, 2));
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
    }

    /**
     * Utility function for checking that out modules (in the DB i.e. {@code moduleFiles}) have the expected states
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
                Assert.assertEquals("there is only one parameter for verifySignature", 1, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                Assert.assertNotNull(moduleFile);
                // check if module should be rejected, fail to verify signature or be accepted
                if (rejectedModules.contains(moduleFile.getGoid())) {
                    throw new ServerModuleFileListener.ModuleRejectedException();
                } else if (signatureErrorModules.contains(moduleFile.getGoid())) {
                    throw new ServerModuleFileListener.ModuleSignatureException();
                }
                // the rest are accepted
                return null;
            }
        }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());

        // calculate expected loaded based on rejectedModules and signatureErrorModules
        final Collection<ServerModuleFile> expectedLoaded = new ArrayList<>();
        for (final ServerModuleFile serverModuleFile : moduleFiles.values()) {
            Assert.assertNotNull(serverModuleFile.getGoid());
            if (!rejectedModules.contains(serverModuleFile.getGoid()) && !signatureErrorModules.contains(serverModuleFile.getGoid()) && !failedModules.contains(serverModuleFile.getGoid())) {
                expectedLoaded.add(serverModuleFile);
            }
        }

        // we have:
        publishInitialStartedEventAndVerifyResult(
                new ModuleState[]{
                        rejectedModules.contains(new Goid(GOID_HI_START, 0))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 0)) || failedModules.contains(new Goid(GOID_HI_START, 0)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_0  => REJECTED => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest1.jar     => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 1))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 1)) || failedModules.contains(new Goid(GOID_HI_START, 1)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_1  => UPLOADED => CUSTOM_ASSERTION;  com.l7tech.DynamicCustomAssertionsTest1.jar       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 2))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 2)) || failedModules.contains(new Goid(GOID_HI_START, 2)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_2  => ERROR    => MODULAR_ASSERTION; com.l7tech.WorkingTest1.aar                       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 3))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 3)) || failedModules.contains(new Goid(GOID_HI_START, 3)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_3  => LOADED   => CUSTOM_ASSERTION;  com.l7tech.DualAssertionsTest1.jar                => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 4))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 4)) || failedModules.contains(new Goid(GOID_HI_START, 4)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_4  => <NONE>   => MODULAR_ASSERTION; com.l7tech.WorkingTest2.aar                       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 5))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 5)) || failedModules.contains(new Goid(GOID_HI_START, 5)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_5  => <NONE>   => MODULAR_ASSERTION; com.l7tech.WorkingTest3.aar                       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 6))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 6)) || failedModules.contains(new Goid(GOID_HI_START, 6)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_6  => <NONE>   => CUSTOM_ASSERTION;  com.l7tech.NonDynamicCustomAssertionTest2.jar     => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 7))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 7)) || failedModules.contains(new Goid(GOID_HI_START, 7)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_7  => ACCEPTED => CUSTOM_ASSERTION;  com.l7tech.BrokenDescriptorTest1.jar (fail)       => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 8))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 8)) || failedModules.contains(new Goid(GOID_HI_START, 8)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_8  => ACCEPTED => MODULAR_ASSERTION; com.l7tech.InvalidAssertionClassTest1.aar (fail)  => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 9))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 9)) || failedModules.contains(new Goid(GOID_HI_START, 9)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED,                    // module_9  => REJECTED => MODULAR_ASSERTION; com.l7tech.NoAssertionsTest1.aar (fail)           => REJECTED/ERROR/LOADED
                        rejectedModules.contains(new Goid(GOID_HI_START, 10))
                                ? ModuleState.REJECTED
                                : (signatureErrorModules.contains(new Goid(GOID_HI_START, 10)) || failedModules.contains(new Goid(GOID_HI_START, 10)))
                                    ? ModuleState.ERROR
                                    : ModuleState.LOADED                     // module_10 => LOADED   => MODULAR_ASSERTION; com.l7tech.WorkingTest4.aar                       => REJECTED/ERROR/LOADED
                },
                expectedLoaded
        );
    }

    @Test
    public void test_started_event() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
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
        createSampleModules();
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
            // doublecheck that every state is in ERROR
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
     */
    private void publishAndVerifyNewModuleFile(
            final long ordinal,
            final ModuleType moduleType,
            final ModuleState initialState,
            final File moduleContent,
            final ModuleState expectedStateAfterLoad,
            final Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException> signatureVerificationCallback
    ) throws Exception {
        Assert.assertNotNull(moduleType);
        Assert.assertNotNull(moduleContent);
        Assert.assertTrue(moduleContent.exists());
        Assert.assertNotNull(expectedStateAfterLoad);

        // simulate signature verification
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertNotNull(invocation);
                Assert.assertEquals("there is only one parameter for verifySignature", 1, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                Assert.assertNotNull(moduleFile);
                // execute callback
                signatureVerificationCallback.call(moduleFile);
                // the rest are accepted
                return null;
            }
        }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());

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
        ServerModuleFile moduleFile = new ServerModuleFileBuilder(create_test_module_without_states(ordinal, moduleType, moduleContent))
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
     * @param signatureVerificationCallback     in case when the module is not currently in loaded state, the module will be loaded; a callback for verifying module signature.
     *                                          Throw {@link ServerModuleFileListener.ModuleRejectedException} to indicate module has been rejected.
     *                                          Throw {@link ServerModuleFileListener.ModuleSignatureException} to indicate an error while verifying module signature.
     *                                          Not throwing means the module is accepted.
     */
    private void publishAndVerifyUpdateModuleFile(
            final Goid goid,
            final String newName,
            final ModuleState expectedStateAfterLoad,
            final Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException> signatureVerificationCallback
    ) throws Exception {
        Assert.assertNotNull(goid);
        Assert.assertNotNull(expectedStateAfterLoad);
        Assert.assertNotNull(signatureVerificationCallback);

        // simulate signature verification
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertNotNull(invocation);
                Assert.assertEquals("there is only one parameter for verifySignature", 1, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
                Assert.assertNotNull(moduleFile);
                // execute callback
                signatureVerificationCallback.call(moduleFile);
                // the rest are accepted
                return null;
            }
        }).when(modulesListener).verifySignature(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());

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
        if (ModuleState.LOADED.equals(moduleState)) {
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
            Assert.assertNotNull(modulesListener.knownModuleFiles.get(goid).getStagingFile());
            Assert.assertTrue(modulesListener.knownModuleFiles.get(goid).getStagingFile().exists());
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
        if (modulesScanner != null && stagedFileName != null) {
            Assert.assertNull(modulesScanner.getModule(stagedFileName));
        }
    }

    @Test
    public void test_entity_crud() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
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
                }
        );

        // update module_5; com.l7tech.WorkingTest3.aar; was REJECTED, should be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 5),
                null,
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 5)));
                        // accept; nothing to do
                    }
                }
        );

        // update module_2; com.l7tech.WorkingTest1.aar; was REJECTED, should be loaded again => ERROR (as signature will fail i.e. throw ModuleSignatureException)
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 2),
                null,
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 2)));
                        throw new ServerModuleFileListener.ModuleSignatureException();
                    }
                }
        );

        // update module_3; com.l7tech.DualAssertionsTest1.jar; was ERROR, should be loaded again => REJECTED (as signature was rejected i.e. throw ModuleRejectedException)
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 3),
                null,
                ModuleState.REJECTED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 3)));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                }
        );

        // update module_100; com.l7tech.DynamicCustomAssertionsTest5.jar; was LOADED, should not be loaded again => remains LOADED (name will change though)
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 100),
                "new name for 100",
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accept; nothing to do
                    }
                }
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
                }
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
                }
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
                }
        );

        final Goid goid100 = new Goid(GOID_HI_START, 100);
        assertThat("module_100", equalTo(moduleFiles.get(goid100).getName()));

        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was REJECTED, should be loaded again => ERROR (as signature will fail i.e. throw ModuleSignatureException)
        publishAndVerifyUpdateModuleFile(
                goid100,
                "load with new name for module 100",
                ModuleState.ERROR,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        throw new ServerModuleFileListener.ModuleSignatureException();
                    }
                }
        );
        assertThat("load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));

        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was ERROR, should be loaded again => REJECTED (as signature will fail i.e. throw ModuleRejectedException)
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new load with new name for module 100",
                ModuleState.REJECTED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                }
        );
        assertThat("new load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));


        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was REJECTED, should be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new new load with new name for module 100",
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        // accept; nothing to do
                    }
                }
        );
        assertThat("new new load with new name for module 100", equalTo(moduleFiles.get(goid100).getName()));

        // update module_100; com.l7tech.DynamicCustomAssertionsTest4.jar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                goid100,
                "new new new load with new name for module 100",
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(goid100));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                }
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
                }
        );
        assertThat("module_200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));

        // update module_200; com.l7tech.WorkingTest5.aar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 200),
                "new name for module 200",
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        throw new ServerModuleFileListener.ModuleRejectedException();
                    }
                }
        );
        assertThat("new name for module 200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));

        // update module_200; com.l7tech.WorkingTest5.aar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 200),
                "new new name for module 200",
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        throw new ServerModuleFileListener.ModuleSignatureException();
                    }
                }
        );
        assertThat("new new name for module 200", equalTo(moduleFiles.get(new Goid(GOID_HI_START, 200)).getName()));

        // update module_200; com.l7tech.WorkingTest5.aar; was LOADED, should not be loaded again => LOADED
        publishAndVerifyUpdateModuleFile(
                new Goid(GOID_HI_START, 200),
                "new new new name for module 200",
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 200)));
                        // accepted; nothing to do
                    }
                }
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
        createSampleModules();
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
                }
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
                }
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
                }
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
                }
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
                }
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
                }
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
                }
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
                }
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
                }
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
        createSampleModules();
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
                }
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
                }
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
                }
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
                }
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
                }
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
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile().exists());
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
                }
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
                }
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
                }
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
                }
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
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 0)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 101)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 4)).getStagingFile().exists());
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 10)).getStagingFile().exists());
        // do scan modules
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
                }
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
                }
        );

    }

    @BugId("SSG-11149")
    @Test
    public void test_runtime_exception_while_loading_module() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
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
                }
         );
    }

    @BugId("SSG-11161")
    @Test
    public void test_delete_after_update() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
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
                ModuleState.LOADED,
                new Functions.UnaryVoidThrows<ServerModuleFile, ServerModuleFileListener.ModuleSignatureException>() {
                    @Override
                    public void call(final ServerModuleFile moduleFile) throws ServerModuleFileListener.ModuleSignatureException {
                        Assert.assertNotNull(moduleFile);
                        Assert.assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 100)));
                        // accepted; nothing to do
                    }
                }
        );

        // verify the module still exists in both listener and scanner
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNotNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)).getStagingFile());
        Assert.assertTrue(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)).getStagingFile().exists());
        final String stagedFileName = modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)).getStagingFile().getName();
        Assert.assertNotNull(customAssertionsScanner.getModule(stagedFileName));

        // remove module_1; com.l7tech.DynamicCustomAssertionsTest1.jar; LOADED
        publishAndVerifyDeletedModuleFile(new Goid(GOID_HI_START, 1));
        // double-check module_1 was deleted
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNull(modulesListener.knownModuleFiles.get(new Goid(GOID_HI_START, 1)));
        Assert.assertNull(customAssertionsScanner.getModule(stagedFileName));
    }
}