package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 13/01/14
 * Time: 9:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloseFutureWrapper {

    private static Class closeFutureClass;
    private static Method closeFutureAwaitUninterruptiblyMethod;

    private Object closeFuture;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        closeFutureClass = Class.forName("org.apache.mina.core.future.CloseFuture", true, classLoader);
        closeFutureAwaitUninterruptiblyMethod = closeFutureClass.getMethod("awaitUninterruptibly");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (closeFutureClass == null || closeFutureAwaitUninterruptiblyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public CloseFutureWrapper(Object closeFuture) {
        this.closeFuture = closeFuture;
    }

    public void awaitUninterruptibly() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            closeFutureAwaitUninterruptiblyMethod.invoke(closeFuture);
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
