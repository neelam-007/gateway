package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class ProtocolCodecFilterWrapper implements IoFilterWrapper {
    private static Class protocolCodecFilterClass;
    private static Constructor protocolCodecFilterConstructor;

    private Object protocolCodecFilter;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        protocolCodecFilterClass = Class.forName("org.apache.mina.filter.codec.ProtocolCodecFilter", true, classLoader);
        Class protocolCodecFactoryClass = Class.forName("org.apache.mina.filter.codec.ProtocolCodecFactory", true, classLoader);
        protocolCodecFilterConstructor = protocolCodecFilterClass.getConstructor(protocolCodecFactoryClass);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (protocolCodecFilterClass == null || protocolCodecFilterConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public ProtocolCodecFilterWrapper(Object protocolCodecFilter) {
        this.protocolCodecFilter = protocolCodecFilter;
    }

    public static ProtocolCodecFilterWrapper create(Object protocolCodecFactory) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new ProtocolCodecFilterWrapper(protocolCodecFilterConstructor.newInstance(protocolCodecFactory));
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InstantiationException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object getFilter() {
        return protocolCodecFilter;
    }
}
