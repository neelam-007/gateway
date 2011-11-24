package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.util.Functions.UnaryVoid;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * <p>
 * A simple implementation of the {@link AbstractIcapResponseHandler} to deal with sending an
 * ICAP request and receiving the server's response.
 * </p>
 *
 * @author Ken Diep
 */
public final class IcapResponseHandler extends AbstractIcapResponseHandler {

    private static final Logger LOGGER = Logger.getLogger(IcapResponseHandler.class.getName());
    private final UnaryVoid<Integer> callback;
    private final BlockingQueue<IcapResponse> responseQueue = new LinkedBlockingQueue<IcapResponse>(10);
    private final AtomicInteger activeExchanges = new AtomicInteger(0);
    private volatile Channel channel;

    public IcapResponseHandler(final UnaryVoid<Integer> callback) {
        this.callback = callback;
    }

    @Override
    public IcapResponse sendOptionsCommand(final String icapUri, final String host) {
        IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.OPTIONS,
                icapUri,
                host);
        return sendRequest(request);
    }

    private IcapResponse sendRequest(IcapRequest request){
        IcapResponse response = null;
        boolean interrupted = false;
        activeExchanges.incrementAndGet();
        try {
            final ChannelFuture fut = channel.write(request);
            fut.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isDone() && channelFuture.isSuccess()){
                        return;
                    }
                    responseQueue.put( new DefaultIcapResponse( IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE ) );
                }
            });
            response = responseQueue.take();
        } catch (InterruptedException e) {
            interrupted = true;
        } finally {
            activeExchanges.decrementAndGet();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return response;
    }

    @Override
    public IcapResponse scan(final String icapUri, final String host, PartInfo partInfo) throws NoSuchPartException, IOException {

        final IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.RESPMOD,
                icapUri,
                host);
        request.addHeader(HttpHeaders.Names.ALLOW, "204");
        final HttpResponse httpResponse = new StreamedHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
        ((StreamedHttpResponse)httpResponse).setContent(partInfo.getInputStream(false));
        httpResponse.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.setHttpResponse(httpResponse);
        return sendRequest(request);
    }


    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channel = e.getChannel();
        LOGGER.finer("opened channel " + channel);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        responseQueue.put( (IcapResponse) e.getMessage() );
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
        LOGGER.warning(e.toString());
        e.getChannel().close();
        responseQueue.put( new DefaultIcapResponse( IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE ) );
    }

    @Override
    public void channelIdle(final ChannelHandlerContext ctx, final IdleStateEvent e) throws Exception {
        LOGGER.finer("closing idle channel " + channel);
        // Ensure any exchange blocked on reading the response queue is unblocked.
        // One cause for this is insufficent threads to service the number of open
        // connections.
        while ( activeExchanges.getAndDecrement() > 0 ) {
            responseQueue.put( new DefaultIcapResponse( IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE ) );
        }
        callback.call(channel.getId());
        channel.close();
    }

    @Override
    public void channelClosed(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        LOGGER.finer("channel closed " + e);
        while ( activeExchanges.getAndDecrement() > 0 ) {
            responseQueue.put( new DefaultIcapResponse( IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE ) );
        }
        callback.call(channel.getId());
    }
}