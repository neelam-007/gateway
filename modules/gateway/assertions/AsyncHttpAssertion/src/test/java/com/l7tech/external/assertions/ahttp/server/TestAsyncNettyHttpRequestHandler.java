package com.l7tech.external.assertions.ahttp.server;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 *
 */
public class TestAsyncNettyHttpRequestHandler extends SimpleChannelUpstreamHandler {
    static final long RESPONSE_DELAY = Long.getLong("responseDelay", 5000L);
    static final int RESPONSE_TIMER_THREADS = Integer.getInteger("responseTimerThreads", 25);

    private static Timer[] timerPool = new Timer[RESPONSE_TIMER_THREADS];
    static {
        for (int i = 0; i < timerPool.length; i++) {
            timerPool[i] = new Timer("Response timer #" + i, true);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final HttpRequest request = (HttpRequest) e.getMessage();

        if (is100ContinueExpected(request)) {
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
            e.getChannel().write(response);
        }

        final HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.setContent(ChannelBuffers.copiedBuffer(request.getContent()));
        String contentType = request.getHeader(CONTENT_TYPE);
        if (contentType != null)
            response.setHeader(CONTENT_TYPE, contentType);

        Timer timer = timerPool[request.hashCode() % timerPool.length];
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                writeResponse(e.getChannel(), request, response);
            }
        }, RESPONSE_DELAY);
    }

    private void writeResponse(Channel channel, HttpRequest request, HttpResponse response) {
        boolean keepAlive = isKeepAlive(request);

        if (keepAlive) {
            response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
            response.setHeader(CONNECTION, "keep-alive");
        }

        String cookieString = request.getHeader(COOKIE);
        if (cookieString != null) {
            CookieDecoder cookieDecoder = new CookieDecoder();
            Set<Cookie> cookies = cookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                CookieEncoder cookieEncoder = new CookieEncoder(true);
                for (Cookie cookie : cookies) {
                    cookieEncoder.addCookie(cookie);
                }
                response.addHeader(SET_COOKIE, cookieEncoder.encode());
            }
        }

        ChannelFuture future = channel.write(response);

        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        //noinspection ThrowableResultOfMethodCallIgnored
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
