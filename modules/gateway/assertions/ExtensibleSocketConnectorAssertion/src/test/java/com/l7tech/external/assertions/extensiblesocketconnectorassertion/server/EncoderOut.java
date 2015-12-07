package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 04/02/14
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class EncoderOut implements ProtocolEncoderOutput {

    private byte[] output = null;

    @Override
    public void write(Object encodedMessage) {
        IoBuffer ioBuffer = (IoBuffer) encodedMessage;
        output = new byte[ioBuffer.limit()];
        ioBuffer.get(output);
    }

    @Override
    public void mergeAll() {
    }

    @Override
    public WriteFuture flush() {
        return null;
    }

    public byte[] getOutput() {
        return output;
    }
}
