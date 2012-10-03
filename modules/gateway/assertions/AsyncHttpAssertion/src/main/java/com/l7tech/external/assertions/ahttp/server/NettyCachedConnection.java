package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a potentially-reusable keepalive connection to some outbound HTTP server.
 */
class NettyCachedConnection {
    private final NettyConnectionCacheKey key;
    private final boolean keepAlive;
    private ChannelFuture channelFuture;
    private final AtomicReference<Functions.UnaryVoid<Either<IOException, HttpResponse>>> responseCallback = new AtomicReference<Functions.UnaryVoid<Either<IOException, HttpResponse>>>();

    NettyCachedConnection(NettyConnectionCacheKey key, boolean keepAlive) {
        this.key = key;
        this.keepAlive = keepAlive;
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    void setChannelFuture(ChannelFuture channelFuture) {
        if (this.channelFuture != null)
            throw new IllegalStateException("channelFuture already set");
        this.channelFuture = channelFuture;
    }

    public Functions.UnaryVoid<Either<IOException, HttpResponse>> getResponseCallback() {
        return responseCallback.get();
    }

    public void setResponseCallback(@Nullable Functions.UnaryVoid<Either<IOException, HttpResponse>> responseCallback) {
        if (responseCallback != null && this.responseCallback.get() != null)
            throw new IllegalStateException("responseCallback is already active");
        this.responseCallback.set(responseCallback);
    }

    public NettyConnectionCacheKey getKey() {
        return key;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }
}
