package com.l7tech.spring.remoting.rmi;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIServerSocketFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationExecutor;

import javax.security.auth.Subject;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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
 * @version $Revision$
 */
public final class ClusterRemoteInvocationExecutor implements RemoteInvocationExecutor {

    //- PUBLIC

    /**
     *
     */
    public ClusterRemoteInvocationExecutor(ClusterInfoManager clusterInfoManager) {
        this.clusterInfoManager = clusterInfoManager;
    }

    /**
     * Redefined here to be visible to RmiInvocationWrapper.
     * Simply delegates to the corresponding superclass method.
     */
    public Object invoke(RemoteInvocation invocation, Object targetObject)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // If we don't have any info then just reject the invocation
        if(clusterInfoManager==null) {
            throw new IllegalAccessException("Cluster invocations not permitted.");
        }

        // Verify invocation is by another node.
        SslRMIServerSocketFactory.Context context = SslRMIServerSocketFactory.getContext();

        if(!context.isRemoteClientCertAuthenticated()) {
            throw new IllegalAccessException("Client certificate authentication required.");
        }

        try {
            InetAddress remoteHost = InetAddress.getByName(context.getRemoteHost());
            if(!remoteHost.isSiteLocalAddress()) {
                throw new IllegalAccessException("Cluster invocation denied from for non-local ip '"+remoteHost+"'.");
            }

            boolean isClusterNode = false;
            Collection clusterNodes = clusterInfoManager.retrieveClusterStatus();
            for ( Iterator i = clusterNodes.iterator(); i.hasNext(); ) {
                ClusterNodeInfo nodeInfo = (ClusterNodeInfo) i.next();
                String nodeAddress = nodeInfo.getAddress();
                InetAddress clusterAddress = InetAddress.getByName(nodeAddress);
                if(clusterAddress.equals(remoteHost)) {
                    isClusterNode = true;
                }
            }

            if(!isClusterNode) {
                throw new IllegalAccessException("Cluster invocation denied for non-cluster ip '"+remoteHost+"'.");
            }
        }
        catch(UnknownHostException uhe) {
            throw (IllegalAccessException) new IllegalAccessException("Cluster invocation denied, unable to check ip.").initCause(uhe);
        }
        catch(FindException fe) {
            throw (IllegalAccessException) new IllegalAccessException("Cluster invocation denied, unable to check ip.").initCause(fe);
        }

        // Ensure invocation subject is set for node to node invocation.
        if(invocation instanceof AdminSessionRemoteInvocation) {
            // We could just use the info passed by the other node, but for now
            // we'll trash it to avoid any security risk.
            AdminSessionRemoteInvocation adminInvocation = (AdminSessionRemoteInvocation) invocation;
            Subject administrator = adminInvocation.getSubject();
            if(administrator!=null) {
                Set principals = administrator.getPrincipals();
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

    private final ClusterInfoManager clusterInfoManager;
}
