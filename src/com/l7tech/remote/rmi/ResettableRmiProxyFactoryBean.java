package com.l7tech.remote.rmi;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.rmi.RmiClientInterceptorUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aopalliance.aop.AspectException;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * The {@link RemoteInvocationBasedAccessor} subclass that specifies the RMI
 * service properties and supports stub reset.
 *
 * @author emil
 * @version 9-Dec-2004
 */
public class ResettableRmiProxyFactoryBean extends RemoteInvocationBasedAccessor
  implements MethodInterceptor, InitializingBean, FactoryBean {

    private Object serviceProxy;

    private boolean lookupStubOnStartup = true;

    private boolean cacheStub = true;

    private boolean refreshStubOnConnectFailure = false;

    private Remote cachedStub;

    private RMIClientSocketFactory registryClientSocketFactory;

    static {
        // RMI properties, see bug parade 4322806
        // todo: maybe this should be setable - em
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "5000");
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "15000");
    }

    public void setRegistryClientSocketFactory(RMIClientSocketFactory csf) {
        this.registryClientSocketFactory = csf;
    }

    /**
     * Reset the stub
     */
    public synchronized void resetStub() {
        cachedStub = null;
    }

    /**
     * Set whether to look up the RMI stub on startup. Default is true.
     * <p>Can be turned off to allow for late start of the RMI server.
     * In this case, the RMI stub will be fetched on first access.
     * @see #setCacheStub
     */
    public void setLookupStubOnStartup(boolean lookupStubOnStartup) {
        this.lookupStubOnStartup = lookupStubOnStartup;
    }

    /**
     * Set whether to cache the RMI stub once it has been located.
     * Default is true.
     * <p>Can be turned off to allow for hot restart of the RMI server.
     * In this case, the RMI stub will be fetched for each invocation.
     * @see #setLookupStubOnStartup
     */
    public void setCacheStub(boolean cacheStub) {
        this.cacheStub = cacheStub;
    }

    /**
     * Set whether to refresh the RMI stub on connect failure.
     * Default is false.
     * <p>Can be turned on to allow for hot restart of the RMI server.
     * If a cached RMI stub throws an RMI exception that indicates a
     * remote connect failure, a fresh proxy will be fetched and the
     * invocation will be retried.
     * @see java.rmi.ConnectException
     * @see java.rmi.ConnectIOException
     * @see java.rmi.NoSuchObjectException
     */
    public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
        this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
    }


    /**
     * Overriden to support setting service url during runtime
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        // cache RMI stub on initialization?
        if (getServiceInterface() == null) {
            throw new IllegalArgumentException("serviceInterface is required");
        }
        this.serviceProxy = ProxyFactory.getProxy(getServiceInterface(), this);

        if (this.lookupStubOnStartup) {
            if (getServiceUrl() == null) {
                throw new IllegalArgumentException("serviceUrl is required with 'lookupStubOnStartup' = true");
            }
            Remote remoteObj = lookupStub();
            if (logger.isInfoEnabled()) {
                if (remoteObj instanceof RmiInvocationHandler) {
                    logger.info("RMI stub [" + getServiceUrl() + "] is an RMI invoker");
                } else if (getServiceInterface() != null) {
                    boolean isImpl = getServiceInterface().isInstance(remoteObj);
                    logger.info("Using service interface [" + getServiceInterface().getName() +
                      "] for RMI stub [" + getServiceUrl() + "] - " +
                      (!isImpl ? "not " : "") + "directly implemented");
                }
            }
            if (this.cacheStub) {
                this.cachedStub = remoteObj;
            }
        }

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

    /**
     * overriden to support programmatic stub reset
     */
    protected Remote lookupStub() throws Exception {
        final String serviceUrl = getServiceUrl();
        if (serviceUrl == null) {
            throw new RemoteAccessException("Service URL cannot be null " + getServiceInterface());
        }
        Remote stub;
        if (registryClientSocketFactory == null) {
            stub = Naming.lookup(serviceUrl);
        } else {
            NamingURL url = NamingURL.parse(serviceUrl);
            stub = LocateRegistry.getRegistry(url.getHost(), url.getPort(), registryClientSocketFactory).lookup(url.getName());
        }
        if (logger.isInfoEnabled()) {
            logger.info("Located object with RMI URL [" + serviceUrl + "]: value=[" + stub + "]");
        }
        return stub;
    }

    /**
     * Return the RMI stub to use. Called for each invocation.
     * <p>Default implementation returns the proxy created on initialization,
     * if any; else, it invokes lookupStub to get a new proxy for each invocation.
     * <p>Can be overridden in subclasses, for example to cache a proxy for
     * a given amount of time before recreating it, or to test the proxy
     * whether it is still alive.
     *
     * @return the RMI stub to use for an invocation
     * @throws Exception if proxy creation failed
     * @see #lookupStub
     */
    protected Remote getStub() throws Exception {
        if (!this.cacheStub || (this.lookupStubOnStartup && !this.refreshStubOnConnectFailure)) {
            return (this.cachedStub != null ? this.cachedStub : lookupStub());
        } else {
            synchronized (this) {
                if (this.cachedStub == null) {
                    this.cachedStub = lookupStub();
                }
                return this.cachedStub;
            }
        }
    }


    /**
     * Fetches an RMI stub and delegates to doInvoke.
     * If configured to refresh on connect failure, it will call
     * refreshAndRetry on corresponding RMI exceptions.
     *
     * @see #getStub
     * @see #doInvoke(org.aopalliance.intercept.MethodInvocation, Remote)
     * @see #refreshAndRetry
     * @see java.rmi.ConnectException
     * @see java.rmi.ConnectIOException
     * @see java.rmi.NoSuchObjectException
     */
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Remote stub = null;
        try {
            stub = getStub();
        } catch (Throwable ex) {
            throw new RemoteLookupFailureException("RMI lookup for service [" + getServiceUrl() + "] failed", ex);
        }
        try {
            return doInvoke(invocation, stub);
        } catch (RemoteConnectFailureException ex) {
            return handleRemoteConnectFailure(invocation, ex);
        } catch (RemoteException ex) {
            if (RmiClientInterceptorUtils.isConnectFailure(ex)) {
                return handleRemoteConnectFailure(invocation, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Refresh the stub and retry the remote invocation if necessary.
     * If not configured to refresh on connect failure, simply rethrows
     * the original exception.
     *
     * @param invocation the invocation that failed
     * @param ex         the exception raised on remote invocation
     * @return the result value of the new invocation, if succeeded
     * @throws Throwable an exception raised by the new invocation, if failed too.
     */
    private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
        if (this.refreshStubOnConnectFailure) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not connect to RMI service [" + getServiceUrl() + "] - retrying", ex);
            } else if (logger.isWarnEnabled()) {
                logger.warn("Could not connect to RMI service [" + getServiceUrl() + "] - retrying");
            }
            return refreshAndRetry(invocation);
        } else {
            throw ex;
        }
    }

    /**
     * Refresh the RMI stub and retry the given invocation.
     * Called by invoke on connect failure.
     *
     * @param invocation the AOP method invocation
     * @return the invocation result, if any
     * @throws Throwable in case of invocation failure
     * @see #invoke
     */
    protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
        Remote freshStub = null;
        synchronized (this) {
            try {
                freshStub = lookupStub();
                if (this.cacheStub) {
                    this.cachedStub = freshStub;
                }
            } catch (Throwable ex) {
                throw new RemoteLookupFailureException("RMI lookup for service [" + getServiceUrl() + "] failed", ex);
            }
        }
        return doInvoke(invocation, freshStub);
    }

    /**
     * Perform the given invocation on the given RMI stub.
     *
     * @param invocation the AOP method invocation
     * @param stub       the RMI stub to invoke
     * @return the invocation result, if any
     * @throws Throwable in case of invocation failure
     */
    protected Object doInvoke(MethodInvocation invocation, Remote stub) throws Throwable {
        if (stub instanceof RmiInvocationHandler) {
            // RMI invoker
            try {
                return doInvoke(invocation, (RmiInvocationHandler)stub);
            } catch (RemoteException ex) {
                throw RmiClientInterceptorUtils.convertRmiAccessException(invocation.getMethod(), ex, getServiceUrl());
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } catch (Throwable ex) {
                throw new AspectException("Failed to invoke remote service [" + getServiceUrl() + "]", ex);
            }
        } else {
            // traditional RMI stub
            return RmiClientInterceptorUtils.invoke(invocation, stub, getServiceUrl());
        }
    }

    /**
     * Apply the given AOP method invocation to the given RmiInvocationHandler.
     * The default implementation calls invoke with a plain RemoteInvocation.
     * <p>Can be overridden in subclasses to provide custom RemoteInvocation
     * subclasses, containing additional invocation parameters like user
     * credentials. Can also process the returned result object.
     *
     * @param methodInvocation  the current AOP method invocation
     * @param invocationHandler the RmiInvocationHandler to apply the invocation to
     * @return the invocation result
     * @throws NoSuchMethodException     if the method name could not be resolved
     * @throws IllegalAccessException    if the method could not be accessed
     * @throws InvocationTargetException if the method invocation resulted in an exception
     * @see org.springframework.remoting.support.RemoteInvocation
     */
    protected Object doInvoke(MethodInvocation methodInvocation, RmiInvocationHandler invocationHandler)
      throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
            return "RMI invoker proxy for service URL [" + getServiceUrl() + "]";
        }

        return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
    }

}
