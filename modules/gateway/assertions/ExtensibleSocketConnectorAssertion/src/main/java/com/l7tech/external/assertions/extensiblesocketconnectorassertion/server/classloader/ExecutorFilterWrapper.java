package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/13
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExecutorFilterWrapper implements IoFilterWrapper {
    private static Class executorFilterClass;
    private static Constructor executorFilterConstructor;
    private static Method destroyMethod;

    private Object executorFilter;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        executorFilterClass = Class.forName("org.apache.mina.filter.executor.ExecutorFilter", true, classLoader);
        executorFilterConstructor = executorFilterClass.getConstructor(Integer.TYPE, Integer.TYPE);
        destroyMethod = executorFilterClass.getMethod("destroy");

    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (executorFilterClass == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ExecutorFilter Class not initialized");
        }
        if (executorFilterConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ExecutorFilter Constructor not initialized");
        }
        if (destroyMethod == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Unexpected Error. ExecutorFilter destroy Method not initialized");
        }
    }

    public ExecutorFilterWrapper(Object executorFilter) {
        this.executorFilter = executorFilter;
    }

    public static ExecutorFilterWrapper create(int corePoolSize, int maximumPoolSize)
            throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new ExecutorFilterWrapper(executorFilterConstructor.newInstance(corePoolSize, maximumPoolSize));
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke method", e);
        }
    }

    @Override
    public Object getFilter() {
        return executorFilter;
    }

    public void destroy() throws ExtensibleSocketConnectorMinaClassException, ExtensibleSocketConnectorClassHelperNotInitializedException {
        checkInitialized();
        try {
            destroyMethod.invoke(executorFilter);
        } catch (Exception e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failed to invoke destroy method.", e);
        }
    }
}
