package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.util.Config;
import com.l7tech.util.FileUtils;
import junit.framework.Assert;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;

/**
 * The Custom Assertion jars used by the unit test. The filename must start with "com.l7tech.". Otherwise, it will
 * not be loaded by the unit test. See idea_project.xml file for list of valid unit test resource names.
 * <p/>
 * Content of module/custom test folders:
 * <pre>
 * dual
 *  |_ com.l7tech.DualAssertionsTest1.jar                   [module having two CustomAssertion interfaces registered]
 *  |_ dummy.png                                            [a placeholder file for this folder]
 * dynamic                                                  [5 different CustomAssertions implementing CustomDynamicLoader interface]
 *  |_ com.l7tech.DynamicCustomAssertionsTest1.jar          [a CustomAssertion implementing CustomDynamicLoader interface]
 *  |_ com.l7tech.DynamicCustomAssertionsTest2.jar          [a CustomAssertion implementing CustomDynamicLoader interface]
 *  |_ com.l7tech.DynamicCustomAssertionsTest3.jar          [a CustomAssertion implementing CustomDynamicLoader interface]
 *  |_ com.l7tech.DynamicCustomAssertionsTest4.jar          [a CustomAssertion implementing CustomDynamicLoader interface]
 *  |_ com.l7tech.DynamicCustomAssertionsTest5.jar          [a CustomAssertion implementing CustomDynamicLoader interface]
 *  |_ dummy.png                                            [a placeholder file for this folder]
 * non_dynamic                                              [5 different basic CustomAssertions (i.e. not implementing CustomDynamicLoader interface)]
 *  |_ com.l7tech.NonDynamicCustomAssertionTest1.jar        [basic CustomAssertion implementing CustomMessageTargetable interface]
 *  |_ com.l7tech.NonDynamicCustomAssertionTest1.jar        [basic CustomAssertion implementing CustomMessageTargetable interface]
 *  |_ com.l7tech.NonDynamicCustomAssertionTest1.jar        [basic CustomAssertion implementing CustomMessageTargetable interface]
 *  |_ com.l7tech.NonDynamicCustomAssertionTest1.jar        [basic CustomAssertion implementing CustomMessageTargetable interface]
 *  |_ com.l7tech.NonDynamicCustomAssertionTest1.jar        [basic CustomAssertion implementing CustomMessageTargetable interface]
 *  |_ dummy.png                                            [a placeholder file for this folder]
 *  |_ life_listener                                                       [5 different CustomAssertions implementing CustomLifecycleListener interface]
 *    |_ com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest1.jar   [a CustomAssertion implementing CustomLifecycleListener interface]
 *    |_ com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest2.jar   [a CustomAssertion implementing CustomLifecycleListener interface]
 *    |_ com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest3.jar   [a CustomAssertion implementing CustomLifecycleListener interface]
 *    |_ com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest4.jar   [a CustomAssertion implementing CustomLifecycleListener interface]
 *    |_ com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest5.jar   [a CustomAssertion implementing CustomLifecycleListener interface]
 *    |_ dummy.png                                                         [a placeholder file for this folder]
 * failed                                                                               [custom assertion failing on either load or unload]
 *  |_ descriptor                                                                       [contains assertions with broken custom_assertions.properties file]
 *    |_ com.l7tech.BrokenDescriptorTest1.jar                                           [having missing CustomAssertion class]
 *    |_ com.l7tech.BrokenDescriptorTest2.jar                                           [having missing ServiceInvocation class]
 *    |_ com.l7tech.BrokenDescriptorTest3.jar                                           [missing custom_assertions.properties file]
 *    |_ com.l7tech.BrokenDescriptorTest4.jar                                           [having class not implementing CustomAssertion interface]
 *    |_ com.l7tech.BrokenDescriptorTest5.jar                                           [having class not implementing ServiceInvocation interface]
 *    |_ dummy.png                                                                      [a placeholder file for this folder]
 *  |_ onLoad                                                                           [contains assertions which will fail during load]
 *    |_ com.l7tech.DynamicOnLoadFailCustomAssertionsTest1                              [CustomAssertion implementing CustomDynamicLoader interface and throwing CustomLoaderException on onLoad]
 *    |_ com.l7tech.DynamicOnLoadFailCustomAssertionsTest2                              [CustomAssertion implementing CustomDynamicLoader interface and throwing RuntimeException on onLoad]
 *    |_ com.l7tech.NonDynamicLifecycleListenerOnLoadFailCustomAssertionsTest1          [CustomAssertion implementing CustomLifecycleListener interface and throwing CustomLoaderException on onLoad]
 *    |_ com.l7tech.NonDynamicLifecycleListenerOnLoadFailCustomAssertionsTest1          [CustomAssertion implementing CustomLifecycleListener interface and throwing RuntimeException on onLoad]
 *    |_ dummy.png                                                                      [a placeholder file for this folder]
 *  |_ onUnload                                                                         [5 different CustomAssertions implementing CustomLifecycleListener interface]
 *    |_ com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar                        [CustomAssertion implementing CustomDynamicLoader interface and throwing RuntimeException on onUnload]
 *    |_ com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar    [CustomAssertion implementing CustomLifecycleListener interface and throwing RuntimeException on onUnload]
 *    |_ dummy.png                                                                      [a placeholder file for this folder]
 * </pre>
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionsScannerTest extends ModulesScannerTestBase {

    // The module temp directory name. This is where third-party jars in Custom Assertion jar are extracted to during startup.
    private static final String MODULES_WORK_TEMP_DIR_NAME = "l7tech-CustomAssertionsScannerTest-Temp";
    private static final String MODULES_TEMP_DIR_NAME = "l7tech-CustomAssertionsScannerTest";

    private static final String NON_DYNAMIC_MODULES_EMPTY_DIR               = "com/l7tech/server/policy/module/custom/non_dynamic/dummy.png";
    private static final String NON_DYNAMIC_LIFE_LISTENER_MODULES_EMPTY_DIR = "com/l7tech/server/policy/module/custom/non_dynamic/life_listener/dummy.png";
    private static final String DYNAMIC_MODULES_EMPTY_DIR                   = "com/l7tech/server/policy/module/custom/dynamic/dummy.png";
    private static final String FAILED_ON_LOAD_MODULES_EMPTY_DIR            = "com/l7tech/server/policy/module/custom/failed/onLoad/dummy.png";
    private static final String FAILED_ON_UNLOAD_MODULES_EMPTY_DIR          = "com/l7tech/server/policy/module/custom/failed/onUnload/dummy.png";
    private static final String DUAL_MODULES_EMPTY_DIR                      = "com/l7tech/server/policy/module/custom/dual/dummy.png";
    private static final String BROKEN_DESCRIPTOR_MODULES_EMPTY_DIR         = "com/l7tech/server/policy/module/custom/failed/descriptor/dummy.png";
    private static final int TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR = 5;

    private static final String DISABLED_MODULES_SUFFIX = ".disabled";

    private static String nonDynamicModulesEmptyDir;
    private static String nonDynamicLifeListenerModulesEmptyDir;
    private static String dynamicModulesEmptyDir;
    private static String failedOnLoadModulesEmptyDir;
    private static String failedOnUnloadModulesEmptyDir;
    private static String dualModulesEmptyDir;
    private static String brokenDescriptorModulesEmptyDir;

    private static File modTmpFolder, modTmpWorkFolder;

    @Mock
    private Config configMock;

    @Mock
    private ScannerCallbacks.CustomAssertion customAssertionCallbacks;

    private CustomAssertionModulesConfig modulesConfig;
    private CustomAssertionsScanner assertionsScanner;

    /**
     * Verifies that correct methods are called from <tt>assertionsScanner</tt> and that
     *
     * @param onModuleLoadCalls              expected number of {@link com.l7tech.server.policy.module.ModulesScanner#onModuleLoad(java.io.File, String, long) onModuleLoad} calls.
     * @param onModuleUnloadCalls            expected number of {@link com.l7tech.server.policy.module.ModulesScanner#onModuleUnload(BaseAssertionModule) onModuleUnload} calls.
     * @param numberOfLoadedModules          expected number of loaded modules
     *                                       (i.e. calls to {@link com.l7tech.server.policy.module.ScannerCallbacks#publishEvent(org.springframework.context.ApplicationEvent) publishEvent}
     *                                       with {@link com.l7tech.server.policy.module.AssertionModuleRegistrationEvent}).
     * @param numberOfUnloadedModules        expected number of unloaded modules
     *                                       (i.e. calls to {@link com.l7tech.server.policy.module.ScannerCallbacks#publishEvent(org.springframework.context.ApplicationEvent) publishEvent}
     *                                       with {@link com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent}).
     * @param numberOfFailedModules          expected number of failed modules.
     * @param numberOfSkippedModules         expected number of skipped modules.
     * @param numberOfAssertionsPerModule    expected number of assertions per module.
     */
    private void verifyAssertionScanner(
            final int onModuleLoadCalls,
            final int onModuleUnloadCalls,
            final int numberOfLoadedModules,
            final int numberOfUnloadedModules,
            final int numberOfFailedModules,
            final int numberOfSkippedModules,
            final int numberOfAssertionsPerModule
    ) throws Exception {
        // make sure we load all our modules
        Mockito.verify(assertionsScanner, Mockito.times(onModuleLoadCalls)).onModuleLoad(Mockito.<File>any(), Mockito.anyString(), Mockito.anyLong());
        // make sure all modules have been registered
        Mockito.verify(customAssertionCallbacks, Mockito.times(numberOfLoadedModules)).publishEvent(Mockito.isA(AssertionModuleRegistrationEvent.class));
        // make sure all assertions have been registered
        Mockito.verify(customAssertionCallbacks, Mockito.times(numberOfLoadedModules*numberOfAssertionsPerModule)).registerAssertion(Mockito.<CustomAssertionDescriptor>any(), Mockito.isA(CustomAssertionClassLoader.class));

        // make sure no modules have been unloaded.
        Mockito.verify(assertionsScanner, Mockito.times(onModuleUnloadCalls)).onModuleUnload(Mockito.<CustomAssertionModule>any());
        // make sure no modules have been unregistered
        Mockito.verify(customAssertionCallbacks, Mockito.times(numberOfUnloadedModules)).publishEvent(Mockito.isA(AssertionModuleUnregistrationEvent.class));
        // make sure all assertions have been unregistered
        Mockito.verify(customAssertionCallbacks, Mockito.times(numberOfUnloadedModules*numberOfAssertionsPerModule)).unregisterAssertion(Mockito.<CustomAssertionDescriptor>any());

        // assert counters and collections
        Assert.assertEquals("correct number of modules have been loaded", numberOfLoadedModules - numberOfUnloadedModules, assertionsScanner.scannedModules.size());
        Assert.assertEquals("correct number of modules have failed", numberOfFailedModules, assertionsScanner.failModTimes.size());
        Assert.assertEquals("correct number of modules were skipped", numberOfSkippedModules, assertionsScanner.skipModTimes.size());

        // assert assertions
        Assert.assertTrue(
                "scannedModules and getModules() are equal",
                Arrays.equals(
                        assertionsScanner.scannedModules.values().toArray(),
                        assertionsScanner.getModules().toArray()
                )
        );
        for (final CustomAssertionModule module : assertionsScanner.getModules()) {
            Assert.assertEquals("expected number of assertion prototypes", numberOfAssertionsPerModule, module.getDescriptors().size());
        }
    }

    /**
     * Utility function for {@link #verifyAssertionScanner(int, int, int, int, int, int, int) verifyAssertionScanner}
     * with reduced arguments, targeted for verifying modules loading process.
     *
     * @param onModuleLoadCalls        expected number of {@link CustomAssertionsScanner#onModuleLoad(java.io.File, String, long) onModuleLoad} calls.
     * @param numberOfLoadedModules    expected number of loaded modules
     *                                 (i.e. calls to {@link ScannerCallbacks.CustomAssertion#publishEvent(org.springframework.context.ApplicationEvent) publishEvent}
     *                                 with {@link AssertionModuleRegistrationEvent}).
     * @throws Exception
     */
    private void verifyLoadedModules(final int onModuleLoadCalls, final int numberOfLoadedModules) throws Exception {
        verifyAssertionScanner(onModuleLoadCalls, 0, numberOfLoadedModules, 0, 0, 0, 1);
    }

    @BeforeClass
    public static void setUpOnce() throws Exception {
        // On Windows platform jar files are locked by the JVM, therefore they cannot be cleaned up on exit.
        // On start, we will loop through all previously created temporary folders and delete them,
        // which means that at a worst case scenario we will only end up with files from a single run.
        cleanUpTemporaryFilesFromPreviousRuns(MODULES_WORK_TEMP_DIR_NAME, MODULES_TEMP_DIR_NAME);

        Assert.assertFalse("NON_DYNAMIC_MODULES_EMPTY_DIR exists", (nonDynamicModulesEmptyDir = extractFolder(NON_DYNAMIC_MODULES_EMPTY_DIR)).isEmpty());
        Assert.assertFalse("NON_DYNAMIC_LIFE_LISTENER_MODULES_EMPTY_DIR exists", (nonDynamicLifeListenerModulesEmptyDir = extractFolder(NON_DYNAMIC_LIFE_LISTENER_MODULES_EMPTY_DIR)).isEmpty());
        Assert.assertFalse("DYNAMIC_MODULES_EMPTY_DIR exists", (dynamicModulesEmptyDir = extractFolder(DYNAMIC_MODULES_EMPTY_DIR)).isEmpty());
        Assert.assertFalse("FAILED_ON_LOAD_MODULES_EMPTY_DIR exists", (failedOnLoadModulesEmptyDir = extractFolder(FAILED_ON_LOAD_MODULES_EMPTY_DIR)).isEmpty());
        Assert.assertFalse("FAILED_ON_UNLOAD_MODULES_EMPTY_DIR exists", (failedOnUnloadModulesEmptyDir = extractFolder(FAILED_ON_UNLOAD_MODULES_EMPTY_DIR)).isEmpty());
        Assert.assertFalse("DUAL_MODULES_EMPTY_DIR exists", (dualModulesEmptyDir = extractFolder(DUAL_MODULES_EMPTY_DIR)).isEmpty());
        Assert.assertFalse("BROKEN_DESCRIPTOR_MODULES_EMPTY_DIR exists", (brokenDescriptorModulesEmptyDir = extractFolder(BROKEN_DESCRIPTOR_MODULES_EMPTY_DIR)).isEmpty());
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

        // Create module temp directory.
        modTmpWorkFolder = getTempFolder(MODULES_WORK_TEMP_DIR_NAME);

        // mock config
        Mockito.when(configMock.getProperty("custom.assertions.file")).thenReturn("custom_assertions.properties");
        Mockito.when(configMock.getProperty("custom.assertions.temp")).thenReturn(modTmpWorkFolder.getAbsolutePath());
        Mockito.when(configMock.getBooleanProperty(Mockito.eq("custom.assertions.rescan.enabled"), Mockito.anyBoolean())).thenReturn(true);
        Mockito.when(configMock.getLongProperty(Mockito.eq("custom.assertions.rescan.millis"), Mockito.anyLong())).thenReturn(1000L);

        // create custom assertions modules config
        modulesConfig = Mockito.spy(new CustomAssertionModulesConfig(configMock));

        // create custom assertions scanner
        assertionsScanner = Mockito.spy(new CustomAssertionsScanner(modulesConfig, customAssertionCallbacks));

        // forcing it to true, since on systems with one second resolution of File.lastModified method
        // (like linux, OSX etc.), quick folder modifications will not be detected.
        //Mockito.doReturn(true).when(assertionsScanner).isScanNeeded(Mockito.<File>any(), Mockito.anyLong());
    }

    @After
    public void tearDown() throws Exception {
        // make sure all resources are cleared
        try { assertionsScanner.destroy(); } catch (Throwable ignore) { }

        // remove any temporary folders used
        if (modTmpWorkFolder != null) {
            FileUtils.deleteDir(modTmpWorkFolder);
            tmpFiles.remove(modTmpWorkFolder.getAbsolutePath());
        }

        // remove any temporary folders used
        if (modTmpFolder != null) {
            FileUtils.deleteDir(modTmpFolder);
            tmpFiles.remove(modTmpFolder.getAbsolutePath());
        }
    }

    @Test
    public void testFeatureDisabled() throws Exception {
        // set modules dir
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(nonDynamicModulesEmptyDir);

        // enable scanning
        Mockito.doReturn(true).when(modulesConfig).isScanningEnabled();

        // disable feature
        Mockito.doReturn(false).when(modulesConfig).isFeatureEnabled();

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("feature disabled, should return false", changesMade);

        verifyLoadedModules(0, 0);
    }

    @Test
    public void testScanningDisabled() throws Exception {
        // set modules dir
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(nonDynamicModulesEmptyDir);

        // enable feature
        Mockito.doReturn(true).when(modulesConfig).isFeatureEnabled();

        // disable scanning
        Mockito.doReturn(false).when(modulesConfig).isScanningEnabled();

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("feature disabled, should return false", changesMade);

        verifyLoadedModules(0, 0);
    }

    @Test
    public void testScanNonDynamicModulesOneTime() throws Exception {
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(nonDynamicModulesEmptyDir);

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);

        verifyLoadedModules(TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR, TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyLoadedModules(TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR, TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR);
    }

    @Test
    public void testScanNonDynamicLifeListenerModulesOneTime() throws Exception {
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(nonDynamicLifeListenerModulesEmptyDir);

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);

        verifyLoadedModules(TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR, TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyLoadedModules(TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR, TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR);
    }

    @Test
    public void testScanDynamicModulesOneTime() throws Exception {
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(dynamicModulesEmptyDir);

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);

        verifyLoadedModules(TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR, TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyLoadedModules(TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR, TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR);
    }

    @Test
    public void testScanAllBrokenDescriptorModules() throws Exception {
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(brokenDescriptorModulesEmptyDir);

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no valid modules, so no chnages", changesMade);

        // There are total of five jars with broken descriptors, 4 of them have a custom_assertion.properties file,
        // (two with missing CustomAssertion and/or ServiceInvocation classes, and two of them doesn't implement
        // CustomAssertion and/or ServiceInvocation interfaces), so they will be not be loaded (new logic), they'll be skipped
        // one doesn't have custom_assertion.properties file, so it will not be loaded (legacy logic), it will be skipped as well
        verifyAssertionScanner(5, 0, 0, 0, 0, 5, 0);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(5, 0, 0, 0, 0, 5, 0);
    }

    @Test
    public void testBrokenDescriptorMissingCustomAssertion() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest1.jar")
                }
        );

        // do initial scan on an empty folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no valid modules, so no changes", changesMade);
        verifyAssertionScanner(1, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void testBrokenDescriptorMissingServiceInvocation() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest2.jar")
                }
        );

        // do initial scan on an empty folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no valid modules, so no changes", changesMade);
        verifyAssertionScanner(1, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void testBrokenDescriptorMissingPropertiesFile() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest3.jar")
                }
        );

        // do initial scan on an empty folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no modules loaded", changesMade);
        verifyAssertionScanner(1, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void testBrokenDescriptorNonCustomAssertion() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest4.jar")
                }
        );

        // do initial scan on an empty folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no valid modules, so no changes", changesMade);
        verifyAssertionScanner(1, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void testBrokenDescriptorNonServiceInvocation() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(brokenDescriptorModulesEmptyDir, "com.l7tech.BrokenDescriptorTest5.jar")
                }
        );

        // do initial scan on an empty folder
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("no valid modules, so no changes", changesMade);
        verifyAssertionScanner(1, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void testLoadingNonDynamicModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        // do initial scan on an empty folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded
        Assert.assertFalse("did not found module changes, since folder is empty", changesMade);

        verifyLoadedModules(0, 0);

        final int numOfFilesToCopy = 3;
        copy_all_files(
                modTmpFolder,
                numOfFilesToCopy,
                new CopyData[]{
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest#.jar")
                }
        );

        // do another scan
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("Even though new files are added in the modules folder, all of them are nun-dynamic therefore will not be loaded", changesMade);

        verifyAssertionScanner(numOfFilesToCopy, 0, 0, 0, 0, numOfFilesToCopy, 1);

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("still no changes", changesMade);

        verifyAssertionScanner(numOfFilesToCopy, 0, 0, 0, 0, numOfFilesToCopy, 1);
    }

    @Test
    public void testLoadingNonDynamicLifeListenerModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        // initially copy few non-dynamic modules
        final int numOfFilesToCopy = 3;
        copy_all_files(
                modTmpFolder,
                numOfFilesToCopy,
                new CopyData[]{
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest#.jar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);

        // verify initial scan (all initial modules should be loaded)
        verifyLoadedModules(numOfFilesToCopy, numOfFilesToCopy);

        // copy additional non-dynamic-life-listener modules
        copy_all_files(
                modTmpFolder,
                numOfFilesToCopy,
                new CopyData[]{
                        new CopyData(nonDynamicLifeListenerModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar")
                }
        );

        // do another scan
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("Even though new modules are added in the modules folder, all of them are nun-dynamic therefore will not be loaded", changesMade);

        // verify initial scan (all initial modules should be loaded)
        verifyAssertionScanner(2 * numOfFilesToCopy, 0, numOfFilesToCopy, 0, 0, numOfFilesToCopy, 1);

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();

        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyAssertionScanner(2 * numOfFilesToCopy, 0, numOfFilesToCopy, 0, 0, numOfFilesToCopy, 1);
    }

    @Test
    public void testLoadingDynamicModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        // initially copy few non-dynamic modules
        final int numOfFilesToCopy = 3;
        copy_all_files(
                modTmpFolder,
                numOfFilesToCopy,
                new CopyData[]{
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest#.jar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);

        // verify initial scan (all initial modules should be loaded)
        verifyLoadedModules(numOfFilesToCopy, numOfFilesToCopy);

        // copy additional non-dynamic-life-listener modules
        copy_all_files(
                modTmpFolder,
                numOfFilesToCopy,
                new CopyData[]{
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest#.jar")
                }
        );

        // do another scan
        changesMade = assertionsScanner.scanModules();
        // new files were dynamic-modules, therefore they'll be loaded
        Assert.assertTrue("there should be modules loaded", changesMade);

        verifyLoadedModules(2 * numOfFilesToCopy, 2 * numOfFilesToCopy);

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyLoadedModules(2 * numOfFilesToCopy, 2 * numOfFilesToCopy);
    }

    /**
     * This test is not valid on Windows platform and should be ignored.<br/>
     * Due to the mandatory file locking, modules cannot be deleted from disk.
     * <p/>
     * On Windows platform run {@link #testUnloadingModulesWorkaround()} unit test instead.
     *
     * @see #testUnloadingModulesWorkaround()
     */
    @Test
    public void testUnloadingModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        CopyData[] copyDataValues;
        copy_all_files(
                modTmpFolder,
                TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR,
                copyDataValues = new CopyData[] {
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest#.jar"),
                        new CopyData(nonDynamicLifeListenerModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest#.jar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);

        // verify initial scan (all initial modules should be loaded)
        final int numberOfModulesLoaded, numberOfOnModuleLoadCalls;
        verifyLoadedModules(
                numberOfOnModuleLoadCalls = copyDataValues.length * TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR,
                numberOfModulesLoaded = copyDataValues.length * TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR
        );

        final int numOfFilesToDelete = 3;
        String[] deleteFiles;
        delete_all_files(
                modTmpFolder,
                numOfFilesToDelete,
                deleteFiles = new String[]{
                        "com.l7tech.NonDynamicCustomAssertionTest#.jar",
                        "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar",
                        "com.l7tech.DynamicCustomAssertionsTest#.jar"
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        // all disabled modules will be unloaded, regardless whether they are dynamic or not
        Assert.assertTrue("removed modules will be unloaded regardless whether they are dynamic or not", changesMade);

        // verify initial scan (all initial modules should be loaded)
        final int numberOfOnModuleUnloadCalls, numberOfUnloadedModules;
        verifyAssertionScanner(
                numberOfOnModuleLoadCalls,
                numberOfOnModuleUnloadCalls = deleteFiles.length * numOfFilesToDelete,
                numberOfModulesLoaded,
                numberOfUnloadedModules = deleteFiles.length * numOfFilesToDelete,
                0,
                0,
                1
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyAssertionScanner(
                numberOfOnModuleLoadCalls,
                numberOfOnModuleUnloadCalls,
                numberOfModulesLoaded,
                numberOfUnloadedModules,
                0,
                0,
                1
        );
    }

    @Test
    public void testUnloadingModulesWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        CopyData[] copyDataValues;
        copy_all_files(
                modTmpFolder,
                TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR,
                copyDataValues = new CopyData[] {
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest#.jar"),
                        new CopyData(nonDynamicLifeListenerModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest#.jar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);

        // verify initial scan (all initial modules should be loaded)
        int numberOfModulesLoaded, numberOfOnModuleLoadCalls;
        verifyLoadedModules(
                numberOfOnModuleLoadCalls = copyDataValues.length * TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR,
                numberOfModulesLoaded = copyDataValues.length * TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR
        );

        final int numOfFilesToDisable = 3;
        copy_all_files(
                modTmpFolder,
                numOfFilesToDisable,
                copyDataValues = new CopyData[]{
                        new CopyData(
                                nonDynamicModulesEmptyDir,
                                "com.l7tech.NonDynamicCustomAssertionTest#.jar",
                                "com.l7tech.NonDynamicCustomAssertionTest#.jar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                nonDynamicLifeListenerModulesEmptyDir,
                                "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar",
                                "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                dynamicModulesEmptyDir,
                                "com.l7tech.DynamicCustomAssertionsTest#.jar",
                                "com.l7tech.DynamicCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        // all disabled modules will be unloaded, regardless whether they are dynamic or not
        Assert.assertTrue("removed modules will be unloaded regardless whether they are dynamic or not", changesMade);

        // verify initial scan (all initial modules should be loaded)
        final int numberOfOnModuleUnloadCalls, numberOfUnloadedModules;
        verifyAssertionScanner(
                numberOfOnModuleLoadCalls,
                numberOfOnModuleUnloadCalls = copyDataValues.length * numOfFilesToDisable,
                numberOfModulesLoaded,
                numberOfUnloadedModules = copyDataValues.length * numOfFilesToDisable,
                0,
                0,
                1
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyAssertionScanner(
                numberOfOnModuleLoadCalls,
                numberOfOnModuleUnloadCalls,
                numberOfModulesLoaded,
                numberOfUnloadedModules,
                0,
                0,
                1
        );

        // remove disabled files
        String[] deletedFiles;
        delete_all_files(
                modTmpFolder,
                numOfFilesToDisable,
                deletedFiles = new String[]{
                        "com.l7tech.NonDynamicCustomAssertionTest#.jar" + DISABLED_MODULES_SUFFIX,
                        "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX,
                        "com.l7tech.DynamicCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX
                }
        );

        // scan modules
        changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("re-enabled module will be loaded", changesMade);

        final int skipped;
        verifyAssertionScanner(
                numberOfOnModuleLoadCalls = numberOfOnModuleLoadCalls + deletedFiles.length * numOfFilesToDisable,
                numberOfOnModuleUnloadCalls,
                numberOfModulesLoaded = numberOfModulesLoaded + numOfFilesToDisable,  // only numOfFilesToDisable are dynamic,
                                                                                      // the rest will not be re-loaded (i.e. skipped)
                numberOfUnloadedModules,
                0,
                skipped = (deletedFiles.length - 1) * numOfFilesToDisable,  // only numOfFilesToDisable are dynamic,
                                                                            // the rest will not be re-loaded (i.e. skipped)
                1
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyAssertionScanner(
                numberOfOnModuleLoadCalls,
                numberOfOnModuleUnloadCalls,
                numberOfModulesLoaded,
                numberOfUnloadedModules,
                0,
                skipped,
                1
        );
    }

    @Test
    public void testUnloadingModulesWorkaroundNotEnabled() throws Exception {
        // disable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(null);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        CopyData[] copyDataValues;
        copy_all_files(
                modTmpFolder,
                TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR,
                copyDataValues = new CopyData[] {
                        new CopyData(nonDynamicModulesEmptyDir, "com.l7tech.NonDynamicCustomAssertionTest#.jar"),
                        new CopyData(nonDynamicLifeListenerModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar"),
                        new CopyData(dynamicModulesEmptyDir, "com.l7tech.DynamicCustomAssertionsTest#.jar")
                }
        );

        // do initial scan
        boolean changesMade = assertionsScanner.scanModules();
        Assert.assertTrue("there should be modules scanned", changesMade);

        // verify initial scan (all initial modules should be loaded)
        final int numberOfModulesLoaded, numberOfOnModuleLoadCalls;
        verifyLoadedModules(
                numberOfOnModuleLoadCalls = copyDataValues.length * TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR,
                numberOfModulesLoaded = copyDataValues.length * TOTAL_NUMBER_OF_FILES_IN_MODULE_DIR
        );

        final int numOfFilesToDisable = 3;
        copy_all_files(
                modTmpFolder,
                numOfFilesToDisable,
                new CopyData[]{
                        new CopyData(
                                nonDynamicModulesEmptyDir,
                                "com.l7tech.NonDynamicCustomAssertionTest#.jar",
                                "com.l7tech.NonDynamicCustomAssertionTest#.jar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                nonDynamicLifeListenerModulesEmptyDir,
                                "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar",
                                "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX
                        ),
                        new CopyData(
                                dynamicModulesEmptyDir,
                                "com.l7tech.DynamicCustomAssertionsTest#.jar",
                                "com.l7tech.DynamicCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do a scan
        changesMade = assertionsScanner.scanModules();
        // all disabled modules will be unloaded, regardless whether they are dynamic or not
        Assert.assertFalse("workaround is disabled therefore there are no changes", changesMade);

        // verify initial scan (all initial modules should be loaded)
        verifyLoadedModules(
                numberOfOnModuleLoadCalls,
                numberOfModulesLoaded
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyLoadedModules(
                numberOfOnModuleLoadCalls,
                numberOfModulesLoaded
        );

        // remove disabled files
        delete_all_files(
                modTmpFolder,
                numOfFilesToDisable,
                new String[]{
                        "com.l7tech.NonDynamicCustomAssertionTest#.jar" + DISABLED_MODULES_SUFFIX,
                        "com.l7tech.NonDynamicLifecycleListenerCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX,
                        "com.l7tech.DynamicCustomAssertionsTest#.jar" + DISABLED_MODULES_SUFFIX
                }
        );

        // scan modules
        changesMade = assertionsScanner.scanModules();
        Assert.assertFalse("workaround disabled, so no changes", changesMade);

        verifyLoadedModules(
                numberOfOnModuleLoadCalls,
                numberOfModulesLoaded
        );

        // do another scan, without any changes to make sure no methods are called
        changesMade = assertionsScanner.scanModules();
        // verify that no module was loaded (even though new files are added)
        Assert.assertFalse("no changes", changesMade);

        verifyLoadedModules(
                numberOfOnModuleLoadCalls,
                numberOfModulesLoaded
        );
    }

    @Test
    public void testScanFailOnLoadModules() throws Exception {
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(failedOnLoadModulesEmptyDir);

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("all modules should fail, therefore no changes", changesMade);

        verifyAssertionScanner(4, 0, 0, 0, 4, 0, 1);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(4, 0, 0, 0, 4, 0, 1);
    }

    /**
     * Even though we are unloading custom assertion modules which throw RuntimeException during unloading
     * (i.e. {@link com.l7tech.policy.assertion.ext.CustomLifecycleListener#onUnload() onUnload}), the module <b>will</b>
     * be unloaded, since it's file was probably deleted (which would cause SSG and SSM to be out-of-sync)
     * <p/>
     * Expected behavior is; all modules will be unloaded regardless of the exception.
     * <p/>
     * This test is not valid on Windows platform and should be ignored.<br/>
     * Due to the mandatory file locking modules cannot be deleted from disk.<br/>
     * On Windows platform run {@link #testScanFailOnUnloadModulesWorkaround()} unit test instead.
     *
     * @see #testScanFailOnUnloadModulesWorkaround()
     */
    @Test
    public void testScanFailOnUnloadModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(failedOnUnloadModulesEmptyDir, "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar"),
                        new CopyData(failedOnUnloadModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("there should be modules scanned", changesMade);

        verifyLoadedModules(2, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyLoadedModules(2, 2);

        delete_all_files(
                modTmpFolder,
                1,
                new String[]{ "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar" }
        );

        // do another scan, after disabling DynamicOnUnloadFailCustomAssertionsTest1.jar
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("one module (DynamicOnUnloadFailCustomAssertionsTest1.jar) will be unloaded, so there will be changes", changesMade);

        verifyAssertionScanner(2, 1, 2, 1, 0, 0, 1);

        delete_all_files(
                modTmpFolder,
                1,
                new String[]{ "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar" }
        );

        // do another scan, after disabling NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("modules will be unloaded, so there will be changes", changesMade);

        verifyAssertionScanner(2, 2, 2, 2, 0, 0, 1);
    }

    @Test
    public void testScanFailOnUnloadModulesWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(failedOnUnloadModulesEmptyDir, "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar"),
                        new CopyData(failedOnUnloadModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("there should be modules scanned", changesMade);

        verifyLoadedModules(2, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyLoadedModules(2, 2);

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                failedOnUnloadModulesEmptyDir,
                                "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar",
                                "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do another scan, after disabling DynamicOnUnloadFailCustomAssertionsTest1.jar
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("one module (DynamicOnUnloadFailCustomAssertionsTest1.jar) will be unloaded, so there will be changes", changesMade);

        verifyAssertionScanner(2, 1, 2, 1, 0, 0, 1);

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                failedOnUnloadModulesEmptyDir,
                                "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar",
                                "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do another scan, after disabling NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("modules will be unloaded, so there will be changes", changesMade);

        verifyAssertionScanner(2, 2, 2, 2, 0, 0, 1);
    }

    @Test
    public void testScanFailOnUnloadModulesWorkaroundNotEnabled() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(null);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(failedOnUnloadModulesEmptyDir, "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar"),
                        new CopyData(failedOnUnloadModulesEmptyDir, "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar")
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("there should be modules scanned", changesMade);

        verifyLoadedModules(2, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyLoadedModules(2, 2);

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                failedOnUnloadModulesEmptyDir,
                                "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar",
                                "com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do another scan, after disabling DynamicOnUnloadFailCustomAssertionsTest1.jar
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("workaround not enabled, so there will be no changes", changesMade);

        verifyLoadedModules(2, 2);

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(
                                failedOnUnloadModulesEmptyDir,
                                "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar",
                                "com.l7tech.NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do another scan, after disabling NonDynamicLifecycleListenerOnUnloadFailCustomAssertionsTest1.jar
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("workaround not enabled, so there will be no changes", changesMade);

        verifyLoadedModules(2, 2);
    }

    /**
     * This test is not valid on Windows platform and should be ignored.<br/>
     * Due to the mandatory file locking, modules cannot be deleted from disk.
     * <p/>
     * On Windows platform run {@link #testDualModulesWorkaround()} unit test instead.
     *
     * @see #testDualModulesWorkaround()
     */
    @Test
    public void testDualModules() throws Exception {
        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(dualModulesEmptyDir, "com.l7tech.DualAssertionsTest1.jar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // remove the module
        delete_all_files(
                modTmpFolder,
                1,
                new String[]{
                        "com.l7tech.DualAssertionsTest1.jar",
                }
        );

        // do another scan to take in count removed modules
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("removed modules should be picked-up", changesMade);

        verifyAssertionScanner(1, 1, 1, 1, 0, 0, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(1, 1, 1, 1, 0, 0, 2);
    }

    @Test
    public void testDualModulesWorkaround() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(DISABLED_MODULES_SUFFIX);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(dualModulesEmptyDir, "com.l7tech.DualAssertionsTest1.jar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // remove the module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(
                                nonDynamicModulesEmptyDir,
                                "com.l7tech.NonDynamicCustomAssertionTest1.jar",
                                "com.l7tech.DualAssertionsTest1.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do another scan to take in count removed modules
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("removed modules should be picked-up", changesMade);

        verifyAssertionScanner(1, 1, 1, 1, 0, 0, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(1, 1, 1, 1, 0, 0, 2);
    }

    @Test
    public void testDualModulesWorkaroundNotEnabled() throws Exception {
        // enable workaround
        Mockito.when(modulesConfig.getDisabledSuffix()).thenReturn(null);

        // create a temporary modules folder for this test
        Assert.assertNotNull(modTmpFolder = getTempFolder(MODULES_TEMP_DIR_NAME));
        // set the modules folder property to the temporary folder
        Mockito.when(configMock.getProperty("custom.assertions.modules")).thenReturn(modTmpFolder.getAbsolutePath());

        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[]{
                        new CopyData(dualModulesEmptyDir, "com.l7tech.DualAssertionsTest1.jar"),
                }
        );

        // scan modules folder
        boolean changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertTrue("did found module changes", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // remove the module
        copy_all_files(
                modTmpFolder,
                1,
                new CopyData[] {
                        new CopyData(
                                nonDynamicModulesEmptyDir,
                                "com.l7tech.NonDynamicCustomAssertionTest1.jar",
                                "com.l7tech.DualAssertionsTest1.jar" + DISABLED_MODULES_SUFFIX
                        )
                }
        );

        // do another scan to take in count removed modules
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("workaround is disabled, so no changes", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);

        // do another scan, without any changes to the modules folder
        changesMade = assertionsScanner.scanModules();
        // verify result
        Assert.assertFalse("no module changes the second time", changesMade);

        verifyAssertionScanner(1, 0, 1, 0, 0, 0, 2);
    }
}
