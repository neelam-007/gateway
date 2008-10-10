package com.l7tech.server.transport.jms.prov.fiorano.proxy;

import fiorano.jms.runtime.IFMQSecurityManager;
import fiorano.jms.common.FioranoException;

import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.ClassLoader;
import java.lang.reflect.Constructor;
import java.net.Socket;

/**
 * The purpose of this class is instantiate the FioranoSecurityManager that is within the Gateway.jar.  This class is
 * needed because fiorano libraries will try to instantiate the security manager that was implemented into the Gateway.jar
 * but has no visibility, hence attempt to instantiate it will result in ClassNotFoundException.  With this, it basically
 * will act like a proxy and instantiate the proper class using the context loader.
 *
 * User: dlee
 * Date: Jun 11, 2008
 */
public class FioranoProxySecurityManager implements IFMQSecurityManager {

    private static final String FIORANO_SECURITY_MANAGER = "com.l7tech.server.transport.jms.prov.fiorano.FioranoSecurityManager";
    private static final Logger logger = Logger.getLogger(FioranoProxySecurityManager.class.getName());

    Object fioranoSecurityManager;

    public FioranoProxySecurityManager(Hashtable environment) {
        try {
            //get threa's context class loader to instantiate the fiorano security manager
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class fioranoSecurityManagerClass = classLoader.loadClass(FIORANO_SECURITY_MANAGER);

            //get the cosntructors so that an instance can be created
            Constructor[] constructors =  fioranoSecurityManagerClass.getConstructors();

            //it is assumed that it has only one constructor!!
            fioranoSecurityManager = constructors[0].newInstance(environment);
            if ( fioranoSecurityManager == null ){
                throw new RuntimeException("Cannot instantiate Fiorano Security Manager.");
            }
            
            logger.log(Level.INFO, "Instantiated the Fiorano security manager through proxy.");
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot instantiate Fiorano Security Manager.", e);
            throw new RuntimeException("Cannot instantiate Fiorano Security Manager.", e);
        }
    }

    public Object getSecurityContext() {
        return ((IFMQSecurityManager)fioranoSecurityManager).getSecurityContext();
    }

    public void checkExecute(Socket socket) throws FioranoException {
        ((IFMQSecurityManager)fioranoSecurityManager).checkExecute(socket);
    }
}
