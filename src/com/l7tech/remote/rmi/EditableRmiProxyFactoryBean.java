package com.l7tech.remote.rmi;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;

/**
 * The {@link RmiProxyFactoryBean} subclass that supports property
 * editing an stub reset.
 *
 * @author emil
 * @version 9-Dec-2004
 */
public class EditableRmiProxyFactoryBean extends RmiProxyFactoryBean {
    private boolean resetStubRequested = false;
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
    public void resetStub() {
        try {
            resetStubRequested = true;
            getStub();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            resetStubRequested = false;
        }
    }

    /**
     * Overriden to support setting service url during runtime
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        String serviceUrl = getServiceUrl();
        try {
            if (serviceUrl == null) {
                setServiceUrl("");
            }
            super.afterPropertiesSet();
        } finally {
            setServiceUrl(serviceUrl);
        }
    }

    /**
     * overriden to support programmatic stub reset
     */
    protected Remote lookupStub() throws Exception {
        final String serviceUrl = getServiceUrl();
        if (resetStubRequested) {
            logger.trace("reset stub service " + serviceUrl);
            return null;
        }
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
}
