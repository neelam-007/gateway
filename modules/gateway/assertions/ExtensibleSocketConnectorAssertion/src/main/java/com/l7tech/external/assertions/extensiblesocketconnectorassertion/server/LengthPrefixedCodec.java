package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/11/12
 * Time: 4:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class LengthPrefixedCodec implements ProtocolCodecFactory, ExtensibleSocketConnectorCodec {
    private byte lengthBytes = 4;

    public byte getLengthBytes() {
        return lengthBytes;
    }

    public void setLengthBytes(byte lengthBytes) {
        this.lengthBytes = lengthBytes;
    }

    @Override
    public void configureCodec(Object codecConfig) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        //lengthBytes = codecConfig.getLengthBytes();
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

                IoBuffer bb = IoBuffer.allocate((int) lengthBytes + bytes.length).setAutoExpand(true);
                if (lengthBytes > 3) {
                    bb.put((byte) (bytes.length >>> 24));
                }
                if (lengthBytes > 2) {
                    bb.put((byte) (bytes.length >>> 16));
                }
                if (lengthBytes > 1) {
                    bb.put((byte) (bytes.length >>> 8));
                }
                bb.put((byte) bytes.length);
                bb.put(bytes);

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
                int lengthBytesRead = 0;
                if (ioSession.containsAttribute("lengthBytesRead")) {
                    lengthBytesRead = (Integer) ioSession.getAttribute("lengthBytesRead");
                }

                int remainingMessageSize = 0;
                if (ioSession.containsAttribute("remainingMessageSize")) {
                    remainingMessageSize = (Integer) ioSession.getAttribute("remainingMessageSize");
                }

                ByteArrayOutputStream baos = (ByteArrayOutputStream) ioSession.getAttribute("previouslyReadBytes");
                if (baos == null) {
                    baos = new ByteArrayOutputStream();
                    ioSession.setAttribute("previouslyReadBytes", baos);
                }

                while (in.hasRemaining()) {
                    if (lengthBytesRead < lengthBytes) {
                        remainingMessageSize = (remainingMessageSize << 8) | in.get();
                        lengthBytesRead++;
                    } else {
                        if (in.remaining() >= remainingMessageSize) { // We have up to the end of a message
                            byte[] bytes = new byte[remainingMessageSize];
                            in.get(bytes);
                            baos.write(bytes);

                            out.write(baos.toByteArray());
                            baos.reset();
                            remainingMessageSize = 0;
                            lengthBytesRead = 0;
                        } else { // We do not have enough bytes to complete the current message
                            byte[] bytes = new byte[in.remaining()];
                            in.get(bytes);
                            baos.write(bytes);
                            remainingMessageSize -= bytes.length;
                        }
                    }
                }

                ioSession.setAttribute("lengthBytesRead", lengthBytesRead);
                ioSession.setAttribute("remainingMessageSize", remainingMessageSize);
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
