package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/19/11
 * Time: 12:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLStreamEncoder implements ProtocolEncoder {
    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if(message instanceof byte[]) {
            byte[] bytes = (byte[])message;
            out.write(IoBuffer.wrap(bytes));
        }
    }

    @Override
    public void dispose(IoSession session) throws Exception {
        // Don't do anything
    }
}
