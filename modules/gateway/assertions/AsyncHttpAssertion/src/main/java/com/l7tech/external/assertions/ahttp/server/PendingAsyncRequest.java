package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;

/**
 * Holds state for an async request that is awaiting a response.
 * <p/>
 * The original PolicyEnforcementContext that handled the original request may have long since been closed by the time
 * a response is delivered to a parked PendingAsyncRequest.
 */
public class PendingAsyncRequest {
    private static final Logger logger = Logger.getLogger(PendingAsyncRequest.class.getName());

    private final String correlationId;
    private final AsyncHttpListenerInfo listenerInfo;
    private final HttpResponse httpResponse;
    private final Channel channel;
    private final boolean keepAlive;
    private final List<Cookie> cookiesToSend = new ArrayList<Cookie>(); // TODO synchronization required?

    /**
     *
     * @param correlationId an arbitrary unique identifier string to identify a parked async request.  Should be something hard for unauthorized agents to guess.  Required.
     * @param listenerInfo the listenerInfo from the transport module that owns this PendingAsyncRequest.  Required.
     * @param httpResponse the Netty HTTP response bean in which we will assemble the final response.  Required.
     * @param channel the Netty channel on which to deliver the httpResponse when it is ready.  Required.
     * @param keepAlive true if this is to be a keepalive connection.  If so, we will include a keepalive header in the eventual response, and will not close the channel after delivering it.
     */
    public PendingAsyncRequest(@NotNull String correlationId,
                               @NotNull AsyncHttpListenerInfo listenerInfo,
                               @NotNull HttpResponse httpResponse,
                               @NotNull Channel channel,
                               boolean keepAlive)
    {
        this.correlationId = correlationId;
        this.listenerInfo = listenerInfo;
        this.httpResponse = httpResponse;
        this.channel = channel;
        this.keepAlive = keepAlive;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public AsyncHttpListenerInfo getListenerInfo() {
        return listenerInfo;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Channel getChannel() {
        return channel;
    }

    // TODO synchronization?  avoid providing raw access?
    public List<Cookie> getCookiesToSend() {
        return cookiesToSend;
    }

    void errorAndClose(HttpResponseStatus status, String message) {
        httpResponse.setStatus(status);
        httpResponse.setHeader("Content-Type", "text/plain");
        httpResponse.setContent(ChannelBuffers.copiedBuffer(message, Charsets.UTF8));
        ChannelFuture future = channel.write(httpResponse);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    public boolean respondAndMaybeClose(Message response, boolean destroyAsRead) {
        // TODO large message support/response streaming -- may need to block current thread in this method until done, since we are only borrowing the response Message
        // TODO maybe copy a biggish chunk at a time, waiting for all write futures to complete before returning
        HttpResponseKnob hrk = response.getKnob(HttpResponseKnob.class);
        if (hrk != null) {
            httpResponse.setStatus(HttpResponseStatus.valueOf(hrk.getStatus()));
        } else {
            httpResponse.setStatus(HttpResponseStatus.OK);
        }

        MimeKnob mk = response.getKnob(MimeKnob.class);
        if (mk != null) {
            try {
                httpResponse.setHeader("Content-Type", mk.getOuterContentType().getFullValue());
                // TODO large message support
                httpResponse.setContent(ChannelBuffers.copiedBuffer(IOUtils.slurpStream(mk.getEntireMessageBodyAsInputStream(destroyAsRead))));
                if (keepAlive) {
                    httpResponse.setHeader(CONTENT_LENGTH, httpResponse.getContent().readableBytes());
                    httpResponse.setHeader(CONNECTION, "keep-alive");
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "I/O error reading async response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                errorAndClose(HttpResponseStatus.BAD_GATEWAY, "Error reading async response: " + ExceptionUtils.getMessage(e));
                return false;
            } catch (NoSuchPartException e) {
                logger.log(Level.INFO, "No such part error reading async response: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                errorAndClose(HttpResponseStatus.INTERNAL_SERVER_ERROR, "No such part error reading async response: " + ExceptionUtils.getMessage(e));
                return false;
            }
        }

        // TODO HTTP challenges (track down the original http response knob, begin challenge)
        // TODO cookies (check original and new http response knob, collect cookies)
        // TODO extra headers (check response knob and outbound headers knobs for extra headers to include)
        // TODO ensure keepalive connections eventually get cleaned up (netty may already be taking care of this)

        ChannelFuture future = channel.write(httpResponse);
        if (!keepAlive)
            future.addListener(ChannelFutureListener.CLOSE);
        return true;
    }
}
