package com.l7tech.server.cluster;

import com.google.common.io.CountingInputStream;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.module.*;
import com.l7tech.gateway.common.security.signer.SignerUtilsTest;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.*;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.licensing.UpdatableCompositeLicenseManager;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.module.ServerModuleFileManagerStub;
import com.l7tech.server.module.ServerModuleFileTestBase;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * For the timer being tests only the {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile} portion
 * of the {@link ClusterStatusAdmin}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterStatusAdminImpTest extends ServerModuleFileTestBase {

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
    private ServerModuleFileManager serverModuleFileManager;
    private ClusterInfoManager clusterInfoManager;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        beforeClass();
    }

    @AfterClass
    public static void cleanUpOnce() throws Exception {
        afterClass();
    }

    @Before
    public void setUp() throws Exception {
        Assert.assertThat("modules signer is created", TRUSTED_SIGNER_CERTS, Matchers.notNullValue());
        clusterInfoManager = new ClusterInfoManagerStub() {
            @Override
            public String thisNodeId() {
                return getSelfNodeInf().getNodeIdentifier();
            }
        };
        serverModuleFileManager = new ServerModuleFileManagerStub(createSampleModules(clusterInfoManager.getSelfNodeInf().getNodeIdentifier()));
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
        // set module signer
        Assert.assertThat(this.admin, Matchers.instanceOf(ClusterStatusAdminImp.class));
        ((ClusterStatusAdminImp) this.admin).setTrustedSignerCertsManager(TRUSTED_SIGNER_CERTS);
    }

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
    public ServerModuleFile[] createSampleModules(final String clusterNodeId) throws Exception {
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
            entity.createData(bytes, null);
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
            Assert.assertThat(orgModuleFile.getData().getSignatureProperties(), Matchers.equalTo(moduleFile.getData().getSignatureProperties()));
        } else {
            Assert.assertNull(moduleFile.getData().getDataBytes()); // this is a copy so no data bytes
            Assert.assertNull(moduleFile.getData().getSignatureProperties()); // this is a copy so no data bytes
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
        // ignore data and signature as data is not copied from ServerModuleFile CollectionUpdateProducer
        assertThat(left.getData().getGoid(), equalTo(right.getData().getGoid()));
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
        byte[] bytes = "test_data_1".getBytes(Charsets.UTF8);
        moduleFile.createData(bytes, null);
        // save
        Goid goid = saveServerModuleFile(moduleFile);
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
            final byte[] bytes = String.valueOf("series_test_data_" + i).getBytes(Charsets.UTF8);
            addedModuleFiles[i].createData(bytes, null);
            // save
            newGoids[i] = saveServerModuleFile(addedModuleFiles[i]);
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
        updateServerModuleFile(moduleFile);
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
        // update
        admin.updateServerModuleFileName(new Goid(GOID_HI_START, 0), "new_new_new_name_123");
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
        Assert.assertThat(addedModule.getData().getSignatureProperties(), Matchers.equalTo(removedModule.getData().getSignatureProperties()));
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
        moduleFile.createData(bytes, null);
        assertThat(moduleFile.getGoid(), not(equalTo(Goid.DEFAULT_GOID)));
        // update
        updateServerModuleFile(moduleFile);
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
        assertThat(addedModule.getModuleSha256(), equalTo(ModuleDigest.hexEncodedDigest(bytes)));
        assertThat(removedModule.getModuleSha256(), equalTo(ModuleDigest.hexEncodedDigest("test_data_0".getBytes(Charsets.UTF8))));
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
            final byte[] bytes = String.valueOf("new_data_for_module_" + updateGoid.getLow()).getBytes(Charsets.UTF8);
            moduleFile.createData(bytes, null);
            setNextModuleType(moduleFile);
            moduleFile.setName("new_name_for_module_" + updateGoid.getLow());
            Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
            // update
            updateServerModuleFile(moduleFile);
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
            final byte[] bytes = String.valueOf("add_add_test_module_test_data_" + i).getBytes(Charsets.UTF8);
            addedModuleFiles.get(i).createData(bytes, null);
            // save
            Goid goid = saveServerModuleFile(addedModuleFiles.get(i));
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
                final byte[] bytes = String.valueOf("new_data_for_module_" + updateGoid.getLow()).getBytes(Charsets.UTF8);
                updatedModuleFile.createData(bytes, null);
                setNextModuleType(updatedModuleFile);
                updatedModuleFile.setName("new_name_for_module_" + updateGoid.getLow());
                Assert.assertNotNull(admin.findServerModuleFileById(updateGoid, false));
                // update
                updateServerModuleFile(updatedModuleFile);
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

    /**
     * Utility method for getting resource files bytes and digest
     */
    private static Triple<byte[], String, Long> loadResource(final String resURL) {
        try {
            Assert.assertThat(resURL, Matchers.not(Matchers.isEmptyOrNullString()));
            final URL fileUrl = ClusterStatusAdminImpTest.class.getClassLoader().getResource(resURL);
            Assert.assertNotNull(fileUrl);
            final File file = new File(fileUrl.toURI());
            Assert.assertNotNull(file);
            Assert.assertTrue(file.exists());
            Assert.assertTrue(file.isFile());
            Assert.assertFalse(file.isDirectory());

            try (
                    final CountingInputStream cis = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
                    final DigestInputStream dis = new DigestInputStream(cis, MessageDigest.getInstance("SHA-256"))
            ) {
                final byte[] bytes = IOUtils.slurpStream(dis);
                Assert.assertNotNull(bytes);
                final byte[] digest = dis.getMessageDigest().digest();
                Assert.assertNotNull(digest);
                final long bytesCount = cis.getCount();
                Assert.assertThat(bytesCount, Matchers.greaterThan(0L));
                return Triple.triple(bytes, HexUtils.hexDump(digest), bytesCount);
            }
        } catch (final Exception e) {
            Assert.fail("Failed to load resource: " + resURL + ":" + System.lineSeparator() + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method for signing the specified {@code content} with the specified signer.
     *
     * @return a pair of signed bytes and signature properties
     */
    private static Pair<byte[], String> sign(final TrustedSignerCertsManager signer, final byte[] content, final String signerCertDn) throws Exception {
        Assert.assertNotNull(content);
        Assert.assertThat(signerCertDn, Matchers.not(Matchers.isEmptyOrNullString()));
        final byte[] signedBytes = SignatureTestUtils.sign(signer, new ByteArrayInputStream(content), signerCertDn);
        Assert.assertNotNull(signedBytes);
        final String signatureProps = SignatureTestUtils.getSignatureString(signedBytes);
        Assert.assertThat(signatureProps, Matchers.not(Matchers.isEmptyOrNullString()));
        return Pair.pair(signedBytes, signatureProps);
    }

    private void validateSuccessfulSaveServerModuleFile(
            final Goid modAssGoid,
            final int modulesSizeBeforeSave,
            final Triple<byte[], String, Long> unsignedModule,
            final String sigProps,
            final ModuleType moduleType,
            final String moduleName,
            final String moduleFileName,
            final String moduleAssertions
    ) throws Exception {
        // make sure the save is successful
        Assert.assertThat(modAssGoid, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID))));
        Assert.assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSizeBeforeSave + 1));
        // make sure the module has the right properties set
        final ServerModuleFile moduleFile = admin.findServerModuleFileById(modAssGoid, true);
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertTrue(Arrays.equals(unsignedModule.left, moduleFile.getData().getDataBytes()));
        Assert.assertEquals(sigProps, moduleFile.getData().getSignatureProperties());
        Assert.assertEquals(unsignedModule.middle, moduleFile.getModuleSha256());
        Assert.assertEquals(moduleType, moduleFile.getModuleType());
        Assert.assertEquals(moduleName, moduleFile.getName());
        Assert.assertEquals(modAssGoid, moduleFile.getGoid());
        Assert.assertEquals(String.valueOf(unsignedModule.right), moduleFile.getProperty(ServerModuleFile.PROP_SIZE));
        Assert.assertEquals(moduleFileName, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME));
        Assert.assertEquals(moduleAssertions, moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS));
        ServerModuleFileState moduleState = getStateForThisNode(moduleFile);
        Assert.assertNotNull(moduleState);
        Assert.assertEquals(ModuleState.UPLOADED, moduleState.getState());
    }

    @Test
    public void testSaveServerModuleFile() throws Exception {
        Assert.assertNotNull(admin);

        // our test modular assertions
        final Triple<byte[], String, Long> unsignedModule1 = loadResource("com/l7tech/server/policy/module/modular/com.l7tech.WorkingTest1.aar");
        final Pair<byte[], String> signedModule1 = sign(TRUSTED_SIGNER_CERTS, unsignedModule1.left, SIGNER_CERT_DNS[0]);
        final Triple<byte[], String, Long> unsignedModule2 = loadResource("com/l7tech/server/policy/module/modular/com.l7tech.WorkingTest2.aar");
        Assert.assertThat(unsignedModule2.left, Matchers.not(Matchers.equalTo(unsignedModule1.left)));
        final Pair<byte[], String> signedModule2 = sign(TRUSTED_SIGNER_CERTS, unsignedModule2.left, SIGNER_CERT_DNS[0]);
        final Triple<byte[], String, Long> unsignedModule3 = loadResource("com/l7tech/server/policy/module/modular/com.l7tech.WorkingTest3.aar");
        Assert.assertThat(unsignedModule3.left, Matchers.allOf(Matchers.not(Matchers.equalTo(unsignedModule1.left)), Matchers.not(Matchers.equalTo(unsignedModule2.left))));
        final Pair<byte[], String> signedModule3 = sign(TRUSTED_SIGNER_CERTS, unsignedModule3.left, SIGNER_CERT_DNS[0]);

        // upload com.l7tech.WorkingTest1.aar
        int modulesSize = admin.findAllServerModuleFiles().size();
        final Goid modAssGoid1 = admin.saveServerModuleFile(signedModule1.left, "new name", "com.l7tech.WorkingTest1.aar");
        validateSuccessfulSaveServerModuleFile(
                modAssGoid1,
                modulesSize,
                unsignedModule1,
                signedModule1.right,
                ModuleType.MODULAR_ASSERTION,
                "new name",
                "com.l7tech.WorkingTest1.aar",
                "ModularTest1Assertion"
        );

        // upload com.l7tech.WorkingTest2.aar with new name but same filename as com.l7tech.WorkingTest1.aar
        modulesSize = admin.findAllServerModuleFiles().size();
        final Goid modAssGoid2 = admin.saveServerModuleFile(signedModule2.left, "new name2", "com.l7tech.WorkingTest1.aar");
        validateSuccessfulSaveServerModuleFile(
                modAssGoid2,
                modulesSize,
                unsignedModule2,
                signedModule2.right,
                ModuleType.MODULAR_ASSERTION,
                "new name2",
                "com.l7tech.WorkingTest1.aar",
                "ModularTest2Assertion"
        );

        // save test; signed.dat missing
        modulesSize = admin.findAllServerModuleFiles().size();
        final byte[] unzippedDataBytes = "unzipped new data".getBytes(Charsets.UTF8);
        try {
            admin.saveServerModuleFile(unzippedDataBytes, "SMF Entity Name", "SMF File Name");
            Assert.fail(SignatureTestUtils.SIGNED_DATA_ZIP_ENTRY + " is missing");
        } catch (SaveException e) {
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(IOException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString(SignatureTestUtils.SIGNED_DATA_ZIP_ENTRY + "' entry is missing"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; signature.properties missing
        modulesSize = admin.findAllServerModuleFiles().size();
        final byte[] zippedDataOnlyWithSignedDataEntry = SignatureTestUtils.createSampleZipContent(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        zos.putNextEntry(new ZipEntry(SignatureTestUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream("some test bytes".getBytes(Charsets.UTF8)), zos);
                    }
                }
        );
        try {
            admin.saveServerModuleFile(zippedDataOnlyWithSignedDataEntry, "SMF Entity Name", "SMF File Name");
            Assert.fail(SignatureTestUtils.SIGNATURE_PROPS_ZIP_ENTRY + " is missing");
        } catch (SaveException e) {
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(IOException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString(SignatureTestUtils.SIGNATURE_PROPS_ZIP_ENTRY + "' entry is missing"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; zipped data with the first entry but signed.dat does not exist
        //final byte[] zippedDataWithoutDataEntry = SignatureTestUtils.createSampleDummyZipFileWithTwoEntries(false, true);
        final byte[] zippedDataWithoutDataEntry =  SignatureTestUtils.createSampleZipContent(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry("RandomName.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("dummy bytes".getBytes(Charsets.UTF8)), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtilsTest.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream("dummy signature props bytes".getBytes(Charsets.UTF8)), zos);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(zippedDataWithoutDataEntry, "SMF Entity Name", "SMF File Name");
            Assert.fail("the first entry of the zip file must be data entry.");
        } catch (SaveException e) {
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(IOException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("First zip entry is not a plain file named '" + SignatureTestUtils.SIGNED_DATA_ZIP_ENTRY + "'"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; zipped data with the second entry but signature.properties does not exist
        //final byte[] zippedDataWithoutSignaturePropsEntry = SignatureTestUtils.createSampleDummyZipFileWithTwoEntries(true, false);
        final byte[] zippedDataWithoutSignaturePropsEntry = SignatureTestUtils.createSampleZipContent(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignatureTestUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream("dummy bytes".getBytes(Charsets.UTF8)), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry("RandomName.properties"));
                        IOUtils.copyStream(new ByteArrayInputStream("dummy signature props bytes".getBytes(Charsets.UTF8)), zos);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(zippedDataWithoutSignaturePropsEntry, "SMF Entity Name", "SMF File Name");
            Assert.fail("the next entry of the zip file must be signature properties entry.");
        } catch (SaveException e) {
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(IOException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("Second zip entry is not a plain file named '" + SignatureTestUtils.SIGNATURE_PROPS_ZIP_ENTRY + "'"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // signature tests
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // save test; unsigned data
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(unsignedModule3.left, "new name3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save unsigned SMF");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(IOException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("Error while verifying module signature"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; data tampering after signing
        byte[] tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(unsignedModule3.left), // com.l7tech.WorkingTest3.aar
                TRUSTED_SIGNER_CERTS,
                SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);
                        return Pair.pair(SignatureTestUtils.flipRandomByte(dataBytes), sigProps);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(tamperedSignedBytes, "new name 3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save tampered SMF (data)");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignatureException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("module file is rejected"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; signature tampering after signing
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(unsignedModule3.left), // com.l7tech.WorkingTest3.aar
                TRUSTED_SIGNER_CERTS,
                SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature property
                        final String signatureB64 = (String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(signatureB64);
                        // flip random byte
                        final byte[] modSignBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signatureB64));
                        // store modified signature
                        sigProps.setProperty(SignatureTestUtils.SIGNATURE_PROP, HexUtils.encodeBase64(modSignBytes));
                        Assert.assertThat(signatureB64, Matchers.not(Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(tamperedSignedBytes, "new name 3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save tampered SMF (data)");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignatureException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("module file is rejected"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; signer cert  tampering after signing
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(unsignedModule3.left), // com.l7tech.WorkingTest3.aar
                TRUSTED_SIGNER_CERTS,
                SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signer cert property
                        final String signerCertB64 = (String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(signerCertB64);
                        // flip random byte
                        final byte[] modSignerCertBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signerCertB64));
                        // store modified signature
                        sigProps.setProperty(SignatureTestUtils.SIGNING_CERT_PROPS, HexUtils.encodeBase64(modSignerCertBytes));
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(tamperedSignedBytes, "new name 3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save tampered SMF (data)");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignatureException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("module file is rejected"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // create untrusted signer with same DNs plus a new one
        final String[] untrustedDNs = ArrayUtils.concat(
                SIGNER_CERT_DNS,
                new String[] {
                        "cn=signer.untrusted.apim.ca.com"
                }
        );
        final TrustedSignerCertsManager untrustedSigner = SignatureTestUtils.createSignerManager(untrustedDNs);

        // save test; sign using untrusted signer
        final Pair<byte[], String> untrustedSignedModule1 = sign(untrustedSigner, unsignedModule3.left, untrustedDNs[0]);
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(untrustedSignedModule1.left, "new name 3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save SMF signed with untrusted signer");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignatureException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("module file is rejected"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; sign using untrusted signer and swap the signature from trusted one
        Assert.assertEquals(untrustedDNs[0], SIGNER_CERT_DNS[0]);
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(unsignedModule3.left), // com.l7tech.WorkingTest3.aar
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature and signer cert property
                        final String signatureB64 = (String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedModule1.left);
                        final String trustedSigB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, trustedSigProps);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(tamperedSignedBytes, "new name 3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save tampered SMF (data)");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignatureException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("module file is rejected"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        // save test; sign using untrusted signer and swap the signer cert from trusted one
        // same as above but instead of swapping the entire signature properties bytes, swap only the signer cert and leave signature unchanged
        Assert.assertEquals(untrustedDNs[0], SIGNER_CERT_DNS[0]);
        tamperedSignedBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(unsignedModule3.left), // com.l7tech.WorkingTest3.aar
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature and signer cert property
                        final String signatureB64 = (String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedModule1.left);
                        final String trustedSigB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // swap signing cert property
                        sigProps.setProperty(SignatureTestUtils.SIGNING_CERT_PROPS, HexUtils.encodeBase64(trustedSignerCertBytes));
                        // make sure after the swap the signer cert is different
                        assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS))));
                        // make sure after the swap the signature is unchanged
                        assertThat(signatureB64, Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP)));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        modulesSize = admin.findAllServerModuleFiles().size();
        try {
            admin.saveServerModuleFile(tamperedSignedBytes, "new name 3", "com.l7tech.WorkingTest3.aar");
            Assert.fail("cannot save tampered SMF (data)");
        } catch (SaveException e) {
            /* data-bytes are mandatory when creating a new ServerModuleFileEntity entity */
            Assert.assertThat(e.getCause(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignatureException.class)));
            Assert.assertThat(e.getMessage(), Matchers.containsString("module file is rejected"));
        }
        // make sure nothing was changed
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize)); // no change

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // finally upload last module; com.l7tech.WorkingTest3.aar with new name but same filename as com.l7tech.WorkingTest1.aar
        modulesSize = admin.findAllServerModuleFiles().size();
        final Goid modAssGoid3 = admin.saveServerModuleFile(signedModule3.left, "new name3", "com.l7tech.WorkingTest3.aar");
        validateSuccessfulSaveServerModuleFile(
                modAssGoid3,
                modulesSize,
                unsignedModule3,
                signedModule3.right,
                ModuleType.MODULAR_ASSERTION,
                "new name3",
                "com.l7tech.WorkingTest3.aar",
                "ModularTest3Assertion"
        );
    }

    @Test
    public void testUpdateServerModuleFile() throws Exception {
        // test update; non existing module
        int modulesSize = admin.findAllServerModuleFiles().size();
        Assert.assertNull(admin.findServerModuleFileById(new Goid(10101, 10101), false)); // shouldn't exist
        try {
            admin.updateServerModuleFileName(new Goid(10101, 10101), "new name");
            Assert.fail("Cannot update module with non-existing Goid(10101, 10101)");
        } catch (UpdateException e) {
            /* Cannot update module with non-existing Goid */
        }
        // make sure nothing was updated
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        Assert.assertNull(admin.findServerModuleFileById(new Goid(10101, 10101), false)); // shouldn't exist

        // test update; missing module type test is redundant as ClusterStatusAdmin#updateServerModuleFileName do not update the module type

        // test update; existing (module_3), update name
        //
        // Goid(GOID_HI_START, 3)
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        Goid goid = new Goid(GOID_HI_START, 3);
        ServerModuleFile moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        String moduleName = moduleFile.getName();
        assertThat(moduleName, equalTo("module_3"));
        ModuleType moduleType = moduleFile.getModuleType();
        String moduleDigest = moduleFile.getModuleSha256();
        String moduleProperties = moduleFile.getXmlProperties();
        Collection<ServerModuleFileState> moduleStates = moduleFile.getStates();
        int moduleVersion = moduleFile.getVersion();
        String signature = moduleFile.getData().getSignatureProperties(); // get signature before save
        byte[] bytes = moduleFile.getData().getDataBytes(); // get module bytes before save
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8)));
        // update name
        admin.updateServerModuleFileName(goid, "new_module_3");
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(goid, true); // copy data-bytes
        Assert.assertNotNull(moduleFile);
        bytes = moduleFile.getData().getDataBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8)));  // bytes are not changed
        Assert.assertThat(moduleFile.getData().getSignatureProperties(), Matchers.equalTo(signature)); // signature are not changed
        assertThat(moduleFile.getModuleSha256(), equalTo(moduleDigest)); // moduleDigest are not changed
        assertThat(moduleFile.getXmlProperties(), equalTo(moduleProperties)); // moduleProperties are not changed
        assertThat(moduleFile.getModuleType(), equalTo(moduleType)); // moduleType is not changed
        Assert.assertNotNull(getStateForThisNode(moduleFile));
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.REJECTED)); // no state change.
        assertThat(moduleFile.getStates(), Matchers.equalTo(moduleStates)); // moduleStates are not changed
        assertThat(moduleFile.getVersion(), equalTo(moduleVersion + 1)); // moduleVersion is incremented by one
        assertThat(moduleFile.getName(), equalTo("new_module_3")); // make sure name was updated accordingly

        // test update; existing (module_3), no updates
        //
        // Goid(GOID_HI_START, 3)
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_2          => LOADED
        //      node_3          => LOADED
        // ----------------------------------------
        modulesSize = admin.findAllServerModuleFiles().size();
        goid = new Goid(GOID_HI_START, 3);
        moduleFile = moduleFiles.get(goid);
        Assert.assertNotNull(moduleFile);
        moduleName = moduleFile.getName();
        assertThat(moduleName, equalTo("new_module_3"));
        moduleType = moduleFile.getModuleType();
        moduleDigest = moduleFile.getModuleSha256();
        moduleProperties = moduleFile.getXmlProperties();
        moduleStates = moduleFile.getStates();
        moduleVersion = moduleFile.getVersion();
        signature = moduleFile.getData().getSignatureProperties(); // get signature before save
        bytes = moduleFile.getData().getDataBytes(); // get module bytes before save
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8)));
        // update name
        admin.updateServerModuleFileName(goid, moduleName);
        assertThat(admin.findAllServerModuleFiles().size(), equalTo(modulesSize));
        // get the updated module
        moduleFile = admin.findServerModuleFileById(goid, true); // copy data-bytes
        Assert.assertNotNull(moduleFile);
        bytes = moduleFile.getData().getDataBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue(Arrays.equals(bytes, "test_data_3".getBytes(Charsets.UTF8)));  // bytes are not changed
        Assert.assertThat(moduleFile.getData().getSignatureProperties(), Matchers.equalTo(signature)); // signature are not changed
        assertThat(moduleFile.getModuleSha256(), equalTo(moduleDigest)); // moduleDigest are not changed
        assertThat(moduleFile.getXmlProperties(), equalTo(moduleProperties)); // moduleProperties are not changed
        assertThat(moduleFile.getModuleType(), equalTo(moduleType)); // moduleType is not changed
        Assert.assertNotNull(getStateForThisNode(moduleFile));
        assertThat(getStateForThisNode(moduleFile).getState(), equalTo(ModuleState.REJECTED)); // no state change.
        assertThat(moduleFile.getStates(), Matchers.equalTo(moduleStates)); // moduleStates are not changed
        assertThat(moduleFile.getName(), equalTo(moduleName)); // moduleName is not changed
        assertThat(moduleFile.getVersion(), equalTo(moduleVersion + 1)); // moduleVersion is incremented by one
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

    /**
     * Save the specified {@code moduleFile} using the SMF manager instead of the {@code ClusterStatusAdmin}.
     * Used by the update consumer tests as the {@link ClusterStatusAdmin#saveServerModuleFile(byte[], String, String)}
     * requires signed bytes and update consumer are designed to use fake module bytes.
     *
     * @param moduleFile    module to save.  Required and cannot be {@code null}.
     */
    private Goid saveServerModuleFile(final ServerModuleFile moduleFile) throws SaveException {
        Assert.assertNotNull(moduleFile);
        Assert.assertNotNull(moduleFile.getData());
        Assert.assertNotNull(moduleFile.getData().getDataBytes());
        Assert.assertNotNull(moduleFile.getName());

        moduleFile.setModuleSha256(ModuleDigest.hexEncodedDigest(moduleFile.getData().getDataBytes()));
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(moduleFile.getData().getDataBytes().length));
        moduleFile.setStateForNode(clusterInfoManager.thisNodeId(), ModuleState.UPLOADED);
        return serverModuleFileManager.save(moduleFile);
    }

    private void updateServerModuleFile(final ServerModuleFile serverModuleFile) throws FindException, UpdateException {
        Assert.assertNotNull(serverModuleFile);
        Assert.assertThat(serverModuleFile.getGoid(), Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID))));

        final Goid goid = serverModuleFile.getGoid();
        final byte[] dataBytes = serverModuleFile.getData() != null ? serverModuleFile.getData().getDataBytes() : null;
        final byte[] digest = dataBytes != null ? ModuleDigest.digest(dataBytes) : null;

        final ServerModuleFile oldMod = serverModuleFileManager.findByPrimaryKey(goid);
        Assert.assertNotNull(oldMod);

        if (dataBytes == null) {
            oldMod.copyFrom(serverModuleFile, false, false, false);
        } else {
            final String byteSha256 = HexUtils.hexDump(digest);
            serverModuleFile.setModuleSha256(byteSha256);
            serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(dataBytes.length));

            final boolean dataReallyChanged = !byteSha256.equals(oldMod.getModuleSha256());
            oldMod.copyFrom(serverModuleFile, dataReallyChanged, dataReallyChanged, false);
            if (dataReallyChanged) {
                oldMod.setStateForNode(clusterInfoManager.thisNodeId(), ModuleState.UPLOADED);
            }
        }

        serverModuleFileManager.update(oldMod);
    }
}
