package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/13
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultIoFilterChainBuilderWrapper {
    private static Class defaultIoFilterChainBuilderClass;
    private static Method defaultIoFilterChainBuilderAddLastMethod;

    private Object defaultIoFilterChainBuilder;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        defaultIoFilterChainBuilderClass = Class.forName("org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder", true, classLoader);
        Class ioFilterClass = Class.forName("org.apache.mina.core.filterchain.IoFilter", true, classLoader);
        defaultIoFilterChainBuilderAddLastMethod = defaultIoFilterChainBuilderClass.getMethod("addLast", String.class, ioFilterClass);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (defaultIoFilterChainBuilderClass == null || defaultIoFilterChainBuilderAddLastMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public DefaultIoFilterChainBuilderWrapper(Object defaultIoFilterChainBuilder) {
        this.defaultIoFilterChainBuilder = defaultIoFilterChainBuilder;
    }

    public void addLast(String name, Object filter) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            Object filterToAdd;
            if (filter instanceof IoFilterWrapper) {
                filterToAdd = ((IoFilterWrapper) filter).getFilter();
            } else {
                filterToAdd = filter;
            }

            defaultIoFilterChainBuilderAddLastMethod.invoke(defaultIoFilterChainBuilder, name, filterToAdd);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
