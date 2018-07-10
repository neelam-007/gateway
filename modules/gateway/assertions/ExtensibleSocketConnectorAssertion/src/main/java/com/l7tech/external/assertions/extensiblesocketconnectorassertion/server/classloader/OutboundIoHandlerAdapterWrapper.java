package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 10/01/14
 * Time: 10:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class OutboundIoHandlerAdapterWrapper {

    private static Class ioHandlerAdapterClass;
    private static Constructor ioHandlerAdapterConstructor;
    private static Method ioHandlerAdapterGetResponseMethod;

    private Object ioHandlerAdapter;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        ioHandlerAdapterClass = Class.forName("com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.OutboundIoHandlerAdapter", true, classLoader);
        ioHandlerAdapterConstructor = ioHandlerAdapterClass.getConstructor();
        ioHandlerAdapterGetResponseMethod = ioHandlerAdapterClass.getMethod("getResponse");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (ioHandlerAdapterClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOHandlerAdapter Class not initialized");
        }
    }

    public static OutboundIoHandlerAdapterWrapper create() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new OutboundIoHandlerAdapterWrapper(ioHandlerAdapterConstructor.newInstance());
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public OutboundIoHandlerAdapterWrapper(Object ioHandler) {
        this.ioHandlerAdapter = ioHandler;
    }

    public Object getIoHandler() {
        return ioHandlerAdapter;
    }

    public byte[] getResponse() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (byte[]) ioHandlerAdapterGetResponseMethod.invoke(ioHandlerAdapter);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
