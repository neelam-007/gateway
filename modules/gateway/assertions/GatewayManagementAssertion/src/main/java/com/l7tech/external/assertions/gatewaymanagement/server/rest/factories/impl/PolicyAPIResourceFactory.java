package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyTransformer;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyDetail;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class PolicyAPIResourceFactory extends WsmanBaseResourceFactory<PolicyMO, PolicyResourceFactory> {

    @Inject
    private PlatformTransactionManager transactionManager;

    public PolicyAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").put("parentFolder.id", "parentFolder.id").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("guid", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("guid", RestResourceFactoryUtils.stringConvert))
                        .put("type", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("type", RestResourceFactoryUtils.stringConvert))
                        .put("soap", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("soap", RestResourceFactoryUtils.booleanConvert))
                        .put("parentFolder.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("folder.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

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

    @Override
    public PolicyMO getResourceTemplate() {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setGuid("Policy Guid");

        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setFolderId("FolderID");
        policyDetail.setName("Policy Name");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("PropertyKey", "PropertyValue").map());

        policyMO.setPolicyDetail(policyDetail);
        return policyMO;
    }

    @Inject
    private PolicyVersionManager policyVersionManager;
    @Inject
    private PolicyManager policyManager;

    @Inject
    private PolicyTransformer policyTransformer;

    public String createResource(@NotNull final PolicyMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        validateCreateResource(null, resource);
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        return tt.execute( new TransactionCallback<String>(){
            @Override
            public String doInTransaction( final TransactionStatus transactionStatus ) {
                try {
                    Policy newPolicy = policyTransformer.convertFromMO(resource);
                    newPolicy.setVersion(0);

                    String id = policyManager.save(newPolicy).toString();
                    policyVersionManager.checkpointPolicy(newPolicy, true , comment, true);
                    resource.setId(id);
                    return id;

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

    public void createResource(@NotNull final String id, @NotNull final PolicyMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        validateCreateResource(id, resource);
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        tt.execute( new TransactionCallback(){
            @Override
            public String doInTransaction( final TransactionStatus transactionStatus ) {
                try {
                    Policy newPolicy = policyTransformer.convertFromMO(resource);
                    newPolicy.setVersion(0);

                    policyManager.save(Goid.parseGoid(id),newPolicy);
                    policyVersionManager.checkpointPolicy(newPolicy, true , comment, true);

                    return null;
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
        resource.setId(id);
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
                    Policy newPolicy = policyTransformer.convertFromMO(resource);
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
}