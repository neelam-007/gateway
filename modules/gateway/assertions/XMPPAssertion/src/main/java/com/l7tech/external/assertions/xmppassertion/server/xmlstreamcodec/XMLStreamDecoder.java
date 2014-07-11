package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * This is from the Apache Vysper project (org.apache.vysper.xml.decoder.XMPPDecoder).
 * Trying to minimize changes from the original file so that we could switch
 * to using the vysper library.
 */
public class XMLStreamDecoder extends CumulativeProtocolDecoder {
    public static final String SESSION_ATTRIBUTE_NAME = "xmlStreamParser";

    private boolean inbound;

    public XMLStreamDecoder(boolean inbound) {
        this.inbound = inbound;
    }

    @Override
    protected boolean doDecode(final IoSession session, IoBuffer in, final ProtocolDecoderOutput out) throws Exception {
        TokenHandler tokenHandler = (TokenHandler)session.getAttribute(SESSION_ATTRIBUTE_NAME);

        if(tokenHandler == null) {
            tokenHandler = new TokenHandler(
                    new XMLStreamContentHandler(session, new XMPPCodecConfiguration(), inbound, new SessionHandler() {
                        @Override
                        public void addDataFragment(byte[] bytes) {
                            out.write(bytes);
                        }

                        @Override
                        public void sessionTerminated() {
                            session.setAttribute("com.l7tech.extensible.socket.close", Boolean.TRUE);
                        }
                    })
            );
            session.setAttribute(SESSION_ATTRIBUTE_NAME, tokenHandler);
        }

        tokenHandler.getContentHandler().reset(in);

        int pos = in.position();
        byte[] bytes = new byte[in.remaining()];
        in.get(bytes);
        in.position(pos);

        tokenHandler.parse(in);

        return tokenHandler.getContentHandler().storeUnwrittenData();
    }
}
