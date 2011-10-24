package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;

import java.io.InputStream;


/**
 * <p>An implementation of {@link org.jboss.netty.channel.ChannelDownstreamHandler} to send chunk requests to the server.</p>
 * <p>This implementation is based on {@link ch.mimo.netty.handler.codec.icap.IcapChunkSeparator} with changes to stream
 * contents to the server.</p>
 *
 * @author Ken Diep
 */
public class IcapChunkSeparator implements ChannelDownstreamHandler {

    private int chunkSize;

    /**
     * @param chunkSize defines the normal chunk size that is to be produced while separating.
     */
    public IcapChunkSeparator(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            MessageEvent msgEvent = (MessageEvent) e;
            Object msg = msgEvent.getMessage();
            if (msg instanceof IcapMessage) {
                IcapMessage message = (IcapMessage) msg;
                InputStream stream = extractContentFromMessage(message);

                fireDownstreamEvent(ctx, message, msgEvent);
                try {
                    if (stream != null) {
                        byte[] data = new byte[chunkSize];
                        boolean isPreview = message.isPreviewMessage();
                        boolean isEarlyTerminated = false;
                        for (int bread = stream.read(data); bread > 0; bread = stream.read(data)) {
                            if (isPreview) {
                                isEarlyTerminated = bread < message.getPreviewAmount();
                            }
                            IcapChunk chunk = new DefaultIcapChunk(ChannelBuffers.wrappedBuffer(data, 0, bread));
                            chunk.setPreviewChunk(isPreview);
                            chunk.setEarlyTermination(isEarlyTerminated);
                            fireDownstreamEvent(ctx, chunk, msgEvent);
                        }
                        IcapChunkTrailer trailer = new DefaultIcapChunkTrailer();
                        trailer.setPreviewChunk(isPreview);
                        trailer.setEarlyTermination(isEarlyTerminated);
                        fireDownstreamEvent(ctx, trailer, msgEvent);
                    }
                } finally {
                    if(stream != null){
                        stream.close();
                    }
                }
            } else {
                ctx.sendDownstream(e);
            }
        } else {
            ctx.sendDownstream(e);
        }
    }

    private InputStream extractContentFromMessage(IcapMessage message) {
        InputStream content = null;
        if (message.getHttpResponse() instanceof StreamedHttpResponse) {
            content = ((StreamedHttpResponse) message.getHttpResponse()).getContentAsStream();
            message.setBody(IcapMessageElementEnum.RESBODY);
        }
        return content;
    }

    private void fireDownstreamEvent(ChannelHandlerContext ctx, Object message, MessageEvent messageEvent) {
        DownstreamMessageEvent downstreamMessageEvent =
                new DownstreamMessageEvent(ctx.getChannel(), messageEvent.getFuture(), message, messageEvent.getRemoteAddress());
        ctx.sendDownstream(downstreamMessageEvent);
    }
}

