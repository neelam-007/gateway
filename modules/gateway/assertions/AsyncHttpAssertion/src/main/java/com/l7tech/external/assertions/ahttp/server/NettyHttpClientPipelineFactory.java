package com.l7tech.external.assertions.ahttp.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;

/**
 *
 */
public class NettyHttpClientPipelineFactory implements ChannelPipelineFactory {
    private final NettyCachedConnection cachedConnection;

    public NettyHttpClientPipelineFactory(NettyCachedConnection cachedConnection) {
        this.cachedConnection = cachedConnection;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        // TODO TLS support
        /*
        SSLContext context = SSLContext.getInstance("TLS");
        SecureRandom secureRandom = JceProvider.getInstance().getSecureRandom();
        context.init(keyManagers, trustManagers, secureRandom != null ? secureRandom : new SecureRandom());
        SSLEngine sslEngine = context.createSSLEngine();
        pipeline.addLast("ssl", new SslHandler(sslEngine));
        */

        pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("inflater", new HttpContentDecompressor()); // automatic decompression of compressed response bodies
        pipeline.addLast("aggregator", new HttpChunkAggregator(10 * 1024 * 1024)); // Automatic buffering of chucked-encoded-responses up to 10mb TODO configurable, or disabled for streaming
        pipeline.addLast("handler", new NettyHttpClientHandler(cachedConnection));

        return pipeline;
    }
}
