package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/12
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class MLLPCodec implements ProtocolCodecFactory, ExtensibleSocketConnectorCodec {
    private byte startByte = (byte) 0x0b;
    private byte endByte1 = (byte) 0x1c;
    private byte endByte2 = (byte) 0x0d;

    public byte getStartByte() {
        return startByte;
    }

    public void setStartByte(byte startByte) {
        this.startByte = startByte;
    }

    public byte getEndByte1() {
        return endByte1;
    }

    public void setEndByte1(byte endByte1) {
        this.endByte1 = endByte1;
    }

    public byte getEndByte2() {
        return endByte2;
    }

    public void setEndByte2(byte endByte2) {
        this.endByte2 = endByte2;
    }

    @Override
    public void configureCodec(Object codecConfig) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        /*
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

                IoBuffer bb = IoBuffer.allocate(3 + bytes.length).setAutoExpand(true);
                bb.put(startByte);
                bb.put(bytes);
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
                byte[] outstandingBytes = (byte[]) ioSession.getAttribute("outstandingBytes");
                ioSession.removeAttribute("outstandingBytes");
                ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();

                boolean endByte1Found = false;

                if (outstandingBytes == null) {
                    // Skip everything up to and including the start byte
                    while (in.hasRemaining()) {
                        byte b = in.get();
                        if (b == startByte) {
                            break;
                        }
                    }
                } else {
                    messageBytes.write(outstandingBytes);

                    Boolean b = (Boolean) ioSession.getAttribute("endByte1Found");
                    if (b != null && b) {
                        endByte1Found = true;
                    }
                }

                while (in.hasRemaining()) {
                    byte b = in.get();

                    if (endByte1Found) {
                        if (b == endByte2) {
                            out.write(messageBytes.toByteArray());
                            messageBytes.reset();
                            endByte1Found = false;

                            // Skip everything up and including start byte
                            while (in.hasRemaining()) {
                                byte x = in.get();
                                if (x == startByte) {
                                    break;
                                }
                            }
                        } else {
                            messageBytes.write(endByte1);
                            endByte1Found = false;
                        }
                    } else if (b == endByte1) {
                        endByte1Found = true;
                    } else {
                        messageBytes.write(b);
                    }
                }

                if (messageBytes.size() > 0) {
                    ioSession.setAttribute("outstandingBytes", messageBytes.toByteArray());
                    ioSession.setAttribute("endByte1Found", endByte1Found);
                } else {
                    ioSession.setAttribute("outstandingBytes", null);
                    ioSession.setAttribute("endByte1Found", null);
                }
            }

            @Override
            public void finishDecode(IoSession ioSession, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {
            }

            @Override
            public void dispose(IoSession ioSession) throws Exception {
            }
        };
    }
}
