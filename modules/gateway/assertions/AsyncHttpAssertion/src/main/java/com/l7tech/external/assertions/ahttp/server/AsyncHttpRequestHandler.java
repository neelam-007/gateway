package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.HexUtils;
import com.l7tech.util.RandomUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.jboss.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
*
*/
class AsyncHttpRequestHandler extends SimpleChannelUpstreamHandler {
    private static final Logger logger = Logger.getLogger(AsyncHttpRequestHandler.class.getName());

    private final AsyncHttpListenerInfo listenerInfo;

    AsyncHttpRequestHandler(AsyncHttpListenerInfo listenerInfo) {
        this.listenerInfo = listenerInfo;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();

        if (is100ContinueExpected(request)) {
            send100Continue(e);
        }

        if (request.isChunked()) {
            // Not supposed to happen with HttpChunkAggregator ahead of us in the upstream pipeline
            e.getChannel().close();
            throw new IOException("Chunked encoding request not automatically deaggregated");
        } else {
            ChannelBuffer content = request.getContent();
            if (content.readable()) {
                submitRequest(e, request, content);
            }
        }
    }

    private void submitRequest(final MessageEvent e, HttpRequest request, ChannelBuffer content) {
        final boolean keepAlive = isKeepAlive(request);
        final HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        final String correlationId = generateCorrelationId();
        PendingAsyncRequest pendingRequest = new PendingAsyncRequest(correlationId, listenerInfo, response, e.getChannel(), keepAlive);
        listenerInfo.getTransportModule().submitRequestToMessageProcessor(
            pendingRequest,
            request,
            response,
            new ByteBuffersInputStream(Arrays.asList(content.toByteBuffers())),
            (InetSocketAddress) e.getRemoteAddress());
    }

    private String generateCorrelationId() {
        byte[] idBytes = new byte[16];
        RandomUtil.nextBytes(idBytes);
        return HexUtils.hexDump(idBytes);
    }

    private void send100Continue(MessageEvent e) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
        e.getChannel().write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        // TODO report errors properly
        //noinspection ThrowableResultOfMethodCallIgnored
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
