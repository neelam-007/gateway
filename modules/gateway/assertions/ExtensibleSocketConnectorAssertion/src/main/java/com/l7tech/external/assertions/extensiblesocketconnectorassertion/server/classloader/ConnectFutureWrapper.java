package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/13
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectFutureWrapper {
    private static Class connectFutureClass;
    private static Method connectFutureAwaitUninterruptiblyMethod;
    private static Method connectFutureGetSessionMethod;

    private Object connectFuture;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        connectFutureClass = Class.forName("org.apache.mina.core.future.ConnectFuture", true, classLoader);
        connectFutureAwaitUninterruptiblyMethod = connectFutureClass.getMethod("awaitUninterruptibly");
        connectFutureGetSessionMethod = connectFutureClass.getMethod("getSession");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (connectFutureClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ConnectFuture Class not initialized");
        }
        if (connectFutureAwaitUninterruptiblyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ConnectFuture AwaitUninterruptibly Method not initialized");
        }
    }

    public ConnectFutureWrapper(Object connectFuture) {
        this.connectFuture = connectFuture;
    }

    public void awaitUninterruptibly() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            connectFutureAwaitUninterruptiblyMethod.invoke(connectFuture);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public IoSessionWrapper getSession() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new IoSessionWrapper(connectFutureGetSessionMethod.invoke(connectFuture));
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
