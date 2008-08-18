package com.l7tech.server.admin;

import com.l7tech.gateway.common.spring.remoting.RemotingProvider;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.spring.remoting.rmi.ssl.SslRMIServerSocketFactory;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.ValidationException;
import com.l7tech.util.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.security.auth.Subject;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Set;
import java.security.AccessControlException;
import java.security.Principal;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Server implementation of the RemotingProvider
 *
 * @author steve
 */
public class AdminRemotingProvider implements RemotingProvider {

    //- PUBLIC

    public AdminRemotingProvider( final LicenseManager licenseManager,
                                  final AdminSessionManager adminSessionManager,
                                  final ClusterInfoManager clusterInfoManager ) {
        this.licenseManager = licenseManager;
        this.adminSessionManager = adminSessionManager;
        this.clusterInfoManager = clusterInfoManager;
    }

    public void enforceLicensed( final String className, final String methodName ) {
        try {
            licenseManager.requireFeature( GatewayFeatureSets.SERVICE_ADMIN);
        } catch ( LicenseException e) {
            logger.log( Level.WARNING, "License checking failed when invoking the method, " + methodName);
            throw new LicenseRuntimeException(e);
        }
    }

    /**
     * Assert that this request arrived over a port that enables either ADMIN_APPLET or ADMIN_REMOTE.
     */
    public void enforceAdminEnabled() {
        HttpServletRequest hreq = RemoteUtils.getHttpServletRequest();
        if (hreq == null)
            throw new AccessControlException("Admin request disallowed: No request context available");

        SsgConnector connector = HttpTransportModule.getConnector(hreq);
        if (connector == null)
            throw new AccessControlException("Admin request disallowed: Unable to determine which connector this request came in on");

        if (!connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET) && !connector.offersEndpoint( SsgConnector.Endpoint.ADMIN_REMOTE))
            throw new AccessControlException("Request not permitted on this port");

        // populate principal information
        Subject subject = JaasUtils.getCurrentSubject();
        if(subject != null){
            try {
                String cookie = subject.getPublicCredentials(String.class).iterator().next();
                Set<Principal> principals = adminSessionManager.getPrincipalsAndResumeSession(cookie);
                if(principals != null){
                    subject.getPrincipals().addAll(principals);
                }
            } catch (ValidationException ve) {
                logger.log(Level.INFO, "Validation failed for administrative user session '"+ExceptionUtils.getMessage(ve)+"'.", ExceptionUtils.getDebugException(ve));
            }
        }
    }

    public void enforceClusterEnabled() {
        // If we don't have any info then just reject the invocation
        if( clusterInfoManager == null ) {
            throw new AccessControlException("Cluster invocations not permitted.");
        }

        // Verify invocation is by another node.
        SslRMIServerSocketFactory.Context context = SslRMIServerSocketFactory.getContext();

        if( context == null || !context.isRemoteClientCertAuthenticated() ) {
            throw new AccessControlException("Client certificate authentication required.");
        }

        try {
            InetAddress remoteHost = InetAddress.getByName( context.getRemoteHost() );
            boolean isClusterNode = false;
            Collection clusterNodes = clusterInfoManager.retrieveClusterStatus();
            for( Object clusterNode : clusterNodes ) {
                ClusterNodeInfo nodeInfo = (ClusterNodeInfo) clusterNode;
                String nodeAddress = nodeInfo.getAddress();
                InetAddress clusterAddress = InetAddress.getByName( nodeAddress );
                if( clusterAddress.equals( remoteHost ) ) {
                    isClusterNode = true;
                }
            }

            if( !isClusterNode ) {
                throw new AccessControlException("Cluster invocation denied for non-cluster ip '"+remoteHost+"'.");
            }
        }
        catch( UnknownHostException uhe) {
            throw (AccessControlException) new AccessControlException("Cluster invocation denied, unable to check ip.").initCause(uhe);
        }
        catch( FindException fe ) {
            throw (AccessControlException) new AccessControlException("Cluster invocation denied, unable to check ip.").initCause(fe);
        }
    }

    /*
    * client uses this method instead of getPrincipalforCookie when the only purpose
    * of calling getPrincipalForCookie is to add the returned Principal to a Subject
    * setPrincipalsForSubject will add the user Principal aswell as any other Principals defined
    * */
    public void setPrincipalsForSubject(String cookie, Subject subject) {
        subject.getPublicCredentials().add(cookie);
    }

    public Principal getPrincipalForCookie( String cookie ) {
        return adminSessionManager.resumeSession( cookie );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( AdminRemotingProvider.class.getName() );

    private final LicenseManager licenseManager;
    private final AdminSessionManager adminSessionManager;
    private final ClusterInfoManager clusterInfoManager;
}
