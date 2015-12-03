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
 * Time: 11:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class IoSessionConfigWrapper {
    private static Class ioSessionConfigClass;
    private static Method ioSessionConfigSetReadBufferSizeMethod;
    private static Method ioSessionConfigSetIdleTimeMethod;
    private static Method ioSessionConfigSetUseReadOperation;

    private Object ioSessionConfig;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        ioSessionConfigClass = Class.forName("org.apache.mina.core.session.IoSessionConfig", true, classLoader);
        ioSessionConfigSetReadBufferSizeMethod = ioSessionConfigClass.getMethod("setReadBufferSize", Integer.TYPE);
        ioSessionConfigSetIdleTimeMethod = ioSessionConfigClass.getMethod("setIdleTime", IdleStatusWrapper.getWrappedClass(), Integer.TYPE);
        ioSessionConfigSetUseReadOperation = ioSessionConfigClass.getMethod("setUseReadOperation", Boolean.TYPE);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (ioSessionConfigClass == null || ioSessionConfigSetReadBufferSizeMethod == null || ioSessionConfigSetIdleTimeMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public IoSessionConfigWrapper(Object ioSessionConfig) {
        this.ioSessionConfig = ioSessionConfig;
    }

    public void setReadBufferSize(int readBufferSize) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionConfigSetReadBufferSizeMethod.invoke(ioSessionConfig, readBufferSize);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void setIdleTime(Object status, int idleTime) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionConfigSetIdleTimeMethod.invoke(ioSessionConfig, status, idleTime);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void setUseReadOperation(boolean value) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            ioSessionConfigSetUseReadOperation.invoke(ioSessionConfig, value);
        } catch (IllegalArgumentException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }
}
