/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.spring.remoting.rmi;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationFactory;

/**
 * This is not really an Exporter, it is a Factory that returns proxies backed
 * by remote references.
 *
 * @author emil, $Author$
 * @version Dec 2, 2004, $Revision$
 */
public class RmiServiceExporterStubFactoryBean
    extends RemoteInvocationBasedExporter implements FactoryBean, InitializingBean, DisposableBean {

    private static final Logger logger = Logger.getLogger(RmiServiceExporterStubFactoryBean.class.getName());

    private String serviceName;

    private int servicePort = 0;  // anonymous port

    private RMIClientSocketFactory clientSocketFactory;

    private RMIServerSocketFactory serverSocketFactory;

    private Registry registry;

    private boolean singleton = true;

    private boolean initialized = false;

    private RmiProxyStub proxyStub; // proxy stub for singletons

    private RemoteInvocationFactory stubRemoteInvocationFactory;

    private final List<Object> exportedObjects;

    private static int rmiExportCount = 0;

    public RmiServiceExporterStubFactoryBean() {
        exportedObjects = new LinkedList<Object>();
    }

    /**
     * Set the name of the exported RMI service,
     * i.e. rmi://localhost:port/NAME
     * If <b>null</b> the exported service will not be bound to a name in the
     * registry. This is useful when sending the remote reference directly to
     * the client.
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Set the port that the exported RMI service will use.
     * Default is 0 (anonymous port).
     */
    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    /**
     * Set the Registry to use for the exported RMI service,
     */
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    /**
     * Set a custom RMI client socket factory to use for exporting.
     * If the given object also implement RMIServerSocketFactory,
     * it will automatically be registered as server socket factory too.
     *
     * @see #setServerSocketFactory
     * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
     */
    public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
        this.clientSocketFactory = clientSocketFactory;
    }

    /**
     * Set a custom RMI server socket factory to use for exporting.
     *
     * @see #setClientSocketFactory
     */
    public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    public void setStubRemoteInvocationFactory(RemoteInvocationFactory stubRemoteInvocationFactory) {
        this.stubRemoteInvocationFactory = stubRemoteInvocationFactory;
    }

    public Object getObject() {
        if (!singleton) {
            try {
                return exportService().getServiceProxy();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return proxyStub.getServiceProxy();
    }

    public Class getObjectType() {
        Class clazz = null;

        if ( initialized ) {
            clazz = getService().getClass();
        }

        return clazz;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public boolean isSingleton() {
        return singleton;
    }

    /**
     * Register the service as RMI object.
     * Creates an RMI registry on the specified port if none exists.
     */
    public void afterPropertiesSet() throws RemoteException {
        checkService();
        if (this.registry==null) {
            throw new IllegalArgumentException("Registry not set (required).");
        }
        if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
            this.serverSocketFactory = (RMIServerSocketFactory)this.clientSocketFactory;
        }
        if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
          (this.clientSocketFactory == null && this.serverSocketFactory != null)) {
            throw new IllegalArgumentException("Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
        }
        if(this.stubRemoteInvocationFactory==null) {
            this.stubRemoteInvocationFactory = new DefaultRemoteInvocationFactory();
        }
        if (singleton) {
            proxyStub = exportService();
        }
        initialized = true;
    }

    private RmiProxyStub exportService() throws RemoteException {
        Object service = getService();
        Remote exportedObject;
        if(service instanceof Remote) {
            exportedObject = (Remote) service;
        }
        else {
            exportedObject = remoteWrapper(service);
        }

        // then track the exported object for cleanup on shutdown.
        recordExport(exportedObject, singleton);

        Remote objectStub;
        if (this.clientSocketFactory != null) {
            objectStub = UnicastRemoteObject.exportObject(exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
        } else {
            objectStub = UnicastRemoteObject.exportObject(exportedObject, this.servicePort);
        }
        rmiExportCount++;
        if (this.serviceName != null) {
            logger.info("Binding RMI service '" + this.serviceName +  "' exported object '" + getDisplayServiceName() +
              "' to registry, export count is: " + rmiExportCount);
            registry.rebind(this.serviceName, exportedObject);
        } else {
            logger.info("Unbound RMI service; exported object '" + getDisplayServiceName() +
              "', export count is: " + rmiExportCount);
        }
        try {
            return new RmiProxyStub(exportedObject, objectStub, getServiceInterface(), stubRemoteInvocationFactory);
        } catch (Exception e) {
            throw new RemoteException("Error exporting service '"+this.serviceName+"'.", e);
        }
    }

    /**
     * Create an invoking remote wrapper for the exported service.
     *
     * @param wrappedObject the object to wrap
     * @return the rmi invoker remote.
     */
    private Remote remoteWrapper(final Object wrappedObject) {
        return new RmiInvocationHandler() {
            public Object invoke(RemoteInvocation invocation)
                throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
                    return RmiServiceExporterStubFactoryBean.this.invoke(invocation, wrappedObject);
            }
            public String getTargetInterfaceName() throws RemoteException {
                return getServiceInterface().getName();
            }
        };
    }

    /**
     * Get the RMI registry for this exporter.
     *
     * @return the RMI registry
     * @throws RemoteException if the registry couldn't be located or created
     */
    protected Registry getRegistry() throws RemoteException {
        return registry;
    }

    /**
     * Unbind the RMI service from the registry at bean factory shutdown.
     */
    public void destroy() throws RemoteException, NotBoundException {
        if (!singleton) {
            unexportObjects();
            return;
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Unbinding RMI service '" + getDisplayServiceName() +
              "' from registry, export count is: " + (rmiExportCount-1));
        }
        if (this.serviceName != null) {
            registry.unbind(this.serviceName);
        }
        unexportObjects();
    }

    /**
     * Remove any old references and add the new one
     */
    private void recordExport(Remote exportedObject, boolean singleton) {
        synchronized(exportedObjects) {
            for (Iterator iterator = exportedObjects.iterator(); iterator.hasNext();) {
                Object refOrObject = iterator.next();
                if(refOrObject instanceof WeakReference) {
                    WeakReference weakReference = (WeakReference) refOrObject;
                    if(weakReference.get()==null) {
                        iterator.remove();
                        rmiExportCount--;
                    }
                }
            }
            Object refOrObject = singleton ? exportedObject : new WeakReference<Remote>(exportedObject);
            exportedObjects.add(refOrObject);
        }
    }

    private void unexportObjects() {
        synchronized(exportedObjects) {
            for (Iterator iterator = exportedObjects.iterator(); iterator.hasNext();) {
                Object refOrObject = iterator.next();
                Remote exported;
                if(refOrObject instanceof WeakReference) {
                    WeakReference weakReference = (WeakReference) refOrObject;
                    exported = (Remote) weakReference.get();
                }
                else {
                    exported = (Remote) refOrObject;
                }
                if(exported!=null) {
                    if (logger.isLoggable(Level.INFO)) {
                        logger.info("Unbinding RMI object '" + getDisplayServiceName() +
                          "' from registry, export count is: " + (rmiExportCount-1));
                    }
                    try {
                        UnicastRemoteObject.unexportObject(exported, true);
                    }
                    catch(NoSuchObjectException nsoe) {
                        logger.log(Level.WARNING, "Error when unexporting object", nsoe);
                    }
                }
                rmiExportCount--;
                iterator.remove();
            }
        }
    }

    private String getDisplayServiceName() {
        if (this.serviceName !=null ) {
            return this.serviceName;
        }
        return getService().getClass().getName();
    }
}