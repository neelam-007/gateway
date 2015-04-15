package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.util.Charsets;
import org.junit.Assert;
import org.apache.commons.lang.StringUtils;
import org.hibernate.LazyInitializationException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

/**
 * Test {@link ServerModuleFileManager}
 */
public class ServerModuleFileManagerTest extends EntityManagerTest {

    private ServerModuleFileManager serverModuleFileManager;
    private String clusterNodeId;

    @Before
    public void setUp() throws Exception {
        serverModuleFileManager = applicationContext.getBean("serverModuleFileManager", ServerModuleFileManager.class);
        clusterNodeId = getClusterNodeId(serverModuleFileManager);
    }

    /**
     * Extract this cluster node-id from the {@code serverModuleFileManager}.<br/>
     * Reflection is used to get {@link ServerModuleFileManagerImpl#clusterNodeId} field value.<br/>
     * Note, if the field is renamed, make sure the code below is changed accordingly.
     */
    private static String getClusterNodeId(final ServerModuleFileManager serverModuleFileManager) throws Exception {
        Assert.assertNotNull(serverModuleFileManager);
        Assert.assertTrue(AopUtils.isAopProxy(serverModuleFileManager));
        Assert.assertTrue(serverModuleFileManager instanceof Advised);
        final Advised advised = (Advised)serverModuleFileManager;
        Assert.assertNotNull(advised);
        final Object targetObj = advised.getTargetSource().getTarget();
        Assert.assertTrue(targetObj instanceof ServerModuleFileManagerImpl);
        final ServerModuleFileManagerImpl moduleFileManagerImpl = (ServerModuleFileManagerImpl)targetObj;
        Assert.assertNotNull(moduleFileManagerImpl);

        final Field clusterNodeIdField = ServerModuleFileManagerImpl.class.getDeclaredField("clusterNodeId");
        clusterNodeIdField.setAccessible(true);
        final Object fieldValue = clusterNodeIdField.get(moduleFileManagerImpl);
        Assert.assertTrue(fieldValue instanceof String);
        return (String)fieldValue;
    }

    // used to generate random GOID
    private static final Random rnd = new Random();
    private static final int typeLength = ModuleType.values().length;
    private static final long GOID_HI_START = Long.MAX_VALUE - 1;

    private void flushSession() {
        session.flush();
        session.clear();
    }

    /**
     * Creates sample modules with sample states for different cluster nodes:
     *
     * ----------------------------------------
     * module_1: Goid(GOID_HI_START, 1)  sha_1
     * ----------------------------------------
     *      currentCluster  => ACCEPTED
     *      node_1          => UPLOADED
     *      node_3          => LOADED
     * ----------------------------------------
     *
     * ----------------------------------------
     * module_2: Goid(GOID_HI_START, 2)  sha_2
     * ----------------------------------------
     *      currentCluster  => UPLOADED
     *      node_1          => ACCEPTED
     *      node_2          => ACCEPTED
     *      node_3          => UPLOADED
     * ----------------------------------------
     *
     * ----------------------------------------
     * module_3: Goid(GOID_HI_START, 3)  sha_3
     * ----------------------------------------
     *      currentCluster  => UPLOADED
     *      node_2          => UPLOADED
     * ----------------------------------------
     *
     * ----------------------------------------
     * module_4: Goid(GOID_HI_START, 4)  sha_4
     * ----------------------------------------
     *      currentCluster  => REJECTED
     *      node_2          => LOADED
     *      node_3          => LOADED
     * ----------------------------------------

     * ----------------------------------------
     * module_5: Goid(GOID_HI_START, 5)  sha_5
     * ----------------------------------------
     *      node_3          => LOADED
     * ----------------------------------------

     * ----------------------------------------
     * module_6: Goid(GOID_HI_START, 6)  sha_6
     * ----------------------------------------
     *      (empty)
     * ----------------------------------------
     */
    private void insertSampleModules() throws Exception {
        for (int i = 1; i <= 6; ++i) {
            final ServerModuleFile newEntity = new ServerModuleFile();
            final ModuleType type = ModuleType.values()[rnd.nextInt(typeLength)];
            newEntity.setName("module_" + i);
            newEntity.setModuleType(type);
            newEntity.createData(String.valueOf("test_data_" + i).getBytes(Charsets.UTF8), "sha_" + i);
            if (i <= 3) newEntity.setStateForNode(clusterNodeId, ModuleState.UPLOADED);
            final Goid goid = new Goid(GOID_HI_START, i);
            serverModuleFileManager.save(goid, newEntity);
            Assert.assertEquals(goid, newEntity.getGoid());
            flushSession();
        }
        flushSession();

        ServerModuleFile entity;
        List<ServerModuleFileState> states;

        // ----------------------------------------
        // module_0:
        // ----------------------------------------
        //      currentCluster  => ACCEPTED
        //      node_1          => UPLOADED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        entity.setStateForNode("node_1", ModuleState.UPLOADED);
        entity.setStateForNode("node_3", ModuleState.LOADED);
        serverModuleFileManager.update(entity);
        flushSession();
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
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
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 2));
        Assert.assertNotNull(entity);
        entity.setStateForNode("node_1", ModuleState.ACCEPTED);
        entity.setStateForNode("node_2", ModuleState.ACCEPTED);
        entity.setStateForNode("node_3", ModuleState.UPLOADED);
        serverModuleFileManager.update(entity);
        flushSession();
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 2));
        Assert.assertNotNull(entity);
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
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 3));
        Assert.assertNotNull(entity);
        entity.setStateForNode("node_2", ModuleState.UPLOADED);
        serverModuleFileManager.update(entity);
        flushSession();
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 3));
        Assert.assertNotNull(entity);
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
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 4));
        Assert.assertNotNull(entity);
        entity.setStateForNode(clusterNodeId, ModuleState.REJECTED);
        entity.setStateForNode("node_2", ModuleState.LOADED);
        entity.setStateForNode("node_3", ModuleState.LOADED);
        serverModuleFileManager.update(entity);
        flushSession();
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 4));
        Assert.assertNotNull(entity);
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
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 5));
        Assert.assertNotNull(entity);
        entity.setStateForNode("node_3", ModuleState.LOADED);
        serverModuleFileManager.update(entity);
        flushSession();
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 5));
        Assert.assertNotNull(entity);
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
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 6));
        Assert.assertNotNull(entity);
        states = entity.getStates();
        Assert.assertNotNull(states);
        Assert.assertEquals(0, states.size());
    }

    @Test
    public void test_update_status() throws Exception {
        insertSampleModules();

        ServerModuleFile entity;
        ServerModuleFileState entityState;

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Goid entityGoid = new Goid(GOID_HI_START, 1);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // ----------------------------------------
        // module_1:    BEFORE
        // ----------------------------------------
        //      currentCluster  => ACCEPTED
        //      node_1          => UPLOADED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(3, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.ACCEPTED, entityState.getState());
        Assert.assertTrue(StringUtils.isBlank(entityState.getErrorMessage()));

        serverModuleFileManager.updateState(entityGoid, ModuleState.REJECTED);
        flushSession();

        // ----------------------------------------
        // module_1:    AFTER
        // ----------------------------------------
        //      currentCluster  => REJECTED
        //      node_1          => UPLOADED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(3, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.REJECTED, entityState.getState());
        Assert.assertTrue(StringUtils.isBlank(entityState.getErrorMessage()));

        serverModuleFileManager.updateState(entityGoid, "some error");
        flushSession();

        // ----------------------------------------
        // module_1:    AFTER with error message
        // ----------------------------------------
        //      currentCluster  => ERROR   ("some error")
        //      node_1          => UPLOADED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(3, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.ERROR, entityState.getState());
        Assert.assertEquals("some error", entityState.getErrorMessage());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////



        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        entityGoid = new Goid(GOID_HI_START, 5);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // ----------------------------------------
        // module_5:
        // ----------------------------------------
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(1, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNull(entityState);

        serverModuleFileManager.updateState(entityGoid, ModuleState.ACCEPTED);
        flushSession();

        // ----------------------------------------
        // module_5:
        // ----------------------------------------
        //      currentCluster  => ACCEPTED
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(2, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.ACCEPTED, entityState.getState());
        Assert.assertTrue(StringUtils.isBlank(entityState.getErrorMessage()));

        serverModuleFileManager.updateState(entityGoid, "some error");
        flushSession();

        // ----------------------------------------
        // module_5:
        // ----------------------------------------
        //      currentCluster  => ERROR   ("some error")
        //      node_3          => LOADED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(2, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.ERROR, entityState.getState());
        Assert.assertEquals("some error", entityState.getErrorMessage());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////



        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        entityGoid = new Goid(GOID_HI_START, 6);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // ----------------------------------------
        // module_6:
        // ----------------------------------------
        //      (empty)
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(0, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNull(entityState);

        serverModuleFileManager.updateState(entityGoid, "some error");
        flushSession();

        // ----------------------------------------
        // module_6:
        // ----------------------------------------
        //      currentCluster  => ERROR   ("some error")
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(1, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.ERROR, entityState.getState());
        Assert.assertEquals("some error", entityState.getErrorMessage());

        serverModuleFileManager.updateState(entityGoid, ModuleState.REJECTED);
        flushSession();

        // ----------------------------------------
        // module_6:
        // ----------------------------------------
        //      currentCluster  => REJECTED
        // ----------------------------------------
        entity = serverModuleFileManager.findByPrimaryKey(entityGoid);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getStates());
        Assert.assertEquals(1, entity.getStates().size());
        entityState = serverModuleFileManager.findStateForCurrentNode(entity);
        Assert.assertNotNull(entityState);
        Assert.assertNotNull(entityState.getServerModuleFile());
        Assert.assertEquals(entityGoid, entityState.getServerModuleFile().getGoid());
        Assert.assertEquals(clusterNodeId, entityState.getNodeId());
        Assert.assertEquals(ModuleState.REJECTED, entityState.getState());
        // error message should be reset
        Assert.assertTrue(StringUtils.isBlank(entityState.getErrorMessage()));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void test_unique_constraints() throws Exception {
        insertSampleModules();

        ServerModuleFile entity;

        // update name only, set same name
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        entity.setName("module_1");
        serverModuleFileManager.update(entity);
        flushSession();

        // update name only, set existing name
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        entity.setName("module_2");
        try {
            serverModuleFileManager.update(entity);
            Assert.fail("update name only, set existing name should have failed with DuplicateObjectException");
        } catch (UpdateException e) {
            Assert.assertTrue(e.getCause() instanceof DuplicateObjectException);
            // for some reason session.clear() didn't work
            // therefore evict this entity from hibernate cache so that it will be fetched from db next time
            session.evict(entity);
        }
        flushSession();
        // make sure entity was not updated
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        session.clear();

        // update sha only, set same sha
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        entity.setModuleSha256("sha_1");
        serverModuleFileManager.update(entity);
        flushSession();

        // update sha only, set existing sha
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        entity.setModuleSha256("sha_2");
        try {
            serverModuleFileManager.update(entity);
            Assert.fail("update sha only, set existing sha should have failed with DuplicateObjectException");
        } catch (UpdateException e) {
            Assert.assertTrue(e.getCause() instanceof DuplicateObjectException);
            session.evict(entity);
        }
        flushSession();
        // make sure entity was not updated
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        session.clear();

        // update name and sha, set same name and same sha
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        entity.setName("module_1");
        entity.setModuleSha256("sha_1");
        serverModuleFileManager.update(entity);
        flushSession();

        // update name and sha, set existing name and same sha
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        entity.setName("module_2");
        entity.setModuleSha256("sha_1");
        try {
            serverModuleFileManager.update(entity);
            Assert.fail("update name and sha, set existing name and same sha should have failed with DuplicateObjectException");
        } catch (UpdateException e) {
            Assert.assertTrue(e.getCause() instanceof DuplicateObjectException);
            session.evict(entity);
        }
        flushSession();
        // make sure entity was not updated
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        session.clear();

        // update name and sha, set same name and existing sha
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        entity.setName("module_1");
        entity.setModuleSha256("sha_2");
        try {
            serverModuleFileManager.update(entity);
            Assert.fail("update name and sha, set same name and existing sha should have failed with DuplicateObjectException");
        } catch (UpdateException e) {
            Assert.assertTrue(e.getCause() instanceof DuplicateObjectException);
            session.evict(entity);
        }
        flushSession();
        // make sure entity was not updated
        entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertEquals("module_1", entity.getName());
        Assert.assertEquals("sha_1", entity.getModuleSha256());
        session.clear();


        // name exist, sha does not exist
        entity = new ServerModuleFile();
        entity.setName("module_1");
        entity.setModuleType(ModuleType.CUSTOM_ASSERTION);
        entity.createData("test123".getBytes(), "non_existent_sha");
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        try {
            serverModuleFileManager.save(entity);
            Assert.fail("name exist, sha does not exist should have failed with DuplicateObjectException");
        } catch (DuplicateObjectException e) {
            session.evict(entity);
        }
        flushSession();

        // name exist sha exists (diff entities)
        entity = new ServerModuleFile();
        entity.setName("module_1");
        entity.setModuleType(ModuleType.CUSTOM_ASSERTION);
        entity.createData("test123".getBytes(), "sha_2");
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        try {
            serverModuleFileManager.save(entity);
            Assert.fail("name exist sha exists (diff entities) should have failed with DuplicateObjectException");
        } catch (DuplicateObjectException e) {
            session.evict(entity);
        }
        flushSession();

        // name exist sha exists (same entity)
        entity = new ServerModuleFile();
        entity.setName("module_1");
        entity.setModuleType(ModuleType.CUSTOM_ASSERTION);
        entity.createData("test123".getBytes(), "sha_1");
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        try {
            serverModuleFileManager.save(entity);
            Assert.fail("name exist sha exists (same entity) should have failed with DuplicateObjectException");
        } catch (DuplicateObjectException e) {
            session.evict(entity);
        }
        flushSession();

        // name does not exist, sha exist
        entity = new ServerModuleFile();
        entity.setName("non_existent_name");
        entity.setModuleType(ModuleType.CUSTOM_ASSERTION);
        entity.createData("test123".getBytes(), "sha_2");
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        try {
            serverModuleFileManager.save(entity);
            Assert.fail("name does not exist, sha exist should have failed with DuplicateObjectException");
        } catch (DuplicateObjectException e) {
            session.evict(entity);
        }
        flushSession();

        // name does not exist sha does not exist
        entity = new ServerModuleFile();
        entity.setName("non_existent_name");
        entity.setModuleType(ModuleType.CUSTOM_ASSERTION);
        entity.createData("test123".getBytes(), "non_existent_sha");
        entity.setStateForNode(clusterNodeId, ModuleState.ACCEPTED);
        Assert.assertNotNull(serverModuleFileManager.save(entity));
        flushSession();
    }

    @Test(expected = LazyInitializationException.class)
    public void test_lazy_fetching_data() throws Exception {
        insertSampleModules();

        final ServerModuleFile entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        session.evict(entity); // remove from cache
        Assert.assertNotNull(entity.getData());
        //noinspection UnusedDeclaration
        int n = entity.getData().getDataBytes().length;

        Assert.fail("getDataBytes should have failed with LazyInitializationException");
    }

    @Test
    public void test_lazy_initializing_data() throws Exception {
        insertSampleModules();

        final ServerModuleFile entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getData());
        Assert.assertTrue(entity.getData().getDataBytes().length > 0); // initialize
        session.evict(entity); // remove from cache
        Assert.assertTrue(entity.getData().getDataBytes().length > 0);
    }

    @Test
    public void test_eagerly_fetching_states() throws Exception {
        insertSampleModules();

        final ServerModuleFile entity = serverModuleFileManager.findByPrimaryKey(new Goid(GOID_HI_START, 1));
        Assert.assertNotNull(entity);
        session.evict(entity); // remove from cache
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
    }
}
