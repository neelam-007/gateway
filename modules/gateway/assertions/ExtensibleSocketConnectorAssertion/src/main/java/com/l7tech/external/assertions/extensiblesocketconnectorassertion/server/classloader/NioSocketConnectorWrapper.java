package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
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
public class NioSocketConnectorWrapper implements NioSocketWrapper {

    private static Class nioSocketConnectorClass;
    private static Constructor nioSocketConnectorConstructor;
    private static Method nioSocketConnectorGetFilterChainMethod;
    private static Method nioSocketConnectorConnectMethod;
    private static Method nioSocketConnectorGetSessionConfigMethod;
    private static Method nioSocketConnectorSetHandlerMethod;
    private static Method nioSocketConnectorGetHandlerMethod;
    private static Method nioSocketConnectorDisposeMethod;
    private static Method nioSocketConnectorIsDisposedMethod;
    private static Method nioSocketConnectorGetManagedSessionCountMethod;
    private static Method nioSocketConnectorGetManagedSessionsMethod;

    private Object nioSocketConnector;

    private int port = 0;
    private String hostName = "";
    private long portCacheTime = 0L;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        nioSocketConnectorClass = Class.forName("org.apache.mina.transport.socket.nio.NioSocketConnector", true, classLoader);
        Class ioHandlerClass = Class.forName("org.apache.mina.core.service.IoHandler", true, classLoader);
        nioSocketConnectorConstructor = nioSocketConnectorClass.getConstructor();
        nioSocketConnectorGetFilterChainMethod = nioSocketConnectorClass.getMethod("getFilterChain");
        nioSocketConnectorConnectMethod = nioSocketConnectorClass.getMethod("connect", SocketAddress.class);
        nioSocketConnectorGetSessionConfigMethod = nioSocketConnectorClass.getMethod("getSessionConfig");
        nioSocketConnectorSetHandlerMethod = nioSocketConnectorClass.getMethod("setHandler", ioHandlerClass);
        nioSocketConnectorGetHandlerMethod = nioSocketConnectorClass.getMethod("getHandler");
        nioSocketConnectorDisposeMethod = nioSocketConnectorClass.getMethod("dispose", Boolean.TYPE);
        nioSocketConnectorIsDisposedMethod = nioSocketConnectorClass.getMethod("isDisposed");
        nioSocketConnectorGetManagedSessionCountMethod = nioSocketConnectorClass.getMethod("getManagedSessionCount");
        nioSocketConnectorGetManagedSessionsMethod = nioSocketConnectorClass.getMethod("getManagedSessions");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (nioSocketConnectorClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketConnector Class not initialized");
        }
        if (nioSocketConnectorConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketConnector Constructor not initialized");
        }
        if (nioSocketConnectorGetFilterChainMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketConnector GetFilterChain Method not initialized");
        }
        if (nioSocketConnectorConnectMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketConnector Connect Method not initialized");
        }
        if (nioSocketConnectorGetSessionConfigMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. NIOSocketConnector GetSessionConfig Method not initialized");
        }
    }

    public NioSocketConnectorWrapper(Object nioSocketConnector) {
        this.nioSocketConnector = nioSocketConnector;
    }

    public static NioSocketConnectorWrapper create() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new NioSocketConnectorWrapper(nioSocketConnectorConstructor.newInstance());
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public DefaultIoFilterChainBuilderWrapper getFilterChain() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object filterChainBuilder = nioSocketConnectorGetFilterChainMethod.invoke(nioSocketConnector);
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
            nioSocketConnectorSetHandlerMethod.invoke(nioSocketConnector, handler);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public OutboundIoHandlerAdapterWrapper getHandler() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object ioHandler = nioSocketConnectorGetHandlerMethod.invoke(nioSocketConnector);
            if (ioHandler == null) {
                return null;
            } else {
                return new OutboundIoHandlerAdapterWrapper(ioHandler);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public IoSessionConfigWrapper getSessionConfig() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object sessionConfig = nioSocketConnectorGetSessionConfigMethod.invoke(nioSocketConnector);
            if (sessionConfig == null) {
                return null;
            } else {
                return new IoSessionConfigWrapper(sessionConfig);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public ConnectFutureWrapper connect() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object connectFuture = nioSocketConnectorConnectMethod.invoke(nioSocketConnector, new InetSocketAddress(hostName, port));
            if (connectFuture == null) {
                return null;
            } else {
                return new ConnectFutureWrapper(connectFuture);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public void dispose(boolean awaitTermination) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            nioSocketConnectorDisposeMethod.invoke(nioSocketConnector, awaitTermination);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public boolean isDisposed() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Boolean) nioSocketConnectorIsDisposedMethod.invoke(nioSocketConnector);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public int getManagedSessionCount() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Integer) nioSocketConnectorGetManagedSessionCountMethod.invoke(nioSocketConnector);
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
            Object managedSessions = nioSocketConnectorGetManagedSessionsMethod.invoke(nioSocketConnector);
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public long getPortCacheTime() {
        return portCacheTime;
    }

    public void setPortCacheTime(long portCacheTime) {
        this.portCacheTime = portCacheTime;
    }
}
