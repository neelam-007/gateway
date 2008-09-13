package com.l7tech.gateway.common.spring.remoting.rmi;

import com.l7tech.identity.UserBean;
import com.l7tech.gateway.common.spring.remoting.rmi.ssl.SslRMIServerSocketFactory;
import com.l7tech.gateway.common.spring.remoting.RemotingProvider;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationExecutor;

import javax.security.auth.Subject;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Annotation;

/**
 * Cluster invoker.
 *
 * <p>Allow invocation from other nodes in the cluster. This checks the node is present
 * in the cluster info table, and is a local Ip.</p>
 *
 * <p>If the above checks succeed AND the client authenticated with an acceptable SSL
 * certificate then we allow the invocation.</p>
 *
 * @author $Author$
 */
public final class ClusterRemoteInvocationExecutor<T extends Annotation> implements RemoteInvocationExecutor {

    //- PUBLIC

    /**
     *
     */
    public ClusterRemoteInvocationExecutor( final String facility, final RemotingProvider<T> remotingProvider ) {
        this.facility = facility;
        this.remotingProvider = remotingProvider;                                     
    }

    /**
     * Redefined here to be visible to RmiInvocationWrapper.
     * Simply delegates to the corresponding superclass method.
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // Enforce cluster permission
        remotingProvider.checkPermitted(null, facility,  invocation.getClass().getName() + "#" + invocation.getMethodName());

        // Ensure invocation subject is set for node to node invocation.
        if(invocation instanceof AdminSessionRemoteInvocation) {
            // We could just use the info passed by the other node, but for now
            // we'll trash it to avoid any security risk.
            AdminSessionRemoteInvocation adminInvocation = (AdminSessionRemoteInvocation) invocation;
            Subject administrator = adminInvocation.getSubject();
            if( administrator != null ) {
                // Get context
                SslRMIServerSocketFactory.Context context = SslRMIServerSocketFactory.getContext();

                UserBean nodeUser = new UserBean();
                nodeUser.setName("NODE-" + context.getRemoteHost());
                nodeUser.setLogin(nodeUser.getName());
                nodeUser.setFirstName("Cluster");
                nodeUser.setFirstName("Node");

                administrator.getPrincipals().clear();
                administrator.getPrincipals().add(nodeUser);
                administrator.getPrivateCredentials().clear(); // not necessary but couldn't hurt
                administrator.getPublicCredentials().clear(); // ditto
            }
        }

        return invocation.invoke(targetObject);
    }

    //- PRIVATE

    private final String facility;
    private final RemotingProvider<T> remotingProvider;
}
