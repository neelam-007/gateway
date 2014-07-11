package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMLStreamCodecConfiguration;
import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMLStreamCodecFactory;
import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPCodecConfiguration;
import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPIoHandlerAdapter;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.SocketAddress;

/**
 * User: njordan
 * Date: 20/06/12
 * Time: 10:20 AM
 */
public class MockXMPPClassHelper implements XMPPClassHelper {
    protected static class MockClassHelperFactory extends XMPPClassHelperFactory {
        @Override
        public XMPPClassHelper createClassHelper() {
            return new MockXMPPClassHelper();
        }
    }

    @Override
    public void closeSession(Object session) {
        ((IoSession)session).close(true);
    }

    @Override
    public Object getService(Object session) {
        return ((IoSession)session).getService();
    }

    @Override
    public Long getSessionId(Object session) {
        return ((IoSession)session).getId();
    }

    @Override
    public Object ioSessionGetFilterChain(Object service) {
        return ((IoService)service).getFilterChain();
    }

    @Override
    public Object nioSocketAcceptorGetFilterChain(Object service) {
        return ((NioSocketAcceptor)service).getFilterChain();
    }

    @Override
    public Object nioSocketConnectorGetFilterChain(Object service) {
        return ((NioSocketConnector)service).getFilterChain();
    }

    @Override
    public Object createNioSocketAcceptor() {
        return new NioSocketAcceptor();
    }

    @Override
    public void addFirstToFilterChain(Object filterChain, String name, Object filter) {
        ((DefaultIoFilterChain)filterChain).addFirst(name, (IoFilter)filter);
    }

    @Override
    public void addLastToFilterChainBuilder(Object filterChainBuilder, String name, Object filter) {
        ((DefaultIoFilterChainBuilder)filterChainBuilder).addLast(name, (IoFilter)filter);
    }

    @Override
    public Object filterChainGet(Object filterChain, String name) {
        return ((DefaultIoFilterChain)filterChain).get(name);
    }

    @Override
    public Object createXmppCodecConfiguration() {
        return new XMPPCodecConfiguration();
    }

    @Override
    public Object createXmlStreamCodecFactory(Object configuration, boolean inbound) {
        return new XMLStreamCodecFactory((XMLStreamCodecConfiguration)configuration, inbound);
    }

    @Override
    public Object createProtocolCodecFilter(Object protocolCodecFactory) {
        return new ProtocolCodecFilter((ProtocolCodecFactory)protocolCodecFactory);
    }

    @Override
    public Object createExecutorFilter(int maximumPoolSize) {
        return new ExecutorFilter(maximumPoolSize);
    }

    @Override
    public Object createXmppIoHandlerAdapter(Object targetObject) {
        return new XMPPIoHandlerAdapter(targetObject);
    }

    @Override
    public SocketAddress getLocalAddress(Object session) {
        return ((IoSession)session).getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress(Object session) {
        return ((IoSession)session).getRemoteAddress();
    }

    @Override
    public void ioSessionWrite(Object session, byte[] bytes) {
        ((IoSession)session).write(bytes);
    }

    @Override
    public void ioServiceSetHandler(Object service, Object handler) {
        ((IoService)service).setHandler((IoHandler)handler);
    }

    @Override
    public Object ioServiceGetSessionConfig(Object acceptor) {
        return ((IoService)acceptor).getSessionConfig();
    }

    @Override
    public Object getIdleStatus_BOTH_IDLE() {
        return IdleStatus.BOTH_IDLE;
    }

    @Override
    public void ioSessionConfigSetReadBufferSize(Object sessionConfig, int i) {
        ((IoSessionConfig)sessionConfig).setReadBufferSize(i);
    }

    @Override
    public void ioSessionConfigSetIdleTime(Object sessionConfig, Object idleTime, int i) {
        ((IoSessionConfig)sessionConfig).setIdleTime((IdleStatus)idleTime, i);
    }

    @Override
    public void ioAcceptorBind(Object acceptor, SocketAddress address) {
        try {
            ((IoAcceptor)acceptor).bind(address);
        } catch(IOException e) {
            // Ignore
        }
    }

    @Override
    public void ioAcceptorDispose(Object acceptor, boolean b) {
        ((IoAcceptor)acceptor).dispose(b);
    }

    @Override
    public Object createNioSocketConnector() {
        return new NioSocketConnector();
    }

    @Override
    public Object nioSocketConnectorConnect(Object connector, SocketAddress address) {
        return ((NioSocketConnector)connector).connect(address);
    }

    @Override
    public void connectFutureAwaitUninterruptibly(Object future) {
        ((ConnectFuture)future).awaitUninterruptibly();
    }

    @Override
    public Object createSslFilter(SSLContext sslContext) {
        return new SslFilter(sslContext);
    }

    @Override
    public void sslFilterSetEnabledCipherSuites(Object sslFilter, String[] suites) {
        ((SslFilter)sslFilter).setEnabledCipherSuites(suites);
    }

    @Override
    public void sslFilterSetUseClientMode(Object sslFilter, boolean b) {
        ((SslFilter)sslFilter).setUseClientMode(b);
    }

    @Override
    public void sslFilterSetNeedClientAuth(Object sslFilter, boolean b) {
        ((SslFilter)sslFilter).setNeedClientAuth(b);
    }

    @Override
    public void sslFilterSetWantClientAuth(Object sslFilter, boolean b) {
        ((SslFilter)sslFilter).setWantClientAuth(b);
    }

    @Override
    public String ioSessionGetAttribute(Object session, Object key) {
        return (String)((IoSession)session).getAttribute(key);
    }

    @Override
    public void ioSessionSetAttribute(Object session, Object key, Object value) {
        ((IoSession)session).setAttribute(key, value);
    }

    @Override
    public Object getSslFilter_DISABLE_ENCRYPTION_ONCE() {
        return SslFilter.DISABLE_ENCRYPTION_ONCE;
    }

    @Override
    public SSLSession sslFilterGetSslSession(Object sslFilter, Object sslSession) {
        return ((SslFilter)sslFilter).getSslSession((IoSession)sslSession);
    }
}
