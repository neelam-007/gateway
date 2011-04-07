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
import com.l7tech.server.ServerConfig;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Set;
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
public class AdminRemotingProvider implements RemotingProvider<Administrative>, SecureHttpInvokerServiceExporter.SecurityCallback, PropertyChangeListener {

    //- PUBLIC

    public final String FACILITY_ADMIN = "ADMIN";
    public final String FACILITY_CLUSTER = "CLUSTER";


    public AdminRemotingProvider( final LicenseManager licenseManager,
                                  final AdminSessionManager adminSessionManager,
                                  final ClusterInfoManager clusterInfoManager,
                                  final DefaultKey defaultKey,
                                  final ServerConfig serverConfig) {
        this.licenseManager = licenseManager;
        this.adminSessionManager = adminSessionManager;
        this.clusterInfoManager = clusterInfoManager;
        this.defaultKey = defaultKey;
        this.serverConfig = serverConfig;
        this.sessionExpiryInMils = serverConfig.getIntProperty(ServerConfig.PARAM_SESSION_EXPIRY, -1)* minuteToMils;
        if (this.sessionExpiryInMils <= 0) {
            this.sessionExpiryInMils = DEFAULT_GATEWAY_SESSION_EXPIRY;
        }
        sessionActivityMap = new ConcurrentHashMap<User,Long>();
    }

    public void checkPermitted( final Administrative adminAnno,
                                final String facility,
                                final String activity ) {
        if ( FACILITY_CLUSTER.equals(facility) ) {
            enforceClusterEnabled();
        } else if ( FACILITY_ADMIN.equals(facility) ) {
            if (adminAnno == null || adminAnno.licensed()) {
                enforceLicensed( activity );
            }

            enforceAdminEnabled( adminAnno == null || adminAnno.authenticated() );

            if (adminAnno == null || !adminAnno.background()) {
                enforceExpiry(activity);
            }

        } else {
            throw new IllegalArgumentException( "Unknown facility " + facility );
        }
    }

    /**
     * Assert that the user of this request has generated activity within the session expiry period
     */
    private void enforceExpiry(String activity) {
        User user = JaasUtils.getCurrentUser();
        if (user!=null){
            Long lastActivity = sessionActivityMap.get(user);
            if(lastActivity== null){
                sessionActivityMap.put(user,System.currentTimeMillis());
            }
            else{
                if(lastActivity<(System.currentTimeMillis() - sessionExpiryInMils)) {
                    //System.out.print("\nUser:"+user.getLogin() +" EXPIRE" + System.currentTimeMillis() + " Expiry:"+ sessionExpiryInMils);
                    sessionActivityMap.remove(user);
                    adminSessionManager.destroySession(user);
                    throw new AccessControlException("Gateway sesson expired");
                }
                sessionActivityMap.put(user,System.currentTimeMillis());
            }
            //System.out.print("\nUser:"+user.getLogin() +" Activity: "+activity + " time:"+ System.currentTimeMillis());
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        String newValue = (String) evt.getNewValue();

        if ( propertyName != null && propertyName.equals(ServerConfig.PARAM_SESSION_EXPIRY) ){
            try {
                this.sessionExpiryInMils = serverConfig.getIntProperty(ServerConfig.PARAM_SESSION_EXPIRY, -1) * minuteToMils;
                if (this.sessionExpiryInMils <= 0) {
                    this.sessionExpiryInMils = DEFAULT_GATEWAY_SESSION_EXPIRY;
                }
            } catch (NumberFormatException nfe) {
                sessionExpiryInMils = DEFAULT_GATEWAY_SESSION_EXPIRY;
                logger.warning("Parameter " + propertyName + " value '" + newValue + "' not a positive integer value. Reuse default value '" + sessionExpiryInMils/minuteToMils + "'");
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( AdminRemotingProvider.class.getName() );

    private final LicenseManager licenseManager;
    private final AdminSessionManager adminSessionManager;
    private final ClusterInfoManager clusterInfoManager;
    private final DefaultKey defaultKey;
    private final ServerConfig serverConfig;

    private final ConcurrentMap<User,Long> sessionActivityMap;
    private static final long minuteToMils = 60000;
    private static final long DEFAULT_GATEWAY_SESSION_EXPIRY = 30 * minuteToMils;
    private long sessionExpiryInMils;

    private void enforceLicensed( String action ) {
        try {
            licenseManager.requireFeature( GatewayFeatureSets.SERVICE_ADMIN );
        } catch ( LicenseException e) {
            logger.log( Level.WARNING, "License checking failed when invoking the method, " + action);
            throw new LicenseRuntimeException(e);
        }
    }

    /**
     * Assert that this request arrived over a port that enables either ADMIN_APPLET or ADMIN_REMOTE.
     */
    private void enforceAdminEnabled( boolean checkAuthenticated ) {
        HttpServletRequest hreq = RemoteUtils.getHttpServletRequest();
        if (hreq == null)
            throw new AccessControlException("Admin request disallowed: No request context available");

        SsgConnector connector = HttpTransportModule.getConnector(hreq);
        if (connector == null)
            throw new AccessControlException("Admin request disallowed: Unable to determine which connector this request came in on");

        if (!connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET) && !connector.offersEndpoint( SsgConnector.Endpoint.ADMIN_REMOTE))
            throw new AccessControlException("Request not permitted on this port");

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
            try {
                final Set<String> credentials = subject.getPublicCredentials(String.class);
                if ( credentials.isEmpty() ) {
                    throw new AccessControlException("Admin request disallowed: no credentials.");
                } else {
                    final String cookie = credentials.iterator().next();
                    final User user = adminSessionManager.resumeSession(cookie);
                    if( user != null ){
                        subject.getPrincipals().add(user);
                    } else {
                        throw new AccessControlException("Admin request disallowed: session not found.");                        
                    }
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
