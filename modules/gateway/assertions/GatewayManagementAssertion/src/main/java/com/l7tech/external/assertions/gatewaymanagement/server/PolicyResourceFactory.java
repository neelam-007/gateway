package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.gateway.api.impl.PolicyValidationContext;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers.*;
import com.l7tech.util.Option;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static com.l7tech.util.Eithers.*;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Option.optional;

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
                                  final SecurityZoneManager securityZoneManager ) {
        super( false, true, true, services, securityFilter, transactionManager, policyManager, securityZoneManager );
        this.policyManager = policyManager;
        this.policyHelper = policyHelper;
        this.folderResourceFactory = folderResourceFactory;
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
    
    //- PROTECTED

    @Override
    protected PolicyMO asResource( final Policy policy ) {
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
        resource.setContent( policy.getXml() );

        policyDetail.setId( policy.getId() );
        policyDetail.setGuid( policy.getGuid() );
        policyDetail.setVersion( policy.getVersion() );
        policyDetail.setFolderId( getFolderId( policy ) );
        policyDetail.setName( policy.getName() );
        switch ( policy.getType() ) {
            case PRIVATE_SERVICE:
            case SHARED_SERVICE:
            case PRE_ROUTING_FRAGMENT:
            case SUCCESSFUL_ROUTING_FRAGMENT:
            case FAILED_ROUTING_FRAGMENT:
            case AUTHENTICATION_SUCCESS_FRAGMENT:
            case AUTHENTICATION_FAILURE_FRAGMENT:
            case AUTHORIZATION_SUCCESS_FRAGMENT:
            case AUTHORIZATION_FAILURE_FRAGMENT:
                throw new ResourceAccessException( "Access of unsupported policy type." );
            case INCLUDE_FRAGMENT:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
                break;
            case INTERNAL:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.INTERNAL );
                break;
            case GLOBAL_FRAGMENT:
                policyDetail.setPolicyType( PolicyDetail.PolicyType.GLOBAL );
                break;
        }
        policyDetail.setProperties( getProperties( policy, Policy.class ) );

        // handle SecurityZone
        doSecurityZoneAsResource( policyRes, policy );

        return policyRes;
    }

    @Override
    protected Policy fromResource( final Object resource ) throws InvalidResourceException {
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
        final Option<Folder> folder = folderResourceFactory.getFolder( optional( policyDetail.getFolderId() ) );
        if ( !folder.isSome() )
            throw new InvalidResourceException( ExceptionType.INVALID_VALUES, "Folder not found");

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
            default:
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "unknown policy type" );
        }
        final String policyXml = policyHelper.validatePolicySyntax(policyResource.getContent()); 
        final Policy policy = new Policy( policyType, policyName, policyXml, false );
        policy.setFolder( folder.some() );
        setProperties( policy, policyDetail.getProperties(), Policy.class );

        // handle SecurityZone
        doSecurityZoneFromResource( policyMO, policy );

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
    protected void updateEntity( final Policy oldEntity, final Policy newEntity ) throws InvalidResourceException {
        oldEntity.setFolder( folderResourceFactory.checkMovePermitted( oldEntity.getFolder(), newEntity.getFolder() ) );
        oldEntity.setType( newEntity.getType() );
        oldEntity.setName( newEntity.getName() );
        oldEntity.setSoap( newEntity.isSoap() );
        oldEntity.setInternalTag( newEntity.getInternalTag() );
        oldEntity.setXml( newEntity.getXml() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
    }

    @Override
    protected Collection<PolicyHeader> filterHeaders( final Collection<PolicyHeader> headers ) {
        final Collection<PolicyHeader> filteredHeaders = new ArrayList<PolicyHeader>(headers.size());

        for ( PolicyHeader policyHeader : headers ) {
            switch ( policyHeader.getPolicyType() ) {
                case INCLUDE_FRAGMENT:
                case INTERNAL:
                case GLOBAL_FRAGMENT:
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
                policy = entity;
                break;
        }

        return policy;
    }

    //- PRIVATE

    private final PolicyManager policyManager;
    private final PolicyHelper policyHelper;
    private final FolderResourceFactory folderResourceFactory;
    private final ResourceHelper resourceHelper = new ResourceHelper();

}
