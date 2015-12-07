package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;

//import org.apache.mina.common.ByteBuffer;

//import org.apache.mina.common.ByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 4/2/13
 * Time: 10:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class HL7Codec implements ProtocolCodecFactory, ExtensibleSocketConnectorCodec {

    private String charset = "ISO-8859-1";
    private byte startByte = 0x0b;
    private byte endByte1 = 0x1c;
    private byte endByte2 = 0x0d;

    private static final int STATE_OUT_OF_MESSAGE = 1;
    private static final int STATE_AFTER_START_BYTE = 2;
    private static final int STATE_AFTER_END_BYTE_1 = 3;

    public HL7Codec() {
    }

    @Override
    public void configureCodec(Object codecConfig) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        //To change body of implemented methods use File | Settings | File Templates.
        /*
        charset = codecConfig.getCharset();
        startByte = codecConfig.getStartByte();
        endByte1 = codecConfig.getEndByte1();
        endByte2 = codecConfig.getEndByte2();
        */
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return new ProtocolEncoder() {

            @Override
            public void encode(IoSession ioSession, Object message, ProtocolEncoderOutput out) throws Exception {
                if (message == null) {
                    throw new IllegalArgumentException("Message to encode is null");
                } else if (message instanceof Exception) {
                    throw (Exception) message;
                }

                byte[] bytes;
                if (message instanceof String) {
                    bytes = ((String) message).getBytes();
                } else if (message instanceof byte[]) {
                    bytes = (byte[]) message;
                } else {
                    throw new IllegalArgumentException("The message to encode is not a supported type: " +
                            message.getClass().getCanonicalName());
                }

                // Marshal. XML -> HL7.
                //
                HL7Marshaler marshaler = new HL7Marshaler(charset);
                byte[] hl7 = marshaler.marshal(bytes);

                // Allocate start byte + end byte 1 + end byte 2 + message
                //
                IoBuffer bb = IoBuffer.allocate(3 + hl7.length).setAutoExpand(true);
                bb.put(startByte);
                bb.put(hl7);
                bb.put(endByte1);
                bb.put(endByte2);

                bb.flip();
                out.write(bb);
            }

            @Override
            public void dispose(IoSession ioSession) throws Exception {
            }
        };
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return new ProtocolDecoder() {

            @Override
            public void decode(IoSession ioSession, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
                int state = STATE_OUT_OF_MESSAGE;
                if (ioSession.containsAttribute("currentMessageState")) {
                    state = (Integer) ioSession.getAttribute("currentMessageState");
                }

                ByteArrayOutputStream baos = (ByteArrayOutputStream) ioSession.getAttribute("previouslyReadBytes");
                if (baos == null) {
                    baos = new ByteArrayOutputStream();
                    ioSession.setAttribute("previouslyReadBytes", baos);
                }

                while (in.hasRemaining()) {
                    byte b = in.get();

                    switch (state) {
                        case STATE_OUT_OF_MESSAGE:
                            if (b == startByte) {
                                state = STATE_AFTER_START_BYTE;
                            }
                            break;
                        case STATE_AFTER_START_BYTE:
                            if (b == endByte1) {
                                state = STATE_AFTER_END_BYTE_1;
                            } else {
                                baos.write(b);
                            }
                            break;
                        case STATE_AFTER_END_BYTE_1:
                            if (b == endByte2) {
                                byte[] bytes = baos.toByteArray();

                                // Unmarshal. HL7 -> XML.
                                //
                                HL7Marshaler marshaler = new HL7Marshaler(charset);
                                byte[] xml = marshaler.unmarshal(bytes);

                                out.write(xml);

                                state = STATE_OUT_OF_MESSAGE;
                                baos.reset();
                            } else {
                                baos.write(endByte1);
                                baos.write(b);
                            }
                    }
                }

                in.clear();

                ioSession.setAttribute("currentMessageState", state);
            }

            @Override
            public void finishDecode(IoSession ioSession, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {
            }

            @Override
            public void dispose(IoSession ioSession) throws Exception {
            }
        };
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }

    public void setStartByte(byte startByte) {
        this.startByte = startByte;
    }

    public byte getStartByte() {
        return startByte;
    }

    public void setEndByte1(byte endByte1) {
        this.endByte1 = endByte1;
    }

    public byte getEndByte1() {
        return endByte1;
    }

    public void setEndByte2(byte endByte2) {
        this.endByte2 = endByte2;
    }

    public byte getEndByte2() {
        return endByte2;
    }
}