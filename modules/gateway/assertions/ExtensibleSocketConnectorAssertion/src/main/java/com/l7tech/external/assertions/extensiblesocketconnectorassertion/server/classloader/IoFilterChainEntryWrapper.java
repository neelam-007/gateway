package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/12/13
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class IoFilterChainEntryWrapper {
    private static Class ioFilterChainEntryClass;
    private static Method ioFilterChainEntryGetFilterMethod;

    private Object ioFilterChainEntry;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        ioFilterChainEntryClass = Class.forName("org.apache.mina.core.filterchain.IoFilterChain$Entry", true, classLoader);
        ioFilterChainEntryGetFilterMethod = ioFilterChainEntryClass.getMethod("getFilter");
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (ioFilterChainEntryClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOFilterChainEntry Class not initialized");
        }
        if (ioFilterChainEntryGetFilterMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. IOFilterChainEntry GetFilter Method not initialized");
        }
    }

    public IoFilterChainEntryWrapper(Object ioFilterChainEntry) {
        this.ioFilterChainEntry = ioFilterChainEntry;
    }

    public Object getFilter() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return ioFilterChainEntryGetFilterMethod.invoke(ioFilterChainEntry);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
