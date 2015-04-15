package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.module.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.*;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.licensing.UpdatableCompositeLicenseManager;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.module.ServerModuleFileManagerStub;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.util.*;
import org.junit.Assert;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * For the timer being tests only the {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile} portion
 * of the {@link ClusterStatusAdmin}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterStatusAdminImpTest {

    @Mock
    private ServiceUsageManager serviceUsageManager;
    @Mock
    private ClusterPropertyManager clusterPropertyManager;
    @Mock
    private UpdatableCompositeLicenseManager licenseManager;
    @Mock
    private ServiceMetricsManager metricsManager;
    @Mock
    private ServiceMetricsServices serviceMetricsServices;
    @Mock
    private ServerAssertionRegistry assertionRegistry;
    @Mock
    private TrustedEsmManager trustedEsmManager;
    @Mock
    private TrustedEsmUserManager trustedEsmUserManager;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private ExtensionInterfaceManager extensionInterfaceManager;
    @Mock
    private DateTimeConfigUtils dateTimeConfigUtils;

    private final ServerConfig serverConfig = new ServerConfigStub();
    private final Map<Goid, ServerModuleFile> moduleFiles = new HashMap<>(); // server module files initial repository
    private ClusterStatusAdmin admin;

    @Before
    public void setUp() throws Exception {
        final ClusterInfoManager clusterInfoManager = new ClusterInfoManagerStub() {
            @Override
            public String thisNodeId() {
                return getSelfNodeInf().getNodeIdentifier();
            }
        };
        final ServerModuleFileManager serverModuleFileManager = new ServerModuleFileManagerStub(createSampleModules(clusterInfoManager.getSelfNodeInf().getNodeIdentifier()));
        final ClusterStatusAdminImp admin = new ClusterStatusAdminImp(
                        clusterInfoManager,
                        serviceUsageManager,
                        clusterPropertyManager,
                        licenseManager,
                        metricsManager,
                        serviceMetricsServices,
                        serverConfig,
                        assertionRegistry,
                        trustedEsmManager,
                        trustedEsmUserManager,
                        rbacServices,
                        extensionInterfaceManager,
                        dateTimeConfigUtils
                );
        admin.setServerModuleFileManager(serverModuleFileManager);
        this.admin = admin;
    }

    // used to generate random GOID
    private static final Random rnd = new Random();
    private static final int typeLength = ModuleType.values().length;
    private static final long GOID_HI_START = Long.MAX_VALUE - 1;

    /**
     * Creates sample modules with sample states for different cluster nodes:
     *
     * ----------------------------------------
     * module_0: Goid(GOID_HI_START, 0)  sha_0
     * ----------------------------------------
     *      currentCluster  => ACCEPTED
     *      node_1          => UPLOADED
     *      node_3          => LOADED
     * ----------------------------------------
     *
     * ----------------------------------------
     * module_1: Goid(GOID_HI_START, 1)  sha_1
     * ----------------------------------------
     *      currentCluster  => UPLOADED
     *      node_1          => ACCEPTED
     *      node_2          => ACCEPTED
     *      node_3          => UPLOADED
     * ----------------------------------------
     *
     * ----------------------------------------
     * module_2: Goid(GOID_HI_START, 2)  sha_2
     * ----------------------------------------
     *      currentCluster  => UPLOADED
     *      node_2          => UPLOADED
     * ----------------------------------------
     *
     * ----------------------------------------
     * module_3: Goid(GOID_HI_START, 3)  sha_3
     * ----------------------------------------
     *      currentCluster  => REJECTED
     *      node_2          => LOADED
     *      node_3          => LOADED
     * ----------------------------------------

     * ----------------------------------------
     * module_4: Goid(GOID_HI_START, 4)  sha_4
     * ----------------------------------------
     *      node_3          => LOADED
     * ----------------------------------------

     * ----------------------------------------
     * module_5: Goid(GOID_HI_START, 5)  sha_5
     * ----------------------------------------
     *      (empty)
     * ----------------------------------------
     */
    public ServerModuleFile[] createSampleModules(final String clusterNodeId) {
        ServerModuleFile entity;
        List<ServerModuleFileState> states;

        for (int i = 0; i < 6; ++i) {
            entity = new ServerModuleFile();
            states = new ArrayList<>();
            entity.setStates(states);
            final ModuleType type = ModuleType.values()[rnd.nextInt(typeLength)];
            entity.setName("module_" + i);
            entity.setModuleType(type);
            final byte[] bytes = String.valueOf("test_data_" + i).getBytes(Charsets.UTF8);
            entity.createData(bytes);
            if (i < 4) entity.setStateForNode(clusterNodeId, ModuleState.UPLOADED);
            entity.setProperty(ServerModuleFile.PROP_FILE_NAME, "module_file_name_" + i);
            entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(i));
            entity.setProperty(ServerModuleFile.PROP_ASSERTIONS, "assertions_" + i);
            final Goid goid = new Goid(GOID_HI_START, i);
            entity.setGoid(goid);
            Assert.assertEquals(goid, entity.getGoid());
            Assert.assertNull(moduleFiles.put(entity.getGoid(), entity));
        }


        // ----------------------------------------
        // module_0:
        // ----------------------------------------
        //      currentCluster  => ACCEPTED
        //      node_1          => UPLOADED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = moduleFiles.get(new Goid(GOID_HI_START, 0));
        Assert.assertNotNull(entity);
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        entity.setStateForNode("node_1", ModuleState.UPLOADED);
        entity.setStateForNode("node_3", ModuleState.LOADED);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(3, states.size());
        Assert.assertEquals(clusterNodeId, states.get(0).getNodeId());
        Assert.assertEquals(ModuleState.ACCEPTED, states.get(0).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(0).getErrorMessage()));
        Assert.assertEquals("node_1", states.get(1).getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, states.get(1).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(1).getErrorMessage()));
        Assert.assertEquals("node_3", states.get(2).getNodeId());
        Assert.assertEquals(ModuleState.LOADED, states.get(2).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(2).getErrorMessage()));

        // ----------------------------------------
        // module_1:
        // ----------------------------------------
        //      currentCluster  => UPLOADED
        //      node_1          => ACCEPTED
        //      node_2          => ACCEPTED
        //      node_3          => UPLOADED
        // ----------------------------------------
        entity = moduleFiles.get(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        entity.setStateForNode("node_1", ModuleState.ACCEPTED);
        entity.setStateForNode("node_2", ModuleState.ACCEPTED);
        entity.setStateForNode("node_3", ModuleState.UPLOADED);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(4, states.size());
        Assert.assertEquals(clusterNodeId, states.get(0).getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, states.get(0).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(0).getErrorMessage()));
        Assert.assertEquals("node_1", states.get(1).getNodeId());
        Assert.assertEquals(ModuleState.ACCEPTED, states.get(1).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(1).getErrorMessage()));
        Assert.assertEquals("node_2", states.get(2).getNodeId());
        Assert.assertEquals(ModuleState.ACCEPTED, states.get(2).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(2).getErrorMessage()));
        Assert.assertEquals("node_3", states.get(3).getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, states.get(3).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(3).getErrorMessage()));

        // ----------------------------------------
        // module_2:
        // ----------------------------------------
        //      currentCluster  => UPLOADED
        //      node_2          => UPLOADED
        // ----------------------------------------
        entity = moduleFiles.get(new Goid(GOID_HI_START, 2));
        Assert.assertNotNull(entity);
        entity.setStateForNode("node_2", ModuleState.UPLOADED);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(2, states.size());
        Assert.assertEquals(clusterNodeId, states.get(0).getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, states.get(0).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(0).getErrorMessage()));
        Assert.assertEquals("node_2", states.get(1).getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, states.get(1).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(1).getErrorMessage()));

        // ----------------------------------------
        // module_3:
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = moduleFiles.get(new Goid(GOID_HI_START, 3));
        Assert.assertNotNull(entity);
        entity.setStateForNode(clusterNodeId, ModuleState.REJECTED);
        entity.setStateForNode("node_2", ModuleState.LOADED);
        entity.setStateForNode("node_3", ModuleState.LOADED);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(3, states.size());
        Assert.assertEquals(clusterNodeId, states.get(0).getNodeId());
        Assert.assertEquals(ModuleState.REJECTED, states.get(0).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(0).getErrorMessage()));
        Assert.assertEquals("node_2", states.get(1).getNodeId());
        Assert.assertEquals(ModuleState.LOADED, states.get(1).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(1).getErrorMessage()));
        Assert.assertEquals("node_3", states.get(2).getNodeId());
        Assert.assertEquals(ModuleState.LOADED, states.get(2).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(2).getErrorMessage()));

        // ----------------------------------------
        // module_4:
        // ----------------------------------------
        //      node_3          => LOADED
        // ----------------------------------------
        entity = moduleFiles.get(new Goid(GOID_HI_START, 4));
        Assert.assertNotNull(entity);
        entity.setStateForNode("node_3", ModuleState.LOADED);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(1, states.size());
        Assert.assertEquals("node_3", states.get(0).getNodeId());
        Assert.assertEquals(ModuleState.LOADED, states.get(0).getState());
        Assert.assertTrue(StringUtils.isBlank(states.get(0).getErrorMessage()));


        // ----------------------------------------
        // module_5:
        // ----------------------------------------
        // (empty)
        // ----------------------------------------
        entity = moduleFiles.get(new Goid(GOID_HI_START, 5));
        Assert.assertNotNull(entity);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(0, states.size());

        return moduleFiles.values().toArray(new ServerModuleFile[moduleFiles.size()]);
    }

    private void testServerModuleFileCopyAgainstModulesRepo(final ServerModuleFile moduleFile, final boolean dataBytesIncluded) {
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getGoid());
        final ServerModuleFile orgModuleFile = moduleFiles.get(moduleFile.getGoid());
        Assert.assertNotNull(orgModuleFile);
        Assert.assertNotSame(orgModuleFile, moduleFile);

        // test data
        Assert.assertNotNull(orgModuleFile.getData());
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertNotNull(orgModuleFile.getData().getDataBytes());
        if (dataBytesIncluded) {
            Assert.assertTrue(Arrays.equals(orgModuleFile.getData().getDataBytes(), moduleFile.getData().getDataBytes()));
        } else {
            Assert.assertNull(moduleFile.getData().getDataBytes()); // this is a copy so no data bytes
        }

        // test the rest of the attributes
        Assert.assertEquals(orgModuleFile.getGoid(), moduleFile.getGoid());
        Assert.assertEquals(orgModuleFile.getName(), moduleFile.getName());
        Assert.assertEquals(orgModuleFile.getModuleType(), moduleFile.getModuleType());
        Assert.assertEquals(orgModuleFile.getModuleSha256(), moduleFile.getModuleSha256());
        Assert.assertEquals(orgModuleFile.getXmlProperties(), moduleFile.getXmlProperties());
        Assert.assertEquals(orgModuleFile.getHumanReadableFileSize(), moduleFile.getHumanReadableFileSize());
        Assert.assertEquals(orgModuleFile.getStates(), moduleFile.getStates());
    }

    @Test
    public void testFindAllServerModuleFiles() throws Exception {
        Assert.assertNotNull(admin);
        // all objects in the collection should be copies without data-bytes
        for (final ServerModuleFile moduleFile : admin.findAllServerModuleFiles()) {
            testServerModuleFileCopyAgainstModulesRepo(moduleFile, false);
        }
    }

    private static void deepCompareModules(final ServerModuleFile left, final ServerModuleFile right) {
        Assert.assertNotNull(left);
        Assert.assertNotNull(right);

        assertThat(left, equalTo(right));
        assertThat(left.getGoid(), equalTo(right.getGoid()));
        assertThat(left.getVersion(), equalTo(right.getVersion()));
        assertThat(left.getName(), equalTo(right.getName()));
        assertThat(left.getModuleType(), equalTo(right.getModuleType()));
        assertThat(left.getModuleSha256(), equalTo(right.getModuleSha256()));
        assertThat(left.getXmlProperties(), equalTo(right.getXmlProperties()));
        assertThat(left.getHumanReadableFileSize(), equalTo(right.getHumanReadableFileSize()));
        assertThat(left.getStates(), equalTo(right.getStates()));
        assertThat(left.getData(), equalTo(right.getData()));
    }

    private static ServerModuleFile findInAddRemoveModules(final ServerModuleFile module, final Collection<ServerModuleFile> modules) {
        Assert.assertNotNull(module);
        Assert.assertNotNull(module.getGoid());
        assertThat(module.getGoid(), not(equalTo(Goid.DEFAULT_GOID)));
        Assert.assertNotNull(modules);

        for (final ServerModuleFile moduleFile : modules) {
            Assert.assertNotNull(moduleFile);
            Assert.assertNotNull(moduleFile.getGoid());
            assertThat(moduleFile.getGoid(), not(equalTo(Goid.DEFAULT_GOID)));
            if (module.getGoid().equals(moduleFile.getGoid())) {
                return moduleFile;
            }
        }
        Assert.fail("module not found in add-remove collection");
        return null;
    }

    private void run_initial_update(
            final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer,
            final Collection<ServerModuleFile> serverModuleFiles
    ) throws FindException {
        // initial update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertNotNull(addedRemoved.left); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        Assert.assertFalse(addedRemoved.left.isEmpty()); // initially update will get all modules
        Assert.assertFalse(serverModuleFiles.isEmpty()); // initially update will get all modules
        assertThat(addedRemoved.left.size(), equalTo(admin.findAllServerModuleFiles().size())); // initially update will get all modules
        assertThat(serverModuleFiles.size(), equalTo(admin.findAllServerModuleFiles().size())); // initially update will get all modules
        // make sure all modules exist
        for (final ServerModuleFile moduleFile : addedRemoved.left) {
            Assert.assertNotNull(moduleFile);
            Assert.assertNotNull(moduleFile.getGoid());
            final ServerModuleFile orgModuleFile = admin.findServerModuleFileById(moduleFile.getGoid(), true);
            Assert.assertNotNull(orgModuleFile);
            Assert.assertNotSame(moduleFile, orgModuleFile);
            assertThat(moduleFile, equalTo(orgModuleFile));
        }
        // make sure all modules exist
        for (final ServerModuleFile moduleFile : serverModuleFiles) {
            Assert.assertNotNull(moduleFile);
            Assert.assertNotNull(moduleFile.getGoid());
            final ServerModuleFile orgModuleFile = admin.findServerModuleFileById(moduleFile.getGoid(), true);
            Assert.assertNotNull(orgModuleFile);
            Assert.assertNotSame(moduleFile, orgModuleFile);
            assertThat(moduleFile, equalTo(orgModuleFile));
        }

        // test update when nothing has changed
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_add_new_module() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // add new entity
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        int modulesSize = admin.findAllServerModuleFiles().size();
        ServerModuleFile moduleFile = new ServerModuleFile();
        assertThat(moduleFile.getGoid(), equalTo(Goid.DEFAULT_GOID));
        moduleFile.setName("test_module_1");
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        moduleFile.createData("test_data_1".getBytes(Charsets.UTF8));
        // save
        Goid goid = admin.saveServerModuleFile(moduleFile);
        Assert.assertNotNull(goid);
        assertThat(goid, not(equalTo(Goid.DEFAULT_GOID)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize + 1));
        // get saved module
        moduleFile = admin.findServerModuleFileById(goid, true);
        Assert.assertNotNull(moduleFile);
        // do update after adding new module
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));
        assertThat(addedRemoved.left.size(), equalTo(1));
        deepCompareModules(moduleFile, (ServerModuleFile) addedRemoved.left.toArray()[0]);

        // test update when nothing has changed
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_add_multiple_modules() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // add multiple entities (3 to be exact)
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        int modulesSize = admin.findAllServerModuleFiles().size();
        ServerModuleFile addedModuleFiles[] = new ServerModuleFile[] { new ServerModuleFile(), new ServerModuleFile(), new ServerModuleFile() };
        Goid newGoids[] = new Goid[] { Goid.DEFAULT_GOID, Goid.DEFAULT_GOID, Goid.DEFAULT_GOID };
        assertThat(addedModuleFiles.length, Matchers.greaterThan(0));
        assertThat(addedModuleFiles.length, equalTo(newGoids.length));
        for (int i = 0; i < newGoids.length; ++i) {
            assertThat(addedModuleFiles[i].getGoid(), equalTo(Goid.DEFAULT_GOID));
            addedModuleFiles[i].setName("series_test_module_" + i);
            addedModuleFiles[i].setModuleType(ModuleType.CUSTOM_ASSERTION);
            addedModuleFiles[i].createData(String.valueOf("series_test_data_" + i).getBytes(Charsets.UTF8));
            // save
            newGoids[i] = admin.saveServerModuleFile(addedModuleFiles[i]);
            Assert.assertNotNull(newGoids[i]);
            assertThat(newGoids[i], not(equalTo(Goid.DEFAULT_GOID)));
        }
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize + newGoids.length));
        // get saved modules
        for (int i = 0; i < newGoids.length; ++i) {
            addedModuleFiles[i] = admin.findServerModuleFileById(newGoids[i], true);
            Assert.assertNotNull(addedModuleFiles[i]);
        }
        // do update after adding the modules
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));
        assertThat(addedRemoved.left.size(), equalTo(newGoids.length));
        for (int i = 0; i < newGoids.length; ++i) {
            deepCompareModules(addedModuleFiles[i], findInAddRemoveModules(addedModuleFiles[i], addedRemoved.left));
        }

        // test update when nothing has changed
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_updates() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // update test; no changes made
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        int modulesSize = admin.findAllServerModuleFiles().size();
        ServerModuleFile moduleFile = new ServerModuleFile();
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 0)), false, true, true);
        assertThat(moduleFile.getGoid(), not(equalTo(Goid.DEFAULT_GOID)));
        // update
        Goid goid = admin.saveServerModuleFile(moduleFile);
        Assert.assertNotNull(goid);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 0)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no additions
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));


        // test update when nothing has changed
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));


        // update test; new name
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 0)), true, true, true);
        moduleFile.setName("new_new_new_name_123");
        assertThat(moduleFile.getGoid(), not(equalTo(Goid.DEFAULT_GOID)));
        // update
        goid = admin.saveServerModuleFile(moduleFile);
        Assert.assertNotNull(goid);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 0)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no additions
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), true);
        Assert.assertNotNull(moduleFile);
        assertThat(moduleFile.getName(), equalTo("new_new_new_name_123"));
        // do update
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (one; the new value)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (one; the old value)
        assertThat(addedRemoved.left.size(), equalTo(1));
        assertThat(addedRemoved.right.size(), equalTo(1));
        ServerModuleFile addedModule = addedRemoved.left.iterator().next();
        ServerModuleFile removedModule = addedRemoved.right.iterator().next();
        Assert.assertNotNull(addedModule);
        Assert.assertNotNull(removedModule);
        Assert.assertNotSame(addedModule, removedModule);
        assertThat(addedModule, not(equalTo(removedModule)));
        assertThat(addedModule.getGoid(), equalTo(removedModule.getGoid()));
        assertThat(addedModule.getVersion(), Matchers.greaterThan(removedModule.getVersion()));
        assertThat(addedModule.getXmlProperties(), equalTo(removedModule.getXmlProperties()));
        assertThat(addedModule.getModuleType(), equalTo(removedModule.getModuleType()));
        assertThat(addedModule.getModuleSha256(), equalTo(removedModule.getModuleSha256()));
        assertThat(addedModule.getStates(), equalTo(removedModule.getStates()));
        Assert.assertTrue(Arrays.equals(addedModule.getData().getDataBytes(), removedModule.getData().getDataBytes()));
        assertThat(addedModule.getName(), not(equalTo(removedModule.getName())));
        assertThat(addedModule.getName(), equalTo("new_new_new_name_123"));
        assertThat(removedModule.getName(), equalTo("module_0"));
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));


        // update test; new data-bytes
        // ----------------------------------------
        // module_0:
        // ----------------------------------------
        //      currentCluster  => ACCEPTED
        //      node_1          => UPLOADED
        //      node_3          => LOADED
        // ----------------------------------------
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 0)), false, true, true);
        byte[] bytes = "new_data_for_module_0".getBytes(Charsets.UTF8);
        moduleFile.createData(bytes);
        assertThat(moduleFile.getGoid(), not(equalTo(Goid.DEFAULT_GOID)));
        // update
        goid = admin.saveServerModuleFile(moduleFile);
        Assert.assertNotNull(goid);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 0)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no additions
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), true);
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), bytes));
        // do update
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (one; the new value)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (one; the old value)
        assertThat(addedRemoved.left.size(), equalTo(1));
        assertThat(addedRemoved.right.size(), equalTo(1));
        addedModule = addedRemoved.left.iterator().next();
        removedModule = addedRemoved.right.iterator().next();
        Assert.assertNotNull(addedModule);
        Assert.assertNotNull(removedModule);
        Assert.assertNotSame(addedModule, removedModule);
        assertThat(addedModule, not(equalTo(removedModule)));
        assertThat(addedModule.getGoid(), equalTo(removedModule.getGoid()));
        assertThat(addedModule.getVersion(), Matchers.greaterThan(removedModule.getVersion()));
        assertThat(addedModule.getName(), equalTo(removedModule.getName()));
        assertThat(addedModule.getModuleType(), equalTo(removedModule.getModuleType()));
        assertThat(addedModule.getModuleSha256(), not(equalTo(removedModule.getModuleSha256())));
        assertThat(addedModule.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes)));
        assertThat(removedModule.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_0".getBytes(Charsets.UTF8))));
        assertThat(addedModule.getXmlProperties(), not(equalTo(removedModule.getXmlProperties())));
        assertThat(addedModule.getProperty(ServerModuleFile.PROP_FILE_NAME), equalTo(removedModule.getProperty(ServerModuleFile.PROP_FILE_NAME)));
        assertThat(addedModule.getProperty(ServerModuleFile.PROP_ASSERTIONS), equalTo(removedModule.getProperty(ServerModuleFile.PROP_ASSERTIONS)));
        assertThat(addedModule.getProperty(ServerModuleFile.PROP_SIZE), not(equalTo(removedModule.getProperty(ServerModuleFile.PROP_SIZE))));
        assertThat(addedModule.getProperty(ServerModuleFile.PROP_SIZE), equalTo(String.valueOf(bytes.length)));
        assertThat(removedModule.getProperty(ServerModuleFile.PROP_SIZE), equalTo("0"));
        assertThat(addedModule.getStates(), not(equalTo(removedModule.getStates())));
        assertThat(getStateForThisNode(addedModule).getState(), not(equalTo(getStateForThisNode(removedModule).getState())));
        assertThat(getStateForThisNode(addedModule).getState(), equalTo(ModuleState.UPLOADED));
        assertThat(getStateForThisNode(removedModule).getState(), equalTo(ModuleState.ACCEPTED));
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));


        // test update when nothing has changed
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_update_state() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // module_1: Goid(GOID_HI_START, 1)
        // ----------------------------------------
        // currentCluster  => UPLOADED
        // node_1          => ACCEPTED
        // node_2          => ACCEPTED
        // node_3          => UPLOADED
        // ----------------------------------------
        // update state for one node
        ServerModuleFile moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 1)); // get from the storage directly, as admin cannot update module states
        Assert.assertNotNull(moduleFile);
        // ACCEPTED => REJECTED
        String nodeToChange = "node_1";
        moduleFile.setStateForNode(nodeToChange, ModuleState.REJECTED);
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (one; the new value)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (one; the old value)
        assertThat(addedRemoved.left.size(), equalTo(1));
        assertThat(addedRemoved.right.size(), equalTo(1));
        ServerModuleFile addedModule = addedRemoved.left.iterator().next();
        ServerModuleFile removedModule = addedRemoved.right.iterator().next();
        Assert.assertNotNull(addedModule);
        Assert.assertNotNull(removedModule);
        Assert.assertNotSame(addedModule, removedModule);
        assertThat(addedModule, equalTo(removedModule));
        assertThat(addedModule.getStates(), not(equalTo(removedModule.getStates())));
        for (final ServerModuleFileState addedState : addedModule.getStates()) {
            Assert.assertNotNull(addedState);
            ServerModuleFileState removedState = null;
            for (final ServerModuleFileState state : removedModule.getStates()) {
                Assert.assertNotNull(state);
                if (addedState.getNodeId().equals(state.getNodeId())) {
                    removedState = state;
                    break;
                }
            }
            Assert.assertNotNull(removedState);
            if (nodeToChange.equals(addedState.getNodeId())) {
                assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                assertThat(addedState.getErrorMessage(), equalTo(removedState.getErrorMessage()));
                assertThat(addedState.getState(), equalTo(ModuleState.REJECTED));
                assertThat(removedState.getState(), equalTo(ModuleState.ACCEPTED));
            } else {
                assertThat(addedState, equalTo(removedState));
            }
        }


        // ----------------------------------------
        // currentCluster  => UPLOADED
        // node_1          => REJECTED
        // node_2          => ACCEPTED
        // node_3          => UPLOADED
        // ----------------------------------------
        // update error message for one node
        moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 1)); // get from the storage directly, as admin cannot update module states
        Assert.assertNotNull(moduleFile);
        // ACCEPTED => (ERROR, "error for node_2")
        nodeToChange = "node_2";
        moduleFile.setStateErrorMessageForNode(nodeToChange, "error for node_2");
        // do update
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (one; the new value)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (one; the old value)
        assertThat(addedRemoved.left.size(), equalTo(1));
        assertThat(addedRemoved.right.size(), equalTo(1));
        addedModule = addedRemoved.left.iterator().next();
        removedModule = addedRemoved.right.iterator().next();
        Assert.assertNotNull(addedModule);
        Assert.assertNotNull(removedModule);
        Assert.assertNotSame(addedModule, removedModule);
        assertThat(addedModule, equalTo(removedModule));
        assertThat(addedModule.getStates(), not(equalTo(removedModule.getStates())));
        for (final ServerModuleFileState addedState : addedModule.getStates()) {
            Assert.assertNotNull(addedState);
            ServerModuleFileState removedState = null;
            for (final ServerModuleFileState state : removedModule.getStates()) {
                Assert.assertNotNull(state);
                if (addedState.getNodeId().equals(state.getNodeId())) {
                    removedState = state;
                    break;
                }
            }
            Assert.assertNotNull(removedState);
            if (nodeToChange.equals(addedState.getNodeId())) {
                assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                assertThat(addedState.getState(), equalTo(ModuleState.ERROR));
                assertThat(removedState.getState(), equalTo(ModuleState.ACCEPTED));
                assertThat(addedState.getErrorMessage(), equalTo("error for node_2"));
                assertThat(removedState.getErrorMessage(), Matchers.isEmptyOrNullString());
            } else {
                assertThat(addedState, equalTo(removedState));
            }
        }


        // test update when nothing has changed
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_update_multiple_states() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // ----------------------------------------
        // module_0: Goid(GOID_HI_START, 0)
        // ----------------------------------------
        // currentCluster  => ACCEPTED
        // node_1          => UPLOADED
        // node_3          => LOADED
        // ----------------------------------------
        // update multiple states for two node
        ServerModuleFile moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 0)); // get from the storage directly, as admin cannot update module states
        Assert.assertNotNull(moduleFile);
        // make sure states are as expected
        ServerModuleFileState tmpState = moduleFile.getStateForNode(admin.getSelfNode().getNodeIdentifier());Assert.assertNotNull(tmpState);
        assertThat(tmpState.getState(), equalTo(ModuleState.ACCEPTED));
        tmpState = moduleFile.getStateForNode("node_3");Assert.assertNotNull(tmpState);
        assertThat(tmpState.getState(), equalTo(ModuleState.LOADED));
        // set states
        // currentCluster | ACCEPTED => (ERROR, "error for current_node")
        // node_3         | LOADED => ACCEPTED
        moduleFile.setStateErrorMessageForNode(admin.getSelfNode().getNodeIdentifier(), "error for current_node");
        moduleFile.setStateForNode("node_3", ModuleState.ACCEPTED);
        // ----------------------------------------
        // module_1: Goid(GOID_HI_START, 1)
        // ----------------------------------------
        // currentCluster  => UPLOADED
        // node_1          => ACCEPTED
        // node_2          => ACCEPTED
        // node_3          => UPLOADED
        // ----------------------------------------
        moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 1)); // get from the storage directly, as admin cannot update module states
        Assert.assertNotNull(moduleFile);
        // make sure states are as expected
        tmpState = moduleFile.getStateForNode(admin.getSelfNode().getNodeIdentifier());Assert.assertNotNull(tmpState);
        assertThat(tmpState.getState(), equalTo(ModuleState.UPLOADED));
        tmpState = moduleFile.getStateForNode("node_2");Assert.assertNotNull(tmpState);
        assertThat(tmpState.getState(), equalTo(ModuleState.ACCEPTED));
        tmpState = moduleFile.getStateForNode("node_1");Assert.assertNotNull(tmpState);
        assertThat(tmpState.getState(), equalTo(ModuleState.ACCEPTED));
        // set states
        // currentCluster | UPLOADED => ACCEPTED
        // node_2         | ACCEPTED   => LOADED
        // node_1         | ACCEPTED   => UPLOADED
        moduleFile.setStateForNode(admin.getSelfNode().getNodeIdentifier(), ModuleState.ACCEPTED);
        moduleFile.setStateForNode("node_2", ModuleState.LOADED);
        moduleFile.setStateForNode("node_1", ModuleState.UPLOADED);
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (two)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (two)
        assertThat(addedRemoved.left.size(), equalTo(2));
        assertThat(addedRemoved.right.size(), equalTo(2));
        for (final ServerModuleFile addedModule : addedRemoved.left) {
            final ServerModuleFile removedModule = findInAddRemoveModules(addedModule, addedRemoved.right);
            Assert.assertNotNull(removedModule);
            Assert.assertNotSame(addedModule, removedModule);
            assertThat(addedModule, equalTo(removedModule));
            assertThat(addedModule.getGoid(), either(equalTo(new Goid(GOID_HI_START, 0))).or(equalTo(new Goid(GOID_HI_START, 1))));
            assertThat(addedModule.getStates(), not(equalTo(removedModule.getStates())));
            if (addedModule.getGoid().equals(new Goid(GOID_HI_START, 0))) {
                // module_0
                assertThat(addedModule.getGoid(), equalTo(new Goid(GOID_HI_START, 0)));
                for (final ServerModuleFileState addedState : addedModule.getStates()) {
                    Assert.assertNotNull(addedState);
                    ServerModuleFileState removedState = null;
                    for (final ServerModuleFileState state : removedModule.getStates()) {
                        Assert.assertNotNull(state);
                        if (addedState.getNodeId().equals(state.getNodeId())) {
                            removedState = state;
                            break;
                        }
                    }
                    Assert.assertNotNull(removedState);
                    if (admin.getSelfNode().getNodeIdentifier().equals(addedState.getNodeId())) {
                        // currentCluster | ACCEPTED => (ERROR, "error for current_node")
                        assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                        assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                        assertThat(addedState.getState(), equalTo(ModuleState.ERROR));
                        assertThat(removedState.getState(), equalTo(ModuleState.ACCEPTED));
                        assertThat(addedState.getErrorMessage(), equalTo("error for current_node"));
                        assertThat(removedState.getErrorMessage(), Matchers.isEmptyOrNullString());
                    } else if ("node_3".equals(addedState.getNodeId())) {
                        // node_3 | LOADED => ACCEPTED
                        assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                        assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                        assertThat(addedState.getState(), equalTo(ModuleState.ACCEPTED));
                        assertThat(removedState.getState(), equalTo(ModuleState.LOADED));
                        assertThat(addedState.getErrorMessage(), Matchers.isEmptyOrNullString());
                        assertThat(addedState.getErrorMessage(), equalTo(removedState.getErrorMessage()));
                    } else {
                        assertThat(addedState, equalTo(removedState));
                    }
                }
            } else {
                // module_1
                assertThat(addedModule.getGoid(), equalTo(new Goid(GOID_HI_START, 1)));
                for (final ServerModuleFileState addedState : addedModule.getStates()) {
                    Assert.assertNotNull(addedState);
                    ServerModuleFileState removedState = null;
                    for (final ServerModuleFileState state : removedModule.getStates()) {
                        Assert.assertNotNull(state);
                        if (addedState.getNodeId().equals(state.getNodeId())) {
                            removedState = state;
                            break;
                        }
                    }
                    Assert.assertNotNull(removedState);
                    if (admin.getSelfNode().getNodeIdentifier().equals(addedState.getNodeId())) {
                        // currentCluster | UPLOADED => ACCEPTED
                        assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                        assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                        assertThat(addedState.getState(), equalTo(ModuleState.ACCEPTED));
                        assertThat(removedState.getState(), equalTo(ModuleState.UPLOADED));
                        assertThat(addedState.getErrorMessage(), Matchers.isEmptyOrNullString());
                        assertThat(addedState.getErrorMessage(), equalTo(removedState.getErrorMessage()));
                    } else if ("node_2".equals(addedState.getNodeId())) {
                        // node_2 | ACCEPTED   => LOADED
                        assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                        assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                        assertThat(addedState.getState(), equalTo(ModuleState.LOADED));
                        assertThat(removedState.getState(), equalTo(ModuleState.ACCEPTED));
                        assertThat(addedState.getErrorMessage(), Matchers.isEmptyOrNullString());
                        assertThat(removedState.getErrorMessage(), Matchers.isEmptyOrNullString());
                    } else if ("node_1".equals(addedState.getNodeId())) {
                        // node_1 | ACCEPTED   => UPLOADED
                        assertThat(addedState.getGoid(), equalTo(removedState.getGoid()));
                        assertThat(addedState.getNodeId(), equalTo(removedState.getNodeId()));
                        assertThat(addedState.getState(), equalTo(ModuleState.UPLOADED));
                        assertThat(removedState.getState(), equalTo(ModuleState.ACCEPTED));
                        assertThat(addedState.getErrorMessage(), Matchers.isEmptyOrNullString());
                        assertThat(addedState.getErrorMessage(), equalTo(removedState.getErrorMessage()));
                    } else {
                        assertThat(addedState, equalTo(removedState));
                    }
                }
            }
        }


        // test update when nothing has changed
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_add_new_state() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // ----------------------------------------
        // module_5:
        // ----------------------------------------
        // (empty)
        // ----------------------------------------
        ServerModuleFile moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 5)); // get from the storage directly, as admin cannot update module states
        Assert.assertNotNull(moduleFile.getStates());
        Assert.assertTrue(moduleFile.getStates().isEmpty());
        moduleFile.setStateForNode("first_node", ModuleState.REJECTED);
        moduleFile.setStateErrorMessageForNode("second_node", "error for second node");
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (new values)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (old values)
        assertThat(addedRemoved.left.size(), equalTo(1));
        assertThat(addedRemoved.right.size(), equalTo(1));
        ServerModuleFile addedModule = addedRemoved.left.iterator().next();
        ServerModuleFile removedModule = addedRemoved.right.iterator().next();
        Assert.assertNotNull(addedModule);
        Assert.assertNotNull(removedModule);
        Assert.assertNotSame(addedModule, removedModule);
        assertThat(addedModule, equalTo(removedModule)); // modules are the same ...
        assertThat(addedModule.getStates(), not(equalTo(removedModule.getStates()))); // ... its that their states are different
        Assert.assertNotNull(addedModule.getStates());
        assertThat(addedModule.getStates().size(), equalTo(2));
        Assert.assertTrue(removedModule.getStates().isEmpty());
        for (final ServerModuleFileState state : addedModule.getStates()) {
            assertThat(state.getNodeId(), either(equalTo("first_node")).or(equalTo("second_node")));
            if ("first_node".equals(state.getNodeId())) {
                assertThat(state.getState(), equalTo(ModuleState.REJECTED));
                assertThat(state.getErrorMessage(), Matchers.isEmptyOrNullString());
            } else {
                assertThat(state.getNodeId(), equalTo("second_node"));
                assertThat(state.getState(), equalTo(ModuleState.ERROR));
                assertThat(state.getErrorMessage(), equalTo("error for second node"));
            }
        }


        // test update when nothing has changed
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }


    @Test
    public void test_getServerModuleFileUpdate_no_changes_state() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);

        // no changes
        // ----------------------------------------
        // module_4: Goid(GOID_HI_START, 4)
        // ----------------------------------------
        // node_3          => LOADED
        // ----------------------------------------
        // test no state changes
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        ServerModuleFile moduleFile = moduleFiles.get(new Goid(GOID_HI_START, 4)); // get from the storage directly, as admin cannot update module states
        Assert.assertNotNull(moduleFile);
        // LOADED => LOADED
        String nodeToChange = "node_3";
        ServerModuleFileState state = moduleFile.getStateForNode(nodeToChange);
        Assert.assertNotNull(state);
        assertThat(state.getState(), equalTo(ModuleState.LOADED));
        moduleFile.setStateForNode(nodeToChange, ModuleState.LOADED); // no change
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // no added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // no removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_update_multiple_modules() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // update test; update multiple (module_4, module_3)  change data, type and name
        // ----------------------------------------
        // module_3 (currentCluster  => REJECTED)
        // module_4 (currentCluster  => <none>)
        // ----------------------------------------
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        int modulesSize = admin.findAllServerModuleFiles().size();
        // goid -> (original, updated)
        Map<Goid, MutablePair<ServerModuleFile, ServerModuleFile>> updatedModuleFileMap = new LinkedHashMap<>();
        updatedModuleFileMap.put(new Goid(GOID_HI_START, 4), new MutablePair<>(new ServerModuleFile(), new ServerModuleFile()));
        updatedModuleFileMap.put(new Goid(GOID_HI_START, 3), new MutablePair<>(new ServerModuleFile(), new ServerModuleFile()));
        // update all
        assertThat(updatedModuleFileMap.size(), Matchers.greaterThan(0));
        for (final Map.Entry<Goid, MutablePair<ServerModuleFile, ServerModuleFile>> entry : updatedModuleFileMap.entrySet()) {
            Goid updateGoid = entry.getKey();
            assertThat(entry.getValue().left.getGoid(), equalTo(Goid.DEFAULT_GOID));
            assertThat(entry.getValue().right.getGoid(), equalTo(Goid.DEFAULT_GOID));
            // copy for original
            entry.getValue().left.copyFrom(moduleFiles.get(updateGoid), true, true, true);
            // copy for updated
            ServerModuleFile moduleFile = entry.getValue().right;
            moduleFile.copyFrom(moduleFiles.get(updateGoid), true, true, true);
            assertThat(entry.getValue().left.getGoid(), equalTo(updateGoid));
            assertThat(entry.getValue().right.getGoid(), equalTo(updateGoid));
            moduleFile.createData(String.valueOf("new_data_for_module_" + updateGoid.getLow()).getBytes(Charsets.UTF8));
            setNextModuleType(moduleFile);
            moduleFile.setName("new_name_for_module_" + updateGoid.getLow());
            Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
            // update
            Goid goid = admin.saveServerModuleFile(moduleFile);
            Assert.assertNotNull(goid);
            assertThat(goid, equalTo(updateGoid));
            // verify
            Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
            entry.getValue().right = admin.findServerModuleFileById(updateGoid, true);
        }
        // verify
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no additions
        for (final Goid updateGoid : updatedModuleFileMap.keySet()) {
            Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
        }
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added (the new values)
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (old values)
        assertThat(addedRemoved.left.size(), equalTo(updatedModuleFileMap.size()));
        assertThat(addedRemoved.right.size(), equalTo(updatedModuleFileMap.size()));
        // process added and removed modules
        for (final ServerModuleFile addedModule : addedRemoved.left) {
            Assert.assertNotNull(addedModule);
            Assert.assertNotNull(addedModule.getGoid());
            ServerModuleFile removedModule = null;
            // find corresponding removed module
            for (final ServerModuleFile removedModule1 : addedRemoved.right) {
                Assert.assertNotNull(removedModule1);
                Assert.assertNotNull(removedModule1.getGoid());
                if (addedModule.getGoid().equals(removedModule1.getGoid())) {
                    removedModule = removedModule1;
                    break;
                }
            }
            // verify added and removed module
            Assert.assertNotNull(removedModule);
            Assert.assertNotSame(addedModule, removedModule);
            assertThat(addedModule, not(equalTo(removedModule)));
            assertThat(addedModule.getGoid(), equalTo(removedModule.getGoid()));
            // get our original and updated module values
            final MutablePair<ServerModuleFile, ServerModuleFile> modulesPair = updatedModuleFileMap.get(addedModule.getGoid());
            Assert.assertNotNull(modulesPair);
            final ServerModuleFile original = modulesPair.left;
            final ServerModuleFile updated = modulesPair.right;
            Assert.assertNotNull(original);
            Assert.assertNotNull(updated);
            assertThat(original.getGoid(), equalTo(removedModule.getGoid()));
            assertThat(updated.getGoid(), equalTo(addedModule.getGoid()));
            // test version
            assertThat(addedModule.getVersion(), Matchers.greaterThan(removedModule.getVersion()));
            assertThat(addedModule.getVersion(), equalTo(updated.getVersion()));
            assertThat(removedModule.getVersion(), equalTo(original.getVersion()));
            // test name
            assertThat(addedModule.getName(), not(equalTo(removedModule.getName())));
            assertThat(addedModule.getName(), equalTo(updated.getName()));
            assertThat(removedModule.getName(), equalTo(original.getName()));
            // test module type
            assertThat(addedModule.getModuleType(), not(equalTo(removedModule.getModuleType())));
            assertThat(addedModule.getModuleType(), equalTo(updated.getModuleType()));
            assertThat(removedModule.getModuleType(), equalTo(original.getModuleType()));
            // test sha256
            assertThat(addedModule.getModuleSha256(), not(equalTo(removedModule.getModuleSha256())));
            assertThat(addedModule.getModuleSha256(), equalTo(updated.getModuleSha256()));
            assertThat(removedModule.getModuleSha256(), equalTo(original.getModuleSha256()));
            // test xml props
            assertThat(addedModule.getXmlProperties(), not(equalTo(removedModule.getXmlProperties())));
            assertThat(addedModule.getXmlProperties(), equalTo(updated.getXmlProperties()));
            assertThat(removedModule.getXmlProperties(), equalTo(original.getXmlProperties()));
            // test individual props
            assertThat(addedModule.getProperty(ServerModuleFile.PROP_FILE_NAME), equalTo(removedModule.getProperty(ServerModuleFile.PROP_FILE_NAME)));
            assertThat(addedModule.getProperty(ServerModuleFile.PROP_ASSERTIONS), equalTo(removedModule.getProperty(ServerModuleFile.PROP_ASSERTIONS)));
            assertThat(addedModule.getProperty(ServerModuleFile.PROP_SIZE), not(equalTo(removedModule.getProperty(ServerModuleFile.PROP_SIZE))));
            assertThat(addedModule.getProperty(ServerModuleFile.PROP_SIZE), equalTo(updated.getProperty(ServerModuleFile.PROP_SIZE)));
            assertThat(removedModule.getProperty(ServerModuleFile.PROP_SIZE), equalTo(original.getProperty(ServerModuleFile.PROP_SIZE)));
            // test state
            assertThat(addedModule.getStates(), not(equalTo(removedModule.getStates())));
            assertThat(getStateForThisNode(addedModule), not(equalTo(getStateForThisNode(removedModule))));
            assertThat(getStateForThisNode(addedModule).getState(), equalTo(ModuleState.UPLOADED));
            // ----------------------------------------
            // module_3 (currentCluster  => REJECTED)
            // module_4 (currentCluster  => <none>)
            // ----------------------------------------
            if (getStateForThisNode(removedModule) != null) {
                assertThat(getStateForThisNode(removedModule).getState(), equalTo(ModuleState.REJECTED));
            } else {
                Assert.assertNull(getStateForThisNode(removedModule));
            }
        }
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));


        // test update when nothing has changed
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_delete_multiple_modules() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);


        // delete test; module_0
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        int modulesSize = admin.findAllServerModuleFiles().size();
        // delete
        admin.deleteServerModuleFile(new Goid(GOID_HI_START, 0));
        // verify
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize-1)); // changed -1
        Assert.assertNull(admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), false));
        // do update
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (one; module_0)
        assertThat(addedRemoved.right.size(), equalTo(1));
        ServerModuleFile removedModule = addedRemoved.right.iterator().next();
        Assert.assertNotNull(removedModule);
        assertThat(removedModule.getGoid(), equalTo(new Goid(GOID_HI_START, 0)));
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));
        assertThat(copyOfServerModuleFiles.size(), Matchers.greaterThan(serverModuleFiles.size()));


        // delete test; multiple deletes (module_1, module_2, module_3)
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        modulesSize = admin.findAllServerModuleFiles().size();
        Goid[] deleteGoids = new Goid[] { new Goid(GOID_HI_START, 1), new Goid(GOID_HI_START, 2), new Goid(GOID_HI_START, 3) };
        // delete all
        for (final Goid deleteGoid : deleteGoids) {
            admin.deleteServerModuleFile(deleteGoid);
        }
        // verify
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize-deleteGoids.length)); // changed
        for (final Goid deleteGoid : deleteGoids) {
            Assert.assertNull(admin.findServerModuleFileById(deleteGoid, false));
        }
        // do update
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed (one; module_1, module_2, module_3)
        assertThat(addedRemoved.right.size(), equalTo(deleteGoids.length));
        for (final ServerModuleFile module: addedRemoved.right) {
            Assert.assertNotNull(module);
            Goid deleteGoid = null;
            for (final Goid delGoid : deleteGoids) {
                Assert.assertNotNull(delGoid);
                if (delGoid.equals(module.getGoid())) {
                    deleteGoid = delGoid;
                    break;
                }
            }
            Assert.assertNotNull(deleteGoid);
        }
        assertThat(copyOfServerModuleFiles, not(equalTo(serverModuleFiles)));
        assertThat(copyOfServerModuleFiles.size(), Matchers.greaterThan(serverModuleFiles.size()));


        // test update when nothing has changed
        copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void test_getServerModuleFileUpdate_update_delete_add_state() throws Exception {
        Assert.assertNotNull(admin);

        final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer =
                new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
                    @Override
                    protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                        return admin.getServerModuleFileUpdate(oldVersionID);
                    }
                };
        final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
        Assert.assertTrue(serverModuleFiles.isEmpty());

        // run initial update
        run_initial_update(serverModuleFilesUpdateConsumer, serverModuleFiles);

        //
        // add(two), update(module_0, module_1, module_2), delete(module_3, module_4, module_5)
        //

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // modules to add (a couple)
        // (goid, module)
        final List<ServerModuleFile> addedModuleFiles = new ArrayList<>(Arrays.asList(
                new ServerModuleFile(),
                new ServerModuleFile()
        ));
        int modulesSize = admin.findAllServerModuleFiles().size();
        // add all
        assertThat(addedModuleFiles.size(), Matchers.greaterThan(0));
        for (int i = 0; i < addedModuleFiles.size(); ++i) {
            assertThat(addedModuleFiles.get(i).getGoid(), equalTo(Goid.DEFAULT_GOID));
            addedModuleFiles.get(i).setName("add_add_test_module_" + i);
            addedModuleFiles.get(i).setModuleType(ModuleType.CUSTOM_ASSERTION);
            addedModuleFiles.get(i).createData(String.valueOf("add_add_test_module_test_data_" + i).getBytes(Charsets.UTF8));
            // save
            Goid goid = admin.saveServerModuleFile(addedModuleFiles.get(i));
            Assert.assertNotNull(goid);
            assertThat(goid, not(equalTo(Goid.DEFAULT_GOID)));
            // get the added module
            Assert.assertNotNull(admin.findServerModuleFileById(goid, true));
            addedModuleFiles.set(i, admin.findServerModuleFileById(goid, true));
            assertThat(addedModuleFiles.get(i).getGoid(), equalTo(goid));
        }
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize + addedModuleFiles.size()));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // modules to update (module_0, module_1, module_2:state)
        // ----------------------------------------
        // module_2:
        // ----------------------------------------
        //      currentCluster  => UPLOADED
        //      node_2          => UPLOADED
        // ----------------------------------------
        // goid -> (original, updated)
        final Map<Goid, MutablePair<ServerModuleFile, ServerModuleFile>> updatedModuleFiles = new LinkedHashMap<>();
        updatedModuleFiles.put(new Goid(GOID_HI_START, 0), new MutablePair<>(new ServerModuleFile(), new ServerModuleFile()));
        updatedModuleFiles.put(new Goid(GOID_HI_START, 1), new MutablePair<>(new ServerModuleFile(), new ServerModuleFile()));
        updatedModuleFiles.put(new Goid(GOID_HI_START, 2), new MutablePair<>(new ServerModuleFile(), new ServerModuleFile()));
        // update all
        modulesSize = admin.findAllServerModuleFiles().size();
        assertThat(updatedModuleFiles.size(), Matchers.greaterThan(0));
        for (final Map.Entry<Goid, MutablePair<ServerModuleFile, ServerModuleFile>> entry : updatedModuleFiles.entrySet()) {
            Goid updateGoid = entry.getKey();
            assertThat(entry.getValue().left.getGoid(), equalTo(Goid.DEFAULT_GOID));
            assertThat(entry.getValue().right.getGoid(), equalTo(Goid.DEFAULT_GOID));
            // copy for original
            ServerModuleFile originalModuleFile = entry.getValue().left;
            originalModuleFile.copyFrom(moduleFiles.get(updateGoid), true, true, true);
            if (updateGoid.equals(new Goid(GOID_HI_START, 2))) {
                ServerModuleFile updatedModuleFile = moduleFiles.get(updateGoid); // get from the storage directly, as admin cannot update module states
                Assert.assertNotNull(updatedModuleFile);
                updatedModuleFile.setStateErrorMessageForNode("some_new_node", "error for some new node");
            } else {
                // copy for updated
                ServerModuleFile updatedModuleFile = entry.getValue().right;
                updatedModuleFile.copyFrom(originalModuleFile, true, true, true);
                assertThat(entry.getValue().left.getGoid(), equalTo(updateGoid));
                assertThat(entry.getValue().right.getGoid(), equalTo(updateGoid));
                updatedModuleFile.createData(String.valueOf("new_data_for_module_" + updateGoid.getLow()).getBytes(Charsets.UTF8));
                setNextModuleType(updatedModuleFile);
                updatedModuleFile.setName("new_name_for_module_" + updateGoid.getLow());
                Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
                // update
                Goid goid = admin.saveServerModuleFile(updatedModuleFile);
                Assert.assertNotNull(goid);
                assertThat(goid, equalTo(updateGoid));
            }
            // verify
            Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
            // get the updated module
            entry.getValue().right = admin.findServerModuleFileById(updateGoid, true);
        }
        // verify
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no additions
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // modules to delete (module_3, module_4, module_5)
        // (goid, module)
        final Map<Goid, ServerModuleFile> removedModuleFiles = new LinkedHashMap<>();
        removedModuleFiles.put(new Goid(GOID_HI_START, 3), new ServerModuleFile());
        removedModuleFiles.put(new Goid(GOID_HI_START, 4), new ServerModuleFile());
        removedModuleFiles.put(new Goid(GOID_HI_START, 5), new ServerModuleFile());
        // delete all
        modulesSize = admin.findAllServerModuleFiles().size();
        assertThat(removedModuleFiles.size(), Matchers.greaterThan(0));
        for (final Map.Entry<Goid, ServerModuleFile> entry : removedModuleFiles.entrySet()) {
            assertThat(entry.getKey(), not(equalTo(Goid.DEFAULT_GOID)));
            Assert.assertNotNull(admin.findServerModuleFileById(entry.getKey(), true));
            entry.setValue(admin.findServerModuleFileById(entry.getKey(), true));
            admin.deleteServerModuleFile(entry.getKey());
            Assert.assertNull(admin.findServerModuleFileById(entry.getKey(), false));
        }
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize - removedModuleFiles.size()));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        // all updates are done!
        // we have:
        // ----------------------------------------------
        // 2 new modules,
        // (3) updated modules (module_0, module_1, module_2)
        // 3 deleted modules (module_3, module_4, module_5)
        // ----------------------------------------------
        // added:   2 + (3) = 5
        // removed: (3) + 3 = 6
        // ----------------------------------------------
        // update consumer
        //
        // do update after adding the modules
        Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left != null && !addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right != null && !addedRemoved.right.isEmpty()); // removed
        assertThat(addedRemoved.left.size(), equalTo(addedModuleFiles.size() + updatedModuleFiles.size()));    // 5
        assertThat(addedRemoved.right.size(), equalTo(updatedModuleFiles.size() + removedModuleFiles.size())); // 6
        // verify added
        for (final ServerModuleFile addedModule : addedRemoved.left) {
            Assert.assertNotNull(addedModule);
            Assert.assertNotNull(addedModule.getGoid());
            boolean found = false;
            // check against added
            for (int i = 0; i < addedModuleFiles.size(); ++i) {
                Assert.assertNotNull(addedModuleFiles.get(i));
                Assert.assertNotNull(addedModuleFiles.get(i).getGoid());
                if (addedModule.getGoid().equals(addedModuleFiles.get(i).getGoid())) {
                    // test values
                    deepCompareModules(addedModule, addedModuleFiles.get(i));
                    addedModuleFiles.remove(i);
                    found = true;
                    break;
                }
            }
            if (found) continue;
            // check against updated
            // goid -> (original, updated)
            for (Map.Entry<Goid, MutablePair<ServerModuleFile, ServerModuleFile>> entry : updatedModuleFiles.entrySet()) {
                Assert.assertNotNull(entry.getKey());
                Assert.assertNotNull(entry.getValue());
                Assert.assertNotNull(entry.getValue().left);
                Assert.assertNotNull(entry.getValue().left.getGoid());
                Assert.assertNotNull(entry.getValue().right);
                Assert.assertNotNull(entry.getValue().right.getGoid());
                assertThat(entry.getValue().left.getGoid(), equalTo(entry.getValue().right.getGoid()));
                assertThat(entry.getValue().left.getGoid(), equalTo(entry.getKey()));
                if (addedModule.getGoid().equals(entry.getKey())) {
                    final ServerModuleFile removedModule = findInAddRemoveModules(addedModule, addedRemoved.right);
                    Assert.assertNotNull(removedModule);
                    Assert.assertNotSame(addedModule, removedModule);
                    assertThat(addedModule.getGoid(), equalTo(entry.getValue().left.getGoid()));
                    assertThat(removedModule.getGoid(), equalTo(entry.getValue().right.getGoid()));
                    deepCompareModules(addedModule, entry.getValue().right);
                    deepCompareModules(removedModule, entry.getValue().left);
                    updatedModuleFiles.remove(entry.getKey());
                    break;
                }
            }
        }
        // verify removed
        for (final ServerModuleFile removedModule : addedRemoved.right) {
            Assert.assertNotNull(removedModule);
            Assert.assertNotNull(removedModule.getGoid());
            // check against deleted
            for (final Map.Entry<Goid, ServerModuleFile> entry : removedModuleFiles.entrySet()) {
                Assert.assertNotNull(entry.getKey());
                Assert.assertNotNull(entry.getValue());
                if (removedModule.getGoid().equals(entry.getKey())) {
                    // test values
                    deepCompareModules(removedModule, entry.getValue());
                    removedModuleFiles.remove(entry.getKey());
                    break;
                }
            }
        }
        // make sure all are verified
        Assert.assertTrue(addedModuleFiles.isEmpty());
        Assert.assertTrue(updatedModuleFiles.isEmpty());
        Assert.assertTrue(removedModuleFiles.isEmpty());


        // test update when nothing has changed
        Collection<ServerModuleFile> copyOfServerModuleFiles = new ArrayList<>(serverModuleFiles);
        addedRemoved = serverModuleFilesUpdateConsumer.update(serverModuleFiles);
        Assert.assertNotNull(addedRemoved);
        Assert.assertTrue(addedRemoved.left == null || addedRemoved.left.isEmpty()); // added
        Assert.assertTrue(addedRemoved.right == null || addedRemoved.right.isEmpty()); // removed
        assertThat(copyOfServerModuleFiles, equalTo(serverModuleFiles));
    }

    @Test
    public void testFindServerModuleFileById() throws Exception {
        Assert.assertNotNull(admin);

        ServerModuleFile moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), false);
        Assert.assertNotNull(moduleFile);
        testServerModuleFileCopyAgainstModulesRepo(moduleFile, false);

        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), true);
        Assert.assertNotNull(moduleFile);
        testServerModuleFileCopyAgainstModulesRepo(moduleFile, true);

        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 100), false);
        Assert.assertNull(moduleFile);

        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 100), true);
        Assert.assertNull(moduleFile);
    }

    private ServerModuleFileState getStateForThisNode(final ServerModuleFile moduleFile) {
        Assert.assertNotNull(moduleFile);
        final Collection<ServerModuleFileState> states = moduleFile.getStates();
        if (states != null) {
            for (final ServerModuleFileState state : states) {
                Assert.assertTrue(StringUtils.isNotBlank(state.getNodeId()));
                if (state.getNodeId().equals(admin.getSelfNode().getNodeIdentifier())) {
                    return state;
                }
            }
        }
        return null;
    }

    /**
     * Sort of increment the module type of the specified {@code moduleFile}.<br/>
     * Updates the specified {@code moduleFile} type to the next value in the enum.
     */
    private ModuleType setNextModuleType(final ServerModuleFile moduleFile) {
        Assert.assertNotNull(moduleFile);
        final ModuleType origModuleType = moduleFile.getModuleType();
        Assert.assertNotNull(origModuleType);
        final ModuleType newModuleType = ModuleType.values()[(origModuleType.ordinal() + 1) % ModuleType.values().length];
        Assert.assertNotNull(newModuleType);
        assertThat(origModuleType, not(equalTo(newModuleType)));
        moduleFile.setModuleType(newModuleType);
        return newModuleType;
    }

    @Test
    public void testSaveServerModuleFile() throws Exception {
        Assert.assertNotNull(admin);

        // save test; missing data-bytes
        int modulesSize = admin.findAllServerModuleFiles().size();
        ServerModuleFile moduleFile = new ServerModuleFile();
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        try {
            admin.saveServerModuleFile(moduleFile);
            Assert.fail("data-bytes are mandatory when creating a new ServerModuleFileEntity entity");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change
        Assert.assertNull(moduleFile.getData());  // no change
        Assert.assertNull(moduleFile.getStates());  // no change
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getModuleSha256()));  // no change
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));  // no change
        assertThat(moduleFile.getModuleType(), equalTo(ModuleType.CUSTOM_ASSERTION));  // no change

        // save test; missing module type
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        moduleFile.createData("new data".getBytes(Charsets.UTF8), "sha");
        try {
            admin.saveServerModuleFile(moduleFile);
            Assert.fail("module type is mandatory when creating a new ServerModuleFileEntity entity");
        } catch (SaveException e) {
            /* module type is mandatory when creating a new ServerModuleFileEntity entity */
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));  // no change
        Assert.assertNull(moduleFile.getStates());  // no change
        assertThat(moduleFile.getModuleSha256(), equalTo("sha"));  // no change
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));  // no change
        Assert.assertNull(moduleFile.getModuleType());  // no change

        // save test; empty entity
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        byte[] bytes = "this is a new data".getBytes(Charsets.UTF8);
        moduleFile.createData(bytes, "sha1"); // set wrong sha256, it should be corrected inside saveServerModuleFile
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        // make sure everything else is default
        assertThat(moduleFile.getGoid(), equalTo(Goid.DEFAULT_GOID));
        Assert.assertNull(moduleFile.getStates());
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));
        assertThat(moduleFile.getModuleSha256(), equalTo("sha1"));
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), bytes));
        // save the entity
        Goid goid = admin.saveServerModuleFile(moduleFile);
        Assert.assertNotNull(goid);
        assertThat(goid, not(equalTo(Goid.DEFAULT_GOID)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize + 1));  // changed +1
        // make sure module has been successfully added
        moduleFile = admin.findServerModuleFileById(goid, true);
        Assert.assertNotNull(moduleFile);
        // size and sha should be set by saveServerModuleFile
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), bytes));
        assertThat(moduleFile.getModuleSha256(), not(equalTo("sha1")));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes)));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo(String.valueOf(bytes.length)));
        Assert.assertNotNull(moduleFile.getStates());
        assertThat(moduleFile.getStates().size(), equalTo(1));
        ServerModuleFileState state = moduleFile.getStates().get(0);
        Assert.assertNotNull(state);
        assertThat(state.getNodeId(), equalTo(admin.getSelfNode().getNodeIdentifier()));
        assertThat(state.getState(), equalTo(ModuleState.UPLOADED));
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        ServerModuleFileState thisNodeState = getStateForThisNode(moduleFile);
        assertThat(thisNodeState, equalTo(state));

        // save test; new entity copied from a existing one (module_0)
        //
        // Goid(GOID_HI_START, 0)
        // ----------------------------------------
        //       currentCluster  => ACCEPTED
        //       node_1          => UPLOADED
        //       node_3          => LOADED
        // ----------------------------------------
        //
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 0)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 0)), true, true, true); // copy everything
        moduleFile.setGoid(Goid.DEFAULT_GOID); // mark it as new
        // save the entity
        goid = admin.saveServerModuleFile(moduleFile);
        Assert.assertNotNull(goid);
        assertThat(goid, not(equalTo(Goid.DEFAULT_GOID))); // make sure its not the default
        assertThat(goid, not(equalTo(new Goid(GOID_HI_START, 0)))); // make sure its not the one we copy data from
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize + 1));  // changed +1
        // make sure module has been successfully added
        moduleFile = admin.findServerModuleFileById(goid, true);
        Assert.assertNotNull(moduleFile);
        bytes = moduleFile.getData().getDataBytes();
        Assert.assertTrue(Arrays.equals(bytes, moduleFiles.get(new Goid(GOID_HI_START, 0)).getData().getDataBytes()));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes)));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo(String.valueOf(bytes.length)));
        thisNodeState = getStateForThisNode(moduleFile);
        Assert.assertNotNull(thisNodeState);
        Assert.assertNotNull(getStateForThisNode(moduleFiles.get(new Goid(GOID_HI_START, 0))));
        assertThat(thisNodeState, not(equalTo(getStateForThisNode(moduleFiles.get(new Goid(GOID_HI_START, 0)))))); // module_0 state for currentCluster is ACCEPTED
        assertThat(getStateForThisNode(moduleFiles.get(new Goid(GOID_HI_START, 0))).getState(), equalTo(ModuleState.ACCEPTED));
        assertThat(thisNodeState.getNodeId(), equalTo(admin.getSelfNode().getNodeIdentifier()));
        assertThat(thisNodeState.getState(), equalTo(ModuleState.UPLOADED));
        Assert.assertTrue(StringUtils.isBlank(thisNodeState.getErrorMessage()));


        // test update; non existing module
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNull(admin.findServerModuleFileById(new Goid(10101, 10101), false)); // shouldn't exist
        moduleFile.setGoid(new Goid(10101, 10101));
        moduleFile.setName("new name");
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        try {
            admin.saveServerModuleFile(moduleFile);
            Assert.fail("Cannot update module with non-existing Goid(10101, 10101)");
        } catch (UpdateException e) {
            /* Cannot update module with non-existing Goid */
        }
        // make sure nothing was updated
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        Assert.assertNull(admin.findServerModuleFileById(new Goid(10101, 10101), false)); // shouldn't exist
        Assert.assertNull(moduleFile.getData());
        Assert.assertNull(moduleFile.getStates());
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getModuleSha256()));
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));

        // test update; missing module type
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        moduleFile.setGoid(new Goid(GOID_HI_START, 1));
        try {
            admin.saveServerModuleFile(moduleFile);
            Assert.fail("module type is mandatory when updating existing ServerModuleFileEntity entity");
        } catch (UpdateException e) {
            /* module type is mandatory when updating existing ServerModuleFileEntity entity */
        }
        // make sure nothing was updated
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        Assert.assertNull(moduleFile.getData());
        Assert.assertNull(moduleFile.getStates());
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getModuleSha256()));
        Assert.assertTrue(StringUtils.isBlank(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));

        // test update; existing (module_3), skip the data-bytes:
        //
        // Goid(GOID_HI_START, 3)
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 3)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 3)), false, true, true); // skip data-bytes
        assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(moduleFile.getName(), equalTo("module_3"));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo(String.valueOf(3)));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_3".getBytes(Charsets.UTF8))));
        // set updated values
        moduleFile.setName("new_module_3");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "100");
        moduleFile.setModuleSha256("new_sha_3");
        ModuleType newModuleType = setNextModuleType(moduleFile); // sort of increment the module type
        // no bytes so only the metadata and name will be changed
        goid = admin.saveServerModuleFile(moduleFile);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 3), true); // copy data-bytes
        Assert.assertNotNull(moduleFile);
        bytes = moduleFile.getData().getDataBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8)));  // bytes are not changed
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes))); // since no bytes are specified sha256 will be unchanged
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("3")); // since no bytes are specified size will be unchanged
        assertThat(moduleFile.getName(), equalTo("new_module_3")); // make sure name was updated accordingly
        assertThat(moduleFile.getModuleType(), equalTo(newModuleType)); // make sure type was updated accordingly
        Assert.assertNotNull(getStateForThisNode(moduleFile));
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.REJECTED)); // no bytes changed so state is not changed to UPLOADED.

        // test update; existing (module_3), modifying sha and size should be ignored if data-bytes are unchanged.
        //
        // Goid(GOID_HI_START, 3)
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 3)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 3)), true, true, true); // include data-bytes
        assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(moduleFile.getName(), equalTo("new_module_3")); // should be the new name updated from the update above
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("3"));
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), "test_data_3".getBytes(Charsets.UTF8)));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_3".getBytes(Charsets.UTF8))));
        // set updated values
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "100");
        moduleFile.setModuleSha256("new_sha_3");
        // no bytes changes (sha and size will be ignored)
        goid = admin.saveServerModuleFile(moduleFile);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 3), true); // copy data-bytes
        Assert.assertNotNull(moduleFile);
        bytes = moduleFile.getData().getDataBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8)));  // bytes are not changed
        assertThat(moduleFile.getModuleSha256(), not(equalTo("new_sha_3")));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes))); // since no bytes are changed sha256 will be unchanged
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("3")); // since no bytes are changed size will be unchanged
        Assert.assertNotNull(getStateForThisNode(moduleFile));
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.REJECTED)); // no bytes changed so state is not changed to UPLOADED.

        // test update; update existing (new_module_3),  modifying sha and size should be ignored if data-bytes are unchanged.
        //
        // Goid(GOID_HI_START, 3)
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 3)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 3)), true, true, true); // include the bytes
        assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(moduleFile.getName(), equalTo("new_module_3")); // should be the new name updated from the update above
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("3"));
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), "test_data_3".getBytes(Charsets.UTF8)));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_3".getBytes(Charsets.UTF8))));
        // set updated values
        moduleFile.setName("new_new_module_3");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "100");
        moduleFile.setModuleSha256("new_sha_3");
        newModuleType = setNextModuleType(moduleFile); // sort of increment the module type
        // no bytes changes (sha and size will be ignored), so only the name and type will be changed
        goid = admin.saveServerModuleFile(moduleFile);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 3), true);
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        bytes = moduleFile.getData().getDataBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8))); // make sure bytes were not updated
        assertThat(moduleFile.getModuleSha256(), not(equalTo("new_sha_3")));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes))); // since no bytes are changed sha256 will be unchanged
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("3")); // since no bytes are changed size will be unchanged
        assertThat(moduleFile.getName(), equalTo("new_new_module_3"));
        assertThat(moduleFile.getModuleType(), equalTo(newModuleType));
        Assert.assertNotNull(getStateForThisNode(moduleFile));
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.REJECTED)); // no bytes changed so state is not changed to UPLOADED.

        // test update; existing (module_3), modifying data-bytes.
        //
        // Goid(GOID_HI_START, 3)
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 3)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 3)), true, true, true); // include data-bytes
        assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(moduleFile.getName(), equalTo("new_new_module_3")); // should be the new name updated from the update above
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("3"));
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), "test_data_3".getBytes(Charsets.UTF8)));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_3".getBytes(Charsets.UTF8))));
        // set updated values
        bytes = "new_test_data_3".getBytes(Charsets.UTF8);
        moduleFile.createData(bytes);
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "100");
        moduleFile.setModuleSha256("new_sha_3");
        // bytes are changed, so sha and size will be corrected
        goid = admin.saveServerModuleFile(moduleFile);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 3)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 3), true); // copy data-bytes
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), bytes));  // bytes are changed
        assertThat(moduleFile.getModuleSha256(), not(equalTo("new_sha_3")));
        assertThat(moduleFile.getModuleSha256(), not(equalTo(ServerModuleFile.calcBytesChecksum("test_data_3".getBytes(Charsets.UTF8)))));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes))); // sha is changed
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), not(equalTo("3")));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo(String.valueOf(bytes.length)));
        Assert.assertNotNull(getStateForThisNode(moduleFile));
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.UPLOADED)); // bytes are changed so state set to UPLOADED.

        // test update; update existing (module_5), modify properties only, skip data-bytes
        //
        // Goid(GOID_HI_START, 5)
        // ----------------------------------------
        // (empty)
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 5)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 5)), true, true, true);
        assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 5)));
        assertThat(moduleFile.getName(), equalTo("module_5"));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("5"));
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), "test_data_5".getBytes(Charsets.UTF8)));
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_5".getBytes(Charsets.UTF8))));
        Assert.assertNull(getStateForThisNode(moduleFile)); // no state for this node
        Assert.assertNotNull(moduleFile.getStates());
        Assert.assertTrue(moduleFile.getStates().isEmpty());
        // set updated values
        moduleFile.setName("new_module_5");
        newModuleType = setNextModuleType(moduleFile); // sort of increment the module type
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "100");
        moduleFile.setModuleSha256("new_sha_5");
        // update module
        goid = admin.saveServerModuleFile(moduleFile);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 5)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 5), true);
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), "test_data_5".getBytes(Charsets.UTF8))); // no change
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum("test_data_5".getBytes(Charsets.UTF8)))); // no change
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("5")); // no change
        assertThat(moduleFile.getName(), equalTo("new_module_5")); // changed
        assertThat(moduleFile.getModuleType(), equalTo(newModuleType)); // changed
        Assert.assertNull(getStateForThisNode(moduleFile)); // no change
        Assert.assertNotNull(moduleFile.getStates()); // no change
        Assert.assertTrue(moduleFile.getStates().isEmpty()); // no change

        // test update; update existing (new_module_5), modify data-bytes
        //
        // Goid(GOID_HI_START, 5)
        // ----------------------------------------
        // currentCluster  => UPLOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        moduleFile = new ServerModuleFile();
        Assert.assertNotNull(moduleFiles.get(new Goid(GOID_HI_START, 5)));
        moduleFile.copyFrom(moduleFiles.get(new Goid(GOID_HI_START, 5)), false, true, true); // do not copy bytes, they'll be set later
        assertThat(moduleFile.getGoid(), equalTo(new Goid(GOID_HI_START, 5)));
        assertThat(moduleFile.getName(), equalTo("new_module_5"));
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertNull(moduleFile.getData().getDataBytes()); // no data-bytes copied
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo("5")); // no change
        Assert.assertNull(getStateForThisNode(moduleFile)); // no change
        Assert.assertNotNull(moduleFile.getStates()); // no change
        Assert.assertTrue(moduleFile.getStates().isEmpty()); // no change
        // set updated values
        bytes = "new_test_data_5".getBytes(Charsets.UTF8);
        moduleFile.createData(bytes);
        moduleFile.setModuleSha256("new_sha_5");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "101");
        // update module
        goid = admin.saveServerModuleFile(moduleFile);
        assertThat(goid, equalTo(new Goid(GOID_HI_START, 5)));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change
        // get the updated module
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 5), true);
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(moduleFile.getData().getDataBytes(), bytes)); // changed
        assertThat(moduleFile.getModuleSha256(), equalTo(ServerModuleFile.calcBytesChecksum(bytes))); // changed
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), equalTo(String.valueOf(bytes.length))); // changed
        assertThat(moduleFile.getName(), equalTo("new_module_5")); // no change
        Assert.assertNotNull(moduleFile.getStates());
        assertThat(moduleFile.getStates().size(), equalTo(1)); // changed
        Assert.assertNotNull(getStateForThisNode(moduleFile)); // changed
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.UPLOADED)); // changed
    }

    @Test
    public void testDeleteServerModuleFile() throws Exception {
        Assert.assertNotNull(admin);

        Assert.assertNotNull(admin.findAllServerModuleFiles());
        final int size = admin.findAllServerModuleFiles().size();

        // existing module
        ServerModuleFile moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), false);
        Assert.assertNotNull(moduleFile);
        admin.deleteServerModuleFile(new Goid(GOID_HI_START, 0));
        moduleFile = admin.findServerModuleFileById(new Goid(GOID_HI_START, 0), false);
        Assert.assertNull(moduleFile);
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(size - 1));

        // non-existing
        admin.deleteServerModuleFile(new Goid(GOID_HI_START, 10111));
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(size - 1));
    }

    @Test
    public void testGetServerModuleConfig() throws Exception {
        final String CUSTOM_ASSERTIONS_PROPERTIES_FILE = "custom_assertions.properties";
        final String MODULAR_ASSERTIONS_FILE_EXTENSIONS = ".assertion .aar";
        final String MODULAR_ASSERTIONS_MANIFEST_ASSERTION_LIST_KEY = "ModularAssertion-List";

        Assert.assertNotNull(serverConfig);
        serverConfig.putProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE, CUSTOM_ASSERTIONS_PROPERTIES_FILE);
        serverConfig.putProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, MODULAR_ASSERTIONS_FILE_EXTENSIONS);

        Assert.assertNotNull(admin);
        final ServerModuleConfig config = admin.getServerModuleConfig();
        Assert.assertNotNull(config);
        Assert.assertEquals(Arrays.asList(".jar"), config.getCustomAssertionModulesExt());
        Assert.assertEquals(CUSTOM_ASSERTIONS_PROPERTIES_FILE, config.getCustomAssertionPropertyFileName());
        Assert.assertEquals(Arrays.asList(".assertion", ".aar"), config.getModularAssertionModulesExt());
        Assert.assertEquals(MODULAR_ASSERTIONS_MANIFEST_ASSERTION_LIST_KEY, config.getModularAssertionManifestAssertionListKey());
    }
}
