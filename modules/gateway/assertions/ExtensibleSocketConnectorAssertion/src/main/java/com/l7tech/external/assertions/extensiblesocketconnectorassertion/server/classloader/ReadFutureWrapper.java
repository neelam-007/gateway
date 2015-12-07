package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 14/01/14
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReadFutureWrapper {

    private static Class readFutureClass;
    private static Method readFutureAwaitUninterruptiblyMethod;
    private static Method readFutureAwaitUninterruptiblyWithTimeoutMethod;

    private Object readFuture;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        readFutureClass = Class.forName("org.apache.mina.core.future.ReadFuture", true, classLoader);
        readFutureAwaitUninterruptiblyMethod = readFutureClass.getMethod("awaitUninterruptibly");
        readFutureAwaitUninterruptiblyWithTimeoutMethod = readFutureClass.getMethod("awaitUninterruptibly", Long.TYPE);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (readFutureClass == null || readFutureAwaitUninterruptiblyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public ReadFutureWrapper(Object readFuture) {
        this.readFuture = readFuture;
    }

    public void awaitUninterruptibly() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            readFutureAwaitUninterruptiblyMethod.invoke(readFuture);
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public boolean awaitUninterruptibly(long timeout) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (Boolean) readFutureAwaitUninterruptiblyWithTimeoutMethod.invoke(readFuture, timeout);
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
