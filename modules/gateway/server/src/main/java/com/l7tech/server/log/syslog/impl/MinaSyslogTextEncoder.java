package com.l7tech.server.log.syslog.impl;

import com.l7tech.util.Charsets;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * ProtocolEncoder for delimited text with a size limit.
 *
 * <p>The charset, length and delimiter can be supplied as part of the given
 * message (when passed as a TextMessage).</p>
 *
 * @author Steve Jones
*/
class MinaSyslogTextEncoder extends ProtocolEncoderAdapter {

    //- PUBLIC

    /**
     * Encode the message to the given output.
     */
    public void encode( final IoSession session,
                        final Object message,
                        final ProtocolEncoderOutput out ) throws Exception {
        Charset charset;
        String delimiter;
        int maxLength;
        String value;

        if ( message instanceof TextMessage ) {
            TextMessage textMessage = (TextMessage) message;
            charset = textMessage.charset;
            delimiter = textMessage.delimiter;
            maxLength = textMessage.maxLength;
            value = textMessage.message;
        } else {
            charset = DEFAULT_TEXT_MESSAGE.charset;
            delimiter = DEFAULT_TEXT_MESSAGE.delimiter;
            maxLength = DEFAULT_TEXT_MESSAGE.maxLength;
            value = message.toString();
        }

        CharsetEncoder encoder = (CharsetEncoder) session.getAttribute( ENCODER_PREFIX + charset.name() );
        if( encoder == null ) {
            encoder = charset.newEncoder();
            session.setAttribute( ENCODER_PREFIX + charset.name(), encoder );
        }

        IoBuffer buf = IoBuffer.allocate( value.length() ).setAutoExpand( true );
        buf.putString( value, encoder );
        int positionBeforeEnding = buf.position();
        buf.putString( delimiter, encoder );
        if( buf.position() > maxLength ) {
            // reposition with room for ending
            buf.position(maxLength - (buf.position() - positionBeforeEnding));
            buf.putString( delimiter, encoder );
        }
        buf.flip();
        out.write( buf );
    }

    //- PACKAGE

    static final class TextMessage {
        private final Charset charset;
        private final String delimiter;
        private final int maxLength;
        private final String message;

        TextMessage(final Charset charset,
                    final String delimiter,
                    final int maxLength,
                    final String message) {
            this.charset = charset;
            this.delimiter = delimiter;
            this.maxLength = maxLength;
            this.message = message;            
        }
    }

    //- PRIVATE

    private static final String ENCODER_PREFIX = MinaSyslogTextEncoder.class.getName() + ".encoder.";
    private static final TextMessage DEFAULT_TEXT_MESSAGE = new TextMessage(Charsets.UTF8, "\r\n", 1024, null);
}
