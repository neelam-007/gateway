package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.InvocationTargetException;
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
        if (connectFutureClass == null || connectFutureAwaitUninterruptiblyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public ConnectFutureWrapper(Object connectFuture) {
        this.connectFuture = connectFuture;
    }

    public void awaitUninterruptibly() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            connectFutureAwaitUninterruptiblyMethod.invoke(connectFuture);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public IoSessionWrapper getSession() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new IoSessionWrapper(connectFutureGetSessionMethod.invoke(connectFuture));
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
