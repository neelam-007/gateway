package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * @author jbufu
 */
public class Upgrade52To53UpdateJmsProviderType implements UpgradeTask {

    private static final Logger logger = Logger.getLogger(Upgrade52To53UpdateJmsProviderType.class.getName());

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {

        JmsConnectionManager connectionManager;
        try {
            connectionManager = applicationContext.getBean("jmsConnectionManager", JmsConnectionManager.class);
        } catch (BeansException be) {
            throw new FatalUpgradeException("Error accessing  bean 'jmsConnectionManager' from ApplicationContext.");
        }

        try {
            for(JmsConnection connection : connectionManager.findAll()) {
                if (connection.getProviderType() != null) continue;

                String icfClassname = connection.getInitialContextFactoryClassname();
                //Fiorano is no longer supported. Commented out to support compiler.
                // This class will never be used again anyway for any release build from source post 5.3.
//                if ( "fiorano.jms.runtime.naming.FioranoInitialContextFactory".equals(icfClassname)) {
//                    connection.setProviderType(JmsProviderType.Fiorano);
//                } else
                if ("com.tibco.tibjms.naming.TibjmsInitialContextFactory".equals(icfClassname)) {
                    connection.setProviderType(JmsProviderType.Tibco);
                } else if ("com.ibm.mq.jms.context.WMQInitialContextFactory".equals(icfClassname) ||
                           "com.sun.jndi.ldap.LdapCtxFactory".equals(icfClassname)) {
                    connection.setProviderType(JmsProviderType.MQ);
                } else {
                    logger.warning("Unknown provider type for initial context factory class: " + icfClassname);
                }
                connectionManager.update(connection);
            }
        } catch (ObjectModelException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }
    }
}
