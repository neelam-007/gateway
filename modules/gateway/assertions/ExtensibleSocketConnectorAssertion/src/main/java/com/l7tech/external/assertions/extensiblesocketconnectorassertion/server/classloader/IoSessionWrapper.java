package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Method;
import java.net.SocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/12/13
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class IoSessionWrapper {
    private static Class ioSessionClass;
    private static Method ioSessionGetFilterChainMethod;
    private static Method ioSessionCloseMethod;
    private static Method ioSessionGetServiceMethod;
    private static Method ioSessionGetIdMethod;
    private static Method ioSessionGetLocalAddressMethod;
    private static Method ioSessionGetRemoteAddressMethod;
    private static Method ioSessionWriteMethod;
    private static Method ioSessionGetAttributeMethod;
    private static Method ioSessionSetAttributeMethod;
    private static Method ioSessionGetCloseFutureMethod;
    private static Method ioSessionReadMethod;
    private static Method ioSessionSuspendReadMethod;
    private static Method ioSessionSuspendWriteMethod;
    private static Method ioSessionGetConfigMethod;

    private Object ioSession;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
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
        ioSessionGetCloseFutureMethod = ioSessionClass.getMethod("getCloseFuture");
        ioSessionReadMethod = ioSessionClass.getMethod("read");
        ioSessionSuspendReadMethod = ioSessionClass.getMethod("suspendRead");
        ioSessionSuspendWriteMethod = ioSessionClass.getMethod("suspendWrite");
        ioSessionGetConfigMethod = ioSessionClass.getMethod("getConfig");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (ioSessionClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession Class not initialized");
        }
        if (ioSessionGetFilterChainMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession GetFilterChain Method not initialized");
        }
        if (ioSessionCloseMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession Close Method not initialized");
        }
        if (ioSessionGetServiceMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession GetService Method not initialized");
        }
        if (ioSessionGetIdMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession GetID Method not initialized");
        }
        if (ioSessionGetLocalAddressMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession GetLocalAddress Method not initialized");
        }
        if (ioSessionGetRemoteAddressMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession GetRemoteAddress Method not initialized");
        }
        if (ioSessionWriteMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession Write Method not initialized");
        }
        if (ioSessionGetAttributeMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession GetAttribute Method not initialized");
        }
        if (ioSessionSetAttributeMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOSession SetAttribute Method not initialized");
        }
    }

    public static Class getWrappedClass() {
        return ioSessionClass;
    }

    public IoSessionWrapper(Object ioSession) {
        this.ioSession = ioSession;
    }

    public DefaultIoFilterChainWrapper getFilterChain() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object filterChain = ioSessionGetFilterChainMethod.invoke(ioSession);
            if (filterChain == null) {
                return null;
            } else {
                return new DefaultIoFilterChainWrapper(filterChain);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public void close(boolean value) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionCloseMethod.invoke(ioSession, value);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public Object getService() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return ioSessionGetServiceMethod.invoke(ioSession);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public Long getId() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Long) ioSessionGetIdMethod.invoke(ioSession);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public SocketAddress getLocalAddress() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (SocketAddress) ioSessionGetLocalAddressMethod.invoke(ioSession);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public SocketAddress getRemoteAddress() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (SocketAddress) ioSessionGetRemoteAddressMethod.invoke(ioSession);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public WriteFutureWrapper write(Object message) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object writeFuture = ioSessionWriteMethod.invoke(ioSession, message);
            if (writeFuture == null) {
                return null;
            } else {
                return new WriteFutureWrapper(writeFuture);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public Object getAttribute(Object key) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return ioSessionGetAttributeMethod.invoke(ioSession, key);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public void setAttribute(Object key, Object value) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionSetAttributeMethod.invoke(ioSession, key, value);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public CloseFutureWrapper getCloseFuture() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object closeFuture = ioSessionGetCloseFutureMethod.invoke(ioSession);
            if (closeFuture == null) {
                return null;
            } else {
                return new CloseFutureWrapper(closeFuture);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public ReadFutureWrapper read() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object readFuture = ioSessionReadMethod.invoke(ioSession);
            if (readFuture == null) {
                return null;
            } else {
                return new ReadFutureWrapper(readFuture);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public void suspendRead() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionSuspendReadMethod.invoke(ioSession);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public void suspendWrite() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionSuspendWriteMethod.invoke(ioSession);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public IoSessionConfigWrapper getConfig() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object ioSessionConfig = ioSessionGetConfigMethod.invoke(ioSession);
            if (ioSessionConfig == null) {
                return null;
            } else {
                return new IoSessionConfigWrapper(ioSessionConfig);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
