package com.l7tech.server.log.syslog.impl;

import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;

import java.nio.charset.Charset;

/**
 * MINA IoHandler for Syslog client
 *
 * @author Steve Jones 
 */
class MinaSyslogHandler extends IoHandlerAdapter {

    //- PUBLIC

    /**
     * Handle new session
     */
    @Override
    public void sessionCreated(final IoSession session) throws Exception {
        session.getFilterChain().addLast("codec", CODEC_FILTER);
    }

    /**
     * Handle session open
     */
    @Override
    public void sessionOpened(final IoSession session) throws Exception {
        sessionCallback.call(session, session.toString());
    }

    /**
     * Handle session close
     */
    @Override
    public void sessionClosed(final IoSession session) throws Exception {
        sessionCallback.call(null, session.toString());
    }

    /**
     * Handle exception
     */
    @Override
    public void exceptionCaught(final IoSession session, final Throwable throwable) throws Exception {
        // close session to cause reconnection
        session.close(true);
    }

    /**
     * Checks the session to see if it's connected and available
     *
     * @param session the ioSession to verify
     * @return true when the IO session is connected and ready for use, false otherwise
     */
    public boolean verifySession(IoSession session) {
        return (session != null && session.isConnected() && !session.isClosing());
    }

    //- PACKAGE

    /**
     * Create a handler with a callback for session lifecycle.
     *
     * @param sessionCallback The session open/close callback
     */
    MinaSyslogHandler(final Functions.BinaryVoid<IoSession, String> sessionCallback) {
        this.sessionCallback = sessionCallback;
    }

    //- PRIVATE

    private static IoFilter CODEC_FILTER = new ProtocolCodecFilter( new SyslogCodecFactory() );

    private final Functions.BinaryVoid<IoSession, String> sessionCallback;

    /**
     * Syslog codec factory
     */
    private static class SyslogCodecFactory implements ProtocolCodecFactory {
        private final MinaSyslogTextEncoder encoder;
        private final TextLineDecoder decoder;

        private SyslogCodecFactory() {
            Charset charset = Charsets.UTF8;
            encoder = new MinaSyslogTextEncoder();
            decoder = new TextLineDecoder(charset, LineDelimiter.AUTO);
        }

        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            return encoder;
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            return decoder;
        }
    }
}
