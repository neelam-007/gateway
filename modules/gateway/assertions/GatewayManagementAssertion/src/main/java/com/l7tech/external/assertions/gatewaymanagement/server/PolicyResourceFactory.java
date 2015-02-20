package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.gateway.api.impl.PolicyValidationContext;
import com.l7tech.gateway.api.impl.VersionComment;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import com.l7tech.util.Eithers.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.*;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.*;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;

/**
 *
 */
@ResourceFactory.ResourceType(type=PolicyMO.class)
public class PolicyResourceFactory extends SecurityZoneableEntityManagerResourceFactory<PolicyMO, Policy, PolicyHeader> {

    //- PUBLIC

    public PolicyResourceFactory( final RbacServices services,
                                  final SecurityFilter securityFilter,
                                  final PlatformTransactionManager transactionManager,
                                  final PolicyManager policyManager,
                                  final PolicyHelper policyHelper,
                                  final FolderResourceFactory folderResourceFactory,
                                  final SecurityZoneManager securityZoneManager,
                                  final PolicyVersionManager policyVersionManager,
                                  final ClusterPropertyManager clusterPropertyManager,
                                  final ServiceManager serviceManager) {
        super( false, true, true, services, securityFilter, transactionManager, policyManager, securityZoneManager );
        this.policyManager = policyManager;
        this.policyHelper = policyHelper;
        this.folderResourceFactory = folderResourceFactory;
        this.policyVersionManager = policyVersionManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.serviceManager = serviceManager;
    }

    @ResourceMethod(name="ImportPolicy", selectors=true, resource=true)
    public PolicyImportResult importPolicy( final Map<String,String> selectorMap,
                                            final PolicyImportContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, PolicyImportResult>>() {
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, PolicyImportResult> execute() throws ObjectModelException {
                try {
                    final Policy policy = selectEntity( selectorMap );
                    policyHelper.checkPolicyAssertionAccess( policy );
                    checkPermitted(OperationType.UPDATE, null, policy);
                    PolicyImportResult result = policyHelper.importPolicy( policy, resource );
                    policyManager.update( policy );
                    return right2( result );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, false ) );
    }

    @ResourceMethod(name="ExportPolicy", selectors=true)
    public PolicyExportResult exportPolicy( final Map<String,String> selectorMap ) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,PolicyExportResult>>(){
            @Override
            public Either<ResourceNotFoundException,PolicyExportResult> execute() throws ObjectModelException {
                try {
                    final Policy policy = selectEntity( selectorMap );
                    checkPermitted( OperationType.READ, null, policy );
                    return right( policyHelper.exportPolicy( policy ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="ValidatePolicy", selectors=true, resource=true)
    public PolicyValidationResult validatePolicy( final Map<String,String> selectorMap,
                                                  final PolicyValidationContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, PolicyValidationResult>>(){
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, PolicyValidationResult> execute() throws ObjectModelException {
                try {
                    checkPermittedForSomeEntity( OperationType.READ, EntityType.POLICY );
                    return right2( policyHelper.validatePolicy( resource, new PolicyHelper.PolicyResolver() {
                        @Override
                        public Policy resolve() throws ResourceNotFoundException {
                            final Policy policy = selectEntity( selectorMap );
                            checkPermitted( OperationType.READ, null, policy );
                            return policy;
                        }

                        @Override
                        public Wsdl resolveWsdl() throws ResourceNotFoundException {
                            return null;
                        }

                        @Override
                        public SoapVersion resolveSoapVersion() throws ResourceNotFoundException {
                            return null;
                        }
                    } ) );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="SetVersionComment", selectors=true, resource=true)
    public void setVersionComment( final Map<String,String> selectorMap,
                                            final VersionComment resource ) throws ResourceNotFoundException, InvalidResourceException {
        Eithers.extract2(transactional(new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, String>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, String> execute() throws ObjectModelException {
                try {
                    final Policy policy = selectEntity(selectorMap);
                    policyHelper.checkPolicyAssertionAccess(policy);
                    checkPermitted(OperationType.READ, null, policy);
                    // update comment
                    PolicyVersion policyVersion = null;
                    if (resource.getVersionNumber() != null) {
                        policyVersion = policyVersionManager.findPolicyVersionForPolicy(policy.getGoid(), resource.getVersionNumber());
                    } else {
                        policyVersion = policyVersionManager.findActiveVersionForPolicy(policy.getGoid());
                    }
                    if (policyVersion == null) throw new InvalidResourceException(ExceptionType.INVALID_VALUES,"Version not found " + resource.getVersionNumber());
                    checkPermitted(OperationType.UPDATE, null, policyVersion);
                    policyVersion.setName(resource.getComment());
                    validate(policyVersion);
                    policyVersionManager.update(policyVersion);
                    return right2(policyVersion.getName());
                } catch (FindException e) {
                    return left2_1(new ResourceNotFoundException(ExceptionUtils.getMessage(e), e));
                } catch (ResourceNotFoundException e) {
                    return left2_1(e);
                } catch (InvalidResourceException e) {
                    return left2_2(e);
                }
            }
        }, false));
    }
    
    //- PROTECTED

    @Override
    public PolicyMO asResource( final Policy policy ) {
        final PolicyMO policyRes = ManagedObjectFactory.createPolicy();
        final PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();

        policyRes.setId( policy.getId() );
        policyRes.setGuid( policy.getGuid() );
        policyRes.setPolicyDetail( policyDetail );
        final List<ResourceSet> resourceSets = new ArrayList<ResourceSet>();
        policyRes.setResourceSets( resourceSets );
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSets.add( resourceSet );
        resourceSet.setTag( ResourceHelper.POLICY_TAG );
        final Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources( Collections.singletonList(resource) );
        resource.setType( ResourceHelper.POLICY_TYPE );
        try {
            // Recreate the policy xml instead of using the existing one so that all ID's will be proper and fully updated.
            // ID's could be incorrect in the stored xml in the case of a policy that was saves pre 8.0 (Goid update) SSG-8854
            resource.setContent( WspWriter.getPolicyXml(policy.getAssertion()) );
        } catch (IOException e) {
            throw new ResourceAccessException( "Could not retrieve policy xml", e );
        }

        policyDetail.setId(policy.getId());
        policyDetail.setGuid( policy.getGuid() );
        policyDetail.setVersion( policy.getVersion() );
        policyDetail.setFolderId( getFolderId( policy ) );
        policyDetail.setName( policy.getName() );
        switch ( policy.getType() ) {
            case INCLUDE_FRAGMENT:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
                break;
            case INTERNAL:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.INTERNAL );
                break;
            case GLOBAL_FRAGMENT:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.GLOBAL );
                break;
            case IDENTITY_PROVIDER_POLICY:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.ID_PROVIDER );
                break;
            case POLICY_BACKED_OPERATION:
                policyDetail.setPolicyType(PolicyDetail.PolicyType.SERVICE_OPERATION);
                break;
            default:
                throw new ResourceAccessException( "Access of unsupported policy type: " + policy.getType() );
        }
        policyDetail.setProperties( getProperties( policy, Policy.class ) );

        // handle SecurityZone
        doSecurityZoneAsResource( policyRes, policy );

        return policyRes;
    }

    @Override
    public Policy fromResource( final Object resource, boolean strict ) throws InvalidResourceException {
        if ( !(resource instanceof PolicyMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected policy");

        final PolicyMO policyMO = (PolicyMO) resource;
        final PolicyDetail policyDetail = policyMO.getPolicyDetail();
        final Map<String,ResourceSet> resourceSetMap = resourceHelper.getResourceSetMap( policyMO.getResourceSets() );
        final Resource policyResource = resourceHelper.getResource( resourceSetMap, ResourceHelper.POLICY_TAG, ResourceHelper.POLICY_TYPE, true, null );
        if ( policyDetail == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing details");
        }
        if ( policyDetail.getPolicyType() == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing details - policy type");
        }
        if ( resourceSetMap.size() > 1 ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unexpected resource sets " + resourceSetMap.keySet().remove( ResourceHelper.POLICY_TAG ));
        }
        Option<Folder> folder = folderResourceFactory.getFolder( optional( policyDetail.getFolderId() ) );
        if ( !folder.isSome() ){
            if(strict) {
                throw new InvalidResourceException( ExceptionType.INVALID_VALUES, "Folder not found");
            } else {
                Folder folderParent = new Folder();
                folderParent.setId(optional(policyDetail.getFolderId()).orSome(Folder.ROOT_FOLDER_ID.toString()));
                folder = some(folderParent);
            }
        }

        final String policyName = asName(policyDetail.getName());
        final PolicyType policyType;
        switch ( policyDetail.getPolicyType() ) {
            case INCLUDE:
                policyType = PolicyType.INCLUDE_FRAGMENT;
                break;
            case INTERNAL:
                policyType = PolicyType.INTERNAL;
                break;
            case GLOBAL:
                policyType = PolicyType.GLOBAL_FRAGMENT;
                break;
            case ID_PROVIDER:
                policyType = PolicyType.IDENTITY_PROVIDER_POLICY;
                break;
            case SERVICE_OPERATION:
                policyType = PolicyType.POLICY_BACKED_OPERATION;
                break;
            default:
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "unknown policy type" );
        }
        final String policyXml = policyHelper.validatePolicySyntax(policyResource.getContent()); 
        final Policy policy = new Policy( policyType, policyName, policyXml, false );
        policy.setFolder( folder.some() );
        setProperties( policy, policyDetail.getProperties(), Policy.class );

        // handle SecurityZone
        doSecurityZoneFromResource( policyMO, policy, strict );

        if(!policy.getType().isSecurityZoneable() && policy.getSecurityZone()!=null){
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Policy of type "+ policy.getType().getName()+ " is not security zoneable");
        }

        return policy;
    }

    @Override
    protected void beforeCreateEntity( final EntityBag<Policy> policyEntityBag ) throws ObjectModelException {
        UUID guid = UUID.randomUUID();
        policyEntityBag.getEntity().setGuid(guid.toString());
        try {
            policyHelper.checkPolicyAssertionAccess( policyEntityBag.getEntity() );
        } catch (InvalidResourceException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void afterCreateEntity( final EntityBag<Policy> entityBag, Goid identifier ) throws ObjectModelException {
        //if this is a debug-trace policy we need to also set the trace.policy.guid cluster property to the newly created guid of this policy (only if one isn't already set.)
        if(PolicyType.INTERNAL.equals(entityBag.getEntity().getType()) && "debug-trace".equals(entityBag.getEntity().getInternalTag()) && clusterPropertyManager.getProperty(ServerConfigParams.PARAM_TRACE_POLICY_GUID) == null){
            clusterPropertyManager.putProperty(ServerConfigParams.PARAM_TRACE_POLICY_GUID, entityBag.getEntity().getGuid());
        }
    }

    @Override
    protected void beforeDeleteEntity(final EntityBag<Policy> entityBag) throws ObjectModelException {
        //if this is a debug trace policy it can only be deleted if there are no other services that have debug trace enabled.
        if (isDebugTracePolicy(entityBag.getEntity()) && atLeastOneServiceHasTracingEnabled()) {
            throw new DeleteException("Cannot delete policy it is currently in use as the global debug trace policy");
        }
    }

    @Override
    protected void afterDeleteEntity(final EntityBag<Policy> entityBag) throws ObjectModelException {
        //if this is the debug trace policy need to clear the cluster property as well
        if (isDebugTracePolicy(entityBag.getEntity())) {
            final ClusterProperty traceProp = clusterPropertyManager.findByUniqueName(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
            if (traceProp == null)
                return;
            clusterPropertyManager.delete(traceProp);
        }
    }

    /**
     * Checks if the given policy is the debug trace policy
     *
     * @param policy The policy to check
     * @return true if this is the debug trace policy
     * @throws FindException
     */
    private boolean isDebugTracePolicy(@NotNull final Policy policy) throws FindException {
        if (PolicyType.INTERNAL.equals(policy.getType()) && "debug-trace".equals(policy.getInternalTag()) && clusterPropertyManager.getProperty(ServerConfigParams.PARAM_TRACE_POLICY_GUID) != null) {
            final String traceGuid = clusterPropertyManager.getProperty(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
            if (traceGuid != null && traceGuid.trim().equals(policy.getGuid())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if any services have trace enabled.
     *
     * @return returns true if any service has trace enabled.
     * @throws FindException
     */
    private boolean atLeastOneServiceHasTracingEnabled() throws FindException {
        final Collection<ServiceHeader> serviceHeaders = serviceManager.findAllHeaders();
        for (final ServiceHeader serviceHeader : serviceHeaders) {
            if (serviceHeader.isTracingEnabled())
                return true;
        }
        return false;
    }

    @Override
    protected void updateEntity( final Policy oldEntity, final Policy newEntity ) throws InvalidResourceException {
        oldEntity.setFolder( folderResourceFactory.checkMovePermitted( oldEntity.getFolder(), newEntity.getFolder() ) );
        oldEntity.setType( newEntity.getType() );
        oldEntity.setName( newEntity.getName() );
        oldEntity.setSoap( newEntity.isSoap() );
        oldEntity.setInternalTag( newEntity.getInternalTag() );
        oldEntity.setInternalSubTag( newEntity.getInternalSubTag() );
        oldEntity.setXml( newEntity.getXml() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
    }

    @Override
    protected List<PolicyHeader> filterHeaders( final List<PolicyHeader> headers ) {
        final List<PolicyHeader> filteredHeaders = new ArrayList<>(headers.size());

        for ( PolicyHeader policyHeader : headers ) {
            switch ( policyHeader.getPolicyType() ) {
                case INCLUDE_FRAGMENT:
                case INTERNAL:
                case GLOBAL_FRAGMENT:
                case IDENTITY_PROVIDER_POLICY:
                    filteredHeaders.add( policyHeader );
                    break;
            }
        }

        return filteredHeaders;
    }

    @Override
    protected Policy filterEntity( final Policy entity ) {
        Policy policy = null;

        switch ( entity.getType() ) {
            case INCLUDE_FRAGMENT:
            case INTERNAL:
            case GLOBAL_FRAGMENT:
            case IDENTITY_PROVIDER_POLICY:
                policy = entity;
                break;
        }

        return policy;
    }

    //- PRIVATE

    private final PolicyManager policyManager;
    private final PolicyHelper policyHelper;
    private final FolderResourceFactory folderResourceFactory;
    private final PolicyVersionManager policyVersionManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final ServiceManager serviceManager;
    private final ResourceHelper resourceHelper = new ResourceHelper();

}
