package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.TimeUnit;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.util.Map;
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

    private final long timeout;

    private static final Logger LOGGER = Logger.getLogger(IcapResponseHandler.class.getName());

    private final BlockingQueue<IcapResponse> responseQueue = new LinkedBlockingQueue<IcapResponse>(10);

    public IcapResponseHandler(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) {
        boolean interrupted = false;
        try {
            responseQueue.put( (IcapResponse) e.getMessage() );
        } catch (InterruptedException e1) {
            interrupted = true;
        }
        if(interrupted){
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) {
        LOGGER.warning(e.toString());
        boolean interrupted = false;
        try {
            responseQueue.put(new DefaultIcapResponse(IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE));
            Channels.close(e.getChannel());
        } catch (InterruptedException e1) {
            interrupted = true;
        }
        if(interrupted){
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public IcapResponse getResponse() {
        boolean interrupted;
        for(;;){
            try {
                return responseQueue.poll(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
                break;
            }
        }
        if(interrupted){
            Thread.currentThread().interrupt();
        }
        return new DefaultIcapResponse(IcapVersion.ICAP_1_0, IcapResponseStatus.SERVICE_UNAVAILABLE);
    }
}