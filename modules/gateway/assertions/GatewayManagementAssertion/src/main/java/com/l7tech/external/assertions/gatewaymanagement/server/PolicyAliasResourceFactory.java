package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.AliasManager;
import com.l7tech.util.Option;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

import static com.l7tech.util.Option.optional;

/**
 *
 */
@ResourceFactory.ResourceType(type = PolicyAliasMO.class)
public class PolicyAliasResourceFactory extends SecurityZoneableEntityManagerResourceFactory<PolicyAliasMO, PolicyAlias, AliasHeader<Policy>> {

    //- PUBLIC

    public PolicyAliasResourceFactory(final RbacServices services,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final AliasManager<PolicyAlias, Policy, PolicyHeader> policyAliasManager,
                                      final PolicyResourceFactory policyResourceFactory,
                                      final FolderResourceFactory folderResourceFactory,
                                      final SecurityZoneManager securityZoneManager) {
        super(false, false, services, securityFilter, transactionManager, policyAliasManager, securityZoneManager);
        this.policyAliasManager = policyAliasManager;
        this.policyResourceFactory = policyResourceFactory;
        this.folderResourceFactory = folderResourceFactory;
    }

    //- PROTECTED

    @Override
    public PolicyAliasMO asResource(final PolicyAlias policyAlias) {
        PolicyAliasMO policyAliasRes = ManagedObjectFactory.createPolicyAlias();

        policyAliasRes.setId(policyAlias.getId());
        policyAliasRes.setVersion(policyAlias.getVersion());
        policyAliasRes.setFolderId(getFolderId(policyAlias));
        policyAliasRes.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyAlias.getEntityGoid().toString()));

        // handle SecurityZone
        doSecurityZoneAsResource(policyAliasRes, policyAlias);

        return policyAliasRes;
    }

    @Override
    protected PolicyAlias fromResource(final Object resource) throws InvalidResourceException {
        if (!(resource instanceof PolicyAliasMO))
            throw new InvalidResourceException(ExceptionType.UNEXPECTED_TYPE, "expected policy alias");

        final PolicyAliasMO policyAliasResource = (PolicyAliasMO) resource;

        final Option<Folder> parentFolder = folderResourceFactory.getFolder(optional(policyAliasResource.getFolderId()));
        if (!parentFolder.isSome())
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Folder not found");
        folderResourceFactory.checkMovePermitted(null,parentFolder.some());

        final String policyId = policyAliasResource.getPolicyReference().getId();
        final Policy policy;
        try {
            policy = policyResourceFactory.selectEntity(Collections.singletonMap(IDENTITY_SELECTOR, policyId));
            if (isRootFolder(policy.getFolder()) ? isRootFolder(parentFolder.some()) : policy.getFolder().equals(parentFolder.some()))
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Cannot create alias in the same folder as original");
        } catch (NullPointerException | ResourceNotFoundException e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "invalid policy reference");
        }

        // policy alias referencing same policy cannot be in same folder
        try{
            PolicyAlias checkAlias = policyAliasManager.findAliasByEntityAndFolder(toInternalId(policyId), parentFolder.some().getGoid());
            if( checkAlias != null )
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES,"Alias of policy " + policyId + " already exists in folder " + parentFolder.some().getGoid());
        } catch (FindException |InvalidResourceSelectors e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Unable to check for existing alias");
        }
        final PolicyAlias policyAliasEntity = new PolicyAlias(policy, parentFolder.some());
        // handle SecurityZone
        doSecurityZoneFromResource(policyAliasResource, policyAliasEntity);

        return policyAliasEntity;
    }

    @Override
    protected void updateEntity(final PolicyAlias oldEntity, final PolicyAlias newEntity) throws InvalidResourceException {

        // Disallow changing the referenced policy
        if (!oldEntity.getEntityGoid().equals(newEntity.getEntityGoid())) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unable to change policy reference of an existing policy alias");
        }

        final Policy policy;
        try {
            policy = policyResourceFactory.selectEntity(Collections.singletonMap(IDENTITY_SELECTOR, newEntity.getEntityGoid().toString()));
            if (isRootFolder(policy.getFolder()) ? isRootFolder(newEntity.getFolder()) : policy.getFolder().equals(newEntity.getFolder()))
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Cannot create alias in the same folder as original");
        } catch (ResourceNotFoundException e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "invalid policy reference");
        }

        // policy alias referencing same policy cannot be in same folder
        try{
            PolicyAlias checkAlias = policyAliasManager.findAliasByEntityAndFolder(newEntity.getEntityGoid(), newEntity.getFolder().getGoid());
            if( checkAlias != null )
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES,"Alias of policy " + newEntity.getEntityGoid() + " already exists in folder " + newEntity.getFolder().getGoid());
        } catch (FindException e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Unable to check for existing alias");
        }

        // update the entity
        if (newEntity.getFolder() != null) {
            oldEntity.setFolder(folderResourceFactory.checkMovePermitted(oldEntity.getFolder(), newEntity.getFolder()));
        }
        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }


    //- PACKAGE


    //- PRIVATE

    private boolean isRootFolder(Folder folder) {
        return folder == null || folder.getId().equals(ROOT_FOLDER_OID) || folder.getId().equals(Folder.ROOT_FOLDER_ID.toString());
    }

    private static final String ROOT_FOLDER_OID = "-5002";

    private FolderResourceFactory folderResourceFactory;
    private AliasManager<PolicyAlias, Policy, PolicyHeader> policyAliasManager;
    private PolicyResourceFactory policyResourceFactory;

}
