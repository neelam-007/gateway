/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

/**
 * The {@link RmiServiceExporterStubFactoryBean } subclass that allows specifying additional properties
 * such as the registry socket factory.
 *
 * @author emil
 * @version Dec 2, 2004
 */
public class RmiServiceExporterStubFactoryBean
  extends RmiServiceExporter implements FactoryBean, InitializingBean {

    private String serviceName;

    private int servicePort = 0;  // anonymous port

    private int registryPort = Registry.REGISTRY_PORT;

    private RMIClientSocketFactory clientSocketFactory;

    private RMIServerSocketFactory serverSocketFactory;

    private Remote exportedObject;

    private RMIClientSocketFactory registryClientSocketFactory;

    private RMIServerSocketFactory registryServerSocketFactory;
    private boolean singleton = true;

    private RmiProxyStub proxyStub;

    private RemoteInvocationFactory stubRemoteInvocationFactory;

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
        super.setServiceName(this.serviceName);
    }

    /**
     * Set the port that the exported RMI service will use.
     * Default is 0 (anonymous port).
     * This is overriden until Spring team makes the properties protectec
     * or offer the getter
     */
    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
        super.setServicePort(this.servicePort);
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
        super.setRegistryPort(this.registryPort);
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
        return getObjectToExport().getClass();
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
        if (singleton) {
            exportService();
        }

    }

    private RmiProxyStub exportService() throws RemoteException {
        this.exportedObject = getObjectToExport();

        Remote objectStub;
        if (this.clientSocketFactory != null) {
            objectStub = UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
        } else {
            objectStub = UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort);
        }
        if (this.serviceName != null) {
            logger.info("Binding RMI service '" + this.serviceName +  "' exported object '" + getDisplayServiceName() +
              "' to registry at port '" + this.registryPort + "'");
            Registry registry = getRegistry(this.registryPort);
            registry.rebind(this.serviceName, this.exportedObject);
        } else {
            logger.info("Unbound RMI service; exported object '" + getDisplayServiceName() +
              "' to registry at port '" + this.registryPort + "'");
        }
        try {
            proxyStub = new RmiProxyStub(objectStub, getServiceInterface());
            if (stubRemoteInvocationFactory !=null) {
                proxyStub.setRemoteInvocationFactory(stubRemoteInvocationFactory);
            }
            return proxyStub;
        } catch (Exception e) {
            throw new RemoteException("Error exporting service ", e);
        }
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
        Registry registry;
        try {
            // retrieve registry
            registry = LocateRegistry.getRegistry(registryPort);
            registry.list();
        } catch (RemoteException ex) {
            logger.debug("RMI registry access threw exception", ex);
            logger.debug("Could not detect RMI registry - creating new one");

            // assume no registry found -> create new one
            if ((this.registryClientSocketFactory != null && this.registryServerSocketFactory == null) ||
              (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null)) {
                throw new IllegalArgumentException("Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
            }
            if (registryClientSocketFactory == null) {
                registry = LocateRegistry.createRegistry(registryPort);
            } else {
                registry = LocateRegistry.createRegistry(registryPort, registryClientSocketFactory, registryServerSocketFactory);
            }
        }
        return registry;
    }

    /**
     * Overriden to shows a bit cleaner loggin ()see super.getObjectToExport())
     * @return the RMI object to export
     */
    protected Remote getObjectToExport() {
        String sn = this.serviceName;
        try {
            setServiceName(getDisplayServiceName());
            return super.getObjectToExport();
        } finally {
           setServiceName(sn);
        }
    }

    /**
     * Redefined here to be visible to RmiInvocationWrapper.
     * Simply delegates to the corresponding superclass method.
     */
    protected Object invoke(RemoteInvocation invocation, Object targetObject)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return super.invoke(invocation, targetObject);
    }


    /**
     * Unbind the RMI service from the registry at bean factory shutdown.
     */
    public void destroy() throws RemoteException, NotBoundException {
        if (logger.isInfoEnabled()) {
            logger.info("Unbinding RMI service '" + getDisplayServiceName() +
              "' from registry at port '" + this.registryPort + "'");
        }
        if (this.serviceName == null) {
            Registry registry = LocateRegistry.getRegistry(this.registryPort);
            registry.unbind(this.serviceName);
        }
        UnicastRemoteObject.unexportObject(this.exportedObject, true);
    }

    private String getDisplayServiceName() {
        if (this.serviceName !=null ) {
            return this.serviceName;
        }
        return getService().getClass().getName();
    }
}