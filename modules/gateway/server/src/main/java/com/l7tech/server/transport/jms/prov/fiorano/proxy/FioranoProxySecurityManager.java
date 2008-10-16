package com.l7tech.server.transport.jms.prov.fiorano.proxy;

import java.util.Hashtable;

import com.l7tech.server.transport.jms.prov.fiorano.FioranoSecurityManager;
import com.l7tech.server.transport.jms.JmsConfigException;

/**
 * This class is here for backwards compatibility with Fiorano SSL configurations created by 4.6.
 */
public class FioranoProxySecurityManager extends FioranoSecurityManager {
    public FioranoProxySecurityManager(Hashtable environment) throws JmsConfigException {
        super( environment );
    }
}
