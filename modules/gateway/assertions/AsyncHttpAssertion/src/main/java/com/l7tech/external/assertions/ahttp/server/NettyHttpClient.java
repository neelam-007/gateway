package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a netty-based HTTP client that can be used by multiple threads simultaneously.
 */
public class NettyHttpClient {

    // TODO configurable pool size
    private static ExecutorService bossExecutor = Executors.newCachedThreadPool();
    private static ExecutorService workerExecutor = Executors.newCachedThreadPool();
    private static final NioClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(bossExecutor, workerExecutor);

    /**
     * Asyncrhonously send a nett HTTP request and invoke the specified callback (possibly on another thread)
     * when a response is available.
     *
     * @param host the hostname or IP address string to which to send the HTTP request, as from the URI.  Required.
     * @param port the TCP port number to which to send the HTTP request, as from the URI.  Required.
     * @param httpRequest  the request to send, including method and local part of URI.  Required.
     * @param responseCallback callback to invoke on error or receiving a response successfully.  Required.
     */
    public static void issueAsyncHttpRequest(@NotNull String host,
                                      int port,
                                      @NotNull final HttpRequest httpRequest,
                                      @NotNull final Functions.UnaryVoid<Either<IOException, HttpResponse>> responseCallback)
    {
        // TODO TLS, and using the Gateway's trust infrastructure for TLS
        final ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new NettyHttpClientPipelineFactory(responseCallback));

        // Block until connect
        ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(host, port));
        connectFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture connectFuture) throws Exception {
                final Channel channel = connectFuture.getChannel();
                if (connectFuture.isSuccess()) {
                    // Start sending request asynchronously, return immediately
                    channel.write(httpRequest);

                } else if (connectFuture.isCancelled()) {
                    // Error may also be reported by our handler in the pipeline TODO test this, check for double-reports
                    responseCallback.call(Either.<IOException, HttpResponse>left(new IOException("Outbound Async HTTP request was canceled", connectFuture.getCause())));
                    channel.close();
                } else {
                    // Error may also be reported by our handler in the pipeline TODO test this, check for double-reports
                    responseCallback.call(Either.<IOException, HttpResponse>left(new IOException(connectFuture.getCause())));
                    channel.close();
                }
            }
        });
    }
}
