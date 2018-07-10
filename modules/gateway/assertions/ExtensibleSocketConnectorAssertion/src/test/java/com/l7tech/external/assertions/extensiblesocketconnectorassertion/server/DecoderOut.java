package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 04/02/14
 * Time: 2:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class DecoderOut implements ProtocolDecoderOutput {

    private String output = "";

    @Override
    public void write(Object o) {
        output = new String((byte[]) o);
    }

    @Override
    public void flush(IoFilter.NextFilter nextFilter, IoSession session) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getOutput() {
        return output;
    }
}
