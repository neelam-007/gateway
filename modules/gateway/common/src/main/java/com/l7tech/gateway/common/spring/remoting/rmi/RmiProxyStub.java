/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.spring.remoting.rmi;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

/**
 * RMI Stub object which backs the Dynamic Proxy passed to the client.
 *
 * @author $Author$
 * @version $Revision$
 */
final class RmiProxyStub implements InvocationHandler, Serializable {

    //- PUBLIC

    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        Object result = null;

        try {
            if(serviceStub instanceof RmiInvocationHandler) {
                RemoteInvocation ri = remoteInvocationFactory.createRemoteInvocation(new MethodInvocation(){
                    //
                    public Method getMethod() {return method;}
                    public Object[] getArguments() {return args;}

                    // not used by spring
                    public Object proceed() throws Throwable {return null;}
                    public Object getThis() {return null;}
                    public AccessibleObject getStaticPart() {return null;}
                });
                result = ((RmiInvocationHandler)serviceStub).invoke(ri);
            }
            else {
                result = method.invoke(serviceStub, args);
            }
        }
        catch(InvocationTargetException ite) { // unwrap any target exception
            throw ite.getCause();
        }
        return result;
    }

    public Object getServiceProxy() {
        return serviceProxy;
    }

    //- PROTECTED

    protected synchronized Remote lookupStub() throws Exception {
        return this.serviceStub;
    }

    //- PACKAGE

    RmiProxyStub(Object exported, Remote stub, Class serviceInterface, RemoteInvocationFactory factory) throws Exception {
        this.exported = exported; // HOLD a reference to this until we are Serialized (to prevend DGC reaping)
        this.serviceStub = stub;
        this.serviceProxy = Proxy.newProxyInstance(RmiProxyStub.class.getClassLoader(), new Class[]{serviceInterface}, this);
        this.remoteInvocationFactory = factory;
    }

    //- PRIVATE

    private static final long serialVersionUID = -7877086141153801344L;

    private final transient Object exported; // only hold until serialized
    private final Object serviceProxy;
    private final Remote serviceStub;
    private final RemoteInvocationFactory remoteInvocationFactory;
}