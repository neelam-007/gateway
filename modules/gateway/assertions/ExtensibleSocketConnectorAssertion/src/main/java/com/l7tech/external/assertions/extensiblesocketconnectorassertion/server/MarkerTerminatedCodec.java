package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/12
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarkerTerminatedCodec implements ProtocolCodecFactory, ExtensibleSocketConnectorCodec {
    private byte[] terminatorBytes = new byte[]{(byte) 0x00};

    public byte[] getTerminatorBytes() {
        return terminatorBytes;
    }

    public void setTerminatorBytes(byte[] terminatorBytes) {
        this.terminatorBytes = terminatorBytes;
    }

    public void configureCodec(Object codecConfig) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getTermnatorBytesMethod = codecConfig.getClass().getMethod("getTermnatorBytes");
        Object data = getTermnatorBytesMethod.invoke(codecConfig, null);
        this.terminatorBytes = (byte[]) data;
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

                IoBuffer bb = IoBuffer.allocate(terminatorBytes.length + bytes.length).setAutoExpand(true);
                bb.put(bytes);
                bb.put(terminatorBytes);

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

                // Check for a partial terminator in the outstanding bytes
                int terminatorBytePos = 0;
                if (outstandingBytes != null) {
                    for (int i = outstandingBytes.length - terminatorBytes.length + 1; i < outstandingBytes.length; i++) {
                        if (outstandingBytes[i] == terminatorBytes[terminatorBytePos]) {
                            terminatorBytePos++;
                        }
                    }

                    messageBytes.write(outstandingBytes);
                }

                in.mark();
                while (in.hasRemaining()) {
                    byte b = in.get();
                    if (terminatorBytes[terminatorBytePos] == b) {
                        terminatorBytePos++;
                    } else {
                        terminatorBytePos = 0;
                    }

                    if (terminatorBytePos >= terminatorBytes.length) {
                        byte[] bytes = messageBytes.toByteArray();
                        byte[] message = new byte[bytes.length - terminatorBytes.length + 1];
                        System.arraycopy(bytes, 0, message, 0, message.length);

                        out.write(message);
                        messageBytes.reset();

                        terminatorBytePos = 0;
                    } else {
                        messageBytes.write(b);
                    }
                }

                byte[] bytes = messageBytes.toByteArray();
                if (bytes.length > 0) {
                    ioSession.setAttribute("outstandingBytes", bytes);
                }

                //in.clear();
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
