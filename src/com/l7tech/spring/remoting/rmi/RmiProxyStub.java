/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.remoting.rmi.RmiClientInterceptor;
import org.springframework.remoting.support.RemoteInvocationFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.Remote;

/**
 * @author emil
 * @version Dec 6, 2004
 */
final class RmiProxyStub extends RmiClientInterceptor implements Serializable {
    private Object serviceProxy;
    private Remote serviceStub;
    private Class remoteInvocationFactoryClass;

    RmiProxyStub(Remote stub, Class serviceInterface) throws Exception {
        this.serviceStub = stub;
        this.setServiceInterface(serviceInterface);
        initialize();
    }


    private void initialize() throws Exception {
        String memoServiceUrl = getServiceUrl();
        try {
            if (memoServiceUrl == null) {
                setServiceUrl(getServiceInterface().getName()); //todo: submit Spring feature request for this
            }
            super.afterPropertiesSet();
            this.serviceProxy = ProxyFactory.getProxy(getServiceInterface(), this);
        } finally {
            setServiceUrl(memoServiceUrl);
        }
    }

    public Object getServiceProxy() {
        return serviceProxy;
    }

    protected synchronized Remote lookupStub() throws Exception {
        return this.serviceStub;
    }

    /**
     * Saves its own fields by calling defaultWriteObject and then explicitly
     * saves the fields of its superclass
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        RemoteInvocationFactory remoteInvocationFactory = getRemoteInvocationFactory();
        remoteInvocationFactoryClass = remoteInvocationFactory.getClass();

        // Take care of this class's field first by calling defaultWriteObject
        out.defaultWriteObject();

        /*
         * Since the superclass does not implement the Serializable interface
         * we explicitly do the saving...
         */
        if (Serializable.class.isAssignableFrom(remoteInvocationFactoryClass)) {
            out.writeObject(remoteInvocationFactory);
        }
    }

    /**
     * Restores its own fields by calling defaultReadObject and then explicitly
     * restores the fields of its superclass.
     */
    private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

        /*
         * Take care of this class's fields first by calling
         * defaultReadObject
         */
        in.defaultReadObject();


        /*
         * Since the superclass does not implement the Serializable
         * interface we explicitly do the restoring...
         */
        if (Serializable.class.isAssignableFrom(remoteInvocationFactoryClass)) {
            setRemoteInvocationFactory((RemoteInvocationFactory)in.readObject());
        }
    }
}