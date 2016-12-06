package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/12/13
 * Time: 12:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class AttributeKeyFactory {
    private static Class attributeKeyClass;
    private static Constructor attributeKeyConstructor;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        attributeKeyClass = Class.forName("org.apache.mina.core.session.AttributeKey", true, classLoader);
        attributeKeyConstructor = attributeKeyClass.getConstructor(Class.class, String.class);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (attributeKeyClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. AttributeKey Class not initialized");
        }
        if (attributeKeyConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. AttributeKey Constructor not initialized");
        }
    }

    public static Object create(Class source, String name) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return attributeKeyConstructor.newInstance(source, name);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }
}
