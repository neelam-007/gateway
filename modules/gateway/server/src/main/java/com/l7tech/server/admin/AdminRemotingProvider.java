package com.l7tech.server.admin;

import com.l7tech.gateway.common.spring.remoting.RemotingProvider;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.spring.remoting.http.SecureHttpInvokerServiceExporter;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.gateway.common.admin.AdminSessionValidationRuntimeException;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.CertUtils;

import javax.servlet.http.HttpServletRequest;
import javax.security.auth.Subject;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.security.AccessControlException;
import java.security.cert.X509Certificate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.ServerNotActiveException;
import java.io.IOException;

/**
 * Server implementation of the RemotingProvider
 *
 * @author steve
 */
public class AdminRemotingProvider implements RemotingProvider<Administrative>, SecureHttpInvokerServiceExporter.SecurityCallback {

    //- PUBLIC

    public final String FACILITY_ADMIN = "ADMIN";
    public final String FACILITY_CLUSTER = "CLUSTER";


    public AdminRemotingProvider( final LicenseManager licenseManager,
                                  final AdminSessionManager adminSessionManager,
                                  final ClusterInfoManager clusterInfoManager,
                                  final DefaultKey defaultKey ) {
        this.licenseManager = licenseManager;
        this.adminSessionManager = adminSessionManager;
        this.clusterInfoManager = clusterInfoManager;
        this.defaultKey = defaultKey;
    }

    @Override
    public void checkPermitted( final Administrative adminAnno,
                                final String facility,
                                final String activity ) {
        if ( FACILITY_CLUSTER.equals( facility ) ) {
            enforceClusterEnabled();
        } else if ( FACILITY_ADMIN.equals(facility) ) {
            if (adminAnno == null || adminAnno.licensed()) {
                enforceLicensed( activity );
            }

            enforceAdminEnabled( adminAnno == null || adminAnno.authenticated() );

            enforceExpiry( adminAnno == null || !adminAnno.background() );

        } else {
            throw new IllegalArgumentException( "Unknown facility " + facility );
        }
    }

    /**
     * Assert that the user of this request has generated activity within the session expiry period
     */
    private void enforceExpiry( final boolean updateActivity ) {
        final User user = JaasUtils.getCurrentUser();
        final String sessionId = AdminLoginHelper.getSessionId();

        if ( user != null && adminSessionManager.isExpired( sessionId, updateActivity ) ) {
            throw new AccessControlException("Session expired");
        }
    }

    @Override
    public void checkSecured() throws IOException {
        try {
            ensureAuthenticated();
        } catch ( Exception e ) {
            throw new IOException( ExceptionUtils.getMessage(e), e );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( AdminRemotingProvider.class.getName() );

    private final LicenseManager licenseManager;
    private final AdminSessionManager adminSessionManager;
    private final ClusterInfoManager clusterInfoManager;
    private final DefaultKey defaultKey;

    private void enforceLicensed( String action ) {
        try {
            licenseManager.requireFeature( GatewayFeatureSets.SERVICE_ADMIN );
        } catch ( LicenseException e) {
            logger.log( Level.WARNING, "License checking failed when invoking the method, " + action);
            throw new LicenseRuntimeException(e);
        }
    }

    /**
     * Assert that this request arrived over a port that enables either ADMIN_APPLET or ADMIN_REMOTE_SSM.
     */
    private void enforceAdminEnabled( boolean checkAuthenticated ) {
        HttpServletRequest hreq = RemoteUtils.getHttpServletRequest();
        if (hreq == null)
            throw new AccessControlException("Admin request disallowed: No request context available");

        SsgConnector connector = HttpTransportModule.getConnector(hreq);
        if (connector == null)
            throw new AccessControlException("Admin request disallowed: Unable to determine which connector this request came in on");

        if (!connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET) && !connector.offersEndpoint( SsgConnector.Endpoint.ADMIN_REMOTE_SSM))
            throw new AccessControlException("Admin request not permitted on this port, " + connector.getPort());

        if ( checkAuthenticated ) {
            ensureAuthenticated();
        }
    }

    private void ensureAuthenticated() {
        // populate principal information if available
        final Subject subject = JaasUtils.getCurrentSubject();
        if (subject == null) {
            throw new AccessControlException("No subject passed, authentication failed.");
        }

        if ( subject.getPrincipals(User.class).isEmpty() ) {
            final String sessionId = AdminLoginHelper.getSessionId( subject );
            if ( sessionId == null ) {
                throw new AccessControlException("Admin request disallowed: no credentials.");
            }

            try {
                final User user = adminSessionManager.resumeSession( sessionId );
                if( user != null ){
                    subject.getPrincipals().add(user);
                } else {
                    throw new AccessControlException("Admin request disallowed: session not found.");
                }
            } catch (ObjectModelException fe) {
                logger.log(Level.WARNING, "Error resuming administrative user session '"+ ExceptionUtils.getMessage(fe)+"'.",fe);
            } catch (AuthenticationException ve) {
                throw new AdminSessionValidationRuntimeException("Permission denied.");
            }
        }
    }

    private void enforceClusterEnabled() {
        // If we don't have any info then just reject the invocation
        if( clusterInfoManager == null ) {
            throw new AccessControlException("Cluster invocations not permitted.");
        }

        try {
            // Verify invocation is by another node using cert auth and is permitted on the HTTP Connector.
            HttpServletRequest request = RemoteUtils.getHttpServletRequest();
            if ( request == null ) {
                throw new AccessControlException("Cluster request disallowed: No request context available");
            }

            SsgConnector connector = HttpTransportModule.getConnector(request);
            if (connector == null)
                throw new AccessControlException("Cluster request disallowed: Unable to determine which connector this request came in on");

            if (!connector.offersEndpoint(SsgConnector.Endpoint.NODE_COMMUNICATION))
                throw new AccessControlException("Request not permitted on this port");

            X509Certificate certificate = RemoteUtils.getClientCertificate();
            if ( certificate==null ||
                 !CertUtils.certsAreEqual(defaultKey.getSslInfo().getCertificate(),certificate) ) {
                throw new AccessControlException("Cluster request disallowed; missing or invalid credentials.");
            }

            InetAddress remoteHost = InetAddress.getByName( RemoteUtils.getClientHost() );
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
        catch( ServerNotActiveException snae) {
            throw (AccessControlException) new AccessControlException("Cluster invocation denied, unable to check ip.").initCause(snae);
        }
        catch( FindException fe ) {
            throw (AccessControlException) new AccessControlException("Cluster invocation denied, unable to check ip.").initCause(fe);
        }
        catch( IOException ioe ) {
            throw (AccessControlException) new AccessControlException("Cluster invocation denied, unable to check ip.").initCause(ioe);
        }
    }

}
