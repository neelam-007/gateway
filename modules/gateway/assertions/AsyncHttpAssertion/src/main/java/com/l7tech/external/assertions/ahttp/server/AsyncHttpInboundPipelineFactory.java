package com.l7tech.external.assertions.ahttp.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

/**
*
*/
class AsyncHttpInboundPipelineFactory implements ChannelPipelineFactory {
    private final AsyncHttpListenerInfo listenerInfo;

    AsyncHttpInboundPipelineFactory(AsyncHttpListenerInfo listenerInfo) {
        this.listenerInfo = listenerInfo;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("deflater", new HttpContentCompressor());
        pipeline.addLast("handler", new AsyncHttpRequestHandler(listenerInfo));

        return pipeline;
    }
}
