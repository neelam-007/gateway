package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.util.ExceptionUtils;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/11/11
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCachesManagerImpl implements RemoteCachesManager {
    private static final Logger LOGGER = Logger.getLogger(RemoteCachesManagerImpl.class.getName());

    private ConcurrentMap<Goid, RemoteCache> currentlyUsedCaches = new ConcurrentHashMap<>();

    private ServerConfig serverConfig;
    private EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    private ClusterPropertyManager clusterPropertyManager;
    private static RemoteCachesManagerImpl INSTANCE;

    private CoherenceClassLoader coherenceClassLoader;
    private TerracottaToolkitClassLoader terracottaToolkitClassLoader;
    private GemFireClassLoader gemfireClassLoader;

    public static synchronized void createRemoteCachesManager(EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager,
                                                 ClusterPropertyManager clusterPropertyManager,
                                                 ServerConfig serverConfig) {
        LOGGER.log(Level.FINE, "Creating RemoteCachesManager singleton instance...");

        if (INSTANCE != null) {
            throw new IllegalStateException("A remote cache manager already exists.");
        }
        INSTANCE = new RemoteCachesManagerImpl(entityManager, clusterPropertyManager, serverConfig);
    }

    public static RemoteCachesManager getInstance() {
        return INSTANCE;
    }

    //For testing only
    static void setInstance(RemoteCachesManagerImpl instance) {
        INSTANCE = instance;
    }

    //For testing only
    ConcurrentMap<Goid, RemoteCache> getCurrentlyUsedCaches() {
        return currentlyUsedCaches;
    }

    private RemoteCachesManagerImpl(EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager,
                                    ClusterPropertyManager clusterPropertyManager,
                                    ServerConfig serverConfig) {
        this.entityManager = entityManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.serverConfig = serverConfig;
    }

    public RemoteCache getRemoteCache(Goid goid) throws RemoteCacheConnectionException {

        // computeIfAbsent() method is new in Java 1.8.
        RemoteCache remoteCache = currentlyUsedCaches.computeIfAbsent(goid, new Function<Goid, RemoteCache>() {
            @Override
            public RemoteCache apply(Goid goid) {
                try {
                    return createRemoteCache(goid);
                } catch (RemoteCacheConnectionException e) {
                    LOGGER.log(Level.WARNING, "Error creating remote cache connection: " + ExceptionUtils.getMessage(e));
                    return null;
                }
            }
        });

        if (null == remoteCache) {
            throw new RemoteCacheConnectionException("Error creating remote cache connection");
        }

        return remoteCache;
    }

    public void invalidateRemoteCache(Goid goid) {
        RemoteCache remoteCache = currentlyUsedCaches.remove(goid);

        if (remoteCache != null) {
            remoteCache.shutdown();
            LOGGER.log(Level.FINE, "Shutting down existing Remote Cache connection for goid {0} ...", goid);
        }
    }

    private RemoteCache createRemoteCache(Goid goid) throws RemoteCacheConnectionException {
        RemoteCacheEntity remoteCacheEntity = null;
        try {
            remoteCacheEntity = entityManager.findByPrimaryKey(goid);
        } catch (FindException e) {
            LOGGER.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new RemoteCacheConnectionException("Error encountered finding Remote Cache with goid " + goid.toString() + ": " + ExceptionUtils.getMessage(e));
        }

        if (remoteCacheEntity == null) {
            throw new RemoteCacheConnectionException("Remote Cache with goid " + goid.toString() + " not found. It may have been removed.");
        }

        RemoteCacheTypes type = RemoteCacheTypes.getEntityEnumType(remoteCacheEntity.getType());
        RemoteCache remoteCache;
        if (!remoteCacheEntity.isEnabled()) {
            LOGGER.log(Level.WARNING, "Remote Cache with name: {0} type: {1} is not enabled", new Object[]{remoteCacheEntity.getName(), type});
            throw new RemoteCacheConnectionException("Remote cache: " + remoteCacheEntity.getName() + " is not enabled.");
        }

        LOGGER.log(Level.FINE, "Creating a new Remote Cache connection for name: {0} type: {1}", new Object[]{remoteCacheEntity.getName(), type});

        switch (type) {
            case Memcached:
                try {
                    remoteCache = new MemcachedRemoteCache(remoteCacheEntity);
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
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            case Redis:
                try {
                    remoteCache = new RedisRemoteCache(remoteCacheEntity);
                    break;
                } catch (Exception e) {
                    throw new RemoteCacheConnectionException("Failed to connect to the remote cache.", e);
                }
            default:
                throw new RemoteCacheConnectionException("Unknown remote cache type.");
        }
        return remoteCache;
    }

    public synchronized static void shutdown() {
        LOGGER.log(Level.FINE, "Shutting down RemoteCachesManager singleton instance");
        RemoteCachesManagerImpl manager = INSTANCE;
        INSTANCE = null;

        if (manager != null) {
            manager.shutdownConnections();
        }
    }

    private void shutdownConnections() {
        for (RemoteCache remoteCache : currentlyUsedCaches.values()) {
            remoteCache.shutdown();
        }

        currentlyUsedCaches.clear();
    }
}
