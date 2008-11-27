package com.l7tech.server.cluster;

import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.Config;
import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.identity.User;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import javax.jws.WebMethod;
import javax.jws.WebResult;

/**
 * Gateway API implementation.
 *
 * @author steve
 */
@Transactional(propagation=Propagation.REQUIRED)
public class GatewayApiImpl implements GatewayApi {

    //- PUBLIC

    public GatewayApiImpl( final Config config,
                           final ClusterInfoManager clusterInfoManager,
                           final SecurityFilter securityFilter,
                           final ServiceManager serviceManager,
                           final PolicyManager policyManager,
                           final FolderManager folderManager ) {
        this.config = config;
        this.clusterInfoManager = clusterInfoManager;
        this.securityFilter = securityFilter;
        this.serviceManager = serviceManager;
        this.policyManager = policyManager;
        this.folderManager = folderManager;
    }

    @Override
    public ClusterInfo getClusterInfo() {
        logger.fine("Processing request for cluster info.");
        
        ClusterInfo info = new ClusterInfo();
        info.setClusterHostname( config.getProperty("clusterHost", "") );
        info.setClusterHttpPort( config.getIntProperty("clusterhttpport", 8080) );
        info.setClusterHttpsPort( config.getIntProperty("clusterhttpsport", 8443) );
        
        return info;
    }

    @Override
    public Collection<GatewayInfo> getGatewayInfo() {
        logger.fine("Processing request for gateway info.");
        
        Set<GatewayInfo> gateways = new LinkedHashSet<GatewayInfo>();

        try {
            Collection<ClusterNodeInfo> clusterNodeInfos = clusterInfoManager.retrieveClusterStatus();
            if ( clusterNodeInfos != null ) {
                for( ClusterNodeInfo info : clusterNodeInfos ) {
                    GatewayInfo gatewayInfo = new GatewayInfo();
                    gatewayInfo.setId( info.getNodeIdentifier() );
                    gatewayInfo.setName( info.getName() );
                    gatewayInfo.setIpAddress( info.getAddress() );
                    gatewayInfo.setSoftwareVersion( BuildInfo.getProductVersion() );
                    gatewayInfo.setStatusTimestamp( info.getLastUpdateTimeStamp() );
                    gateways.add(gatewayInfo);
                }
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error accessing node status", fe );   
        }

        return gateways;
    }

    @Override
    @WebMethod(operationName = "GetEntityInfo")
    @WebResult(name = "EntityInfos", targetNamespace = "http://www.layer7tech.com/management/gateway")
    public Collection<EntityInfo> getEntityInfo( final Collection<EntityType> entityTypes ) throws GatewayException {
        Collection<EntityInfo> info = new ArrayList<EntityInfo>();
        User user = JaasUtils.getCurrentUser();
        if ( user == null ) {
            throw new GatewayException( "Authentication required." );            
        }

        try {
            for ( EntityType type : entityTypes ) {
                switch ( type ) {
                    case SERVICE:
                        findServiceEntities( info, user );
                        break;
                    case POLICY:
                        findPolicyEntities( info, user );
                        break;
                    case FOLDER:
                        findFolderEntities( info, user );
                        break;
                    default:
                        logger.warning("Unsupported entity type requested '"+type+"'.");
                        break;
                }
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error finding entities.", fe );
            throw new GatewayException( "Error finding entities." );
        }

        return info;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GatewayApiImpl.class.getName() );

    private final Config config;
    private final ClusterInfoManager clusterInfoManager;
    private final SecurityFilter securityFilter;
    private final ServiceManager serviceManager;
    private final PolicyManager policyManager;
    private final FolderManager folderManager;

    private void findServiceEntities( final Collection<EntityInfo> info, final User user ) throws FindException {
        Collection<ServiceHeader> serviceHeaders = securityFilter.filter( serviceManager.findAllHeaders(false), user, OperationType.READ, null );

        for ( ServiceHeader header : serviceHeaders ) {
            info.add( new EntityInfo( header.getType(), header.getStrId(), header.getDisplayName(), header.getFolderOid()==null ? null : Long.toString(header.getFolderOid()), 1 ) );
        }
    }

    private void findPolicyEntities( final Collection<EntityInfo> info, final User user ) throws FindException {
        Collection<PolicyHeader> policyHeaders = securityFilter.filter( policyManager.findHeadersByType( PolicyType.INCLUDE_FRAGMENT ), user, OperationType.READ, null );

        for ( PolicyHeader header : policyHeaders ) {
            info.add( new EntityInfo( header.getType(), header.getStrId(), header.getName(), header.getFolderOid()==null ? null : Long.toString(header.getFolderOid()), 1 ) );
        }
    }

    private void findFolderEntities( final Collection<EntityInfo> info, final User user ) throws FindException {
        Collection<FolderHeader> folderHeaders = securityFilter.filter( folderManager.findAllHeaders(), user, OperationType.READ, null );

        for ( FolderHeader header : folderHeaders ) {
            info.add( new EntityInfo( header.getType(), header.getStrId(), header.getName(), header.getParentFolderOid()==null ? null : Long.toString(header.getParentFolderOid()), 1 ) );
        }
    }
}
