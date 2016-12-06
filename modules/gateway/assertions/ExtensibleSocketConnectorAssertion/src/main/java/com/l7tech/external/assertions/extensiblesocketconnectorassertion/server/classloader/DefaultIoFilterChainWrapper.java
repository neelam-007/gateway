package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/12/13
 * Time: 11:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultIoFilterChainWrapper {
    private static Class defaultIoFilterChainClass;
    private static Method defaultIoFilterChainGetEntryMethod;
    private static Method defaultIoFilterChainAddFirstMethod;
    private static Method defaultIoFilterChainGetMethod;

    private Object defaultIoFilterChain;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        defaultIoFilterChainClass = Class.forName("org.apache.mina.core.filterchain.DefaultIoFilterChain", true, classLoader);
        Class ioFilterClass = Class.forName("org.apache.mina.core.filterchain.IoFilter", true, classLoader);
        defaultIoFilterChainGetEntryMethod = defaultIoFilterChainClass.getMethod("getEntry", Class.class);
        defaultIoFilterChainAddFirstMethod = defaultIoFilterChainClass.getMethod("addFirst", String.class, ioFilterClass);
        defaultIoFilterChainGetMethod = defaultIoFilterChainClass.getMethod("get", String.class);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (defaultIoFilterChainClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChain Class not initialized");
        }
        if (defaultIoFilterChainGetEntryMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChain GetEntry Method not initialized");
        }
        if (defaultIoFilterChainAddFirstMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChain AddFirst Method not initialized");
        }
        if (defaultIoFilterChainGetMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChain Get Method not initialized");
        }
    }

    public DefaultIoFilterChainWrapper(Object defaultIoFilterChain) {
        this.defaultIoFilterChain = defaultIoFilterChain;
    }

    public IoFilterChainEntryWrapper getEntry(Class clazz) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object entry = defaultIoFilterChainGetEntryMethod.invoke(defaultIoFilterChain, clazz);
            if (entry == null) {
                return null;
            } else {
                return new IoFilterChainEntryWrapper(entry);
            }
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public void addFirst(String name, Object filter) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object filterToAdd;
            if (filter instanceof IoFilterWrapper) {
                filterToAdd = ((IoFilterWrapper) filter).getFilter();
            } else {
                filterToAdd = filter;
            }

            defaultIoFilterChainAddFirstMethod.invoke(defaultIoFilterChain, name, filterToAdd);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public Object get(String name) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return defaultIoFilterChainGetMethod.invoke(defaultIoFilterChain, name);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
