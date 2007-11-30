package com.l7tech.server.log.syslog.impl;

import java.nio.charset.Charset;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter;
import org.apache.mina.util.SessionLog;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.slf4j.Logger;
import org.slf4j.Marker;

import com.l7tech.common.util.Functions;

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
    public void sessionCreated(final IoSession session) throws Exception {
        session.setAttribute(SessionLog.LOGGER, new SilentLogger());
        session.getFilterChain().addLast( "codec", CODEC_FILTER);
    }

    /**
     * Handle session open
     */
    public void sessionOpened(final IoSession session) throws Exception {
        sessionCallback.call(session);
    }

    /**
     * Handle session close
     */
    public void sessionClosed(final IoSession session) throws Exception {
        sessionCallback.call(null);
    }

    /**
     * Handle exception
     */
    public void exceptionCaught(final IoSession session, final Throwable throwable) throws Exception {
        // close session to cause reconnection
        session.close();
    }

    //- PACKAGE

    /**
     * Create a handler with a callback for session lifecycle.
     *
     * @param sessionCallback The session open/close callback
     */
    MinaSyslogHandler(final Functions.UnaryVoid<IoSession> sessionCallback) {
        this.sessionCallback = sessionCallback;
    }

    //- PRIVATE

    private static IoFilter CODEC_FILTER = new ProtocolCodecFilter( new SyslogCodecFactory() );

    private final Functions.UnaryVoid<IoSession> sessionCallback;

    /**
     * Syslog codec factory
     */
    private static class SyslogCodecFactory implements ProtocolCodecFactory {
        private final MinaSyslogTextEncoder encoder;
        private final TextLineDecoder decoder;

        private SyslogCodecFactory() {
            Charset charset = Charset.forName("UTF-8");
            encoder = new MinaSyslogTextEncoder();
            decoder = new TextLineDecoder(charset, LineDelimiter.AUTO);
        }

        public ProtocolEncoder getEncoder() throws Exception {
            return encoder;
        }

        public ProtocolDecoder getDecoder() throws Exception {
            return decoder;
        }
    }

    /**
     * Logger to ensure no logging for Syslog communication.
     */
    private static class SilentLogger implements Logger {
        public void debug(Marker marker, String string) {}
        public void debug(Marker marker, String string, Object object) {}
        public void debug(Marker marker, String string, Object object, Object object1) {}
        public void debug(Marker marker, String string, Object[] objects) {}
        public void debug(Marker marker, String string, Throwable throwable) {}
        public void debug(String string) {}
        public void debug(String string, Object object) {}
        public void debug(String string, Object object, Object object1) {}
        public void debug(String string, Object[] objects) {}
        public void debug(String string, Throwable throwable) {}
        public void error(Marker marker, String string) {}
        public void error(Marker marker, String string, Object object) {}
        public void error(Marker marker, String string, Object object, Object object1) {}
        public void error(Marker marker, String string, Object[] objects) {}
        public void error(Marker marker, String string, Throwable throwable) {}
        public void error(String string) {}
        public void error(String string, Object object) {}
        public void error(String string, Object object, Object object1) {}
        public void error(String string, Object[] objects) {}
        public void error(String string, Throwable throwable) {}
        public String getName() { return null; }
        public void info(Marker marker, String string) {}
        public void info(Marker marker, String string, Object object) {}
        public void info(Marker marker, String string, Object object, Object object1) {}
        public void info(Marker marker, String string, Object[] objects) {}
        public void info(Marker marker, String string, Throwable throwable) {}
        public void info(String string) {}
        public void info(String string, Object object) {}
        public void info(String string, Object object, Object object1) {}
        public void info(String string, Object[] objects) {}
        public void info(String string, Throwable throwable) {}
        public boolean isDebugEnabled() { return false; }
        public boolean isDebugEnabled(Marker marker) { return false; }
        public boolean isErrorEnabled() { return false; }
        public boolean isErrorEnabled(Marker marker) { return false; }
        public boolean isInfoEnabled() { return false; }
        public boolean isInfoEnabled(Marker marker) { return false; }
        public boolean isWarnEnabled() { return false; }
        public boolean isWarnEnabled(Marker marker) { return false; }
        public void warn(Marker marker, String string) {}
        public void warn(Marker marker, String string, Object object) {}
        public void warn(Marker marker, String string, Object object, Object object1) {}
        public void warn(Marker marker, String string, Object[] objects) {}
        public void warn(Marker marker, String string, Throwable throwable) {}
        public void warn(String string) {}
        public void warn(String string, Object object) {}
        public void warn(String string, Object object, Object object1) {}
        public void warn(String string, Object[] objects) {}
        public void warn(String string, Throwable throwable) {}
    }
}
