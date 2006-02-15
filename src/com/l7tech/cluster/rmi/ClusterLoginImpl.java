package com.l7tech.cluster.rmi;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.beans.factory.InitializingBean;

import com.l7tech.cluster.ClusterLogin;
import com.l7tech.cluster.ClusterContext;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIServerSocketFactory;

/**
 * Cluster login implementation for RMI/SSL.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ClusterLoginImpl extends ApplicationObjectSupport implements ClusterLogin {

    //- PUBLIC

    /**
     *
     */
    public ClusterLoginImpl() {
    }

    /**
     *
     */
    public ClusterContext login() throws RemoteException, LoginException {
        SslRMIServerSocketFactory.Context context = SslRMIServerSocketFactory.getContext();

        if(context==null) {
            logger.fine("Invalid cluster login attempt, no context.");
            throw new LoginException("No context found, you must call through RMI/SSL.");
        }

        if(!context.isRemoteClientCertAuthenticated()) {
            logger.info("Invalid cluster login attempt, no client certificate, host is '"+
                    context.getRemoteHost()+"'.");
            throw new LoginException("No client certificate provided!");
        }

        ClusterContext clusterContext = (ClusterContext) getApplicationContext().getBean("clusterContextRemote");
        logger.info("Cluster login from host '"+context.getRemoteHost()+"'.");

        return clusterContext;
    }

    //- PRIVATE

    /**
     *
     */
    private static final Logger logger = Logger.getLogger(ClusterLoginImpl.class.getName());
}
