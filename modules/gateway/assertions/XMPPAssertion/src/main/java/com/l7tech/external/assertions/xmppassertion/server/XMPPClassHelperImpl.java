package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPMinaClassException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trying to hide the reflection in this class.
 */
public class XMPPClassHelperImpl implements XMPPClassHelper {
    private static final Logger logger = Logger.getLogger(XMPPClassHelperImpl.class.getName());

    private ClassLoader classLoader;

    private Class ioSessionClass;
    private Method ioSessionGetFilterChainMethod;
    private Method ioSessionCloseMethod;
    private Method ioSessionGetServiceMethod;
    private Method ioSessionGetIdMethod;
    private Method ioSessionGetLocalAddressMethod;
    private Method ioSessionGetRemoteAddressMethod;
    private Method ioSessionWriteMethod;
    private Method ioSessionGetAttributeMethod;
    private Method ioSessionSetAttributeMethod;

    private Class ioServiceClass;
    private Method ioServiceSetHandlerMethod;
    private Method ioServiceGetSessionConfigMethod;

    private Class ioAcceptorClass;
    private Method ioAcceptorBindMethod;
    private Method ioAcceptorDisposeMethod;

    private Class nioSocketAcceptorClass;
    private Constructor nioSocketAcceptorConstructor;
    private Method nioSocketAcceptorGetFilterChainMethod;

    private Class ioFilterClass;

    private Class defaultIoFilterChainBuilderClass;
    private Method filterChainBuilderAddLastMethod;

    private Class defaultIoFilterChainClass;
    private Method filterChainAddFirstMethod;
    private Method filterChainAddLastMethod;
    private Method filterChainGetMethod;

    private Class xmppCodecConfigurationClass;
    private Constructor xmppCodecConfigurationConstructor;

    private Class xmlStreamCodecConfigurationClass;

    private Class xmlStreamCodecFactoryClass;
    private Constructor xmlStreamCodecFactoryConstructor;

    private Class protocolCodecFactoryClass;

    private Class protocolCodecFilterClass;
    private Constructor protocolCodecFilterConstructor;

    private Class executorFilterClass;
    private Constructor executorFilterConstructor;

    private Class xmppIoHandlerAdapterClass;
    private Constructor xmppIoHandlerAdapterConstructor;

    private Class ioHandlerClass;

    private Class idleStatusClass;
    private Object idleStatus_BOTH_IDLE;

    private Class ioSessionConfigClass;
    private Method ioSessionConfigSetReadBufferSizeMethod;
    private Method ioSessionConfigSetIdleTimeMethod;

    private Class nioSocketConnectorClass;
    private Constructor nioSocketConnectorConstructor;
    private Method nioSocketConnectorGetFilterChainMethod;
    private Method nioSocketConnectorConnectMethod;

    private Class connectFutureClass;
    private Method connectFutureAwaitUninterruptiblyMethod;

    private Class sslFilterClass;
    private Constructor sslFilterConstructor;
    private Method sslFilterSetEnabledCipherSuitesMethod;
    private Method sslFilterSetUseClientModeMethod;
    private Method sslFilterSetNeedClientAuthMethod;
    private Method sslFilterSetWantClientAuthMethod;
    private Method sslFilterGetSslSessionMethod;
    private Object sslFilter_DISABLE_ENCRYPTION_ONCE;

    boolean initialized = false;

    public XMPPClassHelperImpl() {
        HashSet<Class> classesFromCurrentCL = new HashSet<Class>();
        classesFromCurrentCL.add(org.slf4j.Logger.class);
        classesFromCurrentCL.add(org.slf4j.LoggerFactory.class);

        classLoader = new XMPPClassLoader(
                XMPPClassHelperImpl.class.getClassLoader().getParent().getParent(),
                classesFromCurrentCL,
                new String[] {
                        "com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec"
                }
        );

        try {
            ioSessionClass = Class.forName("org.apache.mina.core.session.IoSession", true, classLoader);
            ioSessionGetFilterChainMethod = ioSessionClass.getMethod("getFilterChain");
            ioSessionCloseMethod = ioSessionClass.getMethod("close", Boolean.TYPE);
            ioSessionGetServiceMethod = ioSessionClass.getMethod("getService");
            ioSessionGetIdMethod = ioSessionClass.getMethod("getId");
            ioSessionGetLocalAddressMethod = ioSessionClass.getMethod("getLocalAddress");
            ioSessionGetRemoteAddressMethod = ioSessionClass.getMethod("getRemoteAddress");
            ioSessionWriteMethod = ioSessionClass.getMethod("write", Object.class);
            ioSessionGetAttributeMethod = ioSessionClass.getMethod("getAttribute", Object.class);
            ioSessionSetAttributeMethod = ioSessionClass.getMethod("setAttribute", Object.class, Object.class);

            ioHandlerClass = Class.forName("org.apache.mina.core.service.IoHandler", true, classLoader);

            ioServiceClass = Class.forName("org.apache.mina.core.service.IoService", true, classLoader);
            ioServiceSetHandlerMethod = ioServiceClass.getMethod("setHandler", ioHandlerClass);
            ioServiceGetSessionConfigMethod = ioServiceClass.getMethod("getSessionConfig");

            ioAcceptorClass = Class.forName("org.apache.mina.core.service.IoAcceptor", true, classLoader);
            ioAcceptorBindMethod = ioAcceptorClass.getMethod("bind", SocketAddress.class);
            ioAcceptorDisposeMethod = ioAcceptorClass.getMethod("dispose", Boolean.TYPE);

            nioSocketAcceptorClass = Class.forName("org.apache.mina.transport.socket.nio.NioSocketAcceptor", true, classLoader);
            nioSocketAcceptorConstructor = nioSocketAcceptorClass.getConstructor();
            nioSocketAcceptorGetFilterChainMethod = nioSocketAcceptorClass.getMethod("getFilterChain");

            ioFilterClass = Class.forName("org.apache.mina.core.filterchain.IoFilter", true, classLoader);

            defaultIoFilterChainBuilderClass = Class.forName("org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder", true, classLoader);
            filterChainBuilderAddLastMethod = defaultIoFilterChainBuilderClass.getMethod("addLast", String.class, ioFilterClass);

            defaultIoFilterChainClass = Class.forName("org.apache.mina.core.filterchain.DefaultIoFilterChain", true, classLoader);
            filterChainAddFirstMethod = defaultIoFilterChainClass.getMethod("addFirst", String.class, ioFilterClass);
            filterChainAddLastMethod = defaultIoFilterChainClass.getMethod("addLast", String.class, ioFilterClass);
            filterChainGetMethod = defaultIoFilterChainClass.getMethod("get", String.class);

            xmppCodecConfigurationClass = Class.forName("com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPCodecConfiguration", true, classLoader);
            xmppCodecConfigurationConstructor = xmppCodecConfigurationClass.getConstructor();

            xmlStreamCodecConfigurationClass = Class.forName("com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMLStreamCodecConfiguration", true, classLoader);

            xmlStreamCodecFactoryClass = Class.forName("com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMLStreamCodecFactory", true, classLoader);
            xmlStreamCodecFactoryConstructor = xmlStreamCodecFactoryClass.getConstructor(xmlStreamCodecConfigurationClass, Boolean.TYPE);

            protocolCodecFactoryClass = Class.forName("org.apache.mina.filter.codec.ProtocolCodecFactory", true, classLoader);

            protocolCodecFilterClass = Class.forName("org.apache.mina.filter.codec.ProtocolCodecFilter", true, classLoader);
            protocolCodecFilterConstructor = protocolCodecFilterClass.getConstructor(protocolCodecFactoryClass);

            executorFilterClass = Class.forName("org.apache.mina.filter.executor.ExecutorFilter", true, classLoader);
            executorFilterConstructor = executorFilterClass.getConstructor(Integer.TYPE);

            xmppIoHandlerAdapterClass = Class.forName("com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPIoHandlerAdapter", true, classLoader);
            xmppIoHandlerAdapterConstructor = xmppIoHandlerAdapterClass.getConstructor(Object.class);

            idleStatusClass = Class.forName("org.apache.mina.core.session.IdleStatus", true, classLoader);
            idleStatus_BOTH_IDLE = idleStatusClass.getField("BOTH_IDLE").get(null);

            ioSessionConfigClass = Class.forName("org.apache.mina.core.session.IoSessionConfig", true, classLoader);
            ioSessionConfigSetReadBufferSizeMethod = ioSessionConfigClass.getMethod("setReadBufferSize", Integer.TYPE);
            ioSessionConfigSetIdleTimeMethod = ioSessionConfigClass.getMethod("setIdleTime", idleStatusClass, Integer.TYPE);

            nioSocketConnectorClass = Class.forName("org.apache.mina.transport.socket.nio.NioSocketConnector", true, classLoader);
            nioSocketConnectorConstructor = nioSocketConnectorClass.getConstructor();
            nioSocketConnectorGetFilterChainMethod = nioSocketConnectorClass.getMethod("getFilterChain");
            nioSocketConnectorConnectMethod = nioSocketConnectorClass.getMethod("connect", SocketAddress.class);

            connectFutureClass = Class.forName("org.apache.mina.core.future.ConnectFuture", true, classLoader);
            connectFutureAwaitUninterruptiblyMethod = connectFutureClass.getMethod("awaitUninterruptibly");

            sslFilterClass = Class.forName("org.apache.mina.filter.ssl.SslFilter", true, classLoader);
            sslFilterConstructor = sslFilterClass.getConstructor(SSLContext.class);
            sslFilterSetEnabledCipherSuitesMethod = sslFilterClass.getMethod("setEnabledCipherSuites", String[].class);
            sslFilterSetUseClientModeMethod = sslFilterClass.getMethod("setUseClientMode", Boolean.TYPE);
            sslFilterSetNeedClientAuthMethod = sslFilterClass.getMethod("setNeedClientAuth", Boolean.TYPE);
            sslFilterSetWantClientAuthMethod = sslFilterClass.getMethod("setWantClientAuth", Boolean.TYPE);
            sslFilterGetSslSessionMethod = sslFilterClass.getMethod("getSslSession", ioSessionClass);
            sslFilter_DISABLE_ENCRYPTION_ONCE = sslFilterClass.getField("DISABLE_ENCRYPTION_ONCE").get(null);

            initialized = true;
        } catch(ClassNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(NoSuchMethodException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(NoSuchFieldException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(IllegalAccessException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(ExceptionInInitializerError e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void checkInitialized() throws XMPPClassHelperNotInitializedException {
        if(!initialized) {
            throw new XMPPClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    @Override
    public void closeSession(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioSessionCloseMethod.invoke(session, true);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object getService(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return ioSessionGetServiceMethod.invoke(session);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Long getSessionId(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return (Long)ioSessionGetIdMethod.invoke(session);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object ioSessionGetFilterChain(Object service) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return ioSessionGetFilterChainMethod.invoke(service);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object nioSocketAcceptorGetFilterChain(Object service) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return nioSocketAcceptorGetFilterChainMethod.invoke(service);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object nioSocketConnectorGetFilterChain(Object service) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return nioSocketConnectorGetFilterChainMethod.invoke(service);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createNioSocketAcceptor() throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return nioSocketAcceptorConstructor.newInstance();
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void addFirstToFilterChain(Object filterChain, String name, Object filter) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            filterChainAddFirstMethod.invoke(filterChain, name, filter);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void addLastToFilterChainBuilder(Object filterChainBuilder, String name, Object filter) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            filterChainBuilderAddLastMethod.invoke(filterChainBuilder, name, filter);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object filterChainGet(Object filterChain, String name) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return filterChainGetMethod.invoke(filterChain, name);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createXmppCodecConfiguration() throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return xmppCodecConfigurationConstructor.newInstance();
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createXmlStreamCodecFactory(Object configuration, boolean inbound) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return xmlStreamCodecFactoryConstructor.newInstance(configuration, inbound);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createProtocolCodecFilter(Object protocolCodecFactory) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return protocolCodecFilterConstructor.newInstance(protocolCodecFactory);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createExecutorFilter(int maximumPoolSize) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return executorFilterConstructor.newInstance(maximumPoolSize);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createXmppIoHandlerAdapter(Object targetObject) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return xmppIoHandlerAdapterConstructor.newInstance(targetObject);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public SocketAddress getLocalAddress(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return (SocketAddress)ioSessionGetLocalAddressMethod.invoke(session);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public SocketAddress getRemoteAddress(Object session) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return (SocketAddress)ioSessionGetRemoteAddressMethod.invoke(session);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void ioSessionWrite(Object session, byte[] bytes) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioSessionWriteMethod.invoke(session, bytes);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void ioServiceSetHandler(Object service, Object handler) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioServiceSetHandlerMethod.invoke(service, handler);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object ioServiceGetSessionConfig(Object acceptor) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return ioServiceGetSessionConfigMethod.invoke(acceptor);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object getIdleStatus_BOTH_IDLE() throws XMPPClassHelperNotInitializedException {
        checkInitialized();
        return idleStatus_BOTH_IDLE;
    }

    @Override
    public void ioSessionConfigSetReadBufferSize(Object sessionConfig, int i) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioSessionConfigSetReadBufferSizeMethod.invoke(sessionConfig, i);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void ioSessionConfigSetIdleTime(Object sessionConfig, Object idleTime, int i) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioSessionConfigSetIdleTimeMethod.invoke(sessionConfig, idleTime, i);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void ioAcceptorBind(Object acceptor, SocketAddress address) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioAcceptorBindMethod.invoke(acceptor, address);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void ioAcceptorDispose(Object acceptor, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioAcceptorDisposeMethod.invoke(acceptor, b);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createNioSocketConnector() throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return nioSocketConnectorConstructor.newInstance();
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object nioSocketConnectorConnect(Object connector, SocketAddress address) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return nioSocketConnectorConnectMethod.invoke(connector, address);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void connectFutureAwaitUninterruptibly(Object future) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            connectFutureAwaitUninterruptiblyMethod.invoke(future);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object createSslFilter(SSLContext sslContext) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return sslFilterConstructor.newInstance(sslContext);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InstantiationException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void sslFilterSetEnabledCipherSuites(Object sslFilter, String[] suites) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            sslFilterSetEnabledCipherSuitesMethod.invoke(sslFilter, new Object[] {suites});
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void sslFilterSetUseClientMode(Object sslFilter, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            sslFilterSetUseClientModeMethod.invoke(sslFilter, b);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void sslFilterSetNeedClientAuth(Object sslFilter, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            sslFilterSetNeedClientAuthMethod.invoke(sslFilter, b);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void sslFilterSetWantClientAuth(Object sslFilter, boolean b) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            sslFilterSetWantClientAuthMethod.invoke(sslFilter, b);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public String ioSessionGetAttribute(Object session, Object key) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return (String)ioSessionGetAttributeMethod.invoke(session, key);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void ioSessionSetAttribute(Object session, Object key, Object value) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            ioSessionSetAttributeMethod.invoke(session, key, value);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object getSslFilter_DISABLE_ENCRYPTION_ONCE() throws XMPPClassHelperNotInitializedException {
        checkInitialized();
        return sslFilter_DISABLE_ENCRYPTION_ONCE;
    }

    @Override
    public SSLSession sslFilterGetSslSession(Object sslFilter, Object sslSession) throws XMPPClassHelperNotInitializedException, XMPPMinaClassException {
        checkInitialized();
        try {
            return (SSLSession)sslFilterGetSslSessionMethod.invoke(sslFilter, sslSession);
        } catch(IllegalAccessException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        } catch(InvocationTargetException e) {
            throw new XMPPMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
