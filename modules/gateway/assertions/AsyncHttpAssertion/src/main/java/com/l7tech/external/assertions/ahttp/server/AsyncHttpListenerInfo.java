package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

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
        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
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
