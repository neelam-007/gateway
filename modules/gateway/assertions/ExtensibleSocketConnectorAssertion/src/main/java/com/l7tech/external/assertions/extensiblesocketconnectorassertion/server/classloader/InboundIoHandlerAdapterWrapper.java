package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 08/01/14
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class InboundIoHandlerAdapterWrapper {

    private static Class ioHandlerAdapterClass;
    private static Constructor ioHandlerAdapterConstructor;

    private Object ioHandlerAdapter;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        ioHandlerAdapterClass = Class.forName("com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.InboundIoHandlerAdapter", true, classLoader);
        ioHandlerAdapterConstructor = ioHandlerAdapterClass.getConstructor(StashManagerFactory.class, MessageProcessor.class, Goid.class);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (ioHandlerAdapterClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public static InboundIoHandlerAdapterWrapper create(StashManagerFactory stashManagerFactory, MessageProcessor messageProcessor, Goid serviceGoid) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new InboundIoHandlerAdapterWrapper(ioHandlerAdapterConstructor.newInstance(stashManagerFactory, messageProcessor, serviceGoid));
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InstantiationException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public InboundIoHandlerAdapterWrapper(Object ioHandler) {
        this.ioHandlerAdapter = ioHandler;
    }

    public Object getIoHandler() {
        return ioHandlerAdapter;
    }
}
