package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for async HTTP response.
 */
public class NettyHttpClientHandler extends SimpleChannelUpstreamHandler {
    private static final Logger logger = Logger.getLogger(NettyHttpClientHandler.class.getName());

    private final NettyCachedConnection cachedConnection;

    public NettyHttpClientHandler(NettyCachedConnection cachedConnection) {
        this.cachedConnection = cachedConnection;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpResponse httpResponse = (HttpResponse) e.getMessage();

        getCallback().call(Either.<IOException, HttpResponse>right(httpResponse));

        cachedConnection.setResponseCallback(null);
        if (cachedConnection.isKeepAlive() && HttpHeaders.isKeepAlive(httpResponse)) {
            NettyConnectionCache.returnConnectionToPool(cachedConnection);
        } else {
            cachedConnection.getChannelFuture().getChannel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        final Throwable ex = e.getCause();
        logger.log(Level.INFO, "Exception on channel: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
        getCallback().call(Either.<IOException, HttpResponse>left(new IOException(ex)));
    }

    private Functions.UnaryVoid<Either<IOException, HttpResponse>> getCallback() {
        Functions.UnaryVoid<Either<IOException, HttpResponse>> callback = cachedConnection.getResponseCallback();
        if (callback == null)
            throw new IllegalStateException("No response handler set to receive async HTTP response to outbound async HTTP request");
        return callback;
    }
}
