package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.IcapResponse;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;

import java.io.IOException;

/**
 * An extension of <tt>SimpleChannelUpstreamHandler</tt> to include two new methods for scanning contents and
 * sending the OPTIONS command to the ICAP server.
 *
 * @author Ken Diep
 */
public abstract class AbstractIcapResponseHandler extends IdleStateAwareChannelHandler {

    /**
     * Send the options command to the ICAP server.
     *
     * @param icapUri the fully qualified URI of the ICAP server.
     * @param host    the ICAP host name.
     * @return the server's response.
     */
    public abstract IcapResponse sendOptionsCommand(final String icapUri, final String host);

    /**
     * Send the server a content to be scanned.
     *
     * @param icapUri  the fully qualified URI of the ICAP server.
     * @param host     the ICAP host name.
     * @param partInfo the part to be scanned.
     * @return the server's response.
     * @throws NoSuchPartException if a part can not be retrieved from the given <tt>partInfo</tt>.
     * @throws IOException         if any IO error(s) occur while reading the part or if any network error(s) occur.
     */
    public abstract IcapResponse scan(final String icapUri, final String host, PartInfo partInfo) throws NoSuchPartException, IOException;
}
