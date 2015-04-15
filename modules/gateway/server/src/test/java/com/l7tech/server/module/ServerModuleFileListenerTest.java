package com.l7tech.server.module;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.module.*;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.RunsOnWindows;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// TODO: update unit tests for phase two

/**
 * Test ServerModuleFileListener
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFileListenerTest extends ModulesScannerTestBase {

    // Modular Assertions deploy directory
    private static final String MODULAR_ASSERTIONS_MODULES_DIR_NAME = "l7tech-modular";
    // Custom Assertions deploy directory
    private static final String CUSTOM_ASSERTIONS_MODULES_DIR_NAME = "l7tech-custom";
    // Staging directory
    private static final String STAGING_MODULES_DIR_NAME = "l7tech-staging";

    // used to generate random GOID
    private static final Random rnd = new Random();
    private static final int typeLength = ModuleType.values().length;
    private static final long GOID_HI_START = Long.MAX_VALUE - 1;

    @Mock
    private ServerModuleFileManager serverModuleFileManager;
    @Mock
    private Config config;
    @Mock
    private ServerAssertionRegistry modularAssertionRegistrar;
    @Mock
    private CustomAssertionsRegistrar customAssertionRegistrar;

    private ServerModuleFileListener modulesListener;

    // server module files initial repository
    private Map<Goid, ServerModuleFile> moduleFiles;
    // modular and custom assertions deploy folders as well as ServerModuleFile staging folder
    private static File modularDeployFolder, customDeployFolder, stagingFolder;

    // emulate this cluster node
    private String currentNodeId = "currentClusterNode";

    @BeforeClass
    public static void setUpOnce() throws Exception {
        // On Windows platform jar files are locked by the JVM, therefore they cannot be cleaned up on exit.
        // On start, we will loop through all previously created temporary folders and delete them,
        // which means that at a worst case scenario we will only end up with files from a single run.
        cleanUpTemporaryFilesFromPreviousRuns(MODULAR_ASSERTIONS_MODULES_DIR_NAME);
        cleanUpTemporaryFilesFromPreviousRuns(CUSTOM_ASSERTIONS_MODULES_DIR_NAME);
        cleanUpTemporaryFilesFromPreviousRuns(STAGING_MODULES_DIR_NAME);
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
        // create a temporary modules folder for this test
        Assert.assertNotNull(modularDeployFolder = getTempFolder(MODULAR_ASSERTIONS_MODULES_DIR_NAME));
        Assert.assertNotNull(customDeployFolder = getTempFolder(CUSTOM_ASSERTIONS_MODULES_DIR_NAME));
        Assert.assertNotNull(stagingFolder = getTempFolder(STAGING_MODULES_DIR_NAME));
        // set the modules folder property to the temporary folders
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY)).thenReturn(modularDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY), Mockito.anyString())).thenReturn(modularDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY)).thenReturn(customDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY), Mockito.anyString())).thenReturn(customDeployFolder.getCanonicalPath());
        Mockito.when(config.getProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_STAGING_FOLDER)).thenReturn(stagingFolder.getCanonicalPath());
        Mockito.when(config.getProperty(Mockito.eq(ServerConfigParams.PARAM_SERVER_MODULE_FILE_STAGING_FOLDER), Mockito.anyString())).thenReturn(stagingFolder.getCanonicalPath());

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
    }

    /**
     * {@link ServerModuleFile} builder class.
     */
    @SuppressWarnings("UnusedDeclaration")
    class ServerModuleFileBuilder {
        private Goid goid;
        private String name;
        private Integer version;
        private ModuleType moduleType;
        private byte[] bytes;
        private String checkSum;

        /**
         * Pre-attached {@link ServerModuleFile}.
         * The builder will append new properties or override existing ones.
         */
        private ServerModuleFile moduleFile;

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
         * @param moduleFile    the {@link ServerModuleFile} to attach to.
         */
        public ServerModuleFileBuilder(final ServerModuleFile moduleFile) {
            this.moduleFile = moduleFile;
        }

        /**
         * @return either the pre-attached {@link #moduleFile} or a new {@link ServerModuleFile} instance.
         */
        private ServerModuleFile getModuleFile() {
            return this.moduleFile == null ? new ServerModuleFile() : this.moduleFile;
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

        public ServerModuleFileBuilder bytes(final byte[] bytes) {
            this.bytes = bytes;
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

        public ServerModuleFile build() {
            final ServerModuleFile moduleFile = getModuleFile();
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
            if (bytes != null) {
                moduleFile.createData(bytes); // also calculates check-sum
            }
            if (checkSum != null) {
                moduleFile.setModuleSha256(checkSum); // override check-sum
            }
            for (final Map.Entry<String, String> property : properties.entrySet()) {
                if (ServerModuleFile.PROP_FILE_NAME.equals(property.getKey())) {
                    moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, property.getValue());
                } else if (ServerModuleFile.PROP_SIZE.equals(property.getKey())) {
                    moduleFile.setProperty(ServerModuleFile.PROP_SIZE, property.getValue());
                } else if (ServerModuleFile.PROP_ASSERTIONS.equals(property.getKey())) {
                    moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, property.getValue());
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
     *-----------------------------------------------------------------
     *      currentCluster  => STAGED
     *      node_1          => UPLOADED
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_1: Goid(GOID_HI_START, 1); test data 1; CUSTOM_ASSERTION
     *-----------------------------------------------------------------
     *      currentCluster  => UPLOADED
     *      node_1          => STAGED
     *      node_2          => STAGED
     *      node_3          => UPLOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_2: Goid(GOID_HI_START, 2); test data 2; MODULAR_ASSERTION
     *-----------------------------------------------------------------
     *      currentCluster  => UPLOADED
     *      node_2          => UPLOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_3: Goid(GOID_HI_START, 3); test data 3; CUSTOM_ASSERTION
     *-----------------------------------------------------------------
     *      currentCluster  => LOADED
     *      node_2          => REJECTED
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_4: Goid(GOID_HI_START, 4); test data 4; MODULAR_ASSERTION
     *-----------------------------------------------------------------
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_5: Goid(GOID_HI_START, 5); test data 5; MODULAR_ASSERTION
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
     *-----------------------------------------------------------------
     *      (empty)
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_7: Goid(GOID_HI_START, 7); test data 7; CUSTOM_ASSERTION
     *-----------------------------------------------------------------
     *      currentCluster  => DEPLOYED
     *      node_2          => REJECTED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_8: Goid(GOID_HI_START, 8); test data 8; MODULAR_ASSERTION
     *-----------------------------------------------------------------
     *      currentCluster  => DEPLOYED
     *      node_3          => LOADED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_9: Goid(GOID_HI_START, 9); test data 9; MODULAR_ASSERTION
     *-----------------------------------------------------------------
     *      currentCluster  => STAGED
     *-----------------------------------------------------------------
     *
     *-----------------------------------------------------------------
     * module_10: Goid(GOID_HI_START, 10); test data 10; MODULAR_ASSERTION
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
                        //-----------------------------------------------------------------
                        // currentCluster  => STAGED
                        // node_1          => UPLOADED
                        // node_3          => LOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 0),
                        new ServerModuleFileBuilder(create_test_module_without_states(0, ModuleType.CUSTOM_ASSERTION))
                                .addState(currentNodeId, ModuleState.REJECTED)
                                .addState("node_1", ModuleState.UPLOADED)
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_1: Goid(GOID_HI_START, 1); test data 1; CUSTOM_ASSERTION
                        //-----------------------------------------------------------------
                        // currentCluster  => UPLOADED
                        // node_1          => STAGED
                        // node_2          => STAGED
                        // node_3          => UPLOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 1),
                        new ServerModuleFileBuilder(create_test_module_without_states(1, ModuleType.CUSTOM_ASSERTION))
                                .addState(currentNodeId, ModuleState.UPLOADED)
                                .addState("node_1", ModuleState.REJECTED)
                                .addState("node_2", ModuleState.REJECTED)
                                .addState("node_3", ModuleState.UPLOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_2: Goid(GOID_HI_START, 2); test data 2; MODULAR_ASSERTION
                        //-----------------------------------------------------------------
                        // currentCluster  => UPLOADED
                        // node_2          => UPLOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 2),
                        new ServerModuleFileBuilder(create_test_module_without_states(2, ModuleType.MODULAR_ASSERTION))
                                .addState(currentNodeId, ModuleState.UPLOADED)
                                .addState("node_2", ModuleState.UPLOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_3: Goid(GOID_HI_START, 3); test data 3; CUSTOM_ASSERTION
                        //-----------------------------------------------------------------
                        // currentCluster  => LOADED
                        // node_2          => REJECTED
                        // node_3          => LOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 3),
                        new ServerModuleFileBuilder(create_test_module_without_states(3, ModuleType.CUSTOM_ASSERTION))
                                .addState(currentNodeId, ModuleState.LOADED)
                                .addState("node_2", ModuleState.REJECTED)
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_4: Goid(GOID_HI_START, 4); test data 4; MODULAR_ASSERTION
                        //-----------------------------------------------------------------
                        // node_3          => LOADED
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 4),
                        new ServerModuleFileBuilder(create_test_module_without_states(4, ModuleType.MODULAR_ASSERTION))
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_5: Goid(GOID_HI_START, 5); test data 5; MODULAR_ASSERTION
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 5),
                        create_test_module_without_states(5, ModuleType.MODULAR_ASSERTION)
                )
                .put(
                        //-----------------------------------------------------------------
                        // module_6: Goid(GOID_HI_START, 6); test data 6; CUSTOM_ASSERTION
                        //-----------------------------------------------------------------
                        // (empty)
                        //-----------------------------------------------------------------
                        new Goid(GOID_HI_START, 6),
                        create_test_module_without_states(6, ModuleType.CUSTOM_ASSERTION)
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_7: Goid(GOID_HI_START, 7); test data 7; CUSTOM_ASSERTION
                        // ----------------------------------------------------------------
                        // currentCluster  => DEPLOYED
                        // node_2          => REJECTED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 7),
                        new ServerModuleFileBuilder(create_test_module_without_states(7, ModuleType.CUSTOM_ASSERTION))
                                .addState(currentNodeId, ModuleState.ACCEPTED)
                                .addState("node_2", ModuleState.REJECTED)
                                .build()
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_8: Goid(GOID_HI_START, 8); test data 8; MODULAR_ASSERTION
                        // ----------------------------------------------------------------
                        // currentCluster  => DEPLOYED
                        // node_3          => LOADED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 8),
                        new ServerModuleFileBuilder(create_test_module_without_states(8, ModuleType.MODULAR_ASSERTION))
                                .addState(currentNodeId, ModuleState.ACCEPTED)
                                .addState("node_3", ModuleState.LOADED)
                                .build()
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_9: Goid(GOID_HI_START, 9); test data 9; MODULAR_ASSERTION
                        // ----------------------------------------------------------------
                        // currentCluster  => STAGED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 9),
                        new ServerModuleFileBuilder(create_test_module_without_states(9, ModuleType.MODULAR_ASSERTION))
                                .addState(currentNodeId, ModuleState.REJECTED)
                                .build()
                )
                .put(
                        // ----------------------------------------------------------------
                        // module_10: Goid(GOID_HI_START, 10); test data 10; MODULAR_ASSERTION
                        // ----------------------------------------------------------------
                        // currentCluster  => LOADED
                        // node_1          => LOADED
                        // ----------------------------------------------------------------
                        new Goid(GOID_HI_START, 10),
                        new ServerModuleFileBuilder(create_test_module_without_states(10, ModuleType.MODULAR_ASSERTION))
                                .addState(currentNodeId, ModuleState.LOADED)
                                .addState("node_1", ModuleState.LOADED)
                                .build()
                )
                .map();

        // modules in our repository with their state for current cluster node:
        // module_0  => STAGED   => CUSTOM_ASSERTION
        // module_1  => UPLOADED => CUSTOM_ASSERTION
        // module_2  => UPLOADED => MODULAR_ASSERTION
        // module_3  => LOADED   => CUSTOM_ASSERTION
        // module_4  => <NONE>   => MODULAR_ASSERTION
        // module_5  => <NONE>   => MODULAR_ASSERTION
        // module_6  => <NONE>   => CUSTOM_ASSERTION
        // module_7  => DEPLOYED => CUSTOM_ASSERTION
        // module_8  => DEPLOYED => MODULAR_ASSERTION
        // module_9  => STAGED   => MODULAR_ASSERTION
        // module_10 => LOADED   => MODULAR_ASSERTION
        // verify
        initialStates = new ModuleState[]{
                ModuleState.REJECTED,
                ModuleState.UPLOADED,
                ModuleState.UPLOADED,
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
    private void init_common_mocks(final boolean uploadEnabled) throws FindException, UpdateException {
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
    }

    /**
     * Convenient method for creating a test sample of {@link ServerModuleFile} without any states, having the following attributes:
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
     */
    private ServerModuleFile create_test_module_without_states(final long ordinal, ModuleType moduleType) {
        moduleType = moduleType != null ? moduleType : ModuleType.values()[rnd.nextInt(typeLength)];
        final byte[] bytes = String.valueOf("test data " + ordinal).getBytes(Charsets.UTF8);
        return new ServerModuleFileBuilder()
                .goid(new Goid(GOID_HI_START, ordinal))
                .name("module_" + ordinal)
                .version(0)
                .moduleType(moduleType)
                .bytes(bytes)
                .addProperty(ServerModuleFile.PROP_ASSERTIONS, "assertion_" + ordinal)
                .addProperty(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                .addProperty(ServerModuleFile.PROP_FILE_NAME, "module_" + ordinal + (ModuleType.MODULAR_ASSERTION.equals(moduleType) ? ".aar" : ".jar"))
                .build();
    }

    /**
     * Gets the specified {@code future} result waiting for default two seconds to finish the task.
     *
     * @param future    the {@link Future} to wait for completion.  Optional and can be {@code null} in case when no task was executed.
     */
    private Future waitForFuture(final Future future) throws ExecutionException, InterruptedException, TimeoutException {
        return waitForFuture(future, 2000);
    }

    /**
     * Gets the specified {@code future} result waiting for specified {@code timeout} in millis.
     *
     * @param future     the {@link Future} to wait for completion.
     * @param timeout    the timeout in millis to wait for {@code future} completion.  Value of {@code -1} will wait indefinitely.
     */
    private Future waitForFuture(final Future future, final int timeout) throws ExecutionException, InterruptedException, TimeoutException {
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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void test_persistence_event_before_started() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        // new module with goid 100
        moduleFiles.put(
                new Goid(GOID_HI_START, 100),
                new ServerModuleFileBuilder(create_test_module_without_states(100, ModuleType.MODULAR_ASSERTION))
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
        // make sure nothing is written to the staging folder
        assertThat(stagingFolder.listFiles(), emptyArray());
    }

    @Test
    public void test_upload_disabled() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(false);

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
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging and deploy folders are empty
        assertThat(modularDeployFolder.listFiles(), emptyArray());
        assertThat(customDeployFolder.listFiles(), emptyArray());


        // send EntityInvalidationEvent, containing two events
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        int knownSize = modulesListener.knownModuleFiles.size();
        // new module with goid 100
        moduleFiles.put(
                new Goid(GOID_HI_START, 100),
                new ServerModuleFileBuilder(create_test_module_without_states(100, ModuleType.MODULAR_ASSERTION))
                        .addState(currentNodeId, ModuleState.UPLOADED)
                        .build()
        );
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
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(knownSize + 1)); // make sure the new module_100 is populated
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging and deploy folders are empty
        assertThat(modularDeployFolder.listFiles(), emptyArray());
        assertThat(customDeployFolder.listFiles(), emptyArray());


        // send EntityInvalidationEvent, containing two events
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        knownSize = modulesListener.knownModuleFiles.size();
        moduleFiles.remove(new Goid(GOID_HI_START, 2));
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
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(knownSize - 1)); // make sure the new module_2 is removed
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size()));
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }
        // make sure staging and deploy folders are empty
        assertThat(modularDeployFolder.listFiles(), emptyArray());
        assertThat(customDeployFolder.listFiles(), emptyArray());
    }

    /**
     * Utility function for comparing two collections ignoring the order.
     */
    private static boolean collectionEqualsIgnoringOrder(final Collection<File> first, final Collection<File> second) {
        Assert.assertNotNull(first);
        Assert.assertNotNull(second);
        return first.containsAll(second) && second.containsAll(first);
    }

    /**
     * Utility method for creating a collection of module files.
     *
     * @param parent              the parent folder, modular or custom assertions staging or deploy folder.
     * @param installedModules    collection of module files.
     */
    private static Collection<File> toFiles(final File parent, final Collection<ServerModuleFile> installedModules) {
        Assert.assertNotNull(parent);
        Assert.assertNotNull(installedModules);
        final Collection<File> files = new ArrayList<>();
        for (final ServerModuleFile moduleFile : installedModules) {
            Assert.assertNotNull(moduleFile);
            files.add(new File(parent, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)));
        }
        return files;
    }

    /**
     * Will send out {@link Started} event, simulating SSG start, and will verify the initial state of the modules listener.
     *
     * @param expectedStates                     the expected modules states (module ordinal order) after the modules listener initializes.
     * @param isModularDeployWritable            indicate whether the modular assertions modules deploy folder is writable.
     * @param isCustomDeployWritable             indicate whether the custom assertions modules deploy folder is writable.
     * @param expectedLoadedModularModules       module files collection of expected modular assertions loaded modules.
     * @param expectedLoadedCustomModules        module files collection of expected custom assertions loaded modules.
     * @param expectedInstalledModularModules    module files collection of expected modular assertions installed modules.
     * @param expectedInstalledCustomModules     module files collection of expected custom assertions installed modules.
     * @throws Exception
     */
    private void publishInitialStartedEventAndVerifyResult(
            final ModuleState[] expectedStates,
            final boolean isModularDeployWritable,
            final boolean isCustomDeployWritable,
            final Collection<ServerModuleFile> expectedLoadedModularModules,
            final Collection<ServerModuleFile> expectedLoadedCustomModules,
            final Collection<ServerModuleFile> expectedInstalledModularModules,
            final Collection<ServerModuleFile> expectedInstalledCustomModules
    ) throws Exception {
        // verify initial state
        Assert.assertNotNull(modulesListener.knownModuleFiles);
        assertThat(modulesListener.knownModuleFiles.values(), empty());
        assertThat(moduleFiles.values(), not(empty()));
        assertThat(customDeployFolder.listFiles(), emptyArray());
        assertThat(modularDeployFolder.listFiles(), emptyArray());

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
        Assert.assertNotNull(expectedLoadedModularModules);
        for (final ServerModuleFile moduleFile : expectedLoadedModularModules) {
            Assert.assertNotNull(moduleFile);
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(ModuleState.LOADED));
        }
        Assert.assertNotNull(expectedLoadedCustomModules);
        for (final ServerModuleFile moduleFile : expectedLoadedCustomModules) {
            Assert.assertNotNull(moduleFile);
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(ModuleState.LOADED));
        }
        // verify installed
        Assert.assertNotNull(expectedInstalledModularModules);
        for (final ServerModuleFile moduleFile : expectedInstalledModularModules) {
            Assert.assertNotNull(moduleFile);
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(isModularDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED));
        }
        Assert.assertNotNull(expectedInstalledCustomModules);
        for (final ServerModuleFile moduleFile : expectedInstalledCustomModules) {
            Assert.assertNotNull(moduleFile);
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(isCustomDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED));
        }

        // we have:
        // module_0  => STAGED          => CUSTOM_ASSERTION
        // module_1  => DEPLOYED/STAGED => CUSTOM_ASSERTION
        // module_2  => DEPLOYED/STAGED => MODULAR_ASSERTION
        // module_3  => LOADED          => CUSTOM_ASSERTION
        // module_4  => DEPLOYED/STAGED => MODULAR_ASSERTION
        // module_5  => DEPLOYED/STAGED => MODULAR_ASSERTION
        // module_6  => DEPLOYED/STAGED => CUSTOM_ASSERTION
        // module_7  => DEPLOYED        => CUSTOM_ASSERTION
        // module_8  => DEPLOYED        => MODULAR_ASSERTION
        // module_9  => STAGED          => MODULAR_ASSERTION
        // module_10 => LOADED          => MODULAR_ASSERTION
        // verify
        Assert.assertNotNull(expectedStates);
        for (int i = 0; i < expectedStates.length; ++i) {
            assertThat(modulesListener.getModuleState(moduleFiles.get(new Goid(GOID_HI_START, i))), equalTo(expectedStates[i]));
        }
        // make sure handleEvent was actually called
        Mockito.verify(modulesListener, Mockito.times(1)).handleEvent(Mockito.<ApplicationEvent>any());
        Mockito.verify(modulesListener, Mockito.times(1)).processGatewayStartedEvent();
        Mockito.verify(modulesListener, Mockito.never()).processServerModuleFileInvalidationEvent(Mockito.<EntityInvalidationEvent>any());  // shouldn't be called
        Mockito.verify(modulesListener, Mockito.times(moduleFiles.size() - (expectedLoadedModularModules.size() + expectedLoadedCustomModules.size()))).loadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any());
        Mockito.verify(modulesListener, Mockito.never()).unloadModule(Mockito.<ServerModuleFileListener.StagedServerModuleFile>any()); // shouldn't be called
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile moduleFile : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(moduleFile.getGoid()), notNullValue());
        }

//        // test custom assertions deploy folder
//        assertThat(isCustomDeployWritable ? modStagingCustomFolder.listFiles() : customDeployFolder.listFiles(), emptyArray());
//        // UPLOADED => DEPLOYED (module_1 and module_6) CUSTOM_ASSERTION
//        File[] files = isCustomDeployWritable ? customDeployFolder.listFiles() : modStagingCustomFolder.listFiles();
//        Assert.assertNotNull(files);
//        assertThat(files.length, equalTo(expectedInstalledCustomModules.size()));
//        Assert.assertTrue(
//                collectionEqualsIgnoringOrder(
//                        Arrays.asList(files),
//                        toFiles(isCustomDeployWritable ? customDeployFolder : modStagingCustomFolder, expectedInstalledCustomModules)
//                )
//        );
//
//        // make sure modular assertions deploy folder is empty, as we do not have write permissions
//        assertThat(isModularDeployWritable ? modStagingModularFolder.listFiles() : modularDeployFolder.listFiles(), emptyArray());
//        // UPLOADED => DEPLOYED (module_2, module_4 and module_5) MODULAR_ASSERTION
//        files = isModularDeployWritable ? modularDeployFolder.listFiles() : modStagingModularFolder.listFiles();
//        Assert.assertNotNull(files);
//        assertThat(files.length, equalTo(expectedInstalledModularModules.size()));
//        Assert.assertTrue(
//                collectionEqualsIgnoringOrder(
//                        Arrays.asList(files),
//                        toFiles(isModularDeployWritable ? modularDeployFolder : modStagingModularFolder, expectedInstalledModularModules)
//                )
//        );
    }

    @Test
    public void test_started_event() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

//        // set module_5 and module_2 as loaded
//        Mockito.doAnswer(new Answer<Boolean>() {
//            @Override
//            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
//                Assert.assertNotNull(invocation);
//                Assert.assertEquals("there is only one parameter for isServerModuleFileLoaded", 1, invocation.getArguments().length);
//                final Object param1 = invocation.getArguments()[0];
//                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
//                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
//                // loaded modular assertions (module_5, module_2)
//                return new Goid(GOID_HI_START, 5).equals(moduleFile.getGoid()) ||
//                        new Goid(GOID_HI_START, 2).equals(moduleFile.getGoid());
//            }
//        }).when(modularAssertionRegistrar).isServerModuleFileLoaded(Mockito.<ServerModuleFile>any());
//
//        // set module_6 as loaded
//        Mockito.doAnswer(new Answer<Boolean>() {
//            @Override
//            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
//                Assert.assertNotNull(invocation);
//                Assert.assertEquals("there is only one parameter for isServerModuleFileLoaded", 1, invocation.getArguments().length);
//                final Object param1 = invocation.getArguments()[0];
//                Assert.assertTrue("Param is ServerModuleFile", param1 instanceof ServerModuleFile);
//                final ServerModuleFile moduleFile = (ServerModuleFile) param1;
//                // loaded custom assertions (module_6)
//                return new Goid(GOID_HI_START, 6).equals(moduleFile.getGoid());
//            }
//        }).when(customAssertionRegistrar).isServerModuleFileLoaded(Mockito.<ServerModuleFile>any());

        // we have:
        publishInitialStartedEventAndVerifyResult(
                new ModuleState[]{
                        ModuleState.REJECTED,    // module_0  => STAGED   => CUSTOM_ASSERTION
                        ModuleState.ACCEPTED,  // module_1  => DEPLOYED => CUSTOM_ASSERTION
                        ModuleState.LOADED,    // module_2  => LOADED   => MODULAR_ASSERTION
                        ModuleState.LOADED,    // module_3  => LOADED   => CUSTOM_ASSERTION
                        ModuleState.ACCEPTED,  // module_4  => DEPLOYED => MODULAR_ASSERTION
                        ModuleState.LOADED,    // module_5  => LOADED   => MODULAR_ASSERTION
                        ModuleState.LOADED,    // module_6  => LOADED   => CUSTOM_ASSERTION
                        ModuleState.ACCEPTED,  // module_7  => DEPLOYED => CUSTOM_ASSERTION
                        ModuleState.ACCEPTED,  // module_8  => DEPLOYED => MODULAR_ASSERTION
                        ModuleState.REJECTED,    // module_9  => STAGED   => MODULAR_ASSERTION
                        ModuleState.LOADED     // module_10 => LOADED   => MODULAR_ASSERTION
                },
                true, // modular deploy
                true, // custom deploy
                // loaded:
                // LOADED => (module_2, module_5 and module_10) => MODULAR_ASSERTIONS
                Arrays.asList(
                        moduleFiles.get(new Goid(GOID_HI_START, 2)),
                        moduleFiles.get(new Goid(GOID_HI_START, 5)),
                        moduleFiles.get(new Goid(GOID_HI_START, 10))
                ),
                // LOADED => (module_3 and module_6)  => CUSTOM_ASSERTIONS
                Arrays.asList(
                        moduleFiles.get(new Goid(GOID_HI_START, 3)),
                        moduleFiles.get(new Goid(GOID_HI_START, 6))
                ),
                // installed:
                // UPLOADED => DEPLOYED (module_4) => MODULAR_ASSERTION
                Arrays.asList(
                        moduleFiles.get(new Goid(GOID_HI_START, 4))
                ),
                // UPLOADED => DEPLOYED (module_1) => CUSTOM_ASSERTION
                Arrays.asList(
                        moduleFiles.get(new Goid(GOID_HI_START, 1))
                )
        );
    }

    /**
     * Do the actual initial Started publish and verify the result.
     *
     * @param isModularDeployWritable    flag indicating whether the modular assertions deploy folder has write permission.
     * @param isCustomDeployWritable     flag indicating whether the custom assertions  deploy folder has write permission.
     */
    private void do_test_started_event(
            final boolean isModularDeployWritable,
            final boolean isCustomDeployWritable
    ) throws Exception {
        // we have:
        publishInitialStartedEventAndVerifyResult(
                new ModuleState[]{
                        ModuleState.REJECTED,                                                     // module_0  => STAGED   => CUSTOM_ASSERTION;   => STAGED
                        isCustomDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED,     // module_1  => UPLOADED => CUSTOM_ASSERTION;   => DEPLOYED/STAGED
                        isModularDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED,    // module_2  => UPLOADED => MODULAR_ASSERTION;  => DEPLOYED/STAGED
                        ModuleState.LOADED,                                                     // module_3  => LOADED   => CUSTOM_ASSERTION;   => LOADED
                        isModularDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED,    // module_4  => <NONE>   => MODULAR_ASSERTION;  => DEPLOYED/STAGED
                        isModularDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED,    // module_5  => <NONE>   => MODULAR_ASSERTION;  => DEPLOYED/STAGED
                        isCustomDeployWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED,     // module_6  => <NONE>   => CUSTOM_ASSERTION;   => DEPLOYED/STAGED
                        ModuleState.ACCEPTED,                                                   // module_7  => DEPLOYED => CUSTOM_ASSERTION;   => DEPLOYED
                        ModuleState.ACCEPTED,                                                   // module_8  => DEPLOYED => MODULAR_ASSERTION;  => DEPLOYED
                        ModuleState.REJECTED,                                                     // module_9  => STAGED   => MODULAR_ASSERTION;  => STAGED
                        ModuleState.LOADED                                                      // module_10 => LOADED   => MODULAR_ASSERTION;  => LOADED
                },
                isModularDeployWritable, // modular deploy
                isCustomDeployWritable, // custom deploy
                Arrays.asList(moduleFiles.get(new Goid(GOID_HI_START, 10))),  // LOADED => (module_10) => MODULAR_ASSERTIONS
                Arrays.asList(moduleFiles.get(new Goid(GOID_HI_START, 3))),   // LOADED => (module_3)  => CUSTOM_ASSERTIONS
                // UPLOADED => DEPLOYED (module_2, module_4 and module_5) => MODULAR_ASSERTION
                Arrays.asList(
                        moduleFiles.get(new Goid(GOID_HI_START, 2)),
                        moduleFiles.get(new Goid(GOID_HI_START, 4)),
                        moduleFiles.get(new Goid(GOID_HI_START, 5))
                ),
                // UPLOADED => DEPLOYED (module_1 and module_6) => CUSTOM_ASSERTION
                Arrays.asList(
                        moduleFiles.get(new Goid(GOID_HI_START, 1)),
                        moduleFiles.get(new Goid(GOID_HI_START, 6))
                )
        );
    }

    /**
     * Test Started event having write permissions to both custom and modular assertions deploy folders
     */
    @Test
    public void test_started_event_with_write_permissions() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        Assert.assertTrue(modularDeployFolder.canWrite());
        Assert.assertTrue(customDeployFolder.canWrite());
        // do actual initial test
        do_test_started_event(true, true);
    }

    /**
     * Test Started event having modular assertions deploy folder read-only and custom assertions deploy folder having write permissions.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_started_event_without_write_permissions_to_modular() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        try {
            // set read-only to modular assertions deploy folder
            Assert.assertTrue(modularDeployFolder.setWritable(false));
            Assert.assertFalse(modularDeployFolder.canWrite());
            Assert.assertTrue(customDeployFolder.canWrite());
            // do actual initial test
            do_test_started_event(false, true);
        } finally {
            // reset before exit the test
            Assert.assertTrue(modularDeployFolder.setWritable(true));
        }
    }

    /**
     * Test Started event having custom assertions deploy folder read-only and modular assertions deploy folder having write permissions.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_started_event_without_write_permissions_to_custom() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        try {
            // set read-only to custom assertions deploy folder
            Assert.assertTrue(customDeployFolder.setWritable(false));
            Assert.assertTrue(modularDeployFolder.canWrite());
            Assert.assertFalse(customDeployFolder.canWrite());
            // do actual initial test
            do_test_started_event(true, false);
        } finally {
            // reset before exit the test
            Assert.assertTrue(customDeployFolder.setWritable(true));
        }
    }

    /**
     * Test Started event having both modular and custom assertions deploy folders read-only.
     */
    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_started_event_without_write_permissions_to_both() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        try {
            // set read-only to custom assertions deploy folder
            Assert.assertTrue(customDeployFolder.setWritable(false));
            Assert.assertTrue(modularDeployFolder.setWritable(false));
            Assert.assertFalse(modularDeployFolder.canWrite());
            Assert.assertFalse(customDeployFolder.canWrite());
            // do actual initial test
            do_test_started_event(false, false);
        } finally {
            // reset before exit the test
            Assert.assertTrue(customDeployFolder.setWritable(true));
            Assert.assertTrue(modularDeployFolder.setWritable(true));
        }
    }

    /**
     * Convenient method for creating new ServerModuleFile entity, with the specified {@code ordinal},
     * then publishing a EntityInvalidationEvent(CREATE), and verifying the result.
     *
     * @param ordinal                           the ordinal of the server module file.
     * @param moduleType                        the module type.
     * @param stateUponCreation                 the module state upon creation, specify {@code null} for none.
     * @param isModuleDeployFolderWritable      flag indicating whether the modules deploy folder (based on the {@code moduleType}) is writable or not.
     */
    private void publishAndVerifyNewModuleFile(
            final long ordinal,
            final ModuleType moduleType,
            final ModuleState stateUponCreation,
            final boolean isModuleDeployFolderWritable
    ) throws Exception {
        Assert.assertNotNull(moduleType);

        final Goid goid = new Goid(GOID_HI_START, ordinal);
        Assert.assertNull(moduleFiles.get(goid)); // new module shouldn't be existing

        // create the new module file
        ServerModuleFile moduleFile = new ServerModuleFileBuilder(create_test_module_without_states(ordinal, moduleType))
                .addState(currentNodeId, stateUponCreation)
                .build();

        // make sure the file doesn't already exist in both deploy and staging folders
        final File moduleDeployFolder = ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? customDeployFolder : modularDeployFolder;
        Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());

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
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile module : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(module.getGoid()), notNullValue());
        }
        moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        assertThat(modulesListener.getModuleState(moduleFile), equalTo(isModuleDeployFolderWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED));

        // verify that the module ended up in the right folder
        if (isModuleDeployFolderWritable) {
            Assert.assertTrue(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
        } else {
            Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
        }
    }

    /**
     * Convenient method for publishing AssertionModuleRegistrationEvent, and optionally verify the result.
     *
     * @param moduleFile    the module file to target.
     * @param verify        optionally verify the result.
     */
    private void publishAssertionModuleRegistrationEvent(final ServerModuleFile moduleFile, final boolean verify) throws Exception {
        Assert.assertNotNull(moduleFile);
        final ModuleType moduleType = moduleFile.getModuleType();
        Assert.assertNotNull(moduleType);

        // send AssertionModuleRegistrationEvent for the new module
        final BaseAssertionModule module = ModuleType.MODULAR_ASSERTION.equals(moduleType) ? Mockito.mock(ModularAssertionModule.class) : Mockito.mock(CustomAssertionModule.class);
        Mockito.when(module.getName()).thenReturn(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME));
        // send AssertionModuleRegistrationEvent
        Assert.assertNotNull(
                waitForFuture(
                        modulesListener.handleEvent(new AssertionModuleRegistrationEvent(this, module))
                )
        );
        if (verify) {
            // verify that the module ended up with LOADED
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(ModuleState.LOADED));
        }
    }



    /**
     * Convenient method for removing ServerModuleFile entity, with the specified {@code goid},
     * then publishing a EntityInvalidationEvent(DELETE), and verifying the result.
     *
     * @param goid                          the module file goid to delete.
     * @param isModuleDeployFolderWritable  flag indicating whether the modules deploy folder (based on the module type) is writable or not.
     * @param skipInitialFileSystemCheck    indicates whether to check initial presence of the file, before sending the delete event.
     */
    private void publishAndVerifyDeletedModuleFile(
            final Goid goid,
            final boolean isModuleDeployFolderWritable,
            final boolean skipInitialFileSystemCheck
    ) throws Exception {
        Assert.assertNotNull(goid);

        final ServerModuleFile moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        final ModuleType moduleType = moduleFile.getModuleType();
        Assert.assertNotNull(moduleType);
        final ModuleState moduleState = modulesListener.getModuleState(moduleFile);
        Assert.assertNotNull(moduleState);

        // make sure the file exists before delete event is sent
        final File moduleDeployFolder = ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? customDeployFolder : modularDeployFolder;
        if (!skipInitialFileSystemCheck) {
            if (isModuleDeployFolderWritable) {
                Assert.assertTrue(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
            } else {
                Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
            }
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
        assertThat(modulesListener.knownModuleFiles.values(), not(empty()));
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile module : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(module.getGoid()), notNullValue());
        }
        assertThat(modulesListener.getModuleState(moduleFile), equalTo(moduleState)); // state not changed

        // verify the file has been removed everywhere
        Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
    }

    /**
     * Convenient method for updating existing ServerModuleFile entity, with the specified {@code goid},
     * then publishing a EntityInvalidationEvent(UPDATE), and verifying the result.
     *
     * @param goid                           the module goid.
     * @param isModuleDeployFolderWritable   flag indicating whether the modules deploy folder (based on the {@code moduleType}) is writable or not.
     * @param skipInitialFileSystemCheck     indicates whether to check initial presence of the file, before sending the delete event.
     */
    private void publishAndVerifyUpdateModuleFile(
            final Goid goid,
            final boolean isModuleDeployFolderWritable,
            final boolean skipInitialFileSystemCheck
    ) throws Exception {
        Assert.assertNotNull(goid);
        final ServerModuleFile moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        final ModuleType moduleType = moduleFile.getModuleType();
        Assert.assertNotNull(moduleType);
        final ModuleState moduleState = modulesListener.getModuleState(moduleFile);
        Assert.assertNotNull(moduleState);

        // make sure the file exist in either deploy or staging folder
        final File moduleDeployFolder = ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? customDeployFolder : modularDeployFolder;
        if (ModuleState.UPLOADED.equals(moduleState)) {
            Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
        } else if (!skipInitialFileSystemCheck) {
            if (isModuleDeployFolderWritable) {
                Assert.assertTrue(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
            } else {
                Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
            }
        }

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
        assertThat(modulesListener.knownModuleFiles.values().size(), equalTo(moduleFiles.size())); // make sure all modules are populate into knownModuleFiles
        for (final ServerModuleFile module : moduleFiles.values()) {
            assertThat(modulesListener.knownModuleFiles.get(module.getGoid()), notNullValue());
        }
        if (ModuleState.UPLOADED.equals(moduleState)) {
            // only if uploaded the module will be installed
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(isModuleDeployFolderWritable ? ModuleState.ACCEPTED : ModuleState.REJECTED));
            if (isModuleDeployFolderWritable) {
                Assert.assertTrue(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
            } else {
                Assert.assertFalse(new File(moduleDeployFolder, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)).exists());
            }
        } else {
            // otherwise the state shouldn't change
            assertThat(modulesListener.getModuleState(moduleFile), equalTo(moduleState));
        }
    }

    /**
     * Returns either {@code isModularDeployWritable} or {@code isCustomDeployWritable} depending whether module file
     * associated with the specified {@code goid} is a {@link ModuleType#MODULAR_ASSERTION modular assertion} or
     * {@link ModuleType#CUSTOM_ASSERTION custom assertion} module.
     *
     * @param goid                       the module goid.
     * @param isModularDeployWritable    flag indicating whether the modular assertions deploy folder has write permission.
     * @param isCustomDeployWritable     flag indicating whether the custom assertions  deploy folder has write permission.
     */
    private boolean determineWritable(final Goid goid, final boolean isModularDeployWritable, final boolean isCustomDeployWritable) {
        Assert.assertNotNull(goid);
        final ServerModuleFile moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        final ModuleType moduleType = moduleFile.getModuleType();
        Assert.assertNotNull(moduleType);
        return ModuleType.MODULAR_ASSERTION.equals(moduleType) ? isModularDeployWritable : isCustomDeployWritable;
    }

    /**
     * Do the actual CRUD test.
     *
     * @param isModularDeployWritable    flag indicating whether the modular assertions deploy folder has write permission.
     * @param isCustomDeployWritable     flag indicating whether the custom assertions  deploy folder has write permission.
     */
    private void do_test_entity_crud(
            final boolean isModularDeployWritable,
            final boolean isCustomDeployWritable
    ) throws Exception {
        // test new custom assertion module (module_100)
        Goid goid = new Goid(GOID_HI_START, 100);
        publishAndVerifyNewModuleFile(100, ModuleType.CUSTOM_ASSERTION, ModuleState.UPLOADED, isCustomDeployWritable);
        publishAssertionModuleRegistrationEvent(moduleFiles.get(goid), true);

        // test new modular assertion module (module_101)
        goid = new Goid(GOID_HI_START, 101);
        publishAndVerifyNewModuleFile(101, ModuleType.MODULAR_ASSERTION, null, isModularDeployWritable);
        publishAssertionModuleRegistrationEvent(moduleFiles.get(goid), true);

        // test delete
        goid = new Goid(GOID_HI_START, 100);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), false);
        goid = new Goid(GOID_HI_START, 101);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), false);
        // test delete (module_1); CUSTOM_ASSERTION; LOADED
        goid = new Goid(GOID_HI_START, 1);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), false);
        // test delete (module_2); MODULAR_ASSERTION; LOADED
        goid = new Goid(GOID_HI_START, 2);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), false);
        // test delete (module_0); CUSTOM_ASSERTION; STAGED
        // skip initial check as the module initial stage is STAGED, so the file was not installed during initial start
        goid = new Goid(GOID_HI_START, 0);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), true);
        // test delete (module_9); MODULAR_ASSERTION; STAGED
        // skip initial check as the module initial stage is STAGED, so the file was not installed during initial start
        goid = new Goid(GOID_HI_START, 9);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), true);
        // test delete (module_3); CUSTOM_ASSERTION LOADED
        // skip initial check as the module initial stage is STAGED, so the file was not installed during initial start
        goid = new Goid(GOID_HI_START, 3);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), true);
        // test delete (module_10); MODULAR_ASSERTION LOADED
        // skip initial check as the module initial stage is STAGED, so the file was not installed during initial start
        goid = new Goid(GOID_HI_START, 10);
        publishAndVerifyDeletedModuleFile(goid, determineWritable(goid, isModularDeployWritable, isCustomDeployWritable), true);


        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 101)));


        // test update custom assertions module
        goid = new Goid(GOID_HI_START, 100);
        Assert.assertNull(moduleFiles.get(goid)); // new module shouldn't be existing
        // create the new module file
        ServerModuleFile moduleFile = new ServerModuleFileBuilder(create_test_module_without_states(100, ModuleType.CUSTOM_ASSERTION))
                .addState(currentNodeId, ModuleState.UPLOADED)
                .build();
        // create new module with the specified goid
        moduleFiles.put(goid, moduleFile);

        // test custom assertions module
        moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 100));
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), false);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.REJECTED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.REJECTED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.ACCEPTED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.LOADED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        publishAndVerifyDeletedModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);


        // test update modular assertions module
        goid = new Goid(GOID_HI_START, 200);
        Assert.assertNull(moduleFiles.get(goid)); // new module shouldn't be existing
        // create the new module file
        moduleFile = new ServerModuleFileBuilder(create_test_module_without_states(200, ModuleType.MODULAR_ASSERTION))
                .addState(currentNodeId, ModuleState.UPLOADED)
                .build();
        // create new module with the specified goid
        moduleFiles.put(goid, moduleFile);

        moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 200));
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), false);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.REJECTED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.REJECTED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.ACCEPTED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        moduleFile.setStateForNode(currentNodeId, ModuleState.LOADED);
        publishAndVerifyUpdateModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);
        publishAssertionModuleRegistrationEvent(moduleFile, true);

        publishAndVerifyDeletedModuleFile(moduleFile.getGoid(), determineWritable(moduleFile.getGoid(), isModularDeployWritable, isCustomDeployWritable), true);


        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 100)));
        Assert.assertNull(moduleFiles.get(new Goid(GOID_HI_START, 200)));
    }


    @Test
    public void test_entity_crud_with_write_permissions() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);
        // do initial test
        do_test_started_event(true, true);
        // do actual test
        do_test_entity_crud(true, true);
    }

    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_entity_crud_without_write_permissions_to_custom() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        try {
            // set read-only to custom assertions deploy folder
            Assert.assertTrue(modularDeployFolder.setWritable(true));
            Assert.assertTrue(customDeployFolder.setWritable(false));
            Assert.assertTrue(modularDeployFolder.canWrite());
            Assert.assertFalse(customDeployFolder.canWrite());
            // do initial test
            do_test_started_event(true, false);
            // do actual test
            do_test_entity_crud(true, false);
        } finally {
            // reset before exit the test
            Assert.assertTrue(modularDeployFolder.setWritable(true));
            Assert.assertTrue(customDeployFolder.setWritable(true));
        }
    }

    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_entity_crud_without_write_permissions_to_modular() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        try {
            // set read-only to modular assertions deploy folder
            Assert.assertTrue(modularDeployFolder.setWritable(false));
            Assert.assertTrue(customDeployFolder.setWritable(true));
            Assert.assertFalse(modularDeployFolder.canWrite());
            Assert.assertTrue(customDeployFolder.canWrite());
            // do initial test
            do_test_started_event(false, true);
            // do actual test
            do_test_entity_crud(false, true);
        } finally {
            // reset before exit the test
            Assert.assertTrue(modularDeployFolder.setWritable(true));
            Assert.assertTrue(customDeployFolder.setWritable(true));
        }
    }

    @Test
    @ConditionalIgnore(condition = RunsOnWindows.class)
    public void test_entity_crud_without_write_permissions_to_both() throws Exception {
        Assert.assertNotNull(modulesListener);
        createSampleModules();
        init_common_mocks(true);

        try {
            // set read-only to both custom and modular assertions deploy folder
            Assert.assertTrue(modularDeployFolder.setWritable(false));
            Assert.assertTrue(customDeployFolder.setWritable(false));
            Assert.assertFalse(modularDeployFolder.canWrite());
            Assert.assertFalse(customDeployFolder.canWrite());
            // do initial test
            do_test_started_event(false, false);
            // do actual test
            do_test_entity_crud(false, false);
        } finally {
            // reset before exit the test
            Assert.assertTrue(modularDeployFolder.setWritable(true));
            Assert.assertTrue(customDeployFolder.setWritable(true));
        }
    }
}