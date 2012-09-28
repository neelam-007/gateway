package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.io.IOException;

/**
 * Handler for async HTTP response.
 */
public class NettyHttpClientHandler extends SimpleChannelUpstreamHandler {
    private final Functions.UnaryVoid<Either<IOException, HttpResponse>> responseCallback;

    public NettyHttpClientHandler(Functions.UnaryVoid<Either<IOException, HttpResponse>> responseCallback) {
        this.responseCallback = responseCallback;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpResponse httpResponse = (HttpResponse) e.getMessage();
        responseCallback.call(Either.<IOException, HttpResponse>right(httpResponse));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        responseCallback.call(Either.<IOException, HttpResponse>left(new IOException(e.getCause())));
    }
}
