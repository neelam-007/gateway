package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.external.assertions.remotecacheassertion.server.RemoteCachesManagerImpl;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteCachesManagerImpl.class, ServerConfig.class})
public class RemoteCacheEntityAdminImplTest {

    private EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    private ServerConfig serverConfig;
    private RemoteCachesManagerImpl connectionManager;
    private ClusterPropertyManager clusterPropertyManager;
    private RemoteCacheEntityAdminImpl remoteCacheEntityAdmin;
    private RemoteCacheEntity entity;
    private final Goid goid = new Goid(2, 1);
    private String ssgHome;

    @Before
    public void setup() {
        entityManager = mock(EntityManager.class);

        serverConfig = PowerMockito.mock(ServerConfig.class);
        PowerMockito.mockStatic(ServerConfig.class);
        String fullpath = getClass().getClassLoader().getResource("var").getPath();
        ssgHome = fullpath.substring(0, fullpath.indexOf("/var"));
        when(serverConfig.getProperty("com.l7tech.server.home")).thenReturn(ssgHome);

        connectionManager = mock(RemoteCachesManagerImpl.class);
        PowerMockito.mockStatic(RemoteCachesManagerImpl.class);

        clusterPropertyManager = mock(ClusterPropertyManager.class);

        when(RemoteCachesManagerImpl.getInstance()).thenReturn(connectionManager);

        entity = new RemoteCacheEntity();
        entity.setEnabled(true);
        entity.setName("cacheName");
        entity.setTimeout(10);
        entity.setGoid(goid);

        remoteCacheEntityAdmin = new RemoteCacheEntityAdminImpl(entityManager, serverConfig, clusterPropertyManager);
    }

    /**
     * test entity is saved successfully when entity is new
     *
     * @throws SaveException
     * @throws UpdateException
     */
    @Test
    public void testSuccessfulSaveEntity() throws SaveException, UpdateException {
        entity.setGoid(RemoteCacheEntity.DEFAULT_GOID);
        when(entityManager.save(entity)).thenReturn(goid);
        Goid savedGoid = remoteCacheEntityAdmin.save(entity);

        assertEquals(goid, savedGoid);
        verify(entityManager).save(entity);

    }

    /**
     * test entity is updated when entity exists
     *
     * @throws SaveException
     * @throws UpdateException
     */
    @Test
    public void testSuccessfulUpdateEntity() throws SaveException, UpdateException {
        Goid savedGoid = remoteCacheEntityAdmin.save(entity);

        assertEquals(goid, savedGoid);
        verify(entityManager).update(entity);

    }

    /**
     * test entity is deleted
     *
     * @throws FindException
     * @throws DeleteException
     */
    @Test
    public void testSuccessfulRemoveEntity() throws FindException, DeleteException {
        remoteCacheEntityAdmin.delete(entity);

        verify(entityManager).delete(entity);
    }

    /**
     * test all entities are found successfully
     *
     * @throws FindException
     */
    @Test
    public void testSuccessfulFindAll() throws FindException {
        Collection<RemoteCacheEntity> entities = new ArrayList<>();
        entities.add(entity);

        when(entityManager.findAll()).thenReturn(entities);

        Collection<RemoteCacheEntity> foundEntities = remoteCacheEntityAdmin.findAll();

        assertEquals(entities.size(), foundEntities.size());
        assertEquals(entities, foundEntities);
        verify(entityManager).findAll();
    }

    /**
     * test entity is found by unique name
     *
     * @throws FindException
     */
    @Test
    public void testSuccessfulFindByUniqueName() throws FindException {
        when(entityManager.findByUniqueName(entity.getName())).thenReturn(entity);

        RemoteCacheEntity foundEntity = remoteCacheEntityAdmin.findByUniqueName(entity.getName());

        assertEquals(entity, foundEntity);
        verify(entityManager).findByUniqueName(entity.getName());
    }

    /**
     * test entity is found by goid
     *
     * @throws FindException
     */
    @Test
    public void testSuccessfulFindByGoid() throws FindException {

        when(entityManager.findByPrimaryKey(entity.getGoid())).thenReturn(entity);

        RemoteCacheEntity foundEntity = remoteCacheEntityAdmin.find(entity.getGoid());

        assertEquals(entity, foundEntity);
        verify(entityManager).findByPrimaryKey(entity.getGoid());
    }

    /**
     * test defined libraries for Terracotta, coherence and gemfire are found
     */
    @Test
    public void testDefinedLibrariesToUpload() {
        Map<String, String> definedLibraries = remoteCacheEntityAdmin.getDefinedLibrariesToUpload();

        assertEquals(5, definedLibraries.size());
        assertTrue(definedLibraries.containsKey("terracotta-toolkit-runtime-ee.jar"));
        assertTrue(definedLibraries.containsKey("ehcache-ee.jar"));
        assertTrue(definedLibraries.containsKey("terracotta-license.key"));
        assertTrue(definedLibraries.containsKey("coherence.jar"));
        assertTrue(definedLibraries.containsKey("gemfire.jar"));
    }

    /**
     * Test libraries are found and installed
     */
    @Test
    public void testUploadedInstalledLibraries() {
        List<String> installedLibraries = remoteCacheEntityAdmin.getInstalledLibraries();

        assertEquals(5, installedLibraries.size());
        assertTrue(installedLibraries.contains("terracotta-toolkit-runtime-ee.jar"));
        assertTrue(installedLibraries.contains("ehcache-ee.jar"));
        assertTrue(installedLibraries.contains("terracotta-license.key"));
     /* assertTrue(installedLibraries.contains("coherence.jar")); // too large for git */
        assertTrue(installedLibraries.contains("gemfire.jar"));
    }

    /**
     * test adding a library
     *
     * @throws SaveException
     * @throws IOException
     */
    @Test
    public void testAddLibraries() throws SaveException, IOException {
        remoteCacheEntityAdmin.addLibrary("terracotta-license.key", "license Key Content".getBytes());
        remoteCacheEntityAdmin.addLibrary("test.txt", "test content".getBytes());

        File licenseFile = new File(ssgHome + "/var/lib/terracotta/terracotta-license.key");
        File testFile = new File(ssgHome + "/var/lib/terracatta/test.txt");
        String licenseContent = IOUtils.toString(getClass().getClassLoader().getResource("var/lib/terracotta/terracotta-license.key"));

        assertTrue(licenseFile.exists());
        assertFalse(testFile.exists());
        assertEquals("license Key Content", licenseContent);
    }
}
