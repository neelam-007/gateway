package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/13
 * Time: 10:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class NioSocketAcceptorWrapper implements NioSocketWrapper {
    private static Class nioSocketAcceptorClass;
    private static Constructor nioSocketAcceptorConstructor;
    private static Method nioSocketAcceptorGetFilterChainMethod;
    private static Method nioSocketAcceptorSetHandlerMethod;
    private static Method nioSocketAcceptorGetHandlerMethod;
    private static Method nioSocketAcceptorGetSessionConfigMethod;
    private static Method nioSocketAcceptorBindMethod;
    private static Method nioSocketAcceptorDisposeMethod;
    private static Method nioSocketAcceptorIsDisposedMethod;
    private static Method nioSocketAcceptorGetManagedSessionCountMethod;
    private static Method nioSocketAcceptorGetManagedSessionsMethod;

    private Object nioSocketAcceptor;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        nioSocketAcceptorClass = Class.forName("org.apache.mina.transport.socket.nio.NioSocketAcceptor", true, classLoader);
        Class ioHandlerClass = Class.forName("org.apache.mina.core.service.IoHandler", true, classLoader);
        nioSocketAcceptorConstructor = nioSocketAcceptorClass.getConstructor();
        nioSocketAcceptorGetFilterChainMethod = nioSocketAcceptorClass.getMethod("getFilterChain");
        nioSocketAcceptorSetHandlerMethod = nioSocketAcceptorClass.getMethod("setHandler", ioHandlerClass);
        nioSocketAcceptorGetHandlerMethod = nioSocketAcceptorClass.getMethod("getHandler");
        nioSocketAcceptorGetSessionConfigMethod = nioSocketAcceptorClass.getMethod("getSessionConfig");
        nioSocketAcceptorBindMethod = nioSocketAcceptorClass.getMethod("bind", SocketAddress.class);
        nioSocketAcceptorDisposeMethod = nioSocketAcceptorClass.getMethod("dispose", Boolean.TYPE);
        nioSocketAcceptorIsDisposedMethod = nioSocketAcceptorClass.getMethod("isDisposed");
        nioSocketAcceptorGetManagedSessionCountMethod = nioSocketAcceptorClass.getMethod("getManagedSessionCount");
        nioSocketAcceptorGetManagedSessionsMethod = nioSocketAcceptorClass.getMethod("getManagedSessions");

    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (nioSocketAcceptorClass == null || nioSocketAcceptorConstructor == null || nioSocketAcceptorGetFilterChainMethod == null || nioSocketAcceptorSetHandlerMethod == null ||
                nioSocketAcceptorGetSessionConfigMethod == null || nioSocketAcceptorBindMethod == null || nioSocketAcceptorDisposeMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public NioSocketAcceptorWrapper(Object nioSocketAcceptor) {
        this.nioSocketAcceptor = nioSocketAcceptor;
    }

    public static NioSocketAcceptorWrapper create() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new NioSocketAcceptorWrapper(nioSocketAcceptorConstructor.newInstance());
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InstantiationException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public DefaultIoFilterChainBuilderWrapper getFilterChain() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object filterChainBuilder = nioSocketAcceptorGetFilterChainMethod.invoke(nioSocketAcceptor);
            if (filterChainBuilder == null) {
                return null;
            } else {
                return new DefaultIoFilterChainBuilderWrapper(filterChainBuilder);
            }
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void setHandler(Object handler) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketAcceptorSetHandlerMethod.invoke(nioSocketAcceptor, handler);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public InboundIoHandlerAdapterWrapper getHandler() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object ioHandler = nioSocketAcceptorGetHandlerMethod.invoke(nioSocketAcceptor);
            if (ioHandler == null) {
                return null;
            } else {
                return new InboundIoHandlerAdapterWrapper(ioHandler);
            }
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public IoSessionConfigWrapper getSessionConfig() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object sessionConfig = nioSocketAcceptorGetSessionConfigMethod.invoke(nioSocketAcceptor);
            if (sessionConfig == null) {
                return null;
            } else {
                return new IoSessionConfigWrapper(sessionConfig);
            }
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void bind(SocketAddress firstLocalAddress) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketAcceptorBindMethod.invoke(nioSocketAcceptor, firstLocalAddress);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public void dispose(boolean awaitTermination) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketAcceptorDisposeMethod.invoke(nioSocketAcceptor, awaitTermination);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public boolean isDisposed() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Boolean) nioSocketAcceptorIsDisposedMethod.invoke(nioSocketAcceptor);
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public Object getNioSocketAcceptor() {
        return nioSocketAcceptor;
    }

    @Override
    public int getManagedSessionCount() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Integer) nioSocketAcceptorGetManagedSessionCountMethod.invoke(nioSocketAcceptor);
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Map<Long, IoSessionWrapper> getManagedSessions() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        IoSessionWrapper ioSessionWrapper = null;
        Map<Long, IoSessionWrapper> resultMap = new HashMap<Long, IoSessionWrapper>();

        checkInitialized();
        try {
            Object managedSessions = nioSocketAcceptorGetManagedSessionsMethod.invoke(nioSocketAcceptor);
            if (managedSessions == null) {
                return null;
            } else {
                Set<Long> keySet = ((Map<Long, Object>) managedSessions).keySet();

                for (Long key : keySet) {
                    ioSessionWrapper = new IoSessionWrapper(((Map<Long, Object>) managedSessions).get(key));
                    resultMap.put(key, ioSessionWrapper);
                }

                return resultMap;
            }
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
