package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.IcapResponse;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * An extension of <tt>SimpleChannelUpstreamHandler</tt> to include two new methods for scanning contents and
 * sending the OPTIONS command to the ICAP server.
 *
 * @author Ken Diep
 */
public abstract class AbstractIcapResponseHandler extends SimpleChannelUpstreamHandler {

    public abstract IcapResponse getResponse();
}
