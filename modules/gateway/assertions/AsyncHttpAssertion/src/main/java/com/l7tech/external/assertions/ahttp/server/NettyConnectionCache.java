package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class NettyConnectionCache {
    private static final Logger logger = Logger.getLogger(NettyConnectionCache.class.getName());

    // TODO configurable thread pool size
    private static ExecutorService bossExecutor = Executors.newCachedThreadPool();
    private static ExecutorService workerExecutor = Executors.newCachedThreadPool();
    private static final NioClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(bossExecutor, workerExecutor);
    private static ConcurrentMap<NettyConnectionCacheKey, Queue<NettyCachedConnection>> connectionPool = new ConcurrentHashMap<NettyConnectionCacheKey, Queue<NettyCachedConnection>>();

    static void getConnection(NettyConnectionCacheKey key, boolean keepAlive, Functions.UnaryVoid<Either<IOException, HttpResponse>> responseCallback, final ChannelFutureListener readyListener) {
        // TODO configurable max connections to host
        // TODO stale checking
        if (keepAlive) {
            NettyCachedConnection conn = getPoolForHostConfig(key).poll();
            if (conn != null) {
                conn.setResponseCallback(responseCallback);
                try {
                    readyListener.operationComplete(conn.getChannelFuture());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected error notifying about available outbound async HTTP connection: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
                return;
            }
        }

        // Create a new connection
        // TODO TLS, and using the Gateway's trust infrastructure for TLS
        final ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
        NettyCachedConnection cachedConnection = new NettyCachedConnection(key, keepAlive);
        cachedConnection.setResponseCallback(responseCallback);
        bootstrap.setPipelineFactory(new NettyHttpClientPipelineFactory(cachedConnection));
        ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(key.getHost(), key.getPort()));
        cachedConnection.setChannelFuture(connectFuture);
        connectFuture.addListener(readyListener);
    }

    static void returnConnectionToPool(NettyCachedConnection cachedConnection) {
        if (!cachedConnection.isKeepAlive())
            throw new IllegalArgumentException("Unable to cache connection for which keepAlive is false");

        cachedConnection.setResponseCallback(null);
        getPoolForHostConfig(cachedConnection.getKey()).offer(cachedConnection);
    }

    private static Queue<NettyCachedConnection> getPoolForHostConfig(NettyConnectionCacheKey key) {
        Queue<NettyCachedConnection> pool = connectionPool.get(key);
        if (null == pool) {
            pool = new ConcurrentLinkedQueue<NettyCachedConnection>();
            Queue<NettyCachedConnection> previous = connectionPool.putIfAbsent(key, pool);
            if (previous != null)
                pool = previous; // Someone else beat us to it, use theirs
        }
        return pool;
    }
}
