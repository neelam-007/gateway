package com.l7tech.external.assertions.ahttp.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 *
 */
public class TestAsyncNettyHttpServer {
    public static final int PORT = Integer.getInteger("port", 3080);

    public static void main(String[] args) {
        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        bootstrap.setPipelineFactory(new TestAsyncNettyHttpServerPipelineFactory());
        bootstrap.bind(new InetSocketAddress(PORT));

        System.out.println("Async echo HTTP server listening on port " + PORT + " with response delay of " + TestAsyncNettyHttpRequestHandler.RESPONSE_DELAY + " ms");
    }
}
