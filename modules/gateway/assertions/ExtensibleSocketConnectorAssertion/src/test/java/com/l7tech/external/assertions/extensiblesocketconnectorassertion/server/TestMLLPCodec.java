package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 23/08/13
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestMLLPCodec {

    public static final String MLLP_MESSAGE = "^KMSH|^~\\\\&|MYSENDER|MYRECEIVER|MYAPPLICATION||200612211200||QRY^A19|1234|P|2.4\n" +
            "QRD|200612211200|R|I|GetPatient|||1^RD|0101701234|DEM||\n" +
            "^\\^M";

    /* Test decoding when we do not have a complete message */
    @Test
    public void testDecode1() throws Exception {
        IoSession session = mock(IoSession.class);

        byte[] bytes1 = MLLP_MESSAGE.getBytes();
        byte[] bytes2 = new byte[bytes1.length + 2];
        bytes2[0] = (byte) 0x0b;
        System.arraycopy(bytes1, 0, bytes2, 1, bytes1.length);
        bytes2[bytes2.length - 1] = (byte) 0x1c;

        IoBuffer in = IoBuffer.wrap(bytes2);
        MockProtocolDecoderOutput output = new MockProtocolDecoderOutput();

        MLLPCodec codec = new MLLPCodec();
        ProtocolDecoder decoder = codec.getDecoder(null);

        decoder.decode(session, in, output);
        assertEquals(0, output.getMessages().size());
    }

    /* Test decoding when we have a complete message */
    @Test
    public void testDecode2() throws Exception {
        IoSession session = mock(IoSession.class);

        byte[] bytes1 = MLLP_MESSAGE.getBytes();
        byte[] bytes2 = new byte[bytes1.length + 3];
        bytes2[0] = (byte) 0x0b;
        System.arraycopy(bytes1, 0, bytes2, 1, bytes1.length);
        bytes2[bytes2.length - 2] = (byte) 0x1c;
        bytes2[bytes2.length - 1] = (byte) 0x0d;

        IoBuffer in = IoBuffer.wrap(bytes2);
        MockProtocolDecoderOutput output = new MockProtocolDecoderOutput();

        MLLPCodec codec = new MLLPCodec();
        ProtocolDecoder decoder = codec.getDecoder(null);

        decoder.decode(session, in, output);
        assertEquals(1, output.getMessages().size());
        assertArrayEquals(MLLP_MESSAGE.getBytes(), (byte[]) output.getMessages().get(0));
    }

    /* Test decoding when we have a partial message and then the rest of it */
    @Test
    public void testDecode3() throws Exception {
        final String MLLP_MESSAGEF_PART = "^KMSH|^~\\\\&|MYSENDER|MYRECEIVER|MYAPPLICATION||200612211200||QRY^A19|1";
        ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
        messageBytes.write(MLLP_MESSAGEF_PART.getBytes());

        IoSession session = mock(IoSession.class);
        when(session.getAttribute("outstandingBytes")).thenReturn(null).thenReturn(messageBytes.toByteArray());
        when(session.getAttribute("endByte1Found")).thenReturn(false);

        byte[] bytes1 = MLLP_MESSAGE.getBytes();
        byte[] bytes2 = new byte[bytes1.length / 2 + 1];
        bytes2[0] = (byte) 0x0b;
        System.arraycopy(bytes1, 0, bytes2, 1, bytes1.length / 2);

        IoBuffer in = IoBuffer.wrap(bytes2);
        MockProtocolDecoderOutput output = new MockProtocolDecoderOutput();

        MLLPCodec codec = new MLLPCodec();
        ProtocolDecoder decoder = codec.getDecoder(null);

        decoder.decode(session, in, output);

        bytes2 = new byte[bytes1.length - bytes1.length / 2 + 2];
        System.arraycopy(bytes1, bytes1.length / 2, bytes2, 0, bytes1.length - bytes1.length / 2);
        bytes2[bytes2.length - 2] = (byte) 0x1c;
        bytes2[bytes2.length - 1] = (byte) 0x0d;
        in = IoBuffer.wrap(bytes2);
        decoder.decode(session, in, output);

        assertEquals(1, output.getMessages().size());
        assertArrayEquals(MLLP_MESSAGE.getBytes(), (byte[]) output.getMessages().get(0));
    }

    /* Test decoding when we have a partial message and then the rest of it */
    @Test
    public void testDecode4() throws Exception {
        ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
        messageBytes.write(MLLP_MESSAGE.getBytes());

        IoSession session = mock(IoSession.class);
        when(session.getAttribute("outstandingBytes")).thenReturn(null).thenReturn(messageBytes.toByteArray());
        when(session.getAttribute("endByte1Found")).thenReturn(true);

        byte[] bytes1 = MLLP_MESSAGE.getBytes();
        byte[] bytes2 = new byte[bytes1.length + 2];
        bytes2[0] = (byte) 0x0b;
        System.arraycopy(bytes1, 0, bytes2, 1, bytes1.length);
        bytes2[bytes2.length - 1] = (byte) 0x1c;

        IoBuffer in = IoBuffer.wrap(bytes2);
        MockProtocolDecoderOutput output = new MockProtocolDecoderOutput();

        MLLPCodec codec = new MLLPCodec();
        ProtocolDecoder decoder = codec.getDecoder(null);

        decoder.decode(session, in, output);

        bytes2 = new byte[]{(byte) 0x0d};
        in = IoBuffer.wrap(bytes2);
        decoder.decode(session, in, output);

        assertEquals(1, output.getMessages().size());
        assertArrayEquals(MLLP_MESSAGE.getBytes(), (byte[]) output.getMessages().get(0));
    }

    /* Test decoding when we have a 2 full messages */
    @Test
    public void testDecode5() throws Exception {
        IoSession session = mock(IoSession.class);

        byte[] bytes1 = MLLP_MESSAGE.getBytes();
        byte[] bytes2 = new byte[bytes1.length * 2 + 6];
        bytes2[0] = (byte) 0x0b;
        System.arraycopy(bytes1, 0, bytes2, 1, bytes1.length);
        bytes2[bytes1.length + 1] = (byte) 0x1c;
        bytes2[bytes1.length + 2] = (byte) 0x0d;
        bytes2[bytes1.length + 3] = (byte) 0x0b;
        System.arraycopy(bytes1, 0, bytes2, bytes1.length + 4, bytes1.length);
        bytes2[bytes2.length - 2] = (byte) 0x1c;
        bytes2[bytes2.length - 1] = (byte) 0x0d;

        IoBuffer in = IoBuffer.wrap(bytes2);
        MockProtocolDecoderOutput output = new MockProtocolDecoderOutput();

        MLLPCodec codec = new MLLPCodec();
        ProtocolDecoder decoder = codec.getDecoder(null);

        decoder.decode(session, in, output);

        assertEquals(2, output.getMessages().size());
        assertArrayEquals(MLLP_MESSAGE.getBytes(), (byte[]) output.getMessages().get(0));
        assertArrayEquals(MLLP_MESSAGE.getBytes(), (byte[]) output.getMessages().get(1));
    }

    /**
     * Created with IntelliJ IDEA.
     * User: njordan
     * Date: 23/08/13
     * Time: 10:23 AM
     * To change this template use File | Settings | File Templates.
     */
    public static class MockProtocolDecoderOutput implements ProtocolDecoderOutput {
        private ArrayList<Object> messages = new ArrayList<Object>();

        @Override
        public void write(Object o) {
            messages.add(o);
        }

        @Override
        public void flush(IoFilter.NextFilter nextFilter, IoSession session) {
        }

        public List<Object> getMessages() {
            return messages;
        }
    }
}
