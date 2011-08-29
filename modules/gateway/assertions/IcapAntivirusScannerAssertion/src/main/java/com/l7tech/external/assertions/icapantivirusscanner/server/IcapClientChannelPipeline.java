package com.l7tech.external.assertions.icapantivirusscanner.server;


import ch.mimo.netty.handler.codec.icap.IcapChunkAggregator;
import ch.mimo.netty.handler.codec.icap.IcapChunkSeparator;
import ch.mimo.netty.handler.codec.icap.IcapRequestEncoder;
import ch.mimo.netty.handler.codec.icap.IcapResponseDecoder;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * An implementation of the {@link org.jboss.netty.channel.ChannelPipelineFactory} to create the components required
 * to operate with the ICAP protocol.
 *
 * @author Ken Diep
 */
public class IcapClientChannelPipeline implements ChannelPipelineFactory {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new IcapRequestEncoder());
        pipeline.addLast("chunkSeparator", new IcapChunkSeparator(DEFAULT_BUFFER_SIZE));
        pipeline.addLast("decoder", new IcapResponseDecoder());
        pipeline.addLast("chunkAggregator", new IcapChunkAggregator(DEFAULT_BUFFER_SIZE));
        pipeline.addLast("handler", new IcapResponseHandler());
        return pipeline;
    }

}
