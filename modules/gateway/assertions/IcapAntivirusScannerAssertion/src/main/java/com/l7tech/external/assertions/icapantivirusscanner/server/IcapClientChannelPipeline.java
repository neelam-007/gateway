package com.l7tech.external.assertions.icapantivirusscanner.server;


import ch.mimo.netty.handler.codec.icap.IcapChunkAggregator;
import ch.mimo.netty.handler.codec.icap.IcapRequestEncoder;
import ch.mimo.netty.handler.codec.icap.IcapResponseDecoder;
import com.l7tech.util.Functions;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

/**
 * An implementation of the {@link org.jboss.netty.channel.ChannelPipelineFactory} to create the components required
 * to operate with the ICAP protocol.
 *
 * @author Ken Diep
 */
public class IcapClientChannelPipeline implements ChannelPipelineFactory {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final Timer timer;
    private final Functions.Unary<Void, Integer> callback;

    public IcapClientChannelPipeline(Timer timer, Functions.Unary<Void, Integer> callback){
        this.timer = timer;
        this.callback = callback;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("idleHandler", new IdleStateHandler(timer, 0, 0, 60)); //1 minute idle time
        pipeline.addLast("encoder", new IcapRequestEncoder());
        pipeline.addLast("chunkSeparator", new IcapChunkSeparator(DEFAULT_BUFFER_SIZE));
        pipeline.addLast("decoder", new IcapResponseDecoder());
        pipeline.addLast("chunkAggregator", new IcapChunkAggregator(DEFAULT_BUFFER_SIZE));
        pipeline.addLast("handler", new IcapResponseHandler(callback));
        return pipeline;
    }

}
