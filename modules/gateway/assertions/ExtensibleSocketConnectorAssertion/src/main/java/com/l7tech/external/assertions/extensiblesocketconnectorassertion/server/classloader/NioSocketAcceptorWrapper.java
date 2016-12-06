package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
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
        if (nioSocketAcceptorClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor Class not initialized");
        }
        if (nioSocketAcceptorConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor Constructor not initialized");
        }
        if (nioSocketAcceptorGetFilterChainMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor GetFilterChain Method not initialized");
        }
        if (nioSocketAcceptorSetHandlerMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor SetHandler Method not initialized");
        }
        if (nioSocketAcceptorGetSessionConfigMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor GetSessionConfig Method not initialized");
        }
        if (nioSocketAcceptorBindMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor Bind Method not initialized");
        }
        if (nioSocketAcceptorDisposeMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketAcceptor Dispose Method not initialized");
        }
    }

    public NioSocketAcceptorWrapper(Object nioSocketAcceptor) {
        this.nioSocketAcceptor = nioSocketAcceptor;
    }

    public static NioSocketAcceptorWrapper create() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new NioSocketAcceptorWrapper(nioSocketAcceptorConstructor.newInstance());
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Error occurred when creating new NioSocketAcceptorWrapper", e);
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
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public void setHandler(Object handler) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketAcceptorSetHandlerMethod.invoke(nioSocketAcceptor, handler);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
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
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
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
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public void bind(SocketAddress firstLocalAddress) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketAcceptorBindMethod.invoke(nioSocketAcceptor, firstLocalAddress);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public void dispose(boolean awaitTermination) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketAcceptorDisposeMethod.invoke(nioSocketAcceptor, awaitTermination);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public boolean isDisposed() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Boolean) nioSocketAcceptorIsDisposedMethod.invoke(nioSocketAcceptor);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
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
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
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
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
