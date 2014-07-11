package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPMinaClassException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.SocketAddress;

/**
 * Access to all of the Apache Mina classes and the com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec
 * package must be done through instances of this interface. This allows the use of a different ClassLoader
 * for all of those classes.
 *
 * If the version of Apache Mina in the core product is updated to a 2.0.x release then this will not be necessary
 * anymore. See the MockXMPPClassHelper class to easily understand the intentions of the methods in this class.
 */
public interface XMPPClassHelper {
    /**
     * Calls ((IoSession)session).close(true)
     * @param session The session to close
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void closeSession(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((IoSession)session).getService()
     * @param session to get the service from
     * @return An IoService object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object getService(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((IoSession)session).getId()
     * @param session The session to get the ID for
     * @return The session's ID
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Long getSessionId(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((IoService)service).getFilterChain()
     * @param service The IoService to get the filter chain for
     * @return The DefaultIoFilterChain for the service
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object ioSessionGetFilterChain(Object service) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((NioSocketAcceptor)service).getFilterChain()
     * @param service The NIO socket acceptor to get the filter chain for
     * @return The DefaultIoFilterChain for service
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object nioSocketAcceptorGetFilterChain(Object service) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((NioSocketConnector)service).getFilterChain()
     * @param service The NIO socket connector to get the filter chain for
     * @return The DefaultIoFilterChain for service
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object nioSocketConnectorGetFilterChain(Object service) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns new NioSocketAcceptor()
     * @return a new NioSocketAcceptor object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createNioSocketAcceptor() throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((DefaultIoFilterChain)filterChain).addFirst(name, (IoFilter)filter)
     * @param filterChain The DefaultIoFilterChain to add to
     * @param name The name to assign to the added filter
     * @param filter An IoFilter to add to the filter chain
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void addFirstToFilterChain(Object filterChain, String name, Object filter) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((DefaultIoFilterChainBuilder)filterChainBuilder).addLast(name, (IoFilter)filter)
     * @param filterChainBuilder The DefaultIoFilterChainBuilder to add to
     * @param name The name to assign to the added filter
     * @param filter An IoFilter to add to the filter chain builder
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void addLastToFilterChainBuilder(Object filterChainBuilder, String name, Object filter) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((DefaultIoFilterChain)filterChain).get(name)
     * @param filterChain The DefaultIoFilterChain object to get the filter from
     * @param name The name of the filter to retrieve
     * @return An IoFilter object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object filterChainGet(Object filterChain, String name) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Retuns new XMPPCodecConfiguration()
     * @return A new XMPPCodecConfiguration object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createXmppCodecConfiguration() throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns new XMLStreamCodecFactory((XMLStreamCodecConfiguration)configuration, inbound)
     * @param configuration The XMLStreamCodecConfiguration object to base the codec factory on
     * @param inbound True if this is for an inbound listener
     * @return A new XMLStreamCodecFactory object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createXmlStreamCodecFactory(Object configuration, boolean inbound) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns new ProtocolCodecFilter((ProtocolCodecFactory)protocolCodecFactory)
     * @param protocolCodecFactory The ProtocolCodecFactory to base the new filter on
     * @return A new ProtocolCodecFilter object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createProtocolCodecFilter(Object protocolCodecFactory) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns new ExecutorFilter(maximumPoolSize)
     * @param maximumPoolSize The maximum size of the thread pool
     * @return A new ExecutorFilter object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createExecutorFilter(int maximumPoolSize) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns Returns new XMPPIoHandlerAdapter(targetObject)
     * @param targetObject The object that will be called when events happen
     * @return A new XMPPIoHandlerAdapter object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createXmppIoHandlerAdapter(Object targetObject) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((IoSession)session).getLocalAddress()
     * @param session The IoSession to get the local address from
     * @return A SocketAddress object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    SocketAddress getLocalAddress(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((IoSession)session).getRemoteAddress()
     * @param session The IoSession to get the remote address from
     * @return A SocketAddress object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    SocketAddress getRemoteAddress(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((IoSession)session).write(bytes)
     * @param session The IoSession to write the bytes to
     * @param bytes The bytes to write
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioSessionWrite(Object session, byte[] bytes) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((IoService)service).setHandler((IoHandler)handler)
     * @param service The IoService to set the handler for
     * @param handler The IoHandler to use
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioServiceSetHandler(Object service, Object handler) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((IoService)acceptor).getSessionConfig()
     * @param acceptor The IoService to get the session configuration for
     * @return An IoSessionConfig object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object ioServiceGetSessionConfig(Object acceptor) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns IdleStatus.BOTH_IDLE
     * @return IdleStatus.BOTH_IDLE
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     */
    Object getIdleStatus_BOTH_IDLE() throws XMPPClassHelperNotInitializedException;

    /**
     * Calls ((IoSessionConfig)sessionConfig).setReadBufferSize(i)
     * @param sessionConfig The IoSessionConfig to update
     * @param i The new read buffer size
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioSessionConfigSetReadBufferSize(Object sessionConfig, int i) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((IoSessionConfig)sessionConfig).setIdleTime((IdleStatus)idleTime, i)
     * @param sessionConfig The IoSessionConfig to update
     * @param idleTime An IdleTime object to indicate which sides are affected
     * @param i The new value of the idle time
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioSessionConfigSetIdleTime(Object sessionConfig, Object idleTime, int i) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((IoAcceptor)acceptor).bind(address)
     * @param acceptor The IoAcceptor to call bind on
     * @param address The SocketAddress to bind to
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioAcceptorBind(Object acceptor, SocketAddress address) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((IoAcceptor)acceptor).dispose(b)
     * @param acceptor The IoAcceptor to dispose
     * @param b Whether to dispose immediately or not
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioAcceptorDispose(Object acceptor, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns new NioSocketConnector()
     * @return A new NioSocketConnector object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createNioSocketConnector() throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns ((NioSocketConnector)connector).connect(address)
     * @param connector The NioSocketConnector to call connect on
     * @param address The SocketAddress to connect to
     * @return A new ConnectFuture object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object nioSocketConnectorConnect(Object connector, SocketAddress address) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((ConnectFuture)future).awaitUninterruptibly()
     * @param future The ConnectFuture object to wait on
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void connectFutureAwaitUninterruptibly(Object future) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns new SslFilter(sslContext)
     * @param sslContext The SSLContext to use to configure the new SslFilter
     * @return A new SslFilter object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    Object createSslFilter(SSLContext sslContext) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((SslFilter)sslFilter).setEnabledCipherSuites(suites)
     * @param sslFilter The SslFilter to update
     * @param suites The list of enabled cipher suites
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void sslFilterSetEnabledCipherSuites(Object sslFilter, String[] suites) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((SslFilter)sslFilter).setUseClientMode(b)
     * @param sslFilter The SslFilter to update
     * @param b Whether to use client mode or not
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void sslFilterSetUseClientMode(Object sslFilter, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((SslFilter)sslFilter).setNeedClientAuth(b)
     * @param sslFilter The SslFilter to update
     * @param b Whether client authentication is required or not
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void sslFilterSetNeedClientAuth(Object sslFilter, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((SslFilter)sslFilter).setWantClientAuth(b)
     * @param sslFilter The SslFilter to update
     * @param b Whether client authentication is available or not
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void sslFilterSetWantClientAuth(Object sslFilter, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns IoSession.getAttribute(name)
     * @param session The session to get the attribute from
     * @param key The name of the attribute to get
     * @return The String value of the specified session attribute
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     */
    String ioSessionGetAttribute(Object session, Object key) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Calls ((IoSession)session).setAttribute(key, value)
     * @param session The IoSession to update
     * @param key The name of the attribute
     * @param value The value of the attribute
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    void ioSessionSetAttribute(Object session, Object key, Object value) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;

    /**
     * Returns SslFilter.DISABLE_ENCRYPTION_ONCE
     * @return SslFilter.DISABLE_ENCRYPTION_ONCE
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     */
    Object getSslFilter_DISABLE_ENCRYPTION_ONCE() throws XMPPClassHelperNotInitializedException;

    /**
     * Returns ((SslFilter)sslFilter).getSslSession((IoSession)sslSession)
     * @param sslFilter The SslFilter to use for looking up the SSLSession
     * @param sslSession The IoSession to get the SSLSession from
     * @return The SSLSession object
     * @throws XMPPClassHelperNotInitializedException If this XMPPClassHelper was not initialized properly
     * @throws XMPPMinaClassException If there was a problem accessing the classes in the other ClassLoader
     */
    SSLSession sslFilterGetSslSession(Object sslFilter, Object sslSession) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException;
}
