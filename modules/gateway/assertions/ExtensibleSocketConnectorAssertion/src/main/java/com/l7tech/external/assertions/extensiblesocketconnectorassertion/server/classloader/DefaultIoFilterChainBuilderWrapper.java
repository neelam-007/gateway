package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

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
    private static Method getEntryMethod;

    private Object defaultIoFilterChainBuilder;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        defaultIoFilterChainBuilderClass = Class.forName("org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder", true, classLoader);
        Class ioFilterClass = Class.forName("org.apache.mina.core.filterchain.IoFilter", true, classLoader);
        defaultIoFilterChainBuilderAddLastMethod = defaultIoFilterChainBuilderClass.getMethod("addLast", String.class, ioFilterClass);
        getEntryMethod = defaultIoFilterChainBuilderClass.getMethod("getEntry", String.class);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (defaultIoFilterChainBuilderClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChainBuilder Class not initialized");
        }
        if (defaultIoFilterChainBuilderAddLastMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChainBuilder AddLast Method not initialized");
        }
        if (getEntryMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. DefaultIOFilterChainBuilder getEntry Method not initialized");
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
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    public Object getEntry(String name) throws ExtensibleSocketConnectorMinaClassException, ExtensibleSocketConnectorClassHelperNotInitializedException {
        checkInitialized();

        try {
            return getEntryMethod.invoke(defaultIoFilterChainBuilder, name);

        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure to invoke getEntry method.", e);
        }
    }
}
