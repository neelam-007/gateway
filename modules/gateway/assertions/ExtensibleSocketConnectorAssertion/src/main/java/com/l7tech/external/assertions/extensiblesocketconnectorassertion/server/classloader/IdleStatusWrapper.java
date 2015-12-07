package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/13
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class IdleStatusWrapper {
    private static Class idleStatusClass;
    private static Object BOTH_IDLE_Constant;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        idleStatusClass = Class.forName("org.apache.mina.core.session.IdleStatus", true, classLoader);
        BOTH_IDLE_Constant = idleStatusClass.getField("BOTH_IDLE").get(null);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (idleStatusClass == null || BOTH_IDLE_Constant == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public static Class getWrappedClass() {
        return idleStatusClass;
    }

    public static Object BOTH_IDLE() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        checkInitialized();
        return BOTH_IDLE_Constant;
    }
}
