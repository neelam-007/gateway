package com.l7tech.server.policy.module;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.RunsOnWindows;
import com.l7tech.util.FileUtils;
import com.l7tech.util.Pair;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The Custom Assertion jars used by the unit test. The filename must start with "com.l7tech.". Otherwise, it will
 * not be loaded by the unit test. See idea_project.xml file for list of valid unit test resource names.
 * <p/>
 * Content of module/custom test folders:
 * <pre>
 * modular
 *  |_ com.l7tech.WorkingTest1.aar                              [sample module without any issues]
 *  |_ com.l7tech.WorkingTest1NewVersion.aar                    [a new version of the above module, having the same assertion classes, but different checksum to trigger module replacement]
 *  |_ com.l7tech.WorkingTest2.aar                              [another sample module without any issues]
 *  |_ com.l7tech.WorkingTest3.aar                              [sample module without any issues having load-listener]
 *  |_ com.l7tech.WorkingTest4.aar                              [another sample module without any issues having load-listener]
 *  |_ com.l7tech.WorkingTest5.aar                              [sample module without any issues having dual assertions]
 *  |_ com.l7tech.NoAssertionsTest1.aar                         [sample module without any assertion classes declared in the manifest file]
 *  |_ com.l7tech.MissingAssertionClassTest1.aar                [sample module declaring a class (in the manifest file) which is not in the aar]
 *  |_ com.l7tech.InvalidManifestTest1.aar                      [sample module having invalid manifest file]
 *  |_ com.l7tech.InvalidAssertionClassTest1.aar                [sample module declaring an invalid (not well formed) class in the manifest file]
 *  |_ com.l7tech.DuplicateClassTest1.aar                       [duplicate module with WorkingTest1]
 *  |_ com.l7tech.DuplicateClassInParentClassLoaderTest1.aar    [sample module declaring assertion class which can be found with the parent ClassLoader (i.e. java.lang.String)]
 *  |_ com.l7tech.DuplicateClassDifferentPackageTest1.aar       [sample module declaring the same assertion class as WorkingTest1, but in different package]
 *  |_ com.l7tech.LoadListenerOnLoadExceptionTest1.aar          [sample module having load-listener and throwing runtime-exception onModuleLoaded method]
 *  |_ com.l7tech.LoadListenerOnUnloadExceptionTest1.aar        [sample module having load-listener and throwing runtime-exception onModuleUnloaded method]
 *  |_ com.l7tech.LoadListenerClassMissingTest1.aar             [sample module registering missing load-listener class]
 *  |_ com.l7tech.LoadListenerClassInvalidTest1.aar             [sample module registering invalid load-listener class-name]
 *  |_ dummy.png                                                [a placeholder file for this folder]
 * </pre>
 */
@RunWith(MockitoJUnitRunner.class)
public class ModularAssertionsScannerTest extends ModulesScannerTestBase {

    // The module temp directory name.
    private static final String MODULES_TEMP_DIR_NAME = "l7tech-ModularAssertionsScannerTest";
    private static final String MODULES_ROOT_EMPTY_DIR = "com/l7tech/server/policy/module/modular/dummy.png";
    private static final String DISABLED_MODULES_SUFFIX = ".disabled";

    private static File modulesRootEmptyDir;
    private static File modTmpFolder;

    @Mock
    private ScannerCallbacks.ModularAssertion modularAssertionCallbacks;

    @Mock
    private ModularAssertionModulesConfig modulesConfig;

    @Mock
    private ApplicationContext applicationContext;

    private ModularAssertionsScanner assertionsScanner;

    /**
     * Verifies that correct methods are called from <tt>assertionsScanner</tt> and that
     *
     * @param onModuleLoadCalls                        expected number of {@link com.l7tech.server.policy.module.ModulesScanner#onModuleLoad(java.io.File, String, long) onModuleLoad} calls.
     *                                                 <code>null</code> if not care.
     * @param onModuleUnloadCalls                      expected number of {@link com.l7tech.server.policy.module.ModularAssertionsScanner#onModuleUnloaded(ModularAssertionModule) onModuleUnloaded}.
     *                                                 <code>null</code> if not care.
     * @param numberOfLoadedModules                    expected number of loaded modules
     *                                                 (i.e. calls to {@link com.l7tech.server.policy.module.ScannerCallbacks#publishEvent(org.springframework.context.ApplicationEvent) publishEvent}
     *                                                 with {@link com.l7tech.server.policy.module.AssertionModuleRegistrationEvent}). <code>null</code> if not care.
     * @param numberOfAssertionLoadListenersCalls      expected number of load-listeners calls
     *                                                 (i.e. calls to {@link com.l7tech.server.policy.module.ModularAssertionsScanner#onModuleLoaded(Class, String) onModuleLoaded}).
     *                                                 <code>null</code> if not care.
     * @param numberOfUnloadedModules                  expected number of unloaded modules
     *                                                 (i.e. calls to {@link com.l7tech.server.policy.module.ScannerCallbacks#publishEvent(org.springframework.context.ApplicationEvent) publishEvent}
     *                                                 with {@link com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent}). <code>null</code> if not care.
     * @param numberOfUnregisteredAssertions           expected number of unregistered assertions
     *                                                 (i.e. calls to {@link com.l7tech.server.policy.module.ScannerCallbacks.ModularAssertion#unregisterAssertion(com.l7tech.policy.assertion.Assertion) unregisterAssertion}.
     *                                                 <code>null</code> if not care.
     * @param numberOfFailedModules                    expected number of failed modules.
     * @param numberOfSkippedModules                   expected number of skipped modules.
     * @param numberOfAssertionsPerModule              expected number of assertions per module.
     */
    private void verifyAssertionScanner(
            @Nullable final Integer onModuleLoadCalls,
            @Nullable final Integer onModuleUnloadCalls,
            @Nullable final Integer numberOfLoadedModules,
            @Nullable final Integer numberOfAssertionLoadListenersCalls,
            @Nullable final Integer numberOfUnloadedModules,
            @Nullable final Integer numberOfUnregisteredAssertions,
            @Nullable final Integer numberOfFailedModules,
            @Nullable final Integer numberOfSkippedModules,
            @Nullable final Integer numberOfAssertionsPerModule
    ) throws Exception {
        // make sure we load all our modules
        if (onModuleLoadCalls != null) {
            Mockito.verify(assertionsScanner, Mockito.times(onModuleLoadCalls)).onModuleLoad(Mockito.<File>any(), Mockito.anyString(), Mockito.anyLong());
        }
        // make sure all modules have been registered
        if (numberOfLoadedModules != null) {
            Mockito.verify(modularAssertionCallbacks, Mockito.times(numberOfLoadedModules)).publishEvent(Mockito.isA(AssertionModuleRegistrationEvent.class));
        }
        // make sure all assertions have been registered
        if (numberOfLoadedModules != null && numberOfAssertionsPerModule != null) {
            Mockito.verify(modularAssertionCallbacks, Mockito.times(numberOfLoadedModules*numberOfAssertionsPerModule)).registerAssertion(Mockito.<Class<? extends Assertion>>any());
        }
        // make sure cluster properties have been registered
        if (numberOfLoadedModules != null) {
            Mockito.verify(modularAssertionCallbacks, Mockito.times(numberOfLoadedModules)).registerClusterProps(Mockito.<Set<? extends Assertion>>any());
        }
        // make sure all assertions load-listener's are called
        if (numberOfAssertionLoadListenersCalls != null) {
            Mockito.verify(assertionsScanner, Mockito.times(numberOfAssertionLoadListenersCalls)).onModuleLoaded(Mockito.<Class<? extends Assertion>>any(), Mockito.anyString());
        }

        // make sure no modules have been unloaded.
        if (onModuleUnloadCalls != null) {
            Mockito.verify(assertionsScanner, Mockito.times(onModuleUnloadCalls)).onModuleUnloaded(Mockito.<ModularAssertionModule>any());
        }
        // make sure no modules have been unregistered
        if (numberOfUnloadedModules != null) {
            Mockito.verify(modularAssertionCallbacks, Mockito.times(numberOfUnloadedModules)).publishEvent(Mockito.isA(AssertionModuleUnregistrationEvent.class));
        }
        // make sure all assertions have been unregistered
        if (numberOfUnregisteredAssertions != null) {
            Mockito.verify(modularAssertionCallbacks, Mockito.times(numberOfUnregisteredAssertions)).unregisterAssertion(Mockito.<Assertion>any());
        }

        // assert counters and collections
        if (numberOfLoadedModules != null && numberOfUnloadedModules != null) {
            Assert.assertEquals("correct number of modules have been loaded", numberOfLoadedModules - numberOfUnloadedModules, assertionsScanner.scannedModules.size());
        }
        if (numberOfFailedModules != null) {
            Assert.assertEquals("correct number of modules have failed", numberOfFailedModules.intValue(), assertionsScanner.failModTimes.size());
        }
        if (numberOfSkippedModules != null) {
            Assert.assertEquals("correct number of modules were skipped", numberOfSkippedModules.intValue(), assertionsScanner.skipModTimes.size());
        }

        // assert assertions
        Assert.assertTrue(
                "scannedModules and getModules() are equal",
                Arrays.equals(
                        assertionsScanner.scannedModules.values().toArray(),
                        assertionsScanner.getModules().toArray()
                )
        );
        if (numberOfAssertionsPerModule != null) {
            for (final ModularAssertionModule module : assertionsScanner.getModules()) {
                Assert.assertEquals("expected number of assertions", numberOfAssertionsPerModule.intValue(), module.getAssertionPrototypes().size());
            }
        }
    }

    /**
     * Utility function for
     * {@link #verifyAssertionScanner(Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer) verifyAssertionScanner}
     * with reduced arguments, targeted for verifying modules loading process.
     *
     * @param onModuleLoadCalls     expected number of {@link com.l7tech.server.policy.module.ModulesScanner#onModuleLoad(java.io.File, String, long) onModuleLoad} calls.
     * @param numberOfLoadedModules expected number of loaded modules (i.e. calls to {@link ScannerCallbacks#publishEvent(org.springframework.context.ApplicationEvent) publishEvent} with {@link AssertionModuleRegistrationEvent}).
     * @throws Exception
     */
    private void verifyLoadedModules(final int onModuleLoadCalls, final int numberOfLoadedModules) throws Exception {
        verifyAssertionScanner(
                onModuleLoadCalls,
                null,  // don't care
                numberOfLoadedModules,
                null,  // don't care
                null,  // don't care
                null,  // don't care
                null,  // don't care
                null,  // don't care
                1
        );
    }

    /**
     * Helper method for creating
     * {@link com.l7tech.server.policy.module.ScannerCallbacks.ModularAssertion#registerAssertion(Class) registerAssertion}
     * and
     * {@link com.l7tech.server.policy.module.ScannerCallbacks.ModularAssertion#isAssertionRegistered(String) isAssertionRegistered}
     * stubs.
     */
    private void stubAssertionRegisterCallbacks() {
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
     * Helper method for stubbing load-listener onModuleLoaded or onModuleUnloaded methods calls.
     *
     * @param numOfModules    expected number of load-listener modules.
     * @return pre-allocated hash-set for the load-listener method calls.
     */
    private Set<Pair<String, String>> stubLoadListeners(final int numOfModules) {
        // all test load-listener modules call getBean with first argument containing the load-listener
        // method (onModuleLoaded or onModuleUnloaded) and second argument containing the class simple name.
        // This way we can distinguish between different objects and methods and verify that the
        // all load-listener methods have been called accordingly
        final Set<Pair<String, String>> loadListenerMethodsCalled = new HashSet<>(2*numOfModules);
        Mockito.when(applicationContext.getBean(Mockito.anyString(), Mockito.anyVararg())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertEquals("make sure exactly two arguments are passed", 2, invocation.getArguments().length);

                final Object param1 = invocation.getArguments()[0];
                Assert.assertTrue("First param is String", param1 instanceof String);
                final String methodName = (String) param1;

                final Object param2 = invocation.getArguments()[1];
                Assert.assertTrue("Second param is String", param2 instanceof String);
                final String className = (String) param2;

                final Pair<String, String> ret = new Pair<>(methodName, className);
                loadListenerMethodsCalled.add(ret);

                return ret;
            }
        });

        return loadListenerMethodsCalled;
    }

    @BeforeClass
    public static void setUpOnce() throws Exception {
        // On Windows platform jar files are locked by the JVM, therefore they cannot be cleaned up on exit.
        // On start, we will loop through all previously created temporary folders and delete them,
        // which means that at a worst case scenario we will only end up with files from a single run.
        cleanUpTemporaryFilesFromPreviousRuns(MODULES_TEMP_DIR_NAME);

        modulesRootEmptyDir = new File(extractFolder(MODULES_ROOT_EMPTY_DIR));
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
        // reset modTmpFolder
        modTmpFolder = null;

        // generate dates array
        setupDates();

        // mock methods
        Mockito.when(modulesConfig.isFeatureEnabled()).thenReturn(true);
        Mockito.when(modulesConfig.isScanningEnabled()).thenReturn(true);
        Mockito.when(modulesConfig.getModulesExt()).thenReturn(Arrays.asList(Pattern.compile("\\s+").split(".jar .assertion .ass .assn .aar")));
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(null); // disabled by default
        Mockito.when(modulesConfig.getManifestHdrAssertionList()).thenReturn("ModularAssertion-List");
        Mockito.when(modulesConfig.getManifestHdrPrivateLibraries()).thenReturn("ModularAssertion-Private-Libraries");

        // create modular assertions scanner
        assertionsScanner = Mockito.spy(new ModularAssertionsScanner(modulesConfig, modularAssertionCallbacks));

        // forcing it to true, since on systems with one second resolution of File.lastModified method
        // (like linux, OSX etc.), quick folder modifications will not be detected.
        //Mockito.doReturn(true).when(assertionsScanner).isScanNeeded(Mockito.<File>any(), Mockito.anyLong());
    }

    @After
    public void tearDown() throws Exception {
        // make sure all resources are cleared
        try { assertionsScanner.destroy(); } catch (Throwable ignore) { }

        // remove any temporary folders used
        if (modTmpFolder != null) {
            FileUtils.deleteDir(modTmpFolder);
            tmpFiles.remove(modTmpFolder.getAbsolutePath());
        }
    }

    @Test
    public void testFeatureDisabled() throws Exception {
        // set modules dir
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modulesRootEmptyDir);

        // enable scanning
        Mockito.doReturn(true).when(modulesConfig).isScanningEnabled();

        // disable feature
        Mockito.doReturn(false).when(modulesConfig).isFeatureEnabled();

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("feature disabled, should return false", changesMade);
        // make sure no modules are loaded
        verifyLoadedModules(0, 0);
    }

    @Test
    public void testScanningDisabled() throws Exception {
        // set modules dir
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modulesRootEmptyDir);

        // enable feature
        Mockito.doReturn(true).when(modulesConfig).isFeatureEnabled();

        // disable scanning
        Mockito.doReturn(false).when(modulesConfig).isScanningEnabled();

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("feature disabled, should return false", changesMade);
        // make sure no modules are loaded
        verifyLoadedModules(0, 0);
    }

    @Test
    public void testScanWorkingModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);
        // verify that our sample module was successfully loaded
        verifyLoadedModules(2, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyLoadedModules(2, 2);
    }

    @Test
    public void testModuleWithoutAssertions() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.NoAssertionsTest1.aar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes found, module is broken", changesMade);

        verifyAssertionScanner(
                1,      // one new module
                null,   // don't care
                0,      // no modules registered (the module doesn't contain any assertions)
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                0,      // shouldn't fail, so zero
                1,      // should be skipped, so one
                1       // one assertion per module
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(1, null, 0, 0, null, null, 0, 1, 1);
    }

    @Test
    public void testModuleWithInvalidManifest() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.InvalidManifestTest1.aar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes found, module is broken", changesMade);

        verifyAssertionScanner(
                1,      // one new module
                null,   // don't care
                0,      // no modules registered (the module manifest file is invalid)
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                1,      // module should fail, so one
                0,      // shouldn't be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(1, null, 0, 0, null, null, 1, 0, 1);
    }

    @Test
    public void testModuleWithMissingAssertionClass() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.MissingAssertionClassTest1.aar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes found, module is broken", changesMade);

        verifyAssertionScanner(
                1,      // one new module
                null,   // don't care
                0,      // no modules registered (specified assertion class, in the manifest, is missing from aar file)
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                1,      // module should fail, so one
                0,      // shouldn't be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(1, null, 0, 0, null, null, 1, 0, 1);
    }

    @Test
    public void testModuleWithInvalidAssertionClass() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.InvalidAssertionClassTest1.aar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes found, module is broken", changesMade);

        verifyAssertionScanner(
                1,      // one new module
                null,   // don't care
                0,      // no modules registered (specified assertion class, in the manifest, is invalid)
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                1,      // module should fail, so one
                0,      // shouldn't be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(1, null, 0, 0, null, null, 1, 0, 1);
    }

    @Test
    public void testModuleWithDuplicateClassInParentClassLoader() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.DuplicateClassInParentClassLoaderTest1.aar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes found, module is broken", changesMade);

        verifyAssertionScanner(
                1,      // one new module
                null,   // don't care
                0,      // no modules registered (specified assertion class already exists from the parent classLoader i.e. java.lang.String)
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                1,      // module should fail, so one
                0,      // shouldn't be skipped, so zero
                1       // one assertion per module
        );


        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(1, null, 0, 0, null, null, 1, 0, 1);
    }

    @Test
    public void testModuleWithDuplicateAssertionClass() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our working module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);
        // there is one new module, and it should be registered
        verifyAssertionScanner(1, null, 1, 0, null, null, 0, 0, 1);

        // copy our duplicate module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.DuplicateClassTest1.aar")
                }
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes found, module is broken", changesMade);

        verifyAssertionScanner(
                2,      // there should be two modules in total
                null,   // don't care
                1,      // one should be registered (the first working one)
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                1,      // last one will fail due to duplicity
                0,      // shouldn't be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(2, null, 1, 0, null, null, 1, 0, 1);
    }

    @Test
    public void testModuleWithDuplicateAssertionClassInDifferentPackage() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our working module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);
        // there is one new module, and it should be registered
        verifyLoadedModules(1, 1);

        // copy our duplicate module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.DuplicateClassDifferentPackageTest1.aar")
                }
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes are found", changesMade);

        verifyAssertionScanner(
                2,      // there should be two modules in total
                null,   // don't care
                2,      // both modules should be registered
                0,      // there are no assertions with load-listeners
                null,   // don't care
                null,   // don't care
                0,      // none should fail, since the duplicate class is in different package
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(2, null, 2, 0, null, null, 0, 0, 1);
    }

    @Test
    public void testDuplicateModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our working module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1NewVersion.aar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                3,      // there should be 3 onLoad calls in total
                0,      // zero module should be unloaded
                2,      // two modules should be registered
                0,      // there are no assertions with load-listeners
                0,      // zero module should be un-registered
                null,   // don't care (due to legacy code unregisterAssertion is not called during replace)
                null,   // don't care
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );
    }

    @Test
    public void testScanReturnValue() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        // copy our sample modules
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar"), // one working
                        new CopyData(modulesRootEmptyDir, "com.l7tech.InvalidAssertionClassTest1.aar"), // rest fail
                        new CopyData(modulesRootEmptyDir, "com.l7tech.MissingAssertionClassTest1.aar"), // rest fail
                        new CopyData(modulesRootEmptyDir, "com.l7tech.InvalidManifestTest1.aar"), // rest fail
                        new CopyData(modulesRootEmptyDir, "com.l7tech.NoAssertionsTest1.aar"), // rest fail
                        new CopyData(modulesRootEmptyDir, "com.l7tech.DuplicateClassInParentClassLoaderTest1.aar") // rest fail
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                6,          // total of 6 new modules
                null,       // don't care
                1,          // only one module should be registered
                0,      // there are no assertions with load-listeners
                null,       // don't care
                null,       // don't care
                null,       // don't care
                1,          // one module should be skipped (the one with no assertions)
                1           // one assertions per module
        );
    }

    /**
     * Due to the mandatory file locking, modules cannot be deleted from disk,
     * therefore this test is not valid on Windows platform, it will fail and should be ignored.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void testModuleReplace() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our working module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);
        // there is one new module, and it should be registered
        verifyLoadedModules(1, 1);
        // make sure isAssertionRegistered is called
        Mockito.verify(modularAssertionCallbacks, Mockito.times(1)).isAssertionRegistered(Mockito.anyString());

        // replace our working sample module
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{"com.l7tech.WorkingTest1.aar"}
        );
        // copy the new version over
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1NewVersion.aar", "com.l7tech.WorkingTest1.aar")
                }
        );

        // do another scan, to load our modified module
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes are found", changesMade);

        verifyAssertionScanner(
                2,      // there should be two onLoad calls in total
                1,      // previous module should be unloaded (i.e. a call to onUnloaded)
                2,      // both modules should be registered
                0,      // there are no assertions with load-listeners
                1,      // previous module should be un-registered
                null,   // don't care (due to legacy code unregisterAssertion is not called during replace)
                0,      // none should fail, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // make sure isAssertionRegistered is not called this time
        // i.e. was called only once, the initial time, meaning the new module offered the same classes
        Mockito.verify(modularAssertionCallbacks, Mockito.times(1)).isAssertionRegistered(Mockito.anyString());

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(2, null, 2, 0, null, null, 0, 0, 1);
    }

    /**
     * Due to the mandatory file locking, modules cannot be deleted from disk,
     * therefore this test is not valid on Windows platform, it will fail and should be ignored.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void testReplaceModuleWithDuplicateClassFromClassLoader() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our working module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);
        // there is one new module, and it should be registered
        verifyLoadedModules(1, 1);
        // make sure isAssertionRegistered is called
        Mockito.verify(modularAssertionCallbacks, Mockito.times(1)).isAssertionRegistered(Mockito.anyString());

        // replace our working sample module
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{"com.l7tech.WorkingTest1.aar"}
        );
        // copy the new version over
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.DuplicateClassInParentClassLoaderTest1.aar", "com.l7tech.WorkingTest1.aar")
                }
        );

        // do another scan, to load our modified module
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes since the new module failed to load", changesMade);

        verifyAssertionScanner(
                2,      // there should be two onLoad calls in total
                0,      // previous module should not be unloaded, since loading the new one failed
                1,      // only the initial module should be registered, since loading the new one failed
                0,      // there are no assertions with load-listeners
                0,      // previous module should not be un-registered
                null,   // don't care (due to legacy code unregisterAssertion is not called during replace)
                1,      // should fail, so one
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // make sure isAssertionRegistered is not called this time
        // i.e. was called only once, the initial time, meaning the new module offered the same classes
        Mockito.verify(modularAssertionCallbacks, Mockito.times(1)).isAssertionRegistered(Mockito.anyString());

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(null, 0, 1, 0, 0, null, null, null, 1);
    }

    /**
     * Due to the mandatory file locking, modules cannot be deleted from disk,
     * therefore this test is not valid on Windows platform, it will fail and should be ignored.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void testReplaceModuleWithDuplicateClass() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our working module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);
        // there is one new module, and it should be registered
        verifyLoadedModules(2, 2);
        // make sure isAssertionRegistered is called twice
        Mockito.verify(modularAssertionCallbacks, Mockito.times(2)).isAssertionRegistered(Mockito.anyString());

        // replace our working sample module
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{"com.l7tech.WorkingTest1.aar"}
        );
        // copy the new version over
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest2.aar", "com.l7tech.WorkingTest1.aar")
                }
        );

        // do another scan, to load our modified module
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes since the new module failed to load", changesMade);

        verifyAssertionScanner(
                3,      // there should be 3 onLoad calls in total
                0,      // previous module should not be unloaded, since loading the new one failed
                2,      // only the initial modules should be registered, since loading the new one failed
                0,      // there are no assertions with load-listeners
                0,      // previous module should not be un-registered
                null,   // don't care (due to legacy code unregisterAssertion is not called during replace)
                1,      // should fail, so one
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // make sure isAssertionRegistered is called this time as well
        Mockito.verify(modularAssertionCallbacks, Mockito.times(3)).isAssertionRegistered(Mockito.anyString());

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no module changes the second time", changesMade);
        verifyAssertionScanner(null, 0, 2, 0, 0, null, null, null, 1);
    }

    /**
     * Due to the mandatory file locking, modules cannot be deleted from disk,
     * therefore this test is not valid on Windows platform, it will fail and should be ignored.
     * <p/>
     * On Windows platform run {@link #testUnloadModulesWorkaround()} unit test instead.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void testUnloadModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        copy_all_files(
                modTmpFolder,
                2,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest#.aar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);
        // verify initial scan (all initial modules should be loaded)
        verifyLoadedModules(
                2,      // there should be 2 onLoad calls in total
                2       // there should be 2 modules registered
        );

        // delete the first module
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.WorkingTest1.aar"
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("removed modules will be unloaded", changesMade);
        // verify module removal
        verifyAssertionScanner(
                2,      // onLoad calls not changed
                1,      // there should be 1 onModuleUnloaded calls in total
                2,      // number of registered modules should remain the same
                0,      // there are no assertions with load-listeners
                1,      // one module should be un-registered
                1,      // one assertion should be un-registered
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // delete the second module
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.WorkingTest2.aar"
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("removed modules will be unloaded", changesMade);
        // verify module removal
        verifyAssertionScanner(
                2,      // onLoad calls not changed
                2,      // there should be 2 onModuleUnloaded calls in total
                2,      // number of registered modules should remain the same
                0,      // there are no assertions with load-listeners
                2,      // 2 modules should be un-registered
                2,      // 2 assertions should be un-registered
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(2, 2, 2, 0, 2, 2, 0, 0, 1);
    }

    @Test
    public void testUnloadModulesWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        copy_all_files(
                modTmpFolder,
                2,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest#.aar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);
        // verify initial scan (all initial modules should be loaded)
        verifyLoadedModules(
                2,      // there should be 2 onLoad calls in total
                2       // there should be 2 modules registered
        );

        // disable the first module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.WorkingTest1.aar",
                                "com.l7tech.WorkingTest1.aar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("disabled modules will be unloaded", changesMade);
        // verify module removal
        verifyAssertionScanner(
                2,      // onLoad calls not changed
                1,      // there should be 1 onModuleUnloaded calls in total
                2,      // number of registered modules should remain the same
                0,      // there are no assertions with load-listeners
                1,      // one module should be un-registered
                1,      // one assertion should be un-registered
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // delete the second module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.WorkingTest2.aar",
                                "com.l7tech.WorkingTest2.aar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("disabled modules will be unloaded", changesMade);
        // verify module removal
        verifyAssertionScanner(
                2,      // onLoad calls not changed
                2,      // there should be 2 onModuleUnloaded calls in total
                2,      // number of registered modules should remain the same
                0,      // there are no assertions with load-listeners
                2,      // 2 modules should be un-registered
                2,      // 2 assertions should be un-registered
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(2, 2, 2, 0, 2, 2, 0, 0, 1);

        // reload them again (delete the first disabled module)
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.WorkingTest1.aar" + DISABLED_MODULES_SUFFIX
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("re-enabled module will be loaded", changesMade);
        // verify module removal
        verifyAssertionScanner(
                3,      // one more onLoad call, so 3 in total
                2,      // onModuleUnloaded calls not changed
                3,      // one more registered module, so 3 in total
                0,      // there are no assertions with load-listeners
                2,      // number of un-registered modules not changed
                2,      // number of un-registered assertions not changed
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // reload them again (delete the second disabled module)
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.WorkingTest2.aar" + DISABLED_MODULES_SUFFIX
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("re-enabled module will be loaded", changesMade);
        // verify module removal
        verifyAssertionScanner(
                4,      // one more onLoad call, so 4 in total
                2,      // onModuleUnloaded calls not changed
                4,      // one more registered module, so 4 in total
                0,      // there are no assertions with load-listeners
                2,      // number of un-registered modules not changed
                2,      // number of un-registered assertions not changed
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(4, 2, 4, 0, 2, 2, 0, 0, 1);
    }

    @Test
    public void testUnloadModulesWorkaroundNotEnabled() throws Exception {
        // disable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(null);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        copy_all_files(
                modTmpFolder,
                2,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest#.aar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);
        // verify initial scan (all initial modules should be loaded)
        verifyLoadedModules(
                2,      // there should be 2 onLoad calls in total
                2       // there should be 2 modules registered
        );

        // disable both module
        copy_all_files(
                modTmpFolder,
                2,
                new CopyData[]{
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.WorkingTest#.aar",
                                "com.l7tech.WorkingTest#.aar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("workaround disabled, so no changes", changesMade);
        // verify module removal
        verifyAssertionScanner(
                2,      // onLoad calls not changed
                0,      // none onModuleUnloaded calls
                2,      // number of registered modules should remain the same
                0,      // there are no assertions with load-listeners, so zero
                0,      // no un-registered modules, so zero
                0,      // no un-registered assertion, so zero
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(2, 0, 2, 0, 0, 0, 0, 0, 1);

        // reload them again (delete the both disabled module)
        delete_all_files(
                modTmpFolder,
                2,
                new String[]{
                        "com.l7tech.WorkingTest#.aar" + DISABLED_MODULES_SUFFIX
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("workaround disabled, so no changes", changesMade);
        // verify module removal
        verifyAssertionScanner(
                2,      // onLoad calls not changed
                0,      // none onModuleUnloaded calls
                2,      // number of registered modules should remain the same
                0,      // there are no assertions with load-listeners, so zero
                0,      // no un-registered modules, so zero
                0,      // no un-registered assertion, so zero
                0,      // none should be failed, so zero
                0,      // none should be skipped, so zero
                1       // one assertion per module
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(2, 0, 2, 0, 0, 0, 0, 0, 1);
    }

    /**
     * Due to the mandatory file locking, modules cannot be deleted from disk,
     * therefore this test is not valid on Windows platform, it will fail and should be ignored.
     * <p/>
     * On Windows platform run {@link #testModulesWithLoadAndUnloadListenersWorkaround()} unit test instead.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void testModulesWithLoadAndUnloadListeners() throws Exception {
        final int numOfModules = 2;
        final Set<Pair<String, String>> loadListenerMethodsCalled = stubLoadListeners(numOfModules);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our sample files
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest3.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar")
                }
        );

        // do a scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // 2 onLoad calls in total
                0,              // none onModuleUnloaded calls
                numOfModules,   // 2 registered modules
                numOfModules,   // 2 modules with load-listeners
                0,              // no un-registered modules, so zero
                0,              // no un-registered assertion, so zero
                0,              // none should be failed, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("two load-listeners detected (onModuleLoaded)", numOfModules, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleLoaded->ModuleLoadListener3 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModuleLoadListener3")));
        Assert.assertFalse("onModuleUnloaded->ModuleLoadListener3 not detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener3")));
        Assert.assertTrue("onModuleLoaded->ModuleLoadListener4 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModuleLoadListener4")));
        Assert.assertFalse("onModuleUnloaded->ModuleLoadListener4 not detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener4")));

        // remove all modules
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.WorkingTest3.aar",
                        "com.l7tech.WorkingTest4.aar"
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // onLoad calls not changed
                numOfModules,   // 2 onModuleUnloaded calls
                numOfModules,   // registered modules not changed
                numOfModules,   // number of modules with load-listeners not changed
                numOfModules,   // 2 modules un-registered
                numOfModules,   // 2 assertion un-registered
                0,              // none should be failed, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("all 4 load-listeners detected", 2 * numOfModules, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleUnloaded->ModuleLoadListener3 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener3")));
        Assert.assertTrue("onModuleUnloaded->ModuleLoadListener4 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener4")));

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, 0, 0, 1);
        Assert.assertEquals("all 4 load-listeners detected", 2 * numOfModules, loadListenerMethodsCalled.size());
    }

    @Test
    public void testModulesWithLoadAndUnloadListenersWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        final int numOfModules = 2;
        final Set<Pair<String, String>> loadListenerMethodsCalled = stubLoadListeners(numOfModules);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our sample files
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest3.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest4.aar")
                }
        );

        // do a scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // 2 onLoad calls in total
                0,              // none onModuleUnloaded calls
                numOfModules,   // 2 registered modules
                numOfModules,   // 2 modules with load-listeners
                0,              // no un-registered modules, so zero
                0,              // no un-registered assertion, so zero
                0,              // none should be failed, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("two load-listeners detected (onModuleLoaded)", numOfModules, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleLoaded->ModuleLoadListener3 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModuleLoadListener3")));
        Assert.assertFalse("onModuleUnloaded->ModuleLoadListener3 not detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener3")));
        Assert.assertTrue("onModuleLoaded->ModuleLoadListener4 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModuleLoadListener4")));
        Assert.assertFalse("onModuleUnloaded->ModuleLoadListener4 not detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener4")));

        // remove all modules
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                modulesRootEmptyDir,
                               "com.l7tech.WorkingTest3.aar",
                                "com.l7tech.WorkingTest3.aar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.WorkingTest4.aar",
                                "com.l7tech.WorkingTest4.aar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // onLoad calls not changed
                numOfModules,   // 2 onModuleUnloaded calls
                numOfModules,   // registered modules not changed
                numOfModules,   // number of modules with load-listeners not changed
                numOfModules,   // 2 modules un-registered
                numOfModules,   // 2 assertion un-registered
                0,              // none should be failed, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("all 4 load-listeners detected", 2 * numOfModules, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleUnloaded->ModuleLoadListener3 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener3")));
        Assert.assertTrue("onModuleUnloaded->ModuleLoadListener4 detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModuleLoadListener4")));

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, 0, 0, 1);
        Assert.assertEquals("all 4 load-listeners detected", 2 * numOfModules, loadListenerMethodsCalled.size());
    }

    /**
     * Due to the mandatory file locking, modules cannot be deleted from disk,
     * therefore this test is not valid on Windows platform, it will fail and should be ignored.
     * <p/>
     * On Windows platform run {@link #testModulesWithLoadListenersThrowExceptionWorkaround()} unit test instead.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void testModulesWithLoadListenersThrowException() throws Exception {
        final int numOfModules = 4;
        final Set<Pair<String, String>> loadListenerMethodsCalled = stubLoadListeners(numOfModules);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our sample files
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerOnLoadExceptionTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerOnUnloadExceptionTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerClassMissingTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerClassInvalidTest1.aar")
                }
        );

        // do a scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("even though the module throw exception during load, SSG will log the exception and load the module, so changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // 4 onLoad calls in total
                0,              // none onModuleUnloaded calls
                numOfModules,   // 4 registered modules
                numOfModules,   // 4 module with load-listeners
                0,              // no un-registered modules, so zero
                0,              // no un-registered assertion, so zero
                0,              // none should fail, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("only one load-listeners detected (LoadListenerOnUnloadExceptionTest1)", 1, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleLoaded->ModularLoadListenerOnUnloadExceptionTest1Listener detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModularLoadListenerOnUnloadExceptionTest1Listener")));

        // remove all modules
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.LoadListenerOnLoadExceptionTest1.aar",
                        "com.l7tech.LoadListenerOnUnloadExceptionTest1.aar",
                        "com.l7tech.LoadListenerClassMissingTest1.aar",
                        "com.l7tech.LoadListenerClassInvalidTest1.aar"
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // onLoad calls not changed
                numOfModules,   // 4 onModuleUnloaded calls
                numOfModules,   // registered modules not changed
                numOfModules,   // number of modules with load-listeners not changed
                numOfModules,   // 4 modules un-registered
                numOfModules,   // 4 assertion un-registered
                0,              // none should be failed, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("2 load-listeners detected", 2, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleLoaded->ModularLoadListenerOnUnloadExceptionTest1Listener detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModularLoadListenerOnUnloadExceptionTest1Listener")));
        Assert.assertTrue("onModuleUnloaded->ModularLoadListenerOnLoadExceptionTest1Listener detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModularLoadListenerOnLoadExceptionTest1Listener")));

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, 0, 0, 1);
        Assert.assertEquals("2 load-listeners detected", 2, loadListenerMethodsCalled.size());
    }

    @Test
    public void testModulesWithLoadListenersThrowExceptionWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        final int numOfModules = 4;
        final Set<Pair<String, String>> loadListenerMethodsCalled = stubLoadListeners(numOfModules);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our sample files
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerOnLoadExceptionTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerOnUnloadExceptionTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerClassMissingTest1.aar"),
                        new CopyData(modulesRootEmptyDir, "com.l7tech.LoadListenerClassInvalidTest1.aar")
                }
        );

        // do a scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("even though the module throw exception during load, SSG will log the exception and load the module, so changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // 4 onLoad calls in total
                0,              // none onModuleUnloaded calls
                numOfModules,   // 4 registered modules
                numOfModules,   // 4 module with load-listeners
                0,              // no un-registered modules, so zero
                0,              // no un-registered assertion, so zero
                0,              // none should fail, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("only one load-listeners detected (LoadListenerOnUnloadExceptionTest1)", 1, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleLoaded->ModularLoadListenerOnUnloadExceptionTest1Listener detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModularLoadListenerOnUnloadExceptionTest1Listener")));

        // remove all modules
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.LoadListenerOnLoadExceptionTest1.aar",
                                "com.l7tech.LoadListenerOnLoadExceptionTest1.aar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.LoadListenerOnUnloadExceptionTest1.aar",
                                "com.l7tech.LoadListenerOnUnloadExceptionTest1.aar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.LoadListenerClassMissingTest1.aar",
                                "com.l7tech.LoadListenerClassMissingTest1.aar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.LoadListenerClassInvalidTest1.aar",
                                "com.l7tech.LoadListenerClassInvalidTest1.aar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                numOfModules,   // onLoad calls not changed
                numOfModules,   // 4 onModuleUnloaded calls
                numOfModules,   // registered modules not changed
                numOfModules,   // number of modules with load-listeners not changed
                numOfModules,   // 4 modules un-registered
                numOfModules,   // 4 assertion un-registered
                0,              // none should be failed, so zero
                0,              // none should be skipped, so zero
                1               // one assertion per module
        );
        Assert.assertEquals("2 load-listeners detected", 2, loadListenerMethodsCalled.size());
        Assert.assertTrue("onModuleLoaded->ModularLoadListenerOnUnloadExceptionTest1Listener detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleLoaded", "ModularLoadListenerOnUnloadExceptionTest1Listener")));
        Assert.assertTrue("onModuleUnloaded->ModularLoadListenerOnLoadExceptionTest1Listener detected", loadListenerMethodsCalled.contains(new Pair<>("onModuleUnloaded", "ModularLoadListenerOnLoadExceptionTest1Listener")));

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, numOfModules, 0, 0, 1);
        Assert.assertEquals("2 load-listeners detected", 2, loadListenerMethodsCalled.size());
    }


    @Test
    public void testModulesWithDualAssertionsWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(modulesConfig.getModuleDir()).thenReturn(modTmpFolder);

        stubAssertionRegisterCallbacks();

        // copy our sample files
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(modulesRootEmptyDir, "com.l7tech.WorkingTest5.aar")
                }
        );

        // do a scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                1,   // one onLoad calls in total
                0,   // none onModuleUnloaded calls
                1,   // one registered module
                0,   // no modules with load-listeners, so zero
                0,   // no un-registered modules, so zero
                0,   // no un-registered assertions, so zero
                0,   // none should fail, so zero
                0,   // none should be skipped, so zero
                2    // two assertions per module
        );

        // remove all modules
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                modulesRootEmptyDir,
                                "com.l7tech.WorkingTest5.aar",
                                "com.l7tech.WorkingTest5.aar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("changes found", changesMade);

        verifyAssertionScanner(
                1,   // onLoad calls not changed
                1,   // one onModuleUnloaded calls
                1,   // registered modules not changed
                0,   // modules with load-listeners not changed
                1,   // one un-registered module
                2,   // one un-registered assertions
                0,   // none should fail, so zero
                0,   // none should be skipped, so zero
                2    // two assertions per module
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no changes", changesMade);
        // verify no changes made
        verifyAssertionScanner(1, 1, 1, 0, 1, 2, 0, 0, 2);
    }
}
