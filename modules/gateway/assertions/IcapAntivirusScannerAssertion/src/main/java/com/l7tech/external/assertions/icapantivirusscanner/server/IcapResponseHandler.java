package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.io.IOException;
import java.net.SocketException;
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
        request.addHeader(HttpHeaders.Names.CONNECTION, "Close");
        channel.write(request);
        IcapResponse response = null;
        boolean interrupted = false;
        try {
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
        request.addHeader(HttpHeaders.Names.CONNECTION, "Close");
        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(partInfo.getInputStream(false), (int) partInfo.getActualContentLength());
        httpResponse.setContent(cb);
        httpResponse.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.setHttpResponse(httpResponse);

        channel.write(request);

        IcapResponse response = null;

        boolean interrupted = false;
        try {
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
        IcapResponseStatus status = IcapResponseStatus.BAD_REQUEST;
        //when to attempt to connect to an invalid service name, the icap server will return a service nto found error
        //and closes the connection.  the icap client api that we use doesn't return this error, but instead
        //will throw these 2 exception.
        if (e.getCause() instanceof SocketException || e.getCause() instanceof IcapDecodingError) {
            status = IcapResponseStatus.ICAP_SERVICE_NOT_FOUND;
        }
        IcapResponse response = new DefaultIcapResponse(IcapVersion.ICAP_1_0, status);
        responses.offer(response);
        e.getChannel().close();
    }
}