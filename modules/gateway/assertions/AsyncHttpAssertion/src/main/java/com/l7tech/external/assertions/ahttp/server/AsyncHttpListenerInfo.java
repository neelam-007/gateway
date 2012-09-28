package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
*
*/
class AsyncHttpListenerInfo implements Closeable {
    private final AsyncHttpTransportModule transportModule;
    private final SsgConnector connector;
    private final InetSocketAddress bindSockAddr;
    private final ServerBootstrap bootstrap;

    public AsyncHttpListenerInfo(AsyncHttpTransportModule transportModule, SsgConnector connector, InetSocketAddress bindSockAddr) {
        this.transportModule = transportModule;
        this.connector = connector;
        this.bindSockAddr = bindSockAddr;
        // TODO better thread pool management

        final int CORE_POOL_SIZE = 50;
        final int MAX_POOL_SIZE = 200;
        long KEEPALIVE_SECONDS = 5L * 60L;
        BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<Runnable>(MAX_POOL_SIZE * 2);
        ExecutorService requestExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEPALIVE_SECONDS, TimeUnit.SECONDS, requestQueue);

        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), requestExecutor));
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setPipelineFactory(new AsyncHttpInboundPipelineFactory(this));
    }

    public void start() {
        bootstrap.bind(bindSockAddr);
    }

    @Override
    public void close() throws IOException {
        bootstrap.releaseExternalResources();
    }

    public AsyncHttpTransportModule getTransportModule() {
        return transportModule;
    }

    public SsgConnector getConnector() {
        return connector;
    }

    public InetSocketAddress getBindAddress() {
        return bindSockAddr;
    }
}
