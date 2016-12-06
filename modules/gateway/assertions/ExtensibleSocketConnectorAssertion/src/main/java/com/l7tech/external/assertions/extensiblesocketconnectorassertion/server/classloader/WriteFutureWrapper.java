package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/12/13
 * Time: 2:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class WriteFutureWrapper {
    private static Class writeFutureClass;
    private static Method writeFutureAwaitUninterruptiblyMethod;

    private Object writeFuture;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        writeFutureClass = Class.forName("org.apache.mina.core.future.WriteFuture", true, classLoader);
        writeFutureAwaitUninterruptiblyMethod = writeFutureClass.getMethod("awaitUninterruptibly");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (writeFutureClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. WriteFuture Class not initialized");
        }
        if (writeFutureAwaitUninterruptiblyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. WriteFuture AwaitUninterruptibly Method not initialized");
        }
    }

    public WriteFutureWrapper(Object writeFuture) {
        this.writeFuture = writeFuture;
    }

    public void awaitUninterruptibly() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            writeFutureAwaitUninterruptiblyMethod.invoke(writeFuture);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
