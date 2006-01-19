/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.spring.remoting.rmi;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

    private String serviceName;

    private int servicePort = 0;  // anonymous port

    private int registryPort = Registry.REGISTRY_PORT;

    private RMIClientSocketFactory clientSocketFactory;

    private RMIServerSocketFactory serverSocketFactory;

    private RMIClientSocketFactory registryClientSocketFactory;

    private RMIServerSocketFactory registryServerSocketFactory;
    private boolean singleton = true;

    private RmiProxyStub proxyStub; // proxy stub for singletons

    private RemoteInvocationFactory stubRemoteInvocationFactory;

    private final List exportedObjects;

    // TODO - when we upgrade Spring (1.2.3+) we can use that to do the registry cleanup, until then ...
    private static Registry theRegistryICreated = null;
    private static int rmiServiceExporterStubFactoryBeanCount = 0; // The last one to leave turns out the lights.
    private static int rmiExportCount = 0;

    public RmiServiceExporterStubFactoryBean() {
        exportedObjects = new LinkedList();
    }

    /**
     * Set the name of the exported RMI service,
     * i.e. rmi://localhost:port/NAME
     * If <b>null</b> the exported service will not be bound to a name in the
     * registry. This is useful when sending the remote reference directly to
     * the client.
     * This is overriden until Spring team makes the properties protectec
     * or offer the getter
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Set the port that the exported RMI service will use.
     * Default is 0 (anonymous port).
     * This is overriden until Spring team makes the properties protectec
     * or offer the getter
     */
    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    /**
     * Set the port of the registry for the exported RMI service,
     * i.e. rmi://localhost:PORT/name
     * Default is Registry.REGISTRY_PORT (1099).
     * This is overriden until Spring team makes the properties protectec
     * or offer the getter
     */
    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
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

    public void setRegistryClientSocketFactory(RMIClientSocketFactory csf) {
        this.registryClientSocketFactory = csf;
    }

    public void setRegistryServerSocketFactory(RMIServerSocketFactory ssf) {
        this.registryServerSocketFactory = ssf;
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
        checkService();
        return getService().getClass();
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
        synchronized(RmiServiceExporterStubFactoryBean.class) {
            rmiServiceExporterStubFactoryBeanCount++;
        }
    }

    private RmiProxyStub exportService() throws RemoteException {
        Object service = getService();
        Remote exportedObject = null;
        if(exportedObject instanceof Remote) {
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
              "' to registry at port '" + this.registryPort + "', export count is: " + rmiExportCount);
            Registry registry = getRegistry(this.registryPort);
            registry.rebind(this.serviceName, exportedObject);
        } else {
            logger.info("Unbound RMI service; exported object '" + getDisplayServiceName() +
              "' to registry at port '" + this.registryPort + "', export count is: " + rmiExportCount);
        }
        try {
            RmiProxyStub stub = new RmiProxyStub(exportedObject, objectStub, getServiceInterface(), stubRemoteInvocationFactory);
            return stub;
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
        };
    }

    /**
     * Locate or create the RMI registry for this exporter.
     *
     * @param registryPort the registry port to use
     * @return the RMI registry
     * @throws RemoteException if the registry couldn't be located or created
     */
    protected Registry getRegistry(int registryPort) throws RemoteException {
        if (logger.isInfoEnabled()) {
            logger.info("Looking for RMI registry at port '" + registryPort + "'");
        }

        // check the registry socket factories
        if ((this.registryClientSocketFactory != null && this.registryServerSocketFactory == null) ||
          (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null)) {
            throw new IllegalArgumentException("Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
        }

        Registry registry;
        try {
            // retrieve registry
            if (registryClientSocketFactory == null) {
                registry = LocateRegistry.getRegistry(registryPort);
            } else {
                registry = LocateRegistry.getRegistry(null, registryPort, registryClientSocketFactory);
            }
            registry.list(); // this will throw a NoSuchObjectException if the registry is not yet created.
        } catch (RemoteException ex) {
            if(!(ex instanceof NoSuchObjectException)) logger.warn("RMI registry access threw exception", ex);
            logger.debug("Could not detect RMI registry - creating new one");
            if (registryClientSocketFactory == null) {
                registry = LocateRegistry.createRegistry(registryPort);
            } else {
                registry = LocateRegistry.createRegistry(registryPort, registryClientSocketFactory, registryServerSocketFactory);
            }
            rmiExportCount++;
            theRegistryICreated = registry;
        }
        return registry;
    }

    /**
     * Unbind the RMI service from the registry at bean factory shutdown.
     */
    public void destroy() throws RemoteException, NotBoundException {
        boolean shutdownRegistry = false;
        synchronized(RmiServiceExporterStubFactoryBean.class) {
            rmiServiceExporterStubFactoryBeanCount--;
            if(rmiServiceExporterStubFactoryBeanCount==0) {
                shutdownRegistry = true;
            }
        }
        try {
            if (!singleton) {
                unexportObjects();
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Unbinding RMI service '" + getDisplayServiceName() +
                  "' from registry at port '" + this.registryPort + "', export count is: " + (rmiExportCount-1));
            }
            if (this.serviceName != null) {
                Registry registry = getRegistry(this.registryPort);
                registry.unbind(this.serviceName);
            }
            unexportObjects();
        }
        finally {
            if(shutdownRegistry && theRegistryICreated!=null) {
                logger.info("Shutting down the registry.");
                UnicastRemoteObject.unexportObject(theRegistryICreated, true);
                rmiExportCount--;

                logger.info("Registry shutdown, export count is: " + rmiExportCount);
            }
        }
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
            Object refOrObject = singleton ? exportedObject : (Object) new WeakReference(exportedObject);
            exportedObjects.add(refOrObject);
        }
    }

    private void unexportObjects() {
        synchronized(exportedObjects) {
            for (Iterator iterator = exportedObjects.iterator(); iterator.hasNext();) {
                Object refOrObject = iterator.next();
                Remote exported = null;
                if(refOrObject instanceof WeakReference) {
                    WeakReference weakReference = (WeakReference) refOrObject;
                    exported = (Remote) weakReference.get();
                }
                else {
                    exported = (Remote) refOrObject;
                }
                if(exported!=null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unbinding RMI object '" + getDisplayServiceName() +
                          "' from registry at port '" + this.registryPort + "', export count is: " + (rmiExportCount-1));
                    }
                    try {
                        UnicastRemoteObject.unexportObject(exported, true);
                    }
                    catch(NoSuchObjectException nsoe) {
                        logger.warn("Error when unexporting object", nsoe);
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