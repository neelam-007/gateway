/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.jini.lookup;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteLookupFailureException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.Collections;

/**
 * @author emil
 * @version Oct 5, 2004
 */
public class JiniProxyFactoryBean
  implements InitializingBean, FactoryBean, MethodInterceptor {

    private ServiceLookup lookup;
    private Class serviceInterface;
    private Object serviceProxy;
    private Remote serviceStub;


    public JiniProxyFactoryBean(ServiceLookup lkp) {
        lookup = lkp;
    }

    public void setServiceInterface(Class serviceInterface) {
        if (serviceInterface != null && !serviceInterface.isInterface()) {
            throw new IllegalArgumentException("serviceInterface must be an interface");
        }
        if (!Remote.class.isAssignableFrom(serviceInterface)) {
            throw new IllegalArgumentException("serviceInterface must implement " + Remote.class);
        }

        this.serviceInterface = serviceInterface;
    }

    public Class getServiceInterface() {
        return serviceInterface;
    }

    public void afterPropertiesSet() {
        if (getServiceInterface() == null) {
            throw new IllegalArgumentException("serviceInterface is required");
        }
        this.serviceProxy = ProxyFactory.getProxy(getServiceInterface(), this);
    }

    public Object getObject() {
        return this.serviceProxy;
    }

    public Class getObjectType() {
        return (this.serviceProxy != null) ? this.serviceProxy.getClass() : getServiceInterface();
    }

    public boolean isSingleton() {
        return true;
    }

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        // synchronized !
        if (serviceStub == null) {
            serviceStub = lookupStub();
        }
        return doInvoke(methodInvocation, serviceStub);
    }

    private synchronized Remote lookupStub() {
        Remote stub = (Remote)lookup.lookup(serviceInterface, Collections.EMPTY_LIST);
        if (stub == null) {
            throw new RemoteLookupFailureException("Lookup for service [" + serviceInterface + "] failed", null);
        }
        return stub;
    }

    /**
     * Reset the stub
     */
    synchronized void resetStub() {
        serviceStub = null;
    }

    /**
     * Perform a raw method invocation on the given RMI stub,
     * letting reflection exceptions through as-is.
     *
     * @param invocation the AOP MethodInvocation
     * @param stub       the RMI stub
     * @return the invocation result, if any
     * @throws NoSuchMethodException     if thrown by reflection
     * @throws IllegalAccessException    if thrown by reflection
     * @throws InvocationTargetException if thrown by reflection
     */
    private Object doInvoke(MethodInvocation invocation, Remote stub)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = invocation.getMethod();
        if (method.getDeclaringClass().isInstance(stub)) {
            // directly implemented
            return method.invoke(stub, invocation.getArguments());
        } else {
            // not directly implemented
            Method stubMethod = stub.getClass().getMethod(method.getName(), method.getParameterTypes());
            return stubMethod.invoke(stub, invocation.getArguments());
        }
    }

}