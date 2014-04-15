package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyTransformer;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class PolicyAPIResourceFactory extends WsmanBaseResourceFactory<PolicyMO, PolicyResourceFactory> {

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private RbacAccessService rbacAccessService;

    public PolicyAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.POLICY.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory factory) {
        super.factory = factory;
    }

    @Inject
    private PolicyVersionManager policyVersionManager;
    @Inject
    private PolicyManager policyManager;

    @Inject
    private PolicyTransformer policyTransformer;

    public String createResource(@NotNull final PolicyMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        return createResourceInternal(null, resource, comment);
    }

    public void createResource(@NotNull final String id, @NotNull final PolicyMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        createResourceInternal(id, resource, comment);
    }

    private String createResourceInternal(@Nullable final String id, @NotNull final PolicyMO resource, @Nullable final String comment) {
        validateCreateResource(id, resource);
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        String savedId = tt.execute( new TransactionCallback<String>(){
            @Override
            public String doInTransaction( final TransactionStatus transactionStatus ) {
                try {
                    final EntityContainer<Policy> newPolicyContainer = policyTransformer.convertFromMO(resource);
                    final Policy newPolicy = newPolicyContainer.getEntity();
                    rbacAccessService.validatePermitted(newPolicy, OperationType.CREATE);
                    newPolicy.setVersion(0);
                    //generate a new Guid if none is set.
                    if(newPolicy.getGuid() == null) {
                        newPolicy.setGuid(UUID.randomUUID().toString());
                    }

                    final String savedId;
                    if(id == null){
                        savedId = policyManager.save(newPolicy).toString();
                    } else {
                        policyManager.save(Goid.parseGoid(id),newPolicy);
                        savedId = id;
                    }
                    policyVersionManager.checkpointPolicy(newPolicy, true , comment, true);

                    return savedId;
                } catch(ResourceFactory.InvalidResourceException e){
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch (IllegalArgumentException e) {
                    transactionStatus.setRollbackOnly();
                    Exception ire =  new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, ExceptionUtils.getMessage(e));
                    throw new ResourceFactory.ResourceAccessException(ire);
                } catch ( ObjectModelException ome ) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to save policy version when updating policy.", ome);
                }
            }
        });
        resource.setId(savedId);
        return savedId;
    }

    private void validateCreateResource(@Nullable String id, PolicyMO resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when creating a new entity, or id must equal new entity id");
        }
    }

    public void updateResource(final @NotNull String id, final @NotNull PolicyMO resource, final String comment, final boolean active) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when updating a new entity, or id must equal entity id");
        }

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        tt.execute( new TransactionCallback(){
            @Override
            public Object doInTransaction( final TransactionStatus transactionStatus ) {
                try {
                    final EntityContainer<Policy> newPolicyContainer = policyTransformer.convertFromMO(resource);
                    final Policy newPolicy = newPolicyContainer.getEntity();
                    rbacAccessService.validatePermitted(newPolicy, OperationType.UPDATE);
                    policyManager.update(newPolicy);
                    policyVersionManager.checkpointPolicy(newPolicy, active, comment, false);

                    return policyTransformer.convertToMO(newPolicy);

                }catch(ResourceFactory.InvalidResourceException e){
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch ( ObjectModelException ome ) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to save policy version when updating policy.", ome);
                }
            }
        });
    }

    /**
     * This will find if a policy exists and if the currently authenticated user has the operationType access to this
     * policy. This will throw an exception if the policy doesn't exist or the user does not have access to the policy.
     *
     * @param id            The id of the policy.
     * @param operationType The operation type that the current user needs to have permissions for on this policy
     * @throws ResourceFactory.ResourceNotFoundException
     *                      This is thrown if the policy cannot be found
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InsufficientPermissionsException
     *                      This is thrown if the user does not have sufficient permissions to access the policy
     */
    public void validateExistsAndHasAccess(@NotNull final String id, @NotNull final OperationType operationType, @NotNull final Collection<PolicyType> policyTypes) throws ResourceFactory.ResourceNotFoundException {
        RestResourceFactoryUtils.transactional(transactionManager, true, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                try {
                    Policy policy = policyManager.findByPrimaryKey(Goid.parseGoid(id));
                    if (policy == null || !policyTypes.contains(policy.getType())) {
                        throw new ResourceFactory.ResourceNotFoundException("Could not find policy with id: " + id);
                    }
                    rbacAccessService.validatePermitted(policy, operationType);
                } catch (FindException e) {
                    throw new ResourceFactory.ResourceNotFoundException("Could not find policy with id: " + id, e);
                }
            }
        });
    }
}