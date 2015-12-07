package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassLoader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

    private Object executorFilter;

    public static void initialize(ExtensibleSocketConnectorClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        executorFilterClass = Class.forName("org.apache.mina.filter.executor.ExecutorFilter", true, classLoader);
        executorFilterConstructor = executorFilterClass.getConstructor(Integer.TYPE);
    }

    private static void checkInitialized() throws ExtensibleSocketConnectorClassHelperNotInitializedException {
        if (executorFilterClass == null || executorFilterConstructor == null) {
            throw new ExtensibleSocketConnectorClassHelperNotInitializedException("Failed to load the Apache Mina components.");
        }
    }

    public ExecutorFilterWrapper(Object executorFilter) {
        this.executorFilter = executorFilter;
    }

    public static ExecutorFilterWrapper create(int maximumPoolSize) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        checkInitialized();
        try {
            return new ExecutorFilterWrapper(executorFilterConstructor.newInstance(maximumPoolSize));
        } catch (IllegalAccessException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InstantiationException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        } catch (InvocationTargetException e) {
            throw new ExtensibleSocketConnectorMinaClassException("Failure with Apache Mina components.", e);
        }
    }

    @Override
    public Object getFilter() {
        return executorFilter;
    }
}
