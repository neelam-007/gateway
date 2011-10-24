package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

    private volatile Channel channel;

    private final BlockingQueue<IcapResponse> responses = new LinkedBlockingQueue<IcapResponse>();

    @Override
    public IcapResponse sendOptionsCommand(final String icapUri, final String host) {
        IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.OPTIONS,
                icapUri,
                host);
        //tell the server to close the connection
        //if we explicitly close the channel, the exceptionCaught will be executed
        //and raise many ClosedChannelException
        request.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        return sendRequest(request);
    }

    private IcapResponse sendRequest(IcapRequest request){
        IcapResponse response = null;
        boolean interrupted = false;
        try {
            ChannelFuture fut = channel.write(request);
            //short pause to detect channel status as channel.write will attempt to return as quickly as possible
            //due to it's async nature.
            //if the channel is closed then we'll be able to detect it and handle the error accordingly.
            Thread.sleep(100);
            if(!fut.getChannel().isConnected()){
                responses.offer(new DefaultIcapResponse(IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE));
            }
            response = responses.take();
        } catch (InterruptedException e) {
            interrupted = true;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return response;
    }

    @Override
    public IcapResponse scan(final String icapUri, final String host, PartInfo partInfo) throws NoSuchPartException, IOException {

        IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.RESPMOD,
                icapUri,
                host);
        request.addHeader(HttpHeaders.Names.ALLOW, "204");
        //tell the server to close the connection
        //if we explicitly close the channel, the exceptionCaught will be executed
        //and raise many ClosedChannelException
        request.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        HttpResponse httpResponse = new StreamedHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
        ((StreamedHttpResponse)httpResponse).setContent(partInfo.getInputStream(false));
        httpResponse.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.setHttpResponse(httpResponse);
        return sendRequest(request);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channel = e.getChannel();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        responses.offer((IcapResponse) e.getMessage());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
        LOGGER.warning(e.toString());
        e.getChannel().close();
        responses.offer(new DefaultIcapResponse(IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE));
    }
}