package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.HexUtils;
import com.l7tech.util.RandomUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
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

    private final List<ByteBuffer> requestBody = new ArrayList<ByteBuffer>();

    private HttpRequest request;
    private boolean readingChunks;

    AsyncHttpRequestHandler(AsyncHttpListenerInfo listenerInfo) {
        this.listenerInfo = listenerInfo;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!readingChunks) {
            HttpRequest request = this.request = (HttpRequest) e.getMessage();

            if (is100ContinueExpected(request)) {
                send100Continue(e);
            }

            if (request.isChunked()) {
                readingChunks = true;
            } else {
                ChannelBuffer content = request.getContent();
                requestBody.add(content.toByteBuffer());
                submitRequest(e);
            }
        } else {
            HttpChunk chunk = (HttpChunk) e.getMessage();
            if (chunk.isLast()) {
                readingChunks = false;
                // TODO grab chunked encoding HTTP trailers here if we care
                submitRequest(e);
            } else {
                requestBody.add(chunk.getContent().toByteBuffer());
            }
        }

    }

    private void submitRequest(final MessageEvent e) {
        final boolean keepAlive = false && isKeepAlive(request); // TODO reenable keepalives after issue debugged
        final HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        final String correlationId = generateCorrelationId();
        PendingAsyncRequest pendingRequest = new PendingAsyncRequest(correlationId, listenerInfo, response, e.getChannel(), keepAlive);
        listenerInfo.getTransportModule().submitRequestToMessageProcessor(pendingRequest, request, response, new ByteBuffersInputStream(requestBody), (InetSocketAddress) e.getRemoteAddress());
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
