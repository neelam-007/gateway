package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/12/13
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class SslFilterWrapper implements IoFilterWrapper {
    private static Class sslFilterClass;
    private static Constructor sslFilterConstructor;
    private static Method sslFilterSetEnabledCipherSuitesMethod;
    private static Method sslFilterSetUseClientModeMethod;
    private static Method sslFilterSetNeedClientAuthMethod;
    private static Method sslFilterSetWantClientAuthMethod;
    private static Method sslFilterGetSslSessionMethod;
    private static Object DISABLE_ENCRYPTION_ONCE_Constant;

    private Object sslFilter;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        sslFilterClass = Class.forName("org.apache.mina.filter.ssl.SslFilter", true, classLoader);
        sslFilterConstructor = sslFilterClass.getConstructor(SSLContext.class);
        sslFilterSetEnabledCipherSuitesMethod = sslFilterClass.getMethod("setEnabledCipherSuites", String[].class);
        sslFilterSetUseClientModeMethod = sslFilterClass.getMethod("setUseClientMode", Boolean.TYPE);
        sslFilterSetNeedClientAuthMethod = sslFilterClass.getMethod("setNeedClientAuth", Boolean.TYPE);
        sslFilterSetWantClientAuthMethod = sslFilterClass.getMethod("setWantClientAuth", Boolean.TYPE);
        sslFilterGetSslSessionMethod = sslFilterClass.getMethod("getSslSession", IoSessionWrapper.getWrappedClass());
        DISABLE_ENCRYPTION_ONCE_Constant = sslFilterClass.getField("DISABLE_ENCRYPTION_ONCE").get(null);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (sslFilterClass == null || sslFilterConstructor == null || sslFilterSetEnabledCipherSuitesMethod == null || sslFilterSetUseClientModeMethod == null ||
                sslFilterSetNeedClientAuthMethod == null || sslFilterSetWantClientAuthMethod == null || sslFilterSetWantClientAuthMethod == null ||
                sslFilterGetSslSessionMethod == null || DISABLE_ENCRYPTION_ONCE_Constant == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public SslFilterWrapper(Object sslFilter) {
        this.sslFilter = sslFilter;
    }

    public static SslFilterWrapper create(SSLContext sslContext) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new SslFilterWrapper(sslFilterConstructor.newInstance(sslContext));
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InstantiationException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void setEnabledCipherSuites(String[] cipherSuites) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            sslFilterSetEnabledCipherSuitesMethod.invoke(sslFilter, new Object[]{cipherSuites});
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void setUseClientMode(boolean clientMode) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            sslFilterSetUseClientModeMethod.invoke(sslFilter, clientMode);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void setNeedClientAuth(boolean needClientAuth) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            sslFilterSetNeedClientAuthMethod.invoke(sslFilter, needClientAuth);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public void setWantClientAuth(boolean wantClientAuth) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            sslFilterSetWantClientAuthMethod.invoke(sslFilter, wantClientAuth);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public SSLSession getSslSession() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return (SSLSession) sslFilterGetSslSessionMethod.invoke(sslFilter);
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    public static Object DISABLE_ENCRYPTION_ONCE() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        return DISABLE_ENCRYPTION_ONCE_Constant;
    }

    public Object getFilter() {
        return sslFilter;
    }
}
