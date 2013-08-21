package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.service.ServiceExternalEntityHeaderEnhancer;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.Config;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                           final FolderManager folderManager,
                           final ServiceExternalEntityHeaderEnhancer serviceExternalEntityHeaderEnhancer ) {
        this.config = config;
        this.clusterInfoManager = clusterInfoManager;
        this.securityFilter = securityFilter;
        this.serviceManager = serviceManager;
        this.policyManager = policyManager;
        this.folderManager = folderManager;
        this.serviceExternalEntityHeaderEnhancer = serviceExternalEntityHeaderEnhancer;
    }

    @Override
    public ClusterInfo getClusterInfo() {
        logger.fine("Processing request for cluster info.");

        ClusterInfo info = new ClusterInfo();
        info.setClusterHostname( config.getProperty("clusterHost", "") );
        info.setClusterHttpPort( config.getIntProperty("clusterhttpport", 8080) );
        info.setClusterHttpsPort( config.getIntProperty("clusterhttpsport", 8443) );
        info.setAdminAppletPort(config.getIntProperty("clusterAdminAppletPort", 9443));

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
                    gatewayInfo.setIpAddress( info.getEsmAddress() );
                    gatewayInfo.setSoftwareVersion( BuildInfo.getProductVersion() );
                    gatewayInfo.setStatusTimestamp( info.getLastUpdateTimeStamp() );
                    gatewayInfo.setGatewayPort( config.getIntProperty("admin.esmPort", 8443) );
                    gatewayInfo.setProcessControllerPort( config.getIntProperty( ServerConfigParams.PARAM_PROCESS_CONTROLLER_EXTERNAL_PORT, 8765) );
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
    public Collection<EntityInfo> getEntityInfo( Collection<EntityType> entityTypes ) throws GatewayException {
        if (entityTypes == null) // null means "all supported types"
            entityTypes = Arrays.asList(EntityType.SERVICE, EntityType.SERVICE_ALIAS, EntityType.POLICY, EntityType.POLICY_ALIAS, EntityType.FOLDER);

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
                    case SERVICE_ALIAS: // The above case SERVICE has handled this case SERVICE_ALIAS.
                        break;
                    case POLICY:
                        findPolicyEntities( info, user );
                        break;
                    case POLICY_ALIAS: // The above case POLICY has handled this case POLICY_ALIAS.
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
    private final ServiceExternalEntityHeaderEnhancer serviceExternalEntityHeaderEnhancer;

    private void findServiceEntities( final Collection<EntityInfo> info, final User user ) throws FindException {
        Collection<ServiceHeader> serviceHeaders = securityFilter.filter( serviceManager.findAllHeaders(true), user, OperationType.READ, null );

        for ( final ServiceHeader header : serviceHeaders ) {
            ExternalEntityHeader external = EntityHeaderUtils.toExternal( header );
            EntityType entityType = header.isAlias()? EntityType.SERVICE_ALIAS : header.getType();
            String entityGoid = header.isAlias()? header.getAliasGoid().toString() : header.getStrId();
            String relatedOid = header.isAlias()? header.getStrId() : null;
            String entityName = header.getDisplayName() + (header.isAlias()? " alias" : "");

            EntityInfo entityInfo = new EntityInfo(entityType, external.getExternalId(), entityGoid, relatedOid, entityName, external.getDescription(), header.getFolderId()==null ? null : Goid.toString(header.getFolderId()), header.getVersion());

            entityInfo.setOperations( serviceExternalEntityHeaderEnhancer.getOperations( external ) );

            info.add(entityInfo);
        }
    }

    private void findPolicyEntities( final Collection<EntityInfo> info, final User user ) throws FindException {
        Collection<PolicyHeader> policyHeaders = securityFilter.filter(policyManager.findHeadersWithTypes(Collections.singleton(PolicyType.INCLUDE_FRAGMENT), true), user, OperationType.READ, null);

        for ( PolicyHeader header : policyHeaders ) {
            ExternalEntityHeader external = EntityHeaderUtils.toExternal(header);
            EntityType entityType = header.isAlias()? EntityType.POLICY_ALIAS : header.getType();
            String entityOid = header.isAlias()? header.getAliasGoid().toString() : header.getStrId();
            String relatedOid = header.isAlias()? header.getStrId() : null;
            String entityName = header.getName() + (header.isAlias()? " alias" : "");

            info.add( new EntityInfo( entityType, external.getExternalId(), entityOid, relatedOid, entityName, header.getDescription(), header.getFolderId()==null ? null : Goid.toString(header.getFolderId()), header.getVersion() ) );
        }
    }

    private void findFolderEntities( final Collection<EntityInfo> info, final User user ) throws FindException {
        Collection<FolderHeader> folderHeaders = securityFilter.filter( folderManager.findAllHeaders(), user, OperationType.READ, null );

        for ( FolderHeader header : folderHeaders ) {
            ExternalEntityHeader external = EntityHeaderUtils.toExternal(header);
            info.add( new EntityInfo( header.getType(), external.getExternalId(), header.getStrId(), null, header.getName(), header.getDescription(), header.getParentFolderGoid()==null ? null : Goid.toString(header.getParentFolderGoid()), null ) );
        }
    }
}
