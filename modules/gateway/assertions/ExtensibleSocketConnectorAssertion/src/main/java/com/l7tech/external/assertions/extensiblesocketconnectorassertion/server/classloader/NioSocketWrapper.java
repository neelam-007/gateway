package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorClassHelperNotInitializedException;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ExtensibleSocketConnectorMinaClassException;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 09/01/14
 * Time: 2:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NioSocketWrapper {

    public DefaultIoFilterChainBuilderWrapper getFilterChain() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;

    public void setHandler(Object handler) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;

    public IoSessionConfigWrapper getSessionConfig() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;

    public void dispose(boolean awaitTermination) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;

    public boolean isDisposed() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;

    public int getManagedSessionCount() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;

    public Map<Long, IoSessionWrapper> getManagedSessions() throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException;
}
