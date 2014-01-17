package com.l7tech.external.assertions.icapantivirusscanner.server;


import ch.mimo.netty.handler.codec.icap.IcapChunkAggregator;
import ch.mimo.netty.handler.codec.icap.IcapRequestEncoder;
import ch.mimo.netty.handler.codec.icap.IcapResponseDecoder;
import com.l7tech.util.Functions.UnaryVoid;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of the {@link org.jboss.netty.channel.ChannelPipelineFactory} to create the components required
 * to operate with the ICAP protocol.
 *
 * @author Ken Diep
 */
public class IcapClientChannelPipeline implements ChannelPipelineFactory {

    private final long timeout;
    private static final int DEFAULT_BUFFER_SIZE = 204800;

    public IcapClientChannelPipeline(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new IcapRequestEncoder());
        pipeline.addLast("chunkSeparator", new IcapChunkSeparator(DEFAULT_BUFFER_SIZE));
        pipeline.addLast("decoder", new IcapResponseDecoder());
        pipeline.addLast("chunkAggregator", new IcapChunkAggregator(DEFAULT_BUFFER_SIZE));
        pipeline.addLast("handler", new IcapResponseHandler(timeout));
        return pipeline;
    }

}
