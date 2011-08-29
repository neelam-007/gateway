package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import com.l7tech.util.IOUtils;
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
 * A simple implementation of the {@link org.jboss.netty.channel.SimpleChannelUpstreamHandler} to deal with sending an
 * ICAP request and receiving the server's response.
 * </p>
 *
 * @author Ken Diep
 */
public final class IcapResponseHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOGGER = Logger.getLogger(IcapResponseHandler.class.getName());

    private volatile Channel channel;

    private final BlockingQueue<IcapResponse> responses = new LinkedBlockingQueue<IcapResponse>();

    public IcapResponse sendOptionsCommand(IcapConnectionDetail connectionDetail) {
        IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.OPTIONS,
                connectionDetail.toString(),
                connectionDetail.getHostname());
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

    /**
     * Scan the content as found in the {@link com.l7tech.common.mime.PartInfo} using the pre-build {@link ch.mimo.netty.handler.codec.icap.IcapRequest}.
     *
     * @param partInfo the content to be scanned.
     * @return an {@link ch.mimo.netty.handler.codec.icap.IcapResponse}.
     * @throws NoSuchPartException if the content of the given <tt>partInfo</tt> can not be retrieved for any reason.
     * @throws IOException         if any IO errors occur while reading the <tt>partInfo</tt>.
     */
    public IcapResponse scan(IcapConnectionDetail connectionDetail, PartInfo partInfo) throws NoSuchPartException, IOException {

        IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.RESPMOD,
                connectionDetail.toString(),
                connectionDetail.getHostname());
        request.addHeader(HttpHeaders.Names.ALLOW, "204");
        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
        httpResponse.setContent(ChannelBuffers.wrappedBuffer(IOUtils.slurpStream(partInfo.getInputStream(false))));
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
        super.channelOpen(ctx, e);
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