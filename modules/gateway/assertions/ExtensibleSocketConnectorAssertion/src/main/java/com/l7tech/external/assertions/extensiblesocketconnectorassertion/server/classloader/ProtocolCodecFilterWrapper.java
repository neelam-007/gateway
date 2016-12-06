package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;

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
        if (protocolCodecFilterClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ProtocolCodecFilter Class not initialized");
        }
        if (protocolCodecFilterConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ProtocolCodecFilter Constructor not initialized");
        }
    }

    public ProtocolCodecFilterWrapper(Object protocolCodecFilter) {
        this.protocolCodecFilter = protocolCodecFilter;
    }

    public static ProtocolCodecFilterWrapper create(Object protocolCodecFactory) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new ProtocolCodecFilterWrapper(protocolCodecFilterConstructor.newInstance(protocolCodecFactory));
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public Object getFilter() {
        return protocolCodecFilter;
    }
}
