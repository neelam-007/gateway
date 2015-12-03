package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.InvocationTargetException;
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
        if (defaultIoFilterChainClass == null || defaultIoFilterChainGetEntryMethod == null || defaultIoFilterChainAddFirstMethod == null || defaultIoFilterChainGetMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
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
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
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
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public Object get(String name) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return defaultIoFilterChainGetMethod.invoke(defaultIoFilterChain, name);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
