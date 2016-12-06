package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

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
        if (closeFutureClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. CloseFuture Class not initialized");
        }
        if (closeFutureAwaitUninterruptiblyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. CloseFuture AwaitUninterruptibly Method not initialized");
        }
    }

    public CloseFutureWrapper(Object closeFuture) {
        this.closeFuture = closeFuture;
    }

    public void awaitUninterruptibly() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            closeFutureAwaitUninterruptiblyMethod.invoke(closeFuture);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
