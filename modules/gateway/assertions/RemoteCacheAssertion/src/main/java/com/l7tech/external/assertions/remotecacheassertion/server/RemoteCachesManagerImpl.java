package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/11/11
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCachesManagerImpl implements ApplicationListener, RemoteCachesManager {
    private static final Logger logger = Logger.getLogger(RemoteCachesManagerImpl.class.getName());

    private HashMap<Goid, RemoteCache> currentlyUsedCaches = new HashMap<>();
    private ServerConfig serverConfig;
    private EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    private ClusterPropertyManager clusterPropertyManager;
    private static RemoteCachesManagerImpl INSTANCE;

    private CoherenceClassLoader coherenceClassLoader;
    private TerracottaToolkitClassLoader terracottaToolkitClassLoader;
    private GemFireClassLoader gemfireClassLoader;

    public static void createRemoteCachesManager(EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager,
                                                 ClusterPropertyManager clusterPropertyManager,
                                                 ServerConfig serverConfig) {
        synchronized (RemoteCachesManagerImpl.class) {
            if (INSTANCE != null) {
                throw new IllegalStateException("A remote cache manager already exists.");
            }
            INSTANCE = new RemoteCachesManagerImpl(entityManager, clusterPropertyManager, serverConfig);
        }
    }

    public static RemoteCachesManager getInstance() {
        return INSTANCE;
    }

    //For testing only
    static void setInstance(RemoteCachesManagerImpl instance) {
        INSTANCE = instance;
    }

    HashMap<Goid, RemoteCache> getCurrentlyUsedCaches() {
        return currentlyUsedCaches;
    }

    private RemoteCachesManagerImpl(EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager,
                                    ClusterPropertyManager clusterPropertyManager,
                                    ServerConfig serverConfig) {
        this.entityManager = entityManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.serverConfig = serverConfig;
        try {
            Collection<RemoteCacheEntity> remoteEntities = entityManager.findAll();
            for (RemoteCacheEntity ce : remoteEntities) {
                connectionAdded(ce);
            }
        } catch (FindException e) {
            logger.log(Level.FINER, e.getMessage(), e);
        }
    }

    public void connectionAdded(RemoteCacheEntity remoteCacheEntity) {
        if (currentlyUsedCaches.get(remoteCacheEntity.getGoid()) != null) {
            throw new RuntimeException("Trying to start new remote cache " + remoteCacheEntity.getName() + " but it already exists.");
        }
        try {
            createRemoteCache(remoteCacheEntity);
        } catch (RemoteCacheConnectionException e) {
            logger.warning("Error occurred while starting the remote cache " + e.getMessage());
        }
    }

    public void connectionUpdated(RemoteCacheEntity remoteCacheEntity) {
        try {
            RemoteCache remoteC = getRemoteCache(remoteCacheEntity.getGoid());
            if (null == remoteC) {
                throw new RuntimeException("Error occurred while updating the remote cache instance. " +
                        remoteCacheEntity.getName() + " cache does not exist, and cannot update");
            }
            remoteC.shutdown();
            createRemoteCache(remoteCacheEntity);
        } catch (RemoteCacheConnectionException e) {
            logger.warning("Error occurred while starting the remote cache " + e.getMessage());
        }
    }

    public void connectionRemoved(RemoteCacheEntity remoteCacheEntity) {
        RemoteCache remoteCache = currentlyUsedCaches.remove(remoteCacheEntity.getGoid());
        if (remoteCache != null) {
            remoteCache.shutdown();
        } else {
            logger.warning("Couldn't shutdown " + remoteCacheEntity.getName() + " as its not present on the SSG cache.");
        }
    }

    public RemoteCache getRemoteCache(Goid cacheGoid) throws RemoteCacheConnectionException {
        synchronized (currentlyUsedCaches) {
            if (currentlyUsedCaches.containsKey(cacheGoid)) {
                return currentlyUsedCaches.get(cacheGoid);
            }

            try {
                RemoteCacheEntity remoteCacheEntity = entityManager.findByPrimaryKey(cacheGoid);
                if (remoteCacheEntity != null) {
                    return createRemoteCache(remoteCacheEntity);
                } else {
                    throw new RemoteCacheConnectionException("Could not find remote cache.  " +
                            "Please ensure the assertion is configured with a valid remote cache");
                }
            } catch (FindException e) {
                logger.warning("Error occurred!!!!" + e.getMessage());
            }
            return null;
        }
    }

    private RemoteCache createRemoteCache(RemoteCacheEntity remoteCacheEntity) throws RemoteCacheConnectionException {
        RemoteCacheTypes type = RemoteCacheTypes.getEntityEnumType(remoteCacheEntity.getType());
        RemoteCache remoteCache;
        if (!remoteCacheEntity.isEnabled()) {
            currentlyUsedCaches.remove(remoteCacheEntity.getGoid());
            throw new RemoteCacheConnectionException("Remote cache: " + remoteCacheEntity.getName() + " is not enabled.");
        }
        switch (type) {
            case Memcached:
                try {
                    remoteCache = new MemcachedRemoteCache(remoteCacheEntity);
                    currentlyUsedCaches.put(remoteCacheEntity.getGoid(), remoteCache);
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            case Terracotta:
                try {
                    if (terracottaToolkitClassLoader == null) {
                        terracottaToolkitClassLoader = TerracottaToolkitClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
                    }
                    remoteCache = new TerracottaRemoteCache(remoteCacheEntity, clusterPropertyManager, terracottaToolkitClassLoader);
                    currentlyUsedCaches.put(remoteCacheEntity.getGoid(), remoteCache);
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            case Coherence:
                try {
                    if (coherenceClassLoader == null) {
                        coherenceClassLoader = CoherenceClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
                    }

                    //
                    // Creating/Updating cache configuration via GUI does not have the permissions required to
                    // create a new Coherence remote cache.  We'll execute the code in the same privileges that the
                    // modular assertion has.
                    //
                    final RemoteCacheEntity rce = remoteCacheEntity;
                    final ClusterPropertyManager cpm = clusterPropertyManager;
                    final CoherenceClassLoader ccl = coherenceClassLoader;

                    remoteCache = AccessController.doPrivileged(new PrivilegedExceptionAction<CoherenceRemoteCache>() {
                        @Override
                        public CoherenceRemoteCache run() throws Exception {
                            return new CoherenceRemoteCache(rce, cpm, ccl);
                        }
                    });

                    currentlyUsedCaches.put(remoteCacheEntity.getGoid(), remoteCache);
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            case GemFire:
                try {
                    if (gemfireClassLoader == null) {
                        gemfireClassLoader = GemFireClassLoader.getInstance(RemoteCachesManagerImpl.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
                    }

                    remoteCache = new GemfireRemoteCache(remoteCacheEntity, gemfireClassLoader);
                    currentlyUsedCaches.put(remoteCacheEntity.getGoid(), remoteCache);
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            case Redis:
                try {
                    remoteCache = new RedisRemoteCache(remoteCacheEntity);
                    currentlyUsedCaches.put(remoteCacheEntity.getGoid(), remoteCache);
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            default:
                throw new RemoteCacheConnectionException("Unknown remote cache type.");
        }
        return remoteCache;
    }

    public static void shutdown() {
        RemoteCachesManagerImpl manager = null;

        synchronized (RemoteCachesManagerImpl.class) {
            manager = INSTANCE;
            INSTANCE = null;
        }

        if (manager != null) {
            manager.shutdownConnections();
        }
    }

    private void shutdownConnections() {
        synchronized (currentlyUsedCaches) {
            for (RemoteCache remoteCache : currentlyUsedCaches.values()) {
                remoteCache.shutdown();
            }
            currentlyUsedCaches.clear();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
}
