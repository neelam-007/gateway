package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.assertion.base.util.classloaders.UploadJarClassLoader;
import com.l7tech.external.assertions.remotecacheassertion.server.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 04/05/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheEntityAdminImpl implements RemoteCacheEntityAdmin {

    private static final Logger logger = Logger.getLogger(RemoteCacheEntityAdminImpl.class.getName());

    private EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    private ServerConfig serverConfig;
    private RemoteCachesManager connectionManager;

    public RemoteCacheEntityAdminImpl(EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager,
                                      ServerConfig serverConfig,
                                      ClusterPropertyManager clusterPropertyManager)
    {
        this.entityManager = entityManager;
        this.serverConfig = serverConfig;
        try {
            RemoteCachesManagerImpl.createRemoteCachesManager(this.entityManager, clusterPropertyManager, serverConfig);
            connectionManager = RemoteCachesManagerImpl.getInstance();
        } catch(IllegalStateException e) {
            logger.log(Level.WARNING, "Error creating the Remote Cache connection manager.", e);
        }
    }

    RemoteCacheEntityAdminImpl(EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Collection<RemoteCacheEntity> findAll() throws FindException {
        return entityManager.findAll();
    }

    @Override
    public Goid save(RemoteCacheEntity entity) throws SaveException, UpdateException {
        if (entity.getGoid().equals(RemoteCacheEntity.DEFAULT_GOID)) {
            Goid goid = entityManager.save(entity);
            entity.setGoid(goid);
            connectionManager.connectionAdded(entity);
            return goid;
        } else {
            entityManager.update(entity);
            connectionManager.connectionUpdated(entity);
            return entity.getGoid();
        }
    }

    @Override
    public void delete(RemoteCacheEntity entity) throws DeleteException, FindException {
        entityManager.delete(entity);
        connectionManager.connectionRemoved(entity);
    }

    @Override
    public RemoteCacheEntity findByUniqueName(String name) throws FindException {
        return entityManager.findByUniqueName(name);
    }

    @Override
    public RemoteCacheEntity find(Goid goid) throws FindException {
        return entityManager.findByPrimaryKey(goid);
    }

    @Override
    public Map<String, String> getDefinedLibrariesToUpload() {
        TerracottaToolkitClassLoader tcClassLoader = TerracottaToolkitClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        CoherenceClassLoader cClassLoader = CoherenceClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        GemFireClassLoader gfClassLoader = GemFireClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));

        Map<String, String> definedLibrariesToUpload = new HashMap<>();
        definedLibrariesToUpload.putAll(tcClassLoader.getDefinedLibrariesToUpload());
        definedLibrariesToUpload.putAll(cClassLoader.getDefinedLibrariesToUpload());
        definedLibrariesToUpload.putAll(gfClassLoader.getDefinedLibrariesToUpload());
        return definedLibrariesToUpload;
    }

    @Override
    public List<String> getInstalledLibraries() {
        TerracottaToolkitClassLoader tcClassLoader = TerracottaToolkitClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        CoherenceClassLoader cClassLoader = CoherenceClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        GemFireClassLoader gfClassLoader = GemFireClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));

        ArrayList<String> results = new ArrayList<>();
        results.addAll(getLibrariesInPath(tcClassLoader.getJarPath()));
        results.addAll(getLibrariesInPath(cClassLoader.getJarPath()));
        results.addAll(getLibrariesInPath(gfClassLoader.getJarPath()));

        return results;
    }

    private List<String> getLibrariesInPath(String path) {
        File libDir = new File(path);
        ArrayList<String> results = new ArrayList<>();

        if(!libDir.exists() || !libDir.isDirectory() || !libDir.canRead()) {
            return results;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") || name.endsWith(".key");
            }
        };
        for(String name : libDir.list(filter)) {
            results.add(name);
        }

        return results;
    }

    @Override
    public void addLibrary(String filename, byte[] bytes) throws SaveException {
        //Notify the correct class loader
        TerracottaToolkitClassLoader tcClassLoader = TerracottaToolkitClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        CoherenceClassLoader cClassLoader = CoherenceClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        GemFireClassLoader gfClassLoader = GemFireClassLoader.getInstance(this.getClass().getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));

        if(tcClassLoader.getDefinedLibrariesToUpload().keySet().contains(filename)) {
            saveFile(filename, bytes, tcClassLoader);
        }

        if(cClassLoader.getDefinedLibrariesToUpload().keySet().contains(filename)) {
            saveFile(filename, bytes, cClassLoader);
        }

        if(gfClassLoader.getDefinedLibrariesToUpload().keySet().contains(filename)) {
            saveFile(filename, bytes, gfClassLoader);
        }
    }

    private void saveFile(String filename, byte[] bytes, UploadJarClassLoader classLoader) throws SaveException {

        File libDir = new File(classLoader.getJarPath());

        if(libDir.exists() && (!libDir.isDirectory() || !libDir.canRead())) {
            throw new SaveException("Unable to create library directory.");
        }

        if(!libDir.exists() && !libDir.mkdir()) {
            throw new SaveException("Unable to create library directory.");
        }

        File libraryFile = new File(libDir, filename);
        try {
            FileOutputStream fos = new FileOutputStream(libraryFile);
            fos.write(bytes);
            fos.close();
            classLoader.notifyLibraryAdded();
        } catch(IOException e) {
            throw new SaveException("Unable to save library.");
        }
    }
}
